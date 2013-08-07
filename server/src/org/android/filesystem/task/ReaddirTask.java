package org.android.filesystem.task;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.android.filesystem.file.FileStat;
import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.server.GlobalSetting;
import org.android.filesystem.util.Errno;

public class ReaddirTask extends Task {
	public ReaddirTask(RFSPacket packet, SelectionKey key, PacketSender sender){
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
			
			if(!dir.isDirectory()){
				_packet.setCode(Errno.ENOTDIR);
				break;
			}
			
			if(!dir.canRead()){
				_packet.setCode(Errno.EACCES);
				break;
			}
			
			Map<String, FileStat> fileStats = new TreeMap<String, FileStat>();
			int bufferSize = 0;
			
			fileStats.put(".", new FileStat(dir));
			bufferSize += (1 + 1 + FileStat.FILE_STAT_SIZE);
			if(dir.equals(new File(rootDir))){
				fileStats.put("..", new FileStat(dir));
			}else{
				fileStats.put("..", new FileStat(dir.getParentFile()));
			}
			bufferSize += (2 + 1 + FileStat.FILE_STAT_SIZE);
			
			try {
				for(File file : dir.listFiles()){
					String fileName = file.getName();
					bufferSize += fileName.getBytes("UTF-8").length + 1 + FileStat.FILE_STAT_SIZE;//string terminator
					fileStats.put(fileName, new FileStat(file));
				}
	
				ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
				Iterator<String> it =  fileStats.keySet().iterator();
				while(it.hasNext()){
					String fileName = it.next();
					buffer.put(fileName.getBytes("UTF-8"));
					buffer.put((byte) 0); //'\0' ASCII code 
					buffer.put(fileStats.get(fileName).serialize());
				}
				
				buffer.flip();
				byte[] bytes = buffer.array();
				
				this._packet.setData(bytes);
				this._packet.setDataSize(bufferSize);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}while(false);
			
		if(_sender != null){
			_sender.send(_key, _packet);
		}
		
	}

}
