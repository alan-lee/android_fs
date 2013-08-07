package org.android.filesystem.task;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class UtimensTask extends Task{

	public UtimensTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		
		do{
			if(_packet.getDataSize() < 8){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			if(!file.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(!file.canWrite()){
				_packet.setCode(Errno.EACCES);
				break;
			}
			
			ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			long time = buffer.getLong();
			
			file.setLastModified(time);
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
		
	}
}
