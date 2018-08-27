package com.mds.lsp.tcl;

import com.google.common.collect.ImmutableList;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class TclLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger LOG = Logger.getLogger("main");
    int maxItems = 50;
    private final CompletableFuture<LanguageClient> client = new CompletableFuture<>();
    private final TclTextDocumentService textDocuments = new TclTextDocumentService(client, this);
    private final TclWorkspaceService workspace =
            new TclWorkspaceService(client, this, textDocuments);
    private Path workspaceRoot = Paths.get(".");

    private Configured cacheConfigured;
    private TclSettings cacheSettings;
    private Path cacheWorkspaceRoot;
    private Instant cacheInferConfig = Instant.EPOCH;
    private Set<Path> cacheSourcePath = Collections.emptySet();

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        if(params.getRootUri() != null) {
            URI uri = null;
            try {
                uri = new URI(params.getRootUri());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (uri != null) {
                workspaceRoot = Paths.get(uri).toAbsolutePath().normalize();
            }
        }

        InitializeResult result = new InitializeResult();
        ServerCapabilities c = new ServerCapabilities();

        c.setTextDocumentSync(TextDocumentSyncKind.Incremental);
        c.setDefinitionProvider(true);
        c.setCompletionProvider(new CompletionOptions(true, ImmutableList.of(".")));
        c.setHoverProvider(true);
        c.setWorkspaceSymbolProvider(true);
        c.setReferencesProvider(true);
        c.setDocumentSymbolProvider(true);
        c.setCodeActionProvider(false);
        c.setSignatureHelpProvider(new SignatureHelpOptions(ImmutableList.of("(", ",")));

        result.setCapabilities(c);

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        try {
            client.get().logMessage(new MessageParams(MessageType.Error,"shutdown"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        try {
            client.get().logMessage(new MessageParams(MessageType.Error,"exit"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocuments;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspace;
    }

    /**
     * Configured java interpreter + indices based on workspace settings and inferred source / class
     * paths
     */
    Configured configured() {
        if(workspaceRoot != null) {
            Instant inferConfig = InferConfig.buildFilesModified(workspaceRoot);

            if (cacheConfigured == null
                    || !Objects.equals(workspace.settings(), cacheSettings)
                    || !Objects.equals(workspaceRoot, cacheWorkspaceRoot)
                    || cacheInferConfig.isBefore(inferConfig)
                    || !cacheConfigured.index.sourcePath().equals(cacheSourcePath)) {
                cacheConfigured = createCompiler(workspace.settings(), workspaceRoot);
                cacheSettings = workspace.settings();
                cacheWorkspaceRoot = workspaceRoot;
                cacheInferConfig = inferConfig;
                cacheSourcePath = cacheConfigured.index.sourcePath();

                clearDiagnostics();
            }
        }
        return cacheConfigured;
    }

    private Configured createCompiler(TclSettings settings, Path workspaceRoot) {

        TclParserAndInterpHolder compiler =
                TclParserAndInterpHolder.create(client);

        SymbolIndex symbolIndex = new SymbolIndex(workspaceRoot, compiler, textDocuments::openFiles, textDocuments::activeContent);
        FindSymbols findSymbols = new FindSymbols(symbolIndex, compiler, textDocuments::activeContent);
        return new Configured(compiler, symbolIndex, findSymbols);
    }

    private void clearDiagnostics() {
        InferConfig.allTclFiles(workspaceRoot).forEach(this::clearFileDiagnostics);
    }

    private void clearFileDiagnostics(Path file) {
        client.thenAccept(
                c ->
                        c.publishDiagnostics(
                                new PublishDiagnosticsParams(
                                        file.toUri().toString(), new ArrayList<>())));
    }

    @Override
    public void connect(LanguageClient client) {
        this.client.complete(client);
    }
}
