package com.mds.lsp.tcl;

import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SymbolIndexTest {

	SymbolIndex index;

	@BeforeEach
	void setUp() {
		Path workspaceRoot = FileSystems.getDefault().getPath(".");
		CompletableFuture<LanguageClient> client = new CompletableFuture<>();
		TclParserAndInterpHolder interpreter = new TclParserAndInterpHolder(client);
		Supplier<Collection<URI>> openFiles = () -> Collections.emptyList();
		Function<URI, Optional<String>> activeContent = uri -> Optional.empty();
		index = new SymbolIndex(workspaceRoot, interpreter, openFiles, activeContent);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void search() {
	}

	@Test
	void allInFile() {
	}
}