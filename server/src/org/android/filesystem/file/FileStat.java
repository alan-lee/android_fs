package org.android.filesystem.file;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.android.filesystem.util.Hash;

public class FileStat {
	public static final Integer FILE_STAT_SIZE = 60;
	
	public int uid() {
		return _uid;
	}
	public int gid() {
		return _gid;
	}
	public long ino() {
		return _ino;
	}
	public int mode() {
		return _mode;
	}
	public long size() {
		return _size;
	}
	public long nlink() {
		return _nlink;
	}
	public long atime() {
		return _atime;
	}
	public long ctime() {
		return _ctime;
	}
	public long mtime() {
		return _mtime;
	}
	private int _uid;
	private int _gid;
	private long _ino;
	private int _mode;
	private long _size;
	private long _nlink;
	private long _atime;
	private long _ctime;
	private long _mtime;
	
	public FileStat(File file){
		this._uid = this._gid = 0;
		this._nlink = 0L;
		this._size = file.length();
		this._ctime = file.lastModified();
		this._atime = file.lastModified();
		this._mtime = file.lastModified();
		this._ino = Hash.BKDRHash(file.getPath());
		this._mode = 0;
		if(file.isFile()){
			this._mode |= 0100000;
		}else if(file.isDirectory()){
			this._mode |= 040000;
		}
		if(file.canRead()){
			this._mode |= 0444;
		}
		if(file.canWrite()){
			this._mode |= 0222;
		}
		if(!file.isDirectory() && file.canExecute()){
			this._mode |= 0111;
		}
	}
	
	public byte[] serialize(){
		ByteBuffer buff = ByteBuffer.allocate(60);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		buff.putInt(_uid);
		buff.putInt(_gid);
		buff.putLong(_ino);
		buff.putInt(_mode);
		buff.putLong(_size);
		buff.putLong(_nlink);
		buff.putLong(_ctime);
		buff.putLong(_atime);
		buff.putLong(_mtime);
		
		buff.flip();
		byte[] bytes = buff.array();
		return bytes;
	}
}
