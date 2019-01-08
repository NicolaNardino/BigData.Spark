package com.projects.bigdata.data_streaming;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.projects.bigdata.utility.Utility;

/**
 * TCP server sending randomly generated strings, be it random words or JSON representations of Objects. 
 * It's not meant to be a fully fledged TCP server, i.e., with a multithreaded request manager, because its only purpose is to send data to a Spark Streaming application, 
 * which is its only client.
 * 
 * The current use case consists of sending data to two ports, i.e., it opens instantiates two ServerSockets, although it could open and send data to an indefinite number of ports.  
 * */
public abstract class AbstractDataStreamingTCPServer implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(AbstractDataStreamingTCPServer.class);
	
	private final ServerSocket serverSocket;
	private final int messageSendDelayMilliSeconds;
	private final int port; 
	private volatile boolean isStopped;
	
	public AbstractDataStreamingTCPServer(final int port, final int messageSendDelayMilliSeconds) {
		this.port = port;
		this.messageSendDelayMilliSeconds = messageSendDelayMilliSeconds;
		try {
			serverSocket = new ServerSocket(port);	
		}
		catch(final Exception e) {
			throw new RuntimeException("Unable to instantiate localhost server@"+port);
		}
		logger.info("Server started, localhost@"+port);
	}

	@Override
	public void run() {
		Thread.currentThread().setName("Server@"+port);
		try (final Socket clientSocket = serverSocket.accept();
			 final PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);) {
			logger.info("Client request received.");
			while (!isStopped) {
				final String line = buildLine();
				logger.info("Sending line: "+line);
				writer.println(line);
				Utility.sleep(TimeUnit.MILLISECONDS, messageSendDelayMilliSeconds);
			}
		}
		catch (final Exception e) {
			logger.error("Error while sending data.", e);
		}
	}

	public void stop() {
		isStopped = true;
		try {
			serverSocket.close();
		}
		catch(final Exception e) {
			logger.error("Error while closing server socket.", e);
		}
		logger.info("Server stopped, localhost@"+port);
	}
	
	protected abstract String buildLine();
	
}