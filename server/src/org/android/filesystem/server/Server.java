package org.android.filesystem.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;

public class Server implements Runnable, PacketSender{
	private int _port;
	private Selector _selector;
	private TaskDispatcher _dispatcher;
	private boolean _running;
	private Queue<SelectionKey> _writingQueue;
	private Thread _thread;
	
	public Server(int port){
		_port = port;
		_writingQueue = new LinkedBlockingQueue<SelectionKey>();
		_dispatcher = TaskDispatcher.getInstance();
		_dispatcher.setSender(this);
	}
	
	public void start() throws IOException{
		_dispatcher.startup();
		_selector = SelectorProvider.provider().openSelector();
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		SocketAddress local = new InetSocketAddress(_port);
		channel.socket().bind(local);
		channel.register(_selector, SelectionKey.OP_ACCEPT);
		
		_running = true;
		_thread = new Thread(this);
		_thread.start();
	}
	
	public void stop() throws IOException, InterruptedException{
		_running = false;
		if(_thread != null){
			_thread.interrupt();
			_thread.join(2 * 60 * 1000);
		}
		_selector.close();
		_dispatcher.shutdown();
	}
	
	@Override
	public void send(SelectionKey key, RFSPacket packet){
		if(key != null){
			key.attach(packet);
			synchronized(_writingQueue){
				_writingQueue.offer(key);
			}
			_selector.wakeup();
		}
	}
	
	@Override
	public void run() {
		ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
		ByteBuffer packetBuffer = ByteBuffer.allocate(0x8FFF);
		sizeBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		while(_running){
			try {
				_selector.select();
				Iterator<SelectionKey> it = _selector.selectedKeys().iterator();
				
				while(it.hasNext()){
					SelectionKey key = it.next();
					it.remove();
					try{
						if(key.isAcceptable()){
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
					        SocketChannel socketChannel = serverSocketChannel.accept();
					        Socket socket = socketChannel.socket();
					        System.out.println(socket.getInetAddress().getHostAddress() + " is connected.");
					        socketChannel.configureBlocking(false);
					        socket.setKeepAlive(true);
					        socketChannel.register(key.selector(), SelectionKey.OP_READ);
						}else if(key.isReadable()){
							SocketChannel channel = (SocketChannel) key.channel();
							sizeBuffer.clear();
							channel.read(sizeBuffer);
							sizeBuffer.flip();
							int packetSize = sizeBuffer.getInt();
														
							packetBuffer.clear();
							int readBytes = 0;
							while(readBytes < packetSize){
								readBytes += channel.read(packetBuffer);
							}
							packetBuffer.flip();		
							byte[] bytes = packetBuffer.array();
							
							RFSPacket request = RFSPacket.marshal(bytes, 0, packetSize);
							System.out.println("==>" + request.toString());
							_dispatcher.dispatch(key, request);
						}else if(key.isWritable()){
							SocketChannel channel = (SocketChannel) key.channel();
							
							RFSPacket packet = (RFSPacket) key.attachment();
							System.out.println("<==" + packet.toString());
							int packetSize = packet.getPacketSize();
							ByteBuffer buffer = ByteBuffer.allocate(packetSize + 4);
							buffer.order(ByteOrder.LITTLE_ENDIAN);
							buffer.clear();
							buffer.putInt(packetSize);
							buffer.put(RFSPacket.serialize(packet));
							buffer.flip();
							channel.write(buffer);
							channel.register(_selector, SelectionKey.OP_READ);
						}else if(key.isConnectable()){
							
						}else{
							
						}
						
						continue;
					}catch(Exception e){
						e.printStackTrace();
						if (key != null) {
				            SocketChannel channel = (SocketChannel)key.channel();
				            key.cancel();
				            try {
				                if (channel != null) {
				                    channel.close();
				                }
				            } catch (IOException ignore) {
				            }
				        }
					}
				}
				
				//wake up for writing
				synchronized(_writingQueue){
					SelectionKey key = _writingQueue.poll();
					while(key != null){
						key.interestOps(SelectionKey.OP_WRITE);
						key = _writingQueue.poll();
					}
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}

	public static void main(String[] args)
	{
		GlobalSetting.setRootDir("D:\\project");
		Server server = new Server(8888);
		try {
			server.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				server.stop();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}
