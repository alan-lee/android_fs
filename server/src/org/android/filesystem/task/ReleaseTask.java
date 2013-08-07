package org.android.filesystem.task;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.android.filesystem.file.FileAccess;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.util.Errno;

public class ReleaseTask extends Task {

	public ReleaseTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		
		do{
			long fd = _packet.getTransactionId();
			FileAccess access = FileAccess.getFileAccess(fd);
			if(access == null){
				_packet.setCode(Errno.EBADF);
				break;
			}
			
			try {
				access.close();
			} catch (IOException e) {
				_packet.setCode(Errno.EIO);
			}
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}

	}

}
