package com.mds.lsp.tcl;

public class VersionedContent {
    final String content;
    final int version;

    VersionedContent(String content, int version) {
        this.content = content;
        this.version = version;
    }
}
