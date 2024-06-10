package com.example.SpringSecurity_Example;

public class toStringTest {
	 public static void main(String[] args) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuilder stringBuilder = new StringBuilder();
		
		new Thread(() -> {
		for(int i=0; i<10000; i++) {
			stringBuffer.append(1);
			stringBuilder.append(1);
		}
	}).start();
	
	new Thread(() -> {
		for(int i=0; i<10000; i++) {
			stringBuffer.append(1);
			stringBuilder.append(1);
		}
	}).start();
	
	new Thread(() -> {
		try {
			Thread.sleep(2000);
	
			System.out.println("StringBuffer.length: "+ stringBuffer.length()); // thread safe 함
			System.out.println("StringBuilder.length: "+ stringBuilder.length()); // thread unsafe 함
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}).start();
	}
}
