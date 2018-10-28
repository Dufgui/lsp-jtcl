package com.mds.lsp.tcl;


import com.mds.lsp.tcl.diagnostic.TclFileObject;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import tcl.lang.Interp;
import tcl.lang.Parser;
import tcl.lang.TclException;
import tcl.lang.TclParse;
import tcl.lang.cmd.EncodingCmd;

import javax.tools.DiagnosticCollector;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TclParserAndInterpHolder {
    private final Interp interp = new Interp();
    private final Parser parser = new Parser();
    private final CompletableFuture<LanguageClient> client;

    public TclParserAndInterpHolder(CompletableFuture<LanguageClient> client) {
        this.client = client;
    }

    public static TclParserAndInterpHolder create(CompletableFuture<LanguageClient> client) {
        return new TclParserAndInterpHolder(client);
    }

    public DiagnosticCollector<TclFileObject> parseBatch(Map<URI,Optional<String>> content) {
        throw new UnsupportedOperationException();
    }

    public List<TclParse> parse(URI source) {
        Optional<TclFileObject> file = getRegularFile(source);
        return file.map(f -> {
            try {
                String script = f.getCharContent(false).toString();
                return Parser.parseCommand(script);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Collections.<TclParse>emptyList();
        }).orElse(Collections.emptyList());
    }

    public FocusedResult compileFocused(URI uri, Optional<String> content, int line, int character, boolean b) {
        content.map(s -> Parser.parseCommand(s)).orElse(Collections.emptyList()).stream();
        FocusedResult result = new FocusedResult();
        return result;
    }

    private Optional<TclFileObject> findFile(URI file, Optional<String> text) {
        return text.map(content -> Optional.<TclFileObject>of(new StringFileObject(content, file)))
                .orElse(getRegularFile(file));
    }

    private Optional<TclFileObject> getRegularFile(URI uri) {
        File source = Paths.get(uri).toFile();
        String sourceContent = null;
        try {
            sourceContent = interp.readScriptFromFile(source.getPath(), EncodingCmd.systemJavaEncoding);
        } catch (TclException e) {
            LanguageClient clientNow = client.getNow(null);
            if(clientNow != null) {
                clientNow.logMessage(new MessageParams(MessageType.Error, "Unable to parse " + source + " caused by : " + e.getMessage()));
            }
        }
        if(sourceContent == null) {
            return Optional.empty();
        }
        return Optional.of(new StringFileObject(sourceContent, uri));
    }

    public void addEvalFlag(int evalFlag) {
        interp.evalFlags |= evalFlag;
    }

    public TclParse parseSubCmd(char[] scriptArray, int scriptIndex, int numChars, String fileName, int lineNum) {
        return Parser.parseCommand(interp, scriptArray, scriptIndex, numChars, fileName, lineNum, true);
    }
}
