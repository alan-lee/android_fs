package org.android.filesystem.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Field;

public class FileUtils {
	public static int getFD(File file){
		//use some trick to get file descriptor, 
		//for windows, it use handle, which is a long value
		//the long value will be truncated as int.
		
		try {
			FileInputStream fis = new FileInputStream(file);
			FileDescriptor fd = fis.getFD();
			
			//for Unix like system, java use fd filed
			Field field = FileDescriptor.class.getDeclaredField("fd");
			field.setAccessible(true);
			int fdValue = field.getInt(fd);
			
			if(fdValue  < 0){
				//invalid, for windows system, java use handle field
				field = FileDescriptor.class.getDeclaredField("handle");
				field.setAccessible(true);
				
				fdValue = (int)field.getLong(fd);
			}
			
			return fdValue;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}
}
