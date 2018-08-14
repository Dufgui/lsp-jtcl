package com.mds.lsp.tcl.diagnostic;

import java.io.*;
import java.net.URI;

public class SimpleTclFileObject implements TclFileObject {
    /**
     * A URI for this file object.
     */
    protected final URI uri;

    /**
     * The kind of this file object.
     */
    protected final TclFileObject.Kind kind;

    /**
     * Construct a SimpleJavaFileObject of the given kind and with the
     * given URI.
     *
     * @param uri  the URI for this file object
     * @param kind the kind of this file object
     */
    protected SimpleTclFileObject(URI uri, TclFileObject.Kind kind) {
        // null checks
        uri.getClass();
        kind.getClass();
        if (uri.getPath() == null)
            throw new IllegalArgumentException("URI must have a path: " + uri);
        this.uri = uri;
        this.kind = kind;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public String getName() {
        return toUri().getPath();
    }

    /**
     * @return {@code this.kind}
     */
    public TclFileObject.Kind getKind() {
        return kind;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }
}
