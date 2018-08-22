'use strict';
// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as VSCode from "vscode";
import * as Path from "path";
import * as FS from "fs";
import * as ChildProcess from "child_process";
import {LanguageClient, LanguageClientOptions, ServerOptions} from "vscode-languageclient";

/** Called when extension is activated */
export function activate(context: VSCode.ExtensionContext) {
    console.log('Activating JTcl');

    let socketPort = getPort();
    if (socketPort) {
        let clientOptions = getClientOptions();

        //TODO replace by real remote exec
        let javaExecutablePath = findJavaExecutable('java');
        let serverOptions = getRemoteServerOptions(context.extensionPath, javaExecutablePath);

        setLanguageConfiguration();

        // Create the language client and start the client.
        let languageClient = new LanguageClient('tcl', 'Tcl Language Server', serverOptions, clientOptions);
        let disposable = languageClient.start();

        // Push the disposable to the context's subscriptions so that the
        // client can be deactivated on extension deactivation
        context.subscriptions.push(disposable);
    } else {
        let javaExecutablePath = findJavaExecutable('java');

        if (javaExecutablePath == null) {
            VSCode.window.showErrorMessage("Couldn't locate java in $JAVA_HOME or $PATH");
            return;
        }

        isJava8(javaExecutablePath).then(eight => {
            if (!eight) {
                VSCode.window.showErrorMessage('Java language support requires Java 8 (using ' + javaExecutablePath + ')');

                return;
            }

            let clientOptions = getClientOptions();
            let serverOptions = getLocalServerOptions(context.extensionPath, javaExecutablePath);

            setLanguageConfiguration();


            // Create the language client and start the client.
            let languageClient = new LanguageClient('tcl', 'Tcl Language Server', serverOptions, clientOptions, true);
            let disposable = languageClient.start();

            // Push the disposable to the context's subscriptions so that the
            // client can be deactivated on extension deactivation
            context.subscriptions.push(disposable);
        });
    }
}

function isJava8(javaExecutablePath: string): Promise<boolean> {
    return new Promise((resolve, reject) => {
        ChildProcess.execFile(javaExecutablePath, ['-version'], { }, (error, stdout, stderr) => {
            let eight = stderr.indexOf('1.8') >= 0, nine = stderr.indexOf('"9"') >= 0;

            resolve(eight || nine);
        });
    });
}

function getLocalServerOptions(extensionPath , javaExecutablePath) {
    let fatJar = Path.resolve(extensionPath, "out", "fat-jar.jar");

    let args = [
        '-cp', fatJar,
        '-Xverify:none', // helps VisualVM avoid 'error 62'
        '-agentlib:jdwp=transport=dt_socket,server=y,address=8910,suspend=y,quiet=y',
        'com.mds.lsp.tcl.Main'
    ];

    // Start the child java process
    let serverOptions: ServerOptions = {
        command: javaExecutablePath,
        args: args,
        options: {cwd: VSCode.workspace.rootPath}
    };

    console.log(javaExecutablePath + ' ' + args.join(' '));
    return serverOptions;
}

function getRemoteServerOptions(extensionPath , javaExecutablePath) {
    //TODO to implement remote socket connection
    return getLocalServerOptions(extensionPath , javaExecutablePath);
}

function setLanguageConfiguration() {
    // Copied from typescript
    VSCode.languages.setLanguageConfiguration('tcl', {
        indentationRules: {
            // ^(.*\*/)?\s*\}.*$
            decreaseIndentPattern: /^((?!.*?\/\*).*\*\/)?\s*[\}\]\)].*$/,
            // ^.*\{[^}"']*$
            increaseIndentPattern: /^((?!\/\/).)*(\{[^}"'`]*|\([^)"'`]*|\[[^\]"'`]*)$/,
            indentNextLinePattern: /^\s*(for|while|if|else)\b(?!.*[;{}]\s*(\/\/.*|\/[*].*[*]\/\s*)?$)/
        },
        onEnterRules: [
            {
                // e.g. /** | */
                beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
                afterText: /^\s*\*\/$/,
                action: {indentAction: VSCode.IndentAction.IndentOutdent, appendText: ' * '}
            }, {
                // e.g. /** ...|
                beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
                action: {indentAction: VSCode.IndentAction.None, appendText: ' * '}
            }, {
                // e.g.  * ...|
                beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
                action: {indentAction: VSCode.IndentAction.None, appendText: '* '}
            }, {
                // e.g.  */|
                beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
                action: {indentAction: VSCode.IndentAction.None, removeText: 1}
            },
            {
                // e.g.  *-----*/|
                beforeText: /^(\t|(\ \ ))*\ \*[^/]*\*\/\s*$/,
                action: {indentAction: VSCode.IndentAction.None, removeText: 1}
            }
        ]
    });
}

function getClientOptions() {
// Options to control the language client
    let clientOptions: LanguageClientOptions = {
        // Register the server for java documents
        documentSelector: ['tcl'],
        synchronize: {
            // Synchronize the setting section 'java' to the server
            // NOTE: this currently doesn't do anything
            configurationSection: 'tcl',
            // Notify the server about file changes to 'javaconfig.json' files contain in the workspace
            fileEvents: [
                VSCode.workspace.createFileSystemWatcher('**/tclconfig.json'),
                VSCode.workspace.createFileSystemWatcher('**/*.tcl')
            ]
        },
        outputChannelName: 'Tcl',
        revealOutputChannelOn: 4 // never
    };
    return clientOptions;
}

function getPort(): number | undefined {
    let arg = process.argv.filter(arg => arg.startsWith('--REMOTE_LSP='))[0]
    if (!arg) {
        return undefined
    } else {
        return Number.parseInt(arg.substring('--REMOTE_LSP='.length), 10)
    }
}

function findJavaExecutable(binname: string) {
	binname = correctBinname(binname);

	// First search java.home setting
    let userJavaHome = VSCode.workspace.getConfiguration('java').get('home') as string;

	if (userJavaHome != null) {
        console.log('Looking for java in settings java.home ' + userJavaHome + '...');

        let candidate = findJavaExecutableInJavaHome(userJavaHome, binname);

        if (candidate != null)
            return candidate;
	}

	// Then search each JAVA_HOME
    let envJavaHome = process.env['JAVA_HOME'];

	if (envJavaHome) {
        console.log('Looking for java in environment variable JAVA_HOME ' + envJavaHome + '...');

        let candidate = findJavaExecutableInJavaHome(envJavaHome, binname);

        if (candidate != null)
            return candidate;
	}

	// Then search PATH parts
	if (process.env['PATH']) {
        console.log('Looking for java in PATH');

		let pathparts = process.env['PATH'].split(Path.delimiter);
		for (let i = 0; i < pathparts.length; i++) {
			let binpath = Path.join(pathparts[i], binname);
			if (FS.existsSync(binpath)) {
				return binpath;
			}
		}
	}

	// Else return the binary name directly (this will likely always fail downstream)
	return null;
}

function correctBinname(binname: string) {
	if (process.platform === 'win32')
		return binname + '.exe';
	else
		return binname;
}

function findJavaExecutableInJavaHome(javaHome: string, binname: string) {
    let workspaces = javaHome.split(Path.delimiter);

    for (let i = 0; i < workspaces.length; i++) {
        let binpath = Path.join(workspaces[i], 'bin', binname);

        if (FS.existsSync(binpath))
            return binpath;
    }
}

// this method is called when your extension is deactivated
export function deactivate() {
}