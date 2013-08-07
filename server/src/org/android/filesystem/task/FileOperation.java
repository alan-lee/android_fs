package org.android.filesystem.task;

public enum FileOperation {
	READDIR((byte)10),
	MKDIR((byte)11),
	MKNOD((byte)12),
	GETATTR((byte)13),
	ACCESS((byte)14),
	RMDIR((byte)15),
	RENAME((byte)16),
	OPEN((byte)17),
	READ((byte)18),
	WRITE((byte)19),
	RELEASE((byte)20),
	CREATE((byte)21),
	UNLINK((byte)22),
	TRUNCATE((byte)23),
	UTIMENS((byte)24);

	FileOperation(byte op){
		this._op = op;
	}
	
	private Byte _op;

	public Byte getOp() {
		return _op;
	}
	
	
}
