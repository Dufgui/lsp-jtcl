package com.mds.lsp.tcl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;

public class Main {

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		startServer();
	}

	public static void startServer() throws InterruptedException, ExecutionException, IOException {
		TclLanguageServer languageServer = new TclLanguageServer();
		Launcher<LanguageClient> l = LSPLauncher.createServerLauncher(languageServer, System.in, System.out);
		languageServer.connect(l.getRemoteProxy());
        Future<?> startListening = l.startListening();
		startListening.get();
		l.getRemoteProxy().logMessage(new MessageParams(MessageType.Error,"End of start"));

//		Function<MessageConsumer, MessageConsumer> wrapper = consumer -> {
//			MessageConsumer result = consumer;
//			return result;
//		};
//		Launcher<LanguageClient> launcher = createSocketLauncher(languageServer, LanguageClient.class, new InetSocketAddress("localhost", 5007), Executors.newCachedThreadPool(), wrapper);
//		Future<?> startListening = launcher.startListening();
//		startListening.get();
	}


	static <T> Launcher<T> createSocketLauncher(Object localService, Class<T> remoteInterface, SocketAddress socketAddress, ExecutorService executorService, Function<MessageConsumer, MessageConsumer> wrapper) throws IOException {
        AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open().bind(socketAddress);
        AsynchronousSocketChannel socketChannel;
        try {
            socketChannel = serverSocket.accept().get();
            return Launcher.createIoLauncher(localService, remoteInterface, Channels.newInputStream(socketChannel), Channels.newOutputStream(socketChannel), executorService, wrapper);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final ObjectMapper JSON =
            new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .registerModule(pathAsJson())
                    .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

	private static SimpleModule pathAsJson() {
        SimpleModule m = new SimpleModule();

        m.addSerializer(
                Path.class,
                new JsonSerializer<Path>() {
                    @Override
                    public void serialize(
                            Path path, JsonGenerator gen, SerializerProvider serializerProvider)
                            throws IOException, JsonProcessingException {
                        gen.writeString(path.toString());
                    }
                });

        m.addDeserializer(
                Path.class,
                new JsonDeserializer<Path>() {
                    @Override
                    public Path deserialize(
							JsonParser parse, DeserializationContext deserializationContext)
                            throws IOException, JsonProcessingException {
                        return Paths.get(parse.getText());
                    }
                });

        return m;
    }
}