package org.android.filesystem.task;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class MknodTask extends Task {

	public MknodTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		File parent = file.getParentFile();
		boolean existed = file.exists();
		do{
			if(_packet.getDataSize() < 4){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			if(!parent.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(!parent.canWrite()){
				_packet.setCode(Errno.EACCES);
			}
			
			if(file.exists()){
				_packet.setCode(Errno.EEXIST);
				break;
			}
			
			try {
				if(file.createNewFile()){
					_packet.setCode(Errno.SUCCESS);
				}else{
					_packet.setCode(Errno.ENOSPC);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				_packet.setCode(Errno.ENOSPC);
				break;
			}
			
			ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int mode = buffer.getInt();
			if((mode & 0444) != 0){
				file.setReadable(true);
			}
			if((mode & 0222) != 0){
				file.setWritable(true);
			}
			if((mode & 0111) != 0){
				file.setWritable(true);
			}
			
		}while(false);
		
		if(_packet.getCode() != Errno.SUCCESS && !existed && file.exists()){
			file.delete();
		}
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}
}
