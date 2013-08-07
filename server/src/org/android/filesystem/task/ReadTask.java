package org.android.filesystem.task;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.android.filesystem.file.FileAccess;
import org.android.filesystem.file.FileLocker;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class ReadTask extends Task {

	public ReadTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		
		do{
			if(file.isDirectory()){
				_packet.setCode(Errno.EISDIR);
				break;
			}
			
			long fd = _packet.getTransactionId();
			FileAccess access = FileAccess.getFileAccess(fd);
			if(access == null){
				_packet.setCode(Errno.EBADF);
				break;
			}
			
			
			FileLocker locker = access.getLocker();
			try {
				synchronized(locker){
					int size = _packet.getSize();
					_packet.setDataSize(size);
					_packet.setData(new byte[size]);
					
					if(_packet.getOffset() > access.length()){
//						_packet.setDataSize(1);
//						_packet.setData(new byte[]{-1});//EOF
						_packet.setDataSize(0);
						_packet.setData(null);
					}else{
						if(_packet.getOffset() + _packet.getSize() > access.length()){
							size = (int) (access.length() - _packet.getOffset()); 
//							_packet.setDataSize(size + 1);
//							_packet.getData()[size] = -1; //EOF
						}
						_packet.setDataSize(size);
						_packet.setData(new byte[size]);
						
						access.seek(_packet.getOffset());
						access.read(_packet.getData(), 0, size);
					}
						
		
				}
			} catch (IOException e) {
				e.printStackTrace();
				_packet.setCode(Errno.EIO);
			}
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
		
	}

}
