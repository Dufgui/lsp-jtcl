package com.mds.lsp.tcl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TclSettings {

    public List<String> scriptPath = new ArrayList<>();
    public String workingDir = new String();
    public List<String> classpath = new ArrayList<>();
    public List<String> loadpath = new ArrayList<>();
    public Optional<String> tclHome = Optional.empty();

}
