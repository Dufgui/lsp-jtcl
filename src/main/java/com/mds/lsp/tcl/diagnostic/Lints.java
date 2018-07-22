package com.mds.lsp.tcl.diagnostic;


import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import javax.tools.Diagnostic;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

public class Lints {

    public static Optional<Diagnostic> convert(javax.tools.Diagnostic<? extends TclFileObject> error) {
        if (error.getStartPosition() != javax.tools.Diagnostic.NOPOS) {
            Range range = position(error);
            TclDiagnostic diagnostic = new TclDiagnostic();
            DiagnosticSeverity severity = severity(error.getKind());

            diagnostic.setSeverity(severity);
            diagnostic.setRange(range);
            diagnostic.setCode(error.getCode());
            diagnostic.setMessage(error.getMessage(null));

            return Optional.of(diagnostic);
        } else {
            LOG.warning("Skipped " + error.getMessage(Locale.getDefault()));

            return Optional.empty();
        }
    }

    private static DiagnosticSeverity severity(javax.tools.Diagnostic.Kind kind) {
        switch (kind) {
            case ERROR:
                return DiagnosticSeverity.Error;
            case WARNING:
            case MANDATORY_WARNING:
                return DiagnosticSeverity.Warning;
            case NOTE:
            case OTHER:
            default:
                return DiagnosticSeverity.Information;
        }
    }

    private static Range position(javax.tools.Diagnostic<? extends TclFileObject> error) {
        TclDiagnostic diagnostic = (TclDiagnostic) error;
        long start = error.getStartPosition(), end = error.getEndPosition();

        if (end == start) end = start + 1;

        return new Range(
                new Position(
                        (int)diagnostic.getLineNumber(),
                        (int)diagnostic.getColumnNumber()),
                new Position(
                        (int)diagnostic.getLineNumber(),
                        (int)diagnostic.getColumnNumber()));
    }

    private static final Logger LOG = Logger.getLogger("main");
}