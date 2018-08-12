package com.mds.lsp.tcl.diagnostic;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TclDiagnostic implements Diagnostic<TclFileObject> {

    private Kind kind;
    private TclFileObject source;
    private long position;
    private long startPosition;
    private long endPosition;
    private long lineNumber;
    private long columnNumber;
    private String code;
    private final Map<Locale, String> messages = new HashMap<>();
    private DiagnosticSeverity severity;
    private Range range;

    @Override
    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    @Override
    public TclFileObject getSource() {
        return source;
    }

    public void setSource(TclFileObject source) {
        this.source = source;
    }

    @Override
    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    @Override
    public long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(long endPosition) {
        this.endPosition = endPosition;
    }

    @Override
    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public long getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(long columnNumber) {
        this.columnNumber = columnNumber;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage(Locale locale) {
        return messages.get(locale);
    }

    public void setMessage(String message) {
        this.messages.put(Locale.getDefault(), message);
    }

    public void setSeverity(DiagnosticSeverity severity) {
        this.severity = severity;
    }

    public void setRange(Range range) {
        this.range = range;
    }
}
