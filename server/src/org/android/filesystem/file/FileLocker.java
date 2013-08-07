package org.android.filesystem.file;

import java.util.concurrent.atomic.AtomicInteger;

public class FileLocker {
	private AtomicInteger _ref;
	
	public FileLocker(){
		_ref = new AtomicInteger(0);
	}
	
	public int incRef(){
		return _ref.incrementAndGet();
	}
	
	public int decRef(){
		return _ref.decrementAndGet();
	} 
}
