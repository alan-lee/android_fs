package org.android.filesystem.task;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class RenameTask extends Task {

	public RenameTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		
		do{
			if(_packet.getDataSize() <= 0){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
			String newPath = null;
			try {
				newPath = new String(buffer.array(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				_packet.setCode(Errno.EINVAL);
				break;
			}
			File newFile = new File(rootDir, newPath);
			
			if(!file.exists() || !newFile.getParentFile().exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(!file.getParentFile().canWrite() || !newFile.getParentFile().canWrite()){
				_packet.setCode(Errno.EACCES);
				break;
			}
			
			if(newFile.getParent().contains(file.getPath())){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			if(newFile.exists()){
				if(!file.isDirectory() && newFile.isDirectory()){
					_packet.setCode(Errno.EISDIR);
					break;
				}
				
				if(file.isDirectory() && !newFile.isDirectory()){
					_packet.setCode(Errno.ENOTDIR);
					break;
				}
				
				if(newFile.isDirectory() && newFile.listFiles().length > 0){
					_packet.setCode(Errno.EEXIST);
					break;
				}
				File tmp = new File(newFile.getPath() + ".tmp");
				
				newFile.renameTo(tmp);
			}
			
			if(file.renameTo(newFile)){
				File tmp = new File(newFile.getPath() + ".tmp");
				if(tmp.exists()){
					tmp.delete();
				}
				_packet.setCode(Errno.SUCCESS);
			}else{
				File tmp = new File(newFile.getPath() + ".tmp");
				if(tmp.exists()){
					tmp.renameTo(newFile);
				}
				_packet.setCode(Errno.EIO);
			}
		}while(false);
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}

}
