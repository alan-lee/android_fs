package org.android.filesystem.packet;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RFSPacket{
	public static final int FIX_PART_SIZE = 26;
	
	private byte 	op;
	private byte	code;
	private long 	transactionId;
	private int  	offset;
	private int		size;
	private int		pathSize;
	private String	path;
	private int		dataSize;
	private byte[]  data;
	
	public byte getOp() {
		return op;
	}

	public void setOp(byte op) {
		this.op = op;
	}

	public byte getCode() {
		return code;
	}

	public void setCode(byte code) {
		this.code = code;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public int getPathSize() {
		return pathSize;
	}

	public void setPathSize(int pathSize) {
		this.pathSize = pathSize;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getDataSize() {
		return dataSize;
	}


	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}


	public byte[] getData() {
		return data;
	}


	public void setData(byte[] data) {
		this.data = data;
	}
	
	public int getPacketSize(){
		return FIX_PART_SIZE + this.pathSize + this.dataSize;
	}

	public static byte[] serialize(RFSPacket packet) {
		ByteBuffer buffer =ByteBuffer.allocate(packet.getPacketSize());
		buffer.clear();
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(packet.op);
		buffer.put(packet.code);
		buffer.putLong(packet.transactionId);
		buffer.putInt(packet.offset);
		buffer.putInt(packet.size);
		buffer.putInt(packet.pathSize);
		if(packet.pathSize > 0){
			try {
				buffer.put(packet.path.getBytes("UTF-8"), 0, packet.pathSize);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		buffer.putInt(packet.dataSize);
		if(packet.dataSize > 0){
			buffer.put(packet.data, 0, packet.dataSize);
		}
		
		return buffer.array();
	}

	public static RFSPacket marshal(byte[] bytes, int offset, int length) {
			ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
			return marshal(buffer);
		
	}
	
	public static RFSPacket marshal(ByteBuffer buffer) {
		RFSPacket packet = null;
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		if(buffer != null){
	        	packet = new RFSPacket();
	        	packet.op = buffer.get();
	        	packet.code = buffer.get();
	        	packet.transactionId = buffer.getLong();
	        	packet.offset = buffer.getInt();
	        	packet.size = buffer.getInt();
	        	packet.pathSize = buffer.getInt();
				if(packet.pathSize > 0){
					byte[] bytes = new byte[packet.pathSize];
					buffer.get(bytes);
					try {
						packet.path = new String(bytes, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				packet.dataSize = buffer.getInt();
				if(packet.pathSize > 0){
					packet.data = new byte[packet.dataSize];
					buffer.get(packet.data);
				}
		}
		
		return packet;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("op = ").append(this.op).append(",");
		buffer.append("code = ").append(this.code).append(",");
		buffer.append("transactionId = ").append(this.transactionId).append(",");
		buffer.append("offset = ").append(this.offset).append(",");
		buffer.append("size = ").append(this.size).append(",");
		buffer.append("filePath = ").append(this.path).append(",");
		buffer.append("dataSize = ").append(this.dataSize).append(".");
		//buffer.append("data = [ ");
		//for(int i = 0; i < dataSize; i++){
		//	buffer.append(String.format("%02x ", data[i]));
		//}
		buffer.append("]");
		return buffer.toString();
	}
}
