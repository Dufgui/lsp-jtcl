package com.mds.lsp.tcl;

import org.eclipse.lsp4j.Location;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
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
    Optional<Location> find(Element symbol) {
        index.updateOpenFiles();

        return findFile(symbol).flatMap(file -> findIn(symbol, file));
    }

    /** Find all references to a symbol */
    Stream<Location> references(Element symbol) {
        String name = symbol.getSimpleName().toString();

        return findReferences(index.potentialReferences(name), symbol).stream();
    }

    private Optional<Location> findIn(Element symbol, URI file) {
        List<Location> result = new ArrayList<>();

        //TODO add

        if (!result.isEmpty()) return Optional.of(result.get(0));
        else return Optional.empty();
    }

    private List<Location> findReferences(Collection<URI> files, Element target) {
        List<Location> found = new ArrayList<>();
        Map<URI, Optional<String>> todo =
                files.stream().collect(Collectors.toMap(uri -> uri, activeContent));

        //TODO add

        return found;
    }

    private Optional<URI> findFile(Element symbol) {
        return topLevelClass(symbol).flatMap(index::findDeclaringFile);
    }

    private Optional<TypeElement> topLevelClass(Element symbol) {
        TypeElement result = null;

        while (symbol != null) {
            if (symbol instanceof TypeElement) result = (TypeElement) symbol;

            symbol = symbol.getEnclosingElement();
        }

        return Optional.ofNullable(result);
    }

    private static void createQualifiedName(Element s, StringJoiner acc) {
        if (s != null) {
            createQualifiedName(s.getEnclosingElement(), acc);

            if (s instanceof PackageElement)
                acc.add(((PackageElement) s).getQualifiedName().toString());
            else if (s.getSimpleName().length() != 0) acc.add(s.getSimpleName().toString());
        }
    }
}