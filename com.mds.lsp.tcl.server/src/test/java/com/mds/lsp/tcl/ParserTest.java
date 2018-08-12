package com.mds.lsp.tcl;


import org.junit.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class ParserTest {
    @Test
    public void missingSemicolon() throws IOException, URISyntaxException {
        String test = "/MissingSemicolon.java";

        List<String> methods = new ArrayList<>();
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        Path path = Paths.get(ParserTest.class.getResource(test).toURI().getPath());
        JavaFileObject file = JavacTaskBuilder.STANDARD_FILE_MANAGER.getRegularFile(path.toFile());
        JavacTask task = JavacTaskBuilder.create()
                                         .fuzzyParser()
                                         .addFile(file)
                                         .reportErrors(errors)
                                         .afterParse(new BridgeExpressionScanner() {
                                             @Override
                                             protected void visitMethod(MethodTree node) {
                                                 methods.add(node.getName().toString());
                                             }
                                         })
                                         .stopIfNoError(CompileStates.CompileState.PARSE)
                                         .build();

        task.call();

        assertThat(methods, hasItem("methodWithMissingSemicolon"));
        assertThat(methods, hasItem("methodAfterMissingSemicolon"));
    }
}