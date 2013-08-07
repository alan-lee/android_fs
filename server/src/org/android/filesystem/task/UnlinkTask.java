package org.android.filesystem.task;

import java.io.File;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class UnlinkTask extends Task {

	public UnlinkTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
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
			
			if(!file.getParentFile().canWrite()){
				_packet.setCode(Errno.EACCES);
			}
			
			if(file.isDirectory()){
				_packet.setCode(Errno.EISDIR);
				break;
			}
			
			if(file.delete()){
				_packet.setCode(Errno.SUCCESS);
			}else{
				_packet.setCode(Errno.ENOSPC);
				break;
			}
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}

	}

}
