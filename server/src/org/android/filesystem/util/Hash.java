package org.android.filesystem.util;

public class Hash {
	public static long  BKDRHash(String s){
		long seed = 13131L;  // 31 131 1313 13131 131313 etc..   
        long hash = 0L;  
  
       for (int i = 0; i < s.length(); i++)  {  
          hash = (hash * seed) + s.charAt(i);  
       }  
  
       return  (hash &  0x7FFFFFFFFFFFFFFFL); 
	}
}
