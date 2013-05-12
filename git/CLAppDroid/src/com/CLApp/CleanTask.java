package com.CLApp;

import java.io.IOException;
import java.util.ArrayList;
import com.audioclean.CleaningAlgorithm;

public class CleanTask extends Thread {
	String fileName;
	ArrayList<byte[]> data;
	
	CleanTask(ArrayList<byte[]> data,String name){
		this.data=data;
		fileName=name;
	}
	
	@Override
	public void run() {
		try {
			byte[] returned=CleaningAlgorithm.cleaner(data, fileName);
			MainActivity.addToFinalTrack(returned);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
