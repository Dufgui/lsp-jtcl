package com.mds.lsp.tcl;

import org.eclipse.lsp4j.Location;
import tcl.lang.TclToken;


import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindSymbols {
    private final SymbolIndex index;
    private final TclParserAndInterpHolder interpreter;
    private final Function<URI, Optional<String>> activeContent;

    FindSymbols(
            SymbolIndex index,
            TclParserAndInterpHolder interpreter,
            Function<URI, Optional<String>> activeContent) {
        this.index = index;
        this.interpreter = interpreter;
        this.activeContent = activeContent;
    }

    /**
     * Find a symbol in its file.
     *
     * <p>It's possible that `symbol` comes from a .class file where the corresponding .java file
     * was not visited during incremental compilation. In order to be sure we have access to the
     * source positions, we will recompile the .java file where `symbol` was declared.
     */
    Optional<Location> find(TclToken symbol) {
        index.updateOpenFiles();

        return findFile(symbol).flatMap(file -> findIn(symbol, file));
    }

    /** Find all references to a symbol */
    Stream<Location> references(TclToken symbol) {
        String name = symbol.getTokenString();

        return findReferences(index.potentialReferences(name), symbol).stream();
    }

    private Optional<Location> findIn(TclToken symbol, URI file) {
        List<Location> result = new ArrayList<>();

        //TODO add

        if (!result.isEmpty()) return Optional.of(result.get(0));
        else return Optional.empty();
    }

    private List<Location> findReferences(Collection<URI> files, TclToken symbol) {
        List<Location> found = new ArrayList<>();
        Map<URI, Optional<String>> todo =
                files.stream().collect(Collectors.toMap(uri -> uri, activeContent));

        //TODO add

        return found;
    }

    private Optional<URI> findFile(TclToken symbol) {
        return index.findDeclaringFile(symbol.getTokenString());
    }

}