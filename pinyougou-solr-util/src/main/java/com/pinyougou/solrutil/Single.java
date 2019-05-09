package com.pinyougou.solrutil;

import java.util.HashMap;
import java.util.Hashtable;

public class Single {
	
	private Single(){
		
	}
	
	public static Single single = new Single();
	
	public static synchronized Single getInstance(){
		if(single == null){
			return new Single();
		}
		
		return single;
	}
	
	public static void main(String[] args) {
		Integer a = 200;
		Integer b = 200;
		
		System.out.println(a == b);
		
		new HashMap<String, Object>();
	}
	
}
