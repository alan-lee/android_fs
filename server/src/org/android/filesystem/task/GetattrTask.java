package org.android.filesystem.task;

import java.io.File;
import java.nio.channels.SelectionKey;

import org.android.filesystem.file.FileStat;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class GetattrTask extends Task {

	public GetattrTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
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
			
			if(!file.canRead()){
				_packet.setCode(Errno.EACCES);
				break;
			}
			
			FileStat stat = new FileStat(file);
			_packet.setDataSize(FileStat.FILE_STAT_SIZE);
			_packet.setData(stat.serialize());
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}

}
