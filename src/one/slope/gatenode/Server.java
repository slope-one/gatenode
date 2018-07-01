package one.slope.gatenode;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import one.slope.slip.io.packet.PacketDefinitionProvider;

public class Server {
	public static void main(String[] args) throws Exception {
		boolean connected = true;
		ServerSocket socket = new ServerSocket();
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", 43594);
		socket.bind(address, 100);
		
		System.out.println("Server running on: " + address);
		PacketDefinitionProvider provider = new GamePacketDefinitionProvider();
		
		while (connected) {
			try {
				Socket client = socket.accept();
				new Thread(new Client(client, provider)).run();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				//connected = false;
			}
		}
		
		socket.close();
	}
}
