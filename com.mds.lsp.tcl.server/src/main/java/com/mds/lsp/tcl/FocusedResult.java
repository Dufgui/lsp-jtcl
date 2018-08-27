package com.mds.lsp.tcl;

import tcl.lang.TclParse;

import java.util.Optional;

public class FocusedResult {
    private final TclParse[] compilationUnit;
    private final Optional<TclParseCursor> cursor;

    FocusedResult() {
        this.compilationUnit = null;
        this.cursor = Optional.empty();
    }

    FocusedResult(
            TclParse[] compilationUnit,
            Optional<TclParseCursor> cursor) {
        this.compilationUnit = compilationUnit;
        this.cursor = cursor;
    }

    public TclParse[] getCompilationUnit() {
        return compilationUnit;
    }

    public Optional<TclParseCursor> getCursor() {
        return cursor;
    }
}
