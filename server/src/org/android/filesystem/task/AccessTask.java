package org.android.filesystem.task;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class AccessTask extends Task {

	public AccessTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		
		do{
			if(!file.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			
			if(_packet.getDataSize() >= 4){
				ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				int mode = buffer.getInt();
				if(mode != 0){
					if((mode & 1) != 0 && !file.canExecute()){
						_packet.setCode(Errno.EACCES);
						break;
					}else if((mode & 2) != 0 && !file.canRead()){
						_packet.setCode(Errno.EACCES);
						break;
					}else if((mode & 4) != 0 && !file.canWrite()){
						_packet.setCode(Errno.EROFS);
						break;
					}else{
						_packet.setCode(Errno.SUCCESS);
					}
				}else{
					//test F_OK
					_packet.setCode(Errno.SUCCESS);
				}
			}else{
				_packet.setCode(Errno.EINVAL);
			}
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}

}
