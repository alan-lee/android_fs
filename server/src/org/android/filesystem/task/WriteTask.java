package org.android.filesystem.task;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.android.filesystem.file.FileAccess;
import org.android.filesystem.file.FileLocker;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.util.Errno;

public class WriteTask extends Task {

	public WriteTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
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
			
			if(_packet.getDataSize() <= 0  || _packet.getData() == null){
				_packet.setCode(Errno.EFAULT);
				break;
			}
			
			FileLocker locker = access.getLocker();
			try {
				synchronized(locker){
					access.seek(_packet.getOffset());
					access.write(_packet.getData(), 0, _packet.getSize());
				}
			} catch (IOException e) {
				e.printStackTrace();
				_packet.setCode(Errno.EIO);
			}
		}while(false);
		
		//reduce send bytes.
		_packet.setDataSize(0);
		_packet.setData(null); 
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}

}
