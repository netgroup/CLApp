package it.uniroma2.clappdroidalpha;

import it.uniroma2.audioclean.CleaningAlgorithm;
import it.uniroma2.audioclean.WaveManipulation;
import it.uniroma2.audioclean.tools.Statistical;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import android.util.Log;

/**
 * Thread that manages the cleaning phase
 * @author Daniele De Angelis
 *
 */
public class CleanTask extends Thread {
	private String fileName; //file name
	private ArrayList<byte[]> data; //Data to clean
	private MainService st; //Service that manages the application
	public double RMSE; //Value of RMSE 
	public boolean forNorm; //Boolean that activate the normalization if it's needed
	
	/**
	 * Constructor
	 * @param data
	 * 		Data to clean
	 * @param name
	 * 		File name
	 * @param st
	 * 		Main service
	 */
	CleanTask(ArrayList<byte[]> data,String name, MainService st){
		this.data=data;
		fileName=name;
		RMSE=0;
		this.st=st;
		forNorm=false;
		this.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
	}
	
	@Override
	public void run() {
		try {
			if(!data.isEmpty()){
				//The cleaning algorithm is called
				float[] returned=CleaningAlgorithm.cleaner(this, data, fileName);
				st.cleaned.lock();
				//Normalization is applied if needed
				if(!st.ctrlNorm)
					st.ctrlNorm=forNorm;
				if(st.ctrlNorm){
					amplitudeNormalizationSingle(st, returned);
				}
				//Data are wrote into the output file
				RandomAccessFile output=st.ar.headerFile;
				byte[] data = WaveManipulation.fromFloatsToByte(returned);
				st.record.lock();
				output.write(data);
				st.record.unlock();
				//st.dataCleaned.add(returned);
				st.RMSE.add(RMSE);
				st.times.add(st.counter);
				st.counter++;
				st.cleaned.unlock();
				Log.i("cleaner","track returned");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	/**
	 * Function that computes the alpha value for the requested number of windows
	 * to keep in memory
	 * @param n
	 * 		Numbres of windows
	 * @return
	 * 		Alpha value
	 * 
	 */
	private static float alphaValue(int n){
		return (float) (2.0/(n+1.0));
	}

	/**
	 * Functions that execute the normalization
	 * @param st
	 * 		Service of the application
	 * @param f
	 * 		Data to normalize
	 */
	public static void amplitudeNormalizationSingle(MainService st, float[] f){
		float alpha=alphaValue(10);
		double amplitudeN=0.0, pwravgW=0.0;
		//Applying the moving exponential average
		if(Double.isInfinite(st.movingAverageOld)){
			st.movingAverageOld=Statistical.RMS(f);
			return;
		}
		else{
			pwravgW=Statistical.RMS(f);
			amplitudeN=alpha*(pwravgW)+(1-alpha)*st.movingAverageOld;
			for(int j=0;j<f.length;j++){
				f[j]*=amplitudeN/pwravgW;
			}
			st.movingAverageOld=amplitudeN;
		}
	}
	
	

}
