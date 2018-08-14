package com.mds.lsp.tcl;


public class Configured {

    final TclParserAndInterpHolder interpreter;
    //final Tcldocs docs;
    final SymbolIndex index;
    final FindSymbols find;

    Configured(TclParserAndInterpHolder interpreter, SymbolIndex index, FindSymbols find/*, Tcldocs docs*/) {
        this.interpreter = interpreter;
        //this.docs = docs;
        this.index = index;
        this.find = find;
    }
}
