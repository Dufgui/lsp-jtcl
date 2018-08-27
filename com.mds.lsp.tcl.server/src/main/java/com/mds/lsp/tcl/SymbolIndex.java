package com.mds.lsp.tcl;

import com.google.common.collect.Maps;
import org.eclipse.lsp4j.*;
import tcl.lang.Parser;
import tcl.lang.TclParse;
import tcl.lang.TclToken;


import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SymbolIndex {



    private final Path workspaceRoot;
    private final TclParserAndInterpHolder interpreter;
    private final Supplier<Collection<URI>> openFiles;
    private final Function<URI, Optional<String>> activeContent;


    /** Source path files, for which we support methods and classes */
    private final Map<URI, SourceFileIndex> sourcePathFiles = new ConcurrentHashMap<>();

    private final CompletableFuture<?> finishedInitialIndex = new CompletableFuture<>();

    SymbolIndex(
            Path workspaceRoot,
            TclParserAndInterpHolder interpreter,
            Supplier<Collection<URI>> openFiles,
            Function<URI, Optional<String>> activeContent) {
        this.workspaceRoot = workspaceRoot;
        this.interpreter = interpreter;
        this.openFiles = openFiles;
        this.activeContent = activeContent;

        new Thread(this::initialIndex, "Initial-Index").start();
    }


    private void initialIndex() {
        updateIndex(InferConfig.allTclFiles(workspaceRoot).map(Path::toUri));

        finishedInitialIndex.complete(null);
    }

    private void updateIndex(Stream<URI> files) {
        files.forEach(this::updateFile);
    }

    private void updateFile(URI each) {
        if (needsUpdate(each)) {
            List<TclParse> parse = interpreter.parse(each);
            update(each, parse);
        }
    }


    private boolean needsUpdate(URI file) {
        if (!sourcePathFiles.containsKey(file)) return true;
        else {
            try {
                Instant updated = sourcePathFiles.get(file).updated;
                Instant modified = Files.getLastModifiedTime(Paths.get(file)).toInstant();

                return updated.isBefore(modified);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Update a file in the index */
    private void update(URI file, List<TclParse> tclParses) {
        SourceFileIndex index = new SourceFileIndex();
        for (TclParse parse: tclParses) {
            for (int i = 0; i < parse.numTokens(); i++) {
                TclToken token = parse.getToken(i);
                switch (token.getType()) {
                case Parser.TCL_TOKEN_TEXT:
                    index.references.add(token.getTokenString());
                    break;
                case Parser.TCL_TOKEN_VARIABLE:
                    index.declarations.add(token.getTokenString());
                    break;
                case Parser.TCL_TOKEN_COMMAND:
                    index.references.add(token.getTokenString());
                    break;
                default:
                    //do nothing
                }
            }
            parse.release();
        }
        sourcePathFiles.put(file, index);
    }

    Set<Path> sourcePath() {
        updateOpenFiles();

        Set<Path> result = new HashSet<>();

        sourcePathFiles.forEach(
                (uri, index) -> {
                    Path dir = Paths.get(uri).getParent();
                    result.add(dir);

                });

        return result;
    }

    public void updateOpenFiles() {
        finishedInitialIndex.join();

        updateIndex(openFiles.get().stream());
    }

    private static final Logger LOG = Logger.getLogger("main");

    public Collection<URI> potentialReferences(String name) {
        updateOpenFiles();

        Map<URI, SourceFileIndex> hasName =
                Maps.filterValues(sourcePathFiles, index -> index.references.contains(name));

        return hasName.keySet();
    }

    public Optional<URI> findDeclaringFile(String qualifiedName) {
        String namespaceName = Completions.mostIds(qualifiedName),
                className = Completions.lastId(qualifiedName);
        Predicate<Map.Entry<URI, SourceFileIndex>> containsClass =
                entry -> {
                    SourceFileIndex index = entry.getValue();

                    return index.namespaceName.equals(namespaceName)
                            && index.topLevelClasses
                                    .stream()
                                    .anyMatch(c -> c.equals(className));
                };

        return sourcePathFiles
                .entrySet()
                .stream()
                .filter(containsClass)
                .map(Map.Entry::getKey)
                .findFirst();
    }

     /** Search all indexed symbols */
    public Stream<SymbolInformation> search(String query) {
        updateOpenFiles();

        Predicate<CharSequence> nameMatchesQuery =
                name -> Completions.containsCharactersInOrder(name, query, true);
        Predicate<URI> fileMatchesQuery =
                uri -> sourcePathFiles.get(uri).declarations.stream().anyMatch(nameMatchesQuery);
        Collection<URI> open = openFiles.get();
        Stream<URI> openFirst =
                Stream.concat(
                        open.stream(),
                        sourcePathFiles.keySet().stream().filter(uri -> !open.contains(uri)));

        return openFirst
                .filter(fileMatchesQuery)
                .flatMap(this::allInFile)
                .filter(info -> nameMatchesQuery.test(info.getName()));
    }

    /** Get all declarations in an open file */
    public Stream<SymbolInformation> allInFile(URI source) {
        LOG.info("Search " + source);

        List<SymbolInformation> result = new ArrayList<>();
        List<TclParse> tclParses = Parser.parseCommand(source.getPath());

        for (TclParse parse: tclParses) {
            for (int i = 0; i < parse.numTokens(); i++) {
                TclToken token = parse.getToken(i);
                switch (token.getType()) {
                case Parser.TCL_TOKEN_TEXT:
                    result.add(new SymbolInformation(token.getTokenString(), SymbolKind.Event, toLocation(source, parse, token)));
                    break;
                case Parser.TCL_TOKEN_VARIABLE:
                    result.add(new SymbolInformation(token.getTokenString(), SymbolKind.Variable, toLocation(source, parse, token)));
                    break;
                case Parser.TCL_TOKEN_COMMAND:
                    result.add(new SymbolInformation(token.getTokenString(), SymbolKind.Function, toLocation(source, parse, token)));
                    break;
                default:
                    //do nothing
                }
            }
        }

        return result.stream();
    }

    private Location toLocation(URI source, TclParse parse, TclToken token) {
        //FIXME optimize it by externalize eol in a cache
        int startLine = parse.getLineNum();

        int lastEolOffset = 0;
        //default eol char
        char eolChar = ' ';
        for (int i = 0; i < token.getScriptIndex(); i++) {
            if(token.getScriptArray()[i] == '\n' || token.getScriptArray()[i] == '\r' ) {
                lastEolOffset = i;
                if(eolChar != ' ' && eolChar == token.getScriptArray()[i]) {
                    ++startLine;
                } else if (eolChar==' ') {
                    eolChar = token.getScriptArray()[i];
                    ++startLine;
                }
            }
        }
        int startCharacter = token.getScriptIndex() - lastEolOffset;
        int endLine = startLine;
        for (int i = 0; i < token.getSize(); i++) {
            int j = i + token.getScriptIndex();
            if(token.getScriptArray()[j] == '\n' || token.getScriptArray()[j] == '\r' ) {
                lastEolOffset = j;
                if(eolChar == token.getScriptArray()[i]) {
                    ++endLine;
                }
            }
        }
        int endCharacter = token.getScriptIndex() - lastEolOffset;

        return new Location(source.getPath(), new Range(new Position(startLine, startCharacter), new Position(endLine, endCharacter)));
    }
}
