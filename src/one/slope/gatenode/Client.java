package one.slope.gatenode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import one.slope.slip.io.SuperBuffer;

public class Client implements Runnable {
	// TODO queued messages for writing (after the encoders have run - independent of this class)
	private final SuperBuffer buffer = new SuperBuffer(1024);
	private final OutputStream output;
	private final InputStream input;
	private final Socket socket;
	
	public Client(Socket socket) throws IOException {
		this.socket = socket;
		this.output = socket.getOutputStream();
		this.input = socket.getInputStream();
	}

	@Override
	public void run() {
		// threads and IO are easier, spawn more nodes = more power, let's keep it simple.
		
		while (true) {
			try {
				// we don't need to do anything if we don't read any data
				// we are waiting for a complete message before processing any more events
				// because the buffer has already been cleared of complete messages the last time this ran
				if (input.available() > 0) {
					buffer.put(input.readAllBytes());
					// flip buffer
					// read from buffer
					// create new super buffers with complete packets (modularity dictates the packet decoders need to be passed in / set external to this client class and called with the complete buffer)
					// pass off to queue for parallel message processing
					// then compact, placing the incomplete message data at the front (if any)
					// set position to start writing to the end of the incomplete message (would be 0 if there is no data in the buffer aka no incomplete message)
				}
			}
			catch (Exception ex) {
				// TODO socket closed/disconnected
			}
		}
	}
	
	public InputStream input() {
		return input;
	}
	
	public OutputStream output() {
		return output;
	}
	
	public Socket socket() {
		return socket;
	}
	
	public SuperBuffer buffer() {
		return buffer;
	}
}
