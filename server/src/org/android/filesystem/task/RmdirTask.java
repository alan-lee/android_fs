package org.android.filesystem.task;

import java.io.File;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class RmdirTask extends Task {

	public RmdirTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File dir = new File(rootDir, path);
		do{
			if(!dir.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(!dir.getParentFile().canWrite()){
				_packet.setCode(Errno.EACCES);
			}
			
			if(!dir.isDirectory()){
				_packet.setCode(Errno.ENOTDIR);
				break;
			}
			
			if(dir.listFiles().length > 0){
				_packet.setCode(Errno.EEXIST);
				break;
			}
			
			if(dir.delete()){
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
