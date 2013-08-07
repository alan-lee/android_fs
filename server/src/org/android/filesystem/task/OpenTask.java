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

public class OpenTask extends Task {
	private static final int O_ACCMODE = 0003;
	private static final int O_RDONLY = 00;
	private static final int O_WRONLY = 01;
	private static final int O_RDWR = 02;
	private static final int O_CREAT = 0100;	/* not fcntl */
	private static final int O_EXCL = 0200;	/* not fcntl */
	private static final int O_TRUNC = 01000;	/* not fcntl */
	private static final int O_APPEND = 02000;
	private static final int O_DIRECT = 040000;
	private static final int O_DIRECTORY = 0200000;
	
	public OpenTask(RFSPacket packet, SelectionKey key, PacketSender sender) {
		super(packet, key, sender);
	}

	@Override
	public void run() {
		String rootDir = GlobalSetting.getRootDir();
		String path = _packet.getPath();
		
		File file = new File(rootDir, path);
		do{
			if(_packet.getDataSize() < 4){
				_packet.setCode(Errno.EINVAL);
				break;
			}
			
			ByteBuffer buffer = ByteBuffer.wrap(_packet.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int flags = buffer.getInt();
			
			if((flags & O_CREAT) != 0){
				try {
					if(file.createNewFile()){
						_packet.setCode(Errno.SUCCESS);
					}else{
						_packet.setCode(Errno.EIO);
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
					_packet.setCode(Errno.EIO);
					break;
				}
				file.setReadable(true);		
				file.setWritable(true);		
				file.setWritable(true);
			}else if(!file.exists()){
				_packet.setCode(Errno.ENOENT);
				break;
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

			if((flags & O_CREAT & O_EXCL) == (O_CREAT | O_EXCL) && file.exists()){
				_packet.setCode(Errno.EEXIST);
				break;
			}
			
			if((flags & O_CREAT) != 0 && !file.getParentFile().exists()){
				_packet.setCode(Errno.EEXIST);
				break;
			}
			
			//File Open OK...
			String mode = "r";
			if(accessMode == O_RDONLY){
				mode = "r";
			}else if((flags | O_DIRECT) != 0){	
				mode = "rwd";
			}else{
				mode = "rw";
			}

			try {
					FileAccess fileAccess = new FileAccess(file, mode);
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
		
		if(_sender != null){
			_sender.send(_key, _packet);
		}
	}
}
