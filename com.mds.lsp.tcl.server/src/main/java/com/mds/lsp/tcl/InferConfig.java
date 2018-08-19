package com.mds.lsp.tcl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class InferConfig {
    /** Root of the workspace that is currently open in VSCode */
    private final Path workspaceRoot;
    /** User-specified external dependencies, configured with java.externalDependencies */
    private final Collection<File> externalDependencies;
    /** User-specified tcl path, configured with tcl.path */
    private final List<Path> tclPath;

    InferConfig(
            Path workspaceRoot,
            Collection<File> externalDependencies,
            List<Path> tclPath) {
        this.workspaceRoot = workspaceRoot;
        this.externalDependencies = externalDependencies;
        this.tclPath = tclPath;
    }

    // TODO move to TclLanguageServer
    static Stream<Path> allTclFiles(Path dir) {
        if(dir == null) {
            return Stream.empty();
        }

        PathMatcher match = FileSystems.getDefault().getPathMatcher("glob:*.tcl");

        try {
            // TODO instead of looking at EVERY file, once you see a few files with the same source directory,
            // ignore all subsequent files in the directory
            return Files.walk(dir).filter(tclFile -> match.matches(tclFile.getFileName()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Instant buildFilesModified(Path workspaceRoot) {
        Instant workspaceModified = fileModified(workspaceRoot.resolve("WORKSPACE"));
        return workspaceModified;
    }

    private static Instant fileModified(Path file) {
        if (Files.exists(file)) {
            try {
                return Files.getLastModifiedTime(file).toInstant();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else return Instant.EPOCH;
    }

}