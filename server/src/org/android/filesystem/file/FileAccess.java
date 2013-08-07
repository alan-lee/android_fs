package org.android.filesystem.file;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileAccess extends RandomAccessFile {
	private static Map<String, FileLocker> fileLocks;
	private static Map<Long, FileAccess> fileAccessers;
	
	private String path;
	
	static{
		fileLocks = new ConcurrentHashMap<String, FileLocker>();
		fileAccessers = new ConcurrentHashMap<Long, FileAccess>();
	}
	
	public FileLocker getLocker(){
		synchronized(fileLocks){
			FileLocker locker = fileLocks.get(path);
			if(locker == null){
				locker =  new FileLocker();
				fileLocks.put(path, locker);
			}
			locker.incRef();
			return locker;
		}
	}
	
	public static FileAccess getFileAccess(long fd){
		return fileAccessers.get(fd);
	}
	
	public FileAccess(File file, String mode) throws FileNotFoundException {
		super(file, mode);
		fileAccessers.put(this.getIntFD(), this);
		path = file.getPath();
	}
	
	@Override
	public void close()throws IOException{
		fileAccessers.remove(this.getIntFD());
		
		synchronized(fileLocks){
			FileLocker locker = fileLocks.get(path);
			if(locker != null){
				int ref = locker.decRef();
				if(ref == 0){
					fileLocks.remove(path);
				}
			}
		}
		
		super.close();
	}
	
	public long getIntFD(){
		//use some trick to get file descriptor, 
		//for windows, it use handle, which is a long value
		//the long value will be truncated as int.
		
		try {
			FileDescriptor fd = this.getFD();
			
			//for Unix like system, java use fd filed
			Field field = FileDescriptor.class.getDeclaredField("fd");
			field.setAccessible(true);
			long fdValue = (long)field.getInt(fd);
			
			if(fdValue  < 0){
				//invalid, for windows system, java use handle field
				field = FileDescriptor.class.getDeclaredField("handle");
				field.setAccessible(true);
				
				fdValue = field.getLong(fd);
			}
			
			return fdValue;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}

}
