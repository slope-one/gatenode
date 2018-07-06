package one.slope.gatenode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;

import one.slope.slip.io.DataType;
import one.slope.slip.io.SuperBuffer;
import one.slope.slip.io.packet.Packet;
import one.slope.slip.io.packet.PacketDefinition;
import one.slope.slip.io.packet.PacketDefinitionProvider;
import one.slope.slip.io.packet.PacketSize;
import one.slope.slip.io.packet.PacketType;

public class Client implements Runnable {
	public transient static final int BUFFER_SIZE = 1024 * 10; // TODO move to configuration - 10kb to start with

	// TODO queued messages for writing (after the encoders have run - independent of this class)
	
	private transient final PacketDefinitionProvider provider;
	private transient final SuperBuffer buffer = new SuperBuffer(BUFFER_SIZE);
	private transient final OutputStream output;
	private transient final InputStream input;
	private transient final Channel channel;
	private transient final Socket socket;
	
	private final String id;
	
	private PacketType state = PacketType.NEGOTIATION;
	
	// threads and IO are easier, spawn more nodes = more power, let's keep it simple.
	public Client(Socket socket, PacketDefinitionProvider provider, Channel channel) throws IOException {
		this.socket = socket;
		this.output = socket.getOutputStream();
		this.input = socket.getInputStream();
		this.provider = provider;
		this.channel = channel;
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public void run() {
		boolean connected = true;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		AMQP.BasicProperties.Builder propertyBuilder = new AMQP.BasicProperties.Builder();
		
		propertyBuilder.correlationId("");
		propertyBuilder.replyTo("outbound");
		
		try {
			channel.queueDeclare("inbound", false, false, false, null);
			channel.queueDeclare("outbound", false, false, false, null);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			connected = false;
		}
		
		System.out.println("Client connected: " + socket.getRemoteSocketAddress());
		
		while (connected) {
			try {
				// we don't need to do anything if we don't read any data
				// we are waiting for a complete message before processing any more events
				// because the buffer has already been cleared of complete messages the last time this ran
				if (buffer.putAvailable(input) > 0) {
					buffer.flip();
					
					// BEGIN DEBUG OUTPUT OF BYTES READ
					buffer.mark();
					byte[] readBytes = buffer.array();
					buffer.reset();
					
					System.out.print("Read bytes (" + readBytes.length + "): ");
					
					for (byte b : readBytes) {
						System.out.print(b + " ");
					}
					
					System.out.println();
					// END DEBUG OUTPUT OF BYTES READ
					
					try {
						while (buffer.hasRemaining()) {
							// retrieve definition from provider
							PacketDefinition def = provider.get(state, buffer.getUnsigned());
							SuperBuffer buf = null;
							
							// initialize packet buffer
							if (def.size() == PacketSize.FIXED) {
								buf = new SuperBuffer(def.length());
							}
							else if (def.size() == PacketSize.VARIABLE) {
								DataType length = DataType.getTypeForWidth(Math.abs(def.length()));
								buf = new SuperBuffer(buffer.getUnsigned(length));
							}
							
							// if we have data to read, read it into the packet's buffer
							if (buf.limit() > 0) {
								buf.put(buffer, buf.limit());
								buf.flip();
							}
							
							buf.mark();
							
							// create new packet object, decoding the data into the k/v map
							Packet p = new Packet(def, buf);
							p.decode();
							
							Map<String, Object> message = new HashMap<>();
							message.put("client", this);
							message.put("message", p);
							
							String json = gson.toJson(message);
							
							// print some information about the packet for debug purposes
							//System.out.println("Read packet \"" + def.name() + "\" - ID " + def.id() + ", SIZE " + def.size() + ", LENGTH " + def.length() + "");
							//System.out.println(packet.values());
							System.out.println(json);
							channel.basicPublish("", "inbound", null, json.getBytes());
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					
					buffer.compact();
				}
				
				Thread.sleep(10);
			}
			catch (Exception ex) {
				connected = false;
				ex.printStackTrace();
				// TODO socket closed/disconnected
			}
		}
		
		// TODO stuff when the socket closes, send a message?
		
		try {
			channel.close();
			socket.close();
		}
		catch (IOException | TimeoutException ex) {
			// socket close fail, who cares
		}
	}
	
	public Channel channel() {
		return channel;
	}
	
	public PacketType state() {
		return state;
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
