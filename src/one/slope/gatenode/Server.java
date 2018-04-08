package one.slope.gatenode;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	public static void main(String[] args) throws Exception {
		ServerSocket socket = new ServerSocket();
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", 43594);
		socket.bind(address, 100);
		
		while (true) {
			Socket client = socket.accept();
			new Thread(new Client(client)).run();
		}
	}
}
