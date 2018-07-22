package com.mds.lsp.tcl.diagnostic;

import javax.tools.FileObject;

public interface TclFileObject extends FileObject {

    /**
     * Kinds of JavaFileObjects.
     */
    enum Kind {
        /**
         * Source files written in the Tcl programming language.
         */
        SOURCE(".tcl");
        /**
         * The extension which (by convention) is normally used for
         * this kind of file object.  If no convention exists, the
         * empty string ({@code ""}) is used.
         */
        public final String extension;
        Kind(String extension) {
            extension.getClass(); // null check
            this.extension = extension;
        }
    };

    /**
     * Gets the kind of this file object.
     *
     * @return the kind
     */
    TclFileObject.Kind getKind();

}
