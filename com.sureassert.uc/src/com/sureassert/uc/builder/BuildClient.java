package com.sureassert.uc.builder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.saserver.SAServerMessage;
import com.sureassert.uc.runtime.saserver.VoidReturn;

/**
 * Client that connects to a BuildServer and requests builds. <br/>
 * <br/>
 * 
 * @author Nathan Dolan
 */
public class BuildClient {

	private final String host;
	private final int port;

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Socket socket;

	private static BuildClient instance = null;

	public static synchronized BuildClient getInstance() {

		if (instance == null)
			instance = new BuildClient(null, 4671);
		return instance;
	}

	private BuildClient(String host, int port) {

		this.host = host;
		this.port = port;

		try {
			// Connect socket to Eclipse
			socket = new Socket(host, port);
			BasicUtils.debug("Connected to BuildServer at " + host + ":" + port);

			out = new ObjectOutputStream(socket.getOutputStream());
			BasicUtils.debug("Got OutputStream");
			in = new ObjectInputStream(socket.getInputStream());
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
