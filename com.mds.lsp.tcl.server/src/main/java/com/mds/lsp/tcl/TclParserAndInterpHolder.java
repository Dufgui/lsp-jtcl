package com.mds.lsp.tcl;


import com.mds.lsp.tcl.diagnostic.TclFileObject;
import tcl.lang.Interp;
import tcl.lang.RelocatedParser;

import javax.tools.DiagnosticCollector;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class TclParserAndInterpHolder {
    private final Interp interp = new Interp();
    private final RelocatedParser parser = new RelocatedParser();

    public static TclParserAndInterpHolder create() {
        return new TclParserAndInterpHolder();
    }

    public DiagnosticCollector<TclFileObject> evalBatch(Map<URI,Optional<String>> content) {
        throw new UnsupportedOperationException();
    }


}
