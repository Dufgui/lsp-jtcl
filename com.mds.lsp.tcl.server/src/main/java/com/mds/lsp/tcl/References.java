package com.mds.lsp.tcl;

import org.eclipse.lsp4j.Location;
import tcl.lang.TclParse;

import java.util.Optional;
import java.util.stream.Stream;

public class References {

    public static Stream<Location> findReferences(FocusedResult compiled, FindSymbols find) {
        return compiled.getCursor()
                .map(cursor -> new References(find).doFindReferences(cursor))
                .orElseGet(Stream::empty);
    }

    public static Optional<Location> gotoDefinition(FocusedResult compiled, FindSymbols find) {
        return compiled.getCursor().flatMap(
                cursor -> new References(find).doGotoDefinition(cursor));
    }


    private final FindSymbols find;

    private References(FindSymbols find) {
        this.find = find;
    }

    private Stream<Location> doFindReferences(TclParseCursor cursor) {
        Stream<TclParse> symbols = cursor.stream();
        return symbols.flatMap(tclParse -> find.references(tclParse.getToken(0)));
    }

    private Optional<Location> doGotoDefinition(TclParseCursor cursor) {
        Stream<TclParse> symbols = cursor.stream();

        return symbols.map(tclParse -> find.find(tclParse.getToken(0))).findAny().filter(Optional::isPresent)
  .map(Optional::get);
    }
}
