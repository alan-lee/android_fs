package org.android.filesystem.packet;

import java.nio.channels.SelectionKey;

public interface PacketSender {
	void send(SelectionKey key, RFSPacket packet);
}
