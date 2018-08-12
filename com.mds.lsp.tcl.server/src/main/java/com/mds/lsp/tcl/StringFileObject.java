package com.mds.lsp.tcl;


import com.mds.lsp.tcl.diagnostic.SimpleTclFileObject;

import java.io.IOException;
import java.net.URI;

class StringFileObject extends SimpleTclFileObject {
    private final String content;
    private final URI path; // TODO rename

    StringFileObject(String content, URI path) {
        super(path, Kind.SOURCE);

        this.content = content;
        this.path = path;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return content;
    }
}