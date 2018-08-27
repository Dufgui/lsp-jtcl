package com.mds.lsp.tcl;

import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;

import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

class Completions {

    private final SymbolIndex sourcePath;

    private Completions(
            SymbolIndex sourcePath) {
        this.sourcePath = sourcePath;
    }

    static String firstId(String qualifiedName) {
        int firstDot = qualifiedName.indexOf('.');

        if (firstDot == -1) return qualifiedName;
        else return qualifiedName.substring(0, firstDot);
    }

    static String mostIds(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf(':');

        if (lastDot == -1) return "";
        else return qualifiedName.substring(0, lastDot);
    }

    static String lastId(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf(':');

        if (lastDot == -1) return qualifiedName;
        else return qualifiedName.substring(lastDot + 1);
    }

    private static final Command TRIGGER_SIGNATURE_HELP =
            new Command("", "editor.action.triggerParameterHints");


    public static boolean containsCharactersInOrder(
            CharSequence candidate, CharSequence pattern, boolean caseSensitive) {
        int iCandidate = 0, iPattern = 0;

        while (iCandidate < candidate.length() && iPattern < pattern.length()) {
            char patternChar = pattern.charAt(iPattern);
            char testChar = candidate.charAt(iCandidate);

            if (!caseSensitive) {
                patternChar = Character.toLowerCase(patternChar);
                testChar = Character.toLowerCase(testChar);
            }

            if (patternChar == testChar) {
                iPattern++;
                iCandidate++;
            } else iCandidate++;
        }

        return iPattern == pattern.length();
    }

    private static final Logger LOG = Logger.getLogger("main");

    public static Stream<CompletionItem> at(FocusedResult compiled, SymbolIndex index) {
        //TODO to implement
        throw new UnsupportedOperationException();

        /*Function<TreePath, Completions> newCompletions =
                path -> new Completions(compiled.task, compiled.classPath, index, docs, path);
        return compiled.cursor.map(newCompletions).map(Completions::get).orElseGet(Stream::empty);*/
    }

}
