package com.mds.lsp.tcl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TclSettings {

    public Tcl tcl = new Tcl();

    public static class Tcl {
        public List<String> classPath = new ArrayList<>();
        public List<String> externalDependencies = new ArrayList<>();
        public Optional<String> tclHome = Optional.empty();
    }
}
