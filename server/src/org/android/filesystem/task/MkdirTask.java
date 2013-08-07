package org.android.filesystem.task;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class MkdirTask extends Task {

	public MkdirTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File dir = new File(rootDir, path);
		File parent = dir.getParentFile();
		do{
			if(!parent.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(!parent.canWrite()){
				_packet.setCode(Errno.EACCES);
			}
			
			if(dir.exists()){
				_packet.setCode(Errno.EEXIST);
				break;
			}
			
			if(_packet.getDataSize() >= 4){
				ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				int mode = buffer.getInt();
				
				if(dir.mkdir()){
					_packet.setCode(Errno.SUCCESS);
				}else{
					_packet.setCode(Errno.ENOSPC);
					break;
				}
			
				if((mode & 0444) != 0){
					dir.setReadable(true);
				}
				if((mode & 0222) != 0){
					dir.setWritable(true);
				}
				if((mode & 0111) != 0){
					dir.setExecutable(true);
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
