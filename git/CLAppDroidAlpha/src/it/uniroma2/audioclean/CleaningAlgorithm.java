package it.uniroma2.audioclean;

import it.uniroma2.clappdroidalpha.CleanTask;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;

/**
 * Class containing the first function to call for the cleaning
 * @author Daniele De Angelis
 *
 */
public class CleaningAlgorithm{
	static float INF=Float.MAX_VALUE;
	public static boolean WINDOW=true;
	public static String fileName;				//name for the saves
	public static short[] amplitude;		//amplitudes in short values
	public static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	public static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	static float[][] hToPlotW;		//matrix with the h value (windows mode)
	static final int offsetXCorr=1000;
	static int SAMPLE_RATE=44100;
	private static WaveHeader wh;
	
	/**
	 * First part of the cleaning algorithm. It does the preparation and the synchronization
	 * @param ct
	 * 		Caller thread
	 * @param data
	 * 		Data
	 * @param name
	 * 		File name
	 * @return
	 * 		An array with the final data
	 * @throws IOException
	 */
	public static float[] cleaner(CleanTask ct, ArrayList<byte[]> data, String name) throws IOException{
		fileName=name;
		Wave w=new Wave(fileName);
		wh=w.getWaveHeader();
		SAMPLE_RATE=wh.getSampleRate();
		int[] index=new int[2];
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		// Input verification
		if(data.size()<=1){
			//If we have only one array of byte then we have only the data from the recorder
			amplitude=getShortsArray(data.get(0));
			WaveManipulation.getNormalizedAmplitudes(wh.getBitsPerSample());
			return normalizedAmplitudes.get(0);
		}				
		ct.forNorm=true;
		offset=offsetXCorr;		// Reading cross correlation offset 
		Syncing.stamp=false;
		Log.i("cleaning", "number of tracks: "+data.size());
		// creating the arrays to contain the tracks 
		for(int i=0;i<data.size();i++){
			amplitude=getShortsArray(data.get(i));
			WaveManipulation.getNormalizedAmplitudes(wh.getBitsPerSample());
		}
		//Preration and synchronization phase
		normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		index=Syncing.selectionSyncronization(offset);	
		//Calling the function that do the next phases
		return AlgorithmWindows.algorithm(ct,index[0],index[1]);
	}

	/**
	 * Converts an array of bytes into an array of shorts
	 * @param data
	 * 		Array of bytes
	 * @return
	 * 		Conversion of the bytes into shorts
	 */
	public static short[] getShortsArray(byte[] data){
		int bytePerSample = wh.getBitsPerSample() / 8;
		int numSamples = data.length / bytePerSample;
		short[] amplitudes = new short[numSamples];
		
		int pointer = 0;
		for (int i = 0; i < numSamples; i++) {
			short amplitude = 0;
			for (int byteNumber = 0; byteNumber < bytePerSample; byteNumber++) {
				// little endian
				amplitude |= (short) ((data[pointer++] & 0xFF) << (byteNumber * 8));
			}
			amplitudes[i] = amplitude;
		}
		
		return amplitudes;
	}
}