package com.mds.lsp.tcl;


public class Configured {

    final TclParserHolder interpreter;
    //final Tcldocs docs;
    final SymbolIndex index;
    //final FindSymbols find;

    Configured(TclParserHolder interpreter, SymbolIndex index/*, Tcldocs docs, FindSymbols find*/) {
        this.interpreter = interpreter;
        //this.docs = docs;
        this.index = index;
        //this.find = find;
    }
}
