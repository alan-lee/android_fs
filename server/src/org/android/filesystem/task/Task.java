package org.android.filesystem.task;

import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;

public abstract class Task implements Runnable{
	protected RFSPacket _packet;
	protected SelectionKey _key;
	protected PacketSender _sender;
	
	public Task(RFSPacket packet, SelectionKey key, PacketSender sender) {
		this._packet = packet;
		this._key = key;
		this._sender = sender;
	}
}
