package com.mds.lsp.tcl;

import com.google.common.collect.ImmutableList;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class TclLanguageServer implements LanguageServer {

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
        workspaceRoot = Paths.get(params.getRootPath()).toAbsolutePath().normalize();

        InitializeResult result = new InitializeResult();
        ServerCapabilities c = new ServerCapabilities();

        c.setTextDocumentSync(TextDocumentSyncKind.Incremental);
        c.setDefinitionProvider(false);
        c.setCompletionProvider(new CompletionOptions(true, ImmutableList.of(".")));
        c.setHoverProvider(false);
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
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {

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

        return cacheConfigured;
    }

    private Configured createCompiler(TclSettings settings, Path workspaceRoot) {

        TclParserHolder compiler =
                TclParserHolder.create();

        SymbolIndex symbolIndex = new SymbolIndex(workspaceRoot, textDocuments::openFiles, textDocuments::activeContent);
        return new Configured(compiler, symbolIndex);
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
}
