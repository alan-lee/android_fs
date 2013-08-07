package org.android.filesystem.task;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

import org.android.filesystem.file.FileAccess;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class CreateTask extends Task {
	private static final int O_ACCMODE = 0003;
	private static final int O_RDONLY = 00;
	private static final int O_WRONLY = 01;
	private static final int O_RDWR = 02;
	private static final int O_TRUNC = 01000;	
	private static final int O_APPEND = 02000;
	private static final int O_DIRECT = 040000;
	private static final int O_DIRECTORY = 0200000;
	
	public CreateTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
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
			if(!parent.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
			}
			
			if(_packet.getDataSize() < 8){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int mode = buffer.getInt();
			int flags = buffer.getInt();
			
			if(!file.exists()){
				if(!parent.canWrite()){
					_packet.setCode(Errno.EACCES);
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
				
				if((mode & 0444) != 0){
					file.setReadable(true);
				}
				if((mode & 0222) != 0){
					file.setWritable(true);
				}
				if((mode & 0111) != 0){
					file.setWritable(true);
				}
			}

			int accessMode = (flags & O_ACCMODE);
			if(accessMode == O_RDONLY && !file.canRead() || 
				accessMode == O_WRONLY && !file.canWrite()||
				accessMode == O_RDWR && !(file.canRead() && file.canWrite())){
				_packet.setCode(Errno.EACCES);
				break;
			}
						
			if(accessMode > O_RDONLY && file.isDirectory()){
				_packet.setCode(Errno.EISDIR);
				break;
			}
			
			if((flags & O_DIRECTORY) != 0 && !file.isDirectory()){
				_packet.setCode(Errno.ENOTDIR);
				break;
			}
			
			//File Open OK...
			String fileMode = "r";
			if(accessMode == O_RDONLY){
				fileMode = "r";
			}else if((flags | O_DIRECT) != 0){	
				fileMode = "rwd";
			}else{
				fileMode = "rw";
			}

			try {
					FileAccess fileAccess = new FileAccess(file, fileMode);
					if((flags & O_TRUNC) != 0 && accessMode >  O_RDONLY && file.exists()){
						fileAccess.setLength(0);
						fileAccess.getChannel().force(false);
					}
				if((flags & O_APPEND) != 0){
					fileAccess.seek(fileAccess.length());
				}
				_packet.setTransactionId(fileAccess.getIntFD());
			} catch (Exception e) {
				e.printStackTrace();
				_packet.setCode(Errno.EIO);
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
