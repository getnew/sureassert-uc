package com.sureassert.uc.builder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.saserver.SAServerMessage;
import com.sureassert.uc.runtime.saserver.VoidReturn;

/**
 * Server that listens for client connections and executes headless builds on request. <br/>
 * <br/>
 * Accepts a handle to which to report update events about the build such as coverage
 * reporting and addition of success and error markers.
 * 
 * @author Nathan Dolan
 */
public class BuildServer {

	private final int port;

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private ServerSocket serverSocket;
	private Socket clientSocket;

	private static BuildServer instance = null;

	public static synchronized BuildServer getInstance() {

		if (instance == null)
			instance = new BuildServer(4671);
		return instance;
	}

	private BuildServer(int port) {

		this.port = port;

		try {
			// Connect socket to Eclipse
			serverSocket = new ServerSocket(port);
			BasicUtils.debug("Waiting for connection on " + port + "...");
			clientSocket = serverSocket.accept();
			BasicUtils.debug("Accepted connection");

			out = new ObjectOutputStream(clientSocket.getOutputStream());
			BasicUtils.debug("Got OutputStream");
			in = new ObjectInputStream(clientSocket.getInputStream());
			BasicUtils.debug("Got InputStream");

			BasicUtils.debug("Created streams");

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <R extends Serializable> R sendMessage(SAServerMessage<R> message) throws IOException, ClassNotFoundException {

		out.writeObject(message);
		if (message.getReturnType().equals(VoidReturn.class))
			return null;
		else
			return message.getReturnType().cast(in.readObject());
	}

	/**
	 * Processes incoming messages until the given message is received.
	 */
	public void waitFor(SAServerMessage<?> stopMessage) {

		while (true) {

			try {
				SAServerMessage<?> request = (SAServerMessage<?>) in.readObject();
				Object retval = request.execute();
				if (!(retval instanceof VoidReturn))
					out.writeObject(retval);

				if (request.equals(stopMessage))
					return;

			} catch (Exception e) {

				BasicUtils.debug(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}

	public void disconnect() {

		try {
			if (out != null)
				out.close();
			if (in != null)
				in.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
