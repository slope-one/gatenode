package one.slope.gatenode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import one.slope.slip.io.DataType;
import one.slope.slip.io.SuperBuffer;
import one.slope.slip.io.packet.Packet;
import one.slope.slip.io.packet.PacketDefinition;
import one.slope.slip.io.packet.PacketDefinitionProvider;
import one.slope.slip.io.packet.PacketSize;
import one.slope.slip.io.packet.PacketType;

public class Client implements Runnable {
	public static final int BUFFER_SIZE = 1024 * 10; // TODO move to configuration - 10kb to start with

	// TODO queued messages for writing (after the encoders have run - independent of this class)
	
	private final PacketDefinitionProvider provider;
	private final SuperBuffer buffer = new SuperBuffer(BUFFER_SIZE);
	private final OutputStream output;
	private final InputStream input;
	private final Socket socket;
	
	private PacketType state = PacketType.NEGOTIATION;
	
	// threads and IO are easier, spawn more nodes = more power, let's keep it simple.
	public Client(Socket socket, PacketDefinitionProvider provider) throws IOException {
		this.socket = socket;
		this.output = socket.getOutputStream();
		this.input = socket.getInputStream();
		this.provider = provider;
	}

	@Override
	public void run() {
		boolean connected = true;
		
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
							Packet packet = new Packet(def, buf);
							packet.decode();
							
							// print some information about the packet for debug purposes
							System.out.println("Read packet \"" + def.name() + "\" - ID " + def.id() + ", SIZE " + def.size() + ", LENGTH " + def.length() + "");
							System.out.println(packet.values());
							
							// TODO pass off to the message queue
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
			socket.close();
		}
		catch (IOException ex) {
			// socket close fail, who cares
		}
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
