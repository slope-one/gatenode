package one.slope.gatenode;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.dieselpoint.norm.Database;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import one.slope.slip.service.DatabasePacketDefinitionProvider;

public class Server {
	public static void main(String[] args) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost"); // TODO config
		Connection connection = factory.newConnection();
		
		Database db = new Database();
		// TODO config
		db.setJdbcUrl("jdbc:mysql://rspserver.localhost:3306/service?useSSL=false");
		db.setUser("root");
		db.setPassword("");
		
		DatabasePacketDefinitionProvider provider = new DatabasePacketDefinitionProvider(db);
		
		if (provider.load("377")) {
			System.out.println("Loaded packet definitions");
		}
		
		boolean connected = true;
		ServerSocket socket = new ServerSocket();
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", 43594);
		socket.bind(address, 100);
		
		System.out.println("Server running on: " + address);
		
		while (connected) {
			try {
				Socket client = socket.accept();
				Channel channel = connection.createChannel();
				new Thread(new Client(client, provider, channel)).start();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				//connected = false;
			}
		}
		
		socket.close();
	}
}
