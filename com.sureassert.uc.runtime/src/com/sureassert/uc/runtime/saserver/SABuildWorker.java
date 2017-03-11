package com.sureassert.uc.runtime.saserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.Socket;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class SABuildWorker {

	private PrintWriter out = null;
	private Reader in = null;

	private static SABuildWorker instance;

	public static synchronized SABuildWorker getInstance() {

		if (instance == null) {
			instance = new SABuildWorker();
		}
		return instance;
	}

	private SABuildWorker() {

		try {
			Socket socket = new Socket(InetAddress.getLocalHost(), 4444);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		} catch (IOException e) {
			throw new SARuntimeException(e);
		}
	}

	public void sendMessage(String message) {

		out.println(message);
	}

	@Override
	public void finalize() {

		BasicUtils.close(out);
		BasicUtils.close(in);
	}

}
