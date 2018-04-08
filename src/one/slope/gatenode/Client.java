package one.slope.gatenode;

import java.net.Socket;

public class Client implements Runnable {
	private final Socket socket;
	
	public Client(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		
	}
	
	public Socket socket() {
		return socket;
	}
}
