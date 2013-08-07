package org.android.filesystem.server;

public class GlobalSetting {
	private static String _rootDir;

	public static String getRootDir() {
		return _rootDir;
	}

	public static void setRootDir(String rootDir) {
		GlobalSetting._rootDir = rootDir;
	}
	
}
