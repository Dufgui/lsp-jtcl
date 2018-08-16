package com.mds.lsp.tcl;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class TclWorkspaceService implements WorkspaceService {
    private static final Logger LOG = Logger.getLogger("main");

    private final CompletableFuture<LanguageClient> client;
    private final TclLanguageServer server;
    private final TclTextDocumentService textDocuments;
    private TclSettings settings = new TclSettings();

    TclWorkspaceService(
            CompletableFuture<LanguageClient> client,
            TclLanguageServer server,
            TclTextDocumentService textDocuments) {
        this.client = client;
        this.server = server;
        this.textDocuments = textDocuments;
    }

    TclSettings settings() {
        return settings;
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
        List<SymbolInformation> infos =
                server.configured()
                        .index
                        .search(params.getQuery())
                        .limit(server.maxItems)
                        .collect(Collectors.toList());

        return CompletableFuture.completedFuture(infos);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams change) {
        settings = Main.JSON.convertValue(change.getSettings(), TclSettings.class);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        for (FileEvent change : params.getChanges()) {
            Path path = Paths.get(URI.create(change.getUri()));

            switch (change.getType()) {
                //TODO optimize this
                case Changed:
                    textDocuments.doLint(textDocuments.openFiles());
                case Deleted:
                    textDocuments.doLint(textDocuments.openFiles());
                case Created:
                    textDocuments.doLint(textDocuments.openFiles());
                default:
                    // Nothing to do
            }


        }
    }
}
