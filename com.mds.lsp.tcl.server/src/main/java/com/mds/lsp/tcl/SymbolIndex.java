package com.mds.lsp.tcl;

import com.mds.lsp.tcl.diagnostic.TclFileObject;
import tcl.lang.RelocatedParser;
import tcl.lang.TclParse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SymbolIndex {



    private final Path workspaceRoot;
    private final Supplier<Collection<URI>> openFiles;
    private final Function<URI, Optional<String>> activeContent;
    private final RelocatedParser parser = new RelocatedParser();

    /** Source path files, for which we support methods and classes */
    private final Map<URI, SourceFileIndex> sourcePathFiles = new ConcurrentHashMap<>();

    private final CompletableFuture<?> finishedInitialIndex = new CompletableFuture<>();

    private final Map<URI, String> warnedPackageDirectoryConflict = new HashMap<>();

    SymbolIndex(
            Path workspaceRoot,
            Supplier<Collection<URI>> openFiles,
            Function<URI, Optional<String>> activeContent) {
        this.workspaceRoot = workspaceRoot;
        this.openFiles = openFiles;
        this.activeContent = activeContent;

        new Thread(this::initialIndex, "Initial-Index").start();
    }


    private void initialIndex() {
        // TODO send a progress bar to the user
        updateIndex(InferConfig.allTclFiles(workspaceRoot).map(Path::toUri));

        finishedInitialIndex.complete(null);
    }

    private void updateIndex(Stream<URI> files) {
        files.forEach(this::updateFile);
    }

    private void updateFile(URI each) {
        if (needsUpdate(each)) {
            List<TclParse> parse = parse(each);
            update(parse);
        }
    }


    private boolean needsUpdate(URI file) {
        if (!sourcePathFiles.containsKey(file)) return true;
        else {
            try {
                Instant updated = sourcePathFiles.get(file).updated;
                Instant modified = Files.getLastModifiedTime(Paths.get(file)).toInstant();

                return updated.isBefore(modified);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Guess the source path by looking at package declarations in .java files.
     *
     * <p>For example, if the file src/com/example/Test.java has the package declaration `package
     * com.example;` then the source root is `src`.
     */
    Set<Path> sourcePath() {
        updateOpenFiles();

        Set<Path> result = new HashSet<>();

        sourcePathFiles.forEach(
                (uri, index) -> {
                    Path dir = Paths.get(uri).getParent();
                    String packagePath = index.packageName.replace('.', File.separatorChar);

                    if (!dir.endsWith(packagePath)
                            && !warnedPackageDirectoryConflict
                                    .getOrDefault(uri, "?")
                                    .equals(packagePath)) {
                        LOG.warning("Java source file " + uri + " is not in " + packagePath);

                        warnedPackageDirectoryConflict.put(uri, packagePath);
                    } else {
                        int up = Paths.get(packagePath).getNameCount();
                        Path truncate = dir;

                        for (int i = 0; i < up; i++) truncate = truncate.getParent();

                        result.add(truncate);
                    }
                });

        return result;
    }

    private List<TclParse> parse(URI source) {
        try {
            return RelocatedParser.parseCommand();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private static final Logger LOG = Logger.getLogger("main");
}
