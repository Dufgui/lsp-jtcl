package com.mds.lsp.tcl;


import tcl.lang.TclParse;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Stream;

public class TclParseCursor {

    private final LinkedList<TclParse> tclParses = new LinkedList<>();

    public TclParseCursor(Collection<TclParse> tclParses){
        this.tclParses.addAll(tclParses);
    }

    public Stream<TclParse> stream() {
        return tclParses.stream();
    }
}
