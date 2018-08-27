package com.mds.lsp.tcl;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mds.lsp.tcl.diagnostic.Lints;
import com.mds.lsp.tcl.diagnostic.TclDiagnostic;
import com.mds.lsp.tcl.diagnostic.TclFileObject;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class TclTextDocumentService implements TextDocumentService {

    private static final Logger LOG = Logger.getLogger("main");

    private final CompletableFuture<LanguageClient> client;
    private final TclLanguageServer server;
    private final Map<URI, VersionedContent> activeDocuments = new HashMap<>();

    TclTextDocumentService(CompletableFuture<LanguageClient> client, TclLanguageServer server) {
        this.client = client;
        this.server = server;
    }

    /** Text of file, if it is in the active set */
    Optional<String> activeContent(URI file) {
        return Optional.ofNullable(activeDocuments.get(file)).map(doc -> doc.content);
    }

    /** All open files, not including things like old git-versions in a diff view */
    Set<URI> openFiles() {
        return Sets.filter(activeDocuments.keySet(), uri -> uri.getScheme().equals("file"));
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        Instant started = Instant.now();
        URI uri = URI.create(params.getTextDocument().getUri());
        Optional<String> content = activeContent(uri);
        int line = params.getPosition().getLine() + 1;
        int character = params.getPosition().getCharacter() + 1;

        LOG.info(String.format("completion at %s %d:%d", uri, line, character));

        Configured config = server.configured();
        FocusedResult result = config.interpreter.compileFocused(uri, content, line, character, true);
        List<CompletionItem> items =
                Completions.at(result, config.index)
                        .limit(server.maxItems)
                        .collect(Collectors.toList());
        CompletionList list = new CompletionList(items.size() == server.maxItems, items);
        Duration elapsed = Duration.between(started, Instant.now());

        if (list.isIncomplete())
            LOG.info(
                    String.format(
                            "Found %d items (incomplete) in %d ms",
                            items.size(), elapsed.toMillis()));
        else LOG.info(String.format("Found %d items in %d ms", items.size(), elapsed.toMillis()));

        return CompletableFuture.completedFuture(Either.forRight(list));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem completionItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams params) {
        URI uri = URI.create(params.getTextDocument().getUri());
        Optional<String> content = activeContent(uri);
        int line = params.getPosition().getLine() + 1;
        int character = params.getPosition().getCharacter() + 1;

        LOG.info(String.format("definition at %s %d:%d", uri, line, character));

        Configured config = server.configured();
        FocusedResult result = config.interpreter.compileFocused(uri, content, line, character, false);
        List<Location> locations =
                References.gotoDefinition(result, config.find)
                        .map(Collections::singletonList)
                        .orElseGet(Collections::emptyList);
        return CompletableFuture.completedFuture(locations);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        URI uri = URI.create(params.getTextDocument().getUri());
        Optional<String> content = activeContent(uri);
        int line = params.getPosition().getLine() + 1;
        int character = params.getPosition().getCharacter() + 1;

        LOG.info(String.format("references at %s %d:%d", uri, line, character));

        Configured config = server.configured();
        FocusedResult result = config.interpreter.compileFocused(uri, content, line, character, false);
        List<Location> locations =
                References.findReferences(result, config.find)
                        .limit(server.maxItems)
                        .collect(Collectors.toList());

        return CompletableFuture.completedFuture(locations);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
        URI uri = URI.create(params.getTextDocument().getUri());
        List<SymbolInformation> symbols =
                server.configured().index.allInFile(uri).collect(Collectors.toList());

        return CompletableFuture.completedFuture(symbols);
    }

    @Override
    public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams codeActionParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams codeLensParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens codeLens) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams documentFormattingParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams documentRangeFormattingParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams documentOnTypeFormattingParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams renameParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem document = params.getTextDocument();
        URI uri = URI.create(document.getUri());

        activeDocuments.put(uri, new VersionedContent(document.getText(), document.getVersion()));

        doLint(Collections.singleton(uri));
    }

    void doLint(Collection<URI> paths) {
        LOG.info("Lint " + Joiner.on(", ").join(paths));

        List<Diagnostic<? extends TclFileObject>> errors = new ArrayList<>();
        Map<URI, Optional<String>> content =
                paths.stream().collect(Collectors.toMap(f -> f, this::activeContent));
        DiagnosticCollector<TclFileObject> compile =
                server.configured().interpreter.parseBatch(content);

        errors.addAll(compile.getDiagnostics());

        publishDiagnostics(paths, errors);
    }

    private void publishDiagnostics(
            Collection<URI> touched,
            List<javax.tools.Diagnostic<? extends TclFileObject>> diagnostics) {
        Map<URI, PublishDiagnosticsParams> files =
                touched.stream()
                        .collect(
                                Collectors.toMap(
                                        uri -> uri,
                                        newUri ->
                                                new PublishDiagnosticsParams(
                                                        newUri.toString(), new ArrayList<>())));

        // Organize diagnostics by file
        for (javax.tools.Diagnostic<? extends TclFileObject> error : diagnostics) {
            TclDiagnostic tclDiagnostic = (TclDiagnostic) error;
            URI uri = error.getSource().toUri();
            PublishDiagnosticsParams publish =
                    files.computeIfAbsent(
                            uri,
                            newUri ->
                                    new PublishDiagnosticsParams(
                                            newUri.toString(), new ArrayList<>()));
            Lints.convert(tclDiagnostic).ifPresent(d -> publish.getDiagnostics().add(d));
        }

        // If there are no errors in a file, put an empty PublishDiagnosticsParams
        for (URI each : touched) files.putIfAbsent(each, new PublishDiagnosticsParams());

        files.forEach(
                (file, errors) -> {
                    if (touched.contains(file)) {
                        client.join().publishDiagnostics(errors);

                        LOG.info(
                                "Published "
                                        + errors.getDiagnostics().size()
                                        + " errors from "
                                        + file);
                    } else
                        LOG.info(
                                "Ignored "
                                        + errors.getDiagnostics().size()
                                        + " errors from not-open "
                                        + file);
                });
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        VersionedTextDocumentIdentifier document = params.getTextDocument();
        URI uri = URI.create(document.getUri());
        VersionedContent existing = activeDocuments.get(uri);
        String newText = existing.content;

        if (document.getVersion() > existing.version) {
            for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
                if (change.getRange() == null)
                    activeDocuments.put(
                            uri, new VersionedContent(change.getText(), document.getVersion()));
                else newText = patch(newText, change);
            }

            activeDocuments.put(uri, new VersionedContent(newText, document.getVersion()));
        } else
            LOG.warning(
                    "Ignored change with version "
                            + document.getVersion()
                            + " <= "
                            + existing.version);
    }


    private String patch(String sourceText, TextDocumentContentChangeEvent change) {
        try {
            Range range = change.getRange();
            BufferedReader reader = new BufferedReader(new StringReader(sourceText));
            StringWriter writer = new StringWriter();

            // Skip unchanged lines
            int line = 0;

            while (line < range.getStart().getLine()) {
                writer.write(reader.readLine() + '\n');
                line++;
            }

            // Skip unchanged chars
            for (int character = 0; character < range.getStart().getCharacter(); character++)
                writer.write(reader.read());

            // Write replacement text
            writer.write(change.getText());

            // Skip replaced text
            reader.skip(change.getRangeLength());

            // Write remaining text
            while (true) {
                int next = reader.read();

                if (next == -1) return writer.toString();
                else writer.write(next);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        TextDocumentIdentifier document = params.getTextDocument();
        URI uri = URI.create(document.getUri());

        // Remove from source cache
        activeDocuments.remove(uri);

        // Clear diagnostics
        client.join()
                .publishDiagnostics(
                        new PublishDiagnosticsParams(uri.toString(), new ArrayList<>()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Re-lint all active documents
        doLint(openFiles());
    }
}
