package com.audioclean;

import java.io.IOException;
import java.util.ArrayList;
import java.text.DecimalFormat;

import android.util.Log;

import com.CLApp.ExtAudioRecorder;
import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;

public class CleaningAlgorithm{
	static float INF=Float.MAX_VALUE;
	public static boolean WINDOW=false;
	public static String fileName;				//name for the saves
	public static short[] amplitude;		//amplitudes in short values
	public static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	public static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	static float[][] hToPlotW;		//matrix with the h value (windows mode)
	static final int offsetXCorr=10;
	//Computes the error, one window at time
	/*public static void errorTrackWindow(ArrayList<float[][]> tracks, float[][] finalTrack, int[] bestCombo){
		Wave render;
		//GraphicRender r=new GraphicRender();
		//File remove;
		int windows=tracks.get(0).length;
		int winLen=tracks.get(0)[0].length;
		int lastWin=tracks.get(0)[windows-1].length;
		float[][] temp=new float[windows][];
		int multiplier=0;
	
		WaveManipulation.save("PrecomparisonFinal.wav",WaveManipulation.convertFloatsToDoubles(AlgorithmWindows.mergingWindows(finalTrack)));
		render=new Wave("PrecomparisonFinal.wav");
		r.renderWaveform(render, "PrecomparisonFinal.wav.jpg");
		//remove=new File("errore.wav");
		//remove.delete();
		render=null;
		
		for(int i=0;i<tracks.size();i++){
			WaveManipulation.save("PrecomparisonTrack"+i+".wav",WaveManipulation.convertFloatsToDoubles(AlgorithmWindows.mergingWindows(tracks.get(i))));
			render=new Wave("PrecomparisonTrack"+i+".wav");
			r.renderWaveform(render, "PrecomparisonTrack"+i+".wav.jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
			
			for(int j=0;j<windows;j++){
				double rms_final = Statistical.avg_mod(finalTrack[j]);
				double rms_track_i = Statistical.avg_mod(tracks.get(i)[j]);
				temp[j]=new float[tracks.get(i)[j].length];
				for(int z=0;z<tracks.get(i)[j].length;z++){
					//temp[multiplier+z]=(((bestCombo[j]+1)*tracks.get(i)[j][z])-finalTrack[j][z])/(bestCombo[j]+1);
					temp[j][z] =(float) (tracks.get(i)[j][z] * rms_final /rms_track_i - finalTrack[j][z]);
				}
				//multiplier+=winLen;
			}
			WaveManipulation.save(i+"-errorWind.wav",WaveManipulation.convertFloatsToDoubles(AlgorithmWindows.mergingWindows(temp)));
			render=new Wave(i+"-errorWind.wav");
			
			r.renderWaveform(render, name+(i)+"error-file-window"+windows+".jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
			multiplier=0;
			temp=new float[windows][];
		}
	}*/
	
	public static byte[] cleaner(ArrayList<byte[]> data, String name) throws IOException{
		fileName=name;
		Wave w=new Wave(name);
		WaveHeader wh=w.getWaveHeader();
		int[] index=new int[2];
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		fileName=name;
		
		// Input verification
		if(data.size()<=1){
			//System.err.println("Usage: java -jar cleaningAlgorithm.jar <head file output name> <windows enable (true or false)> <offset crosscorrelation> <path audiofile (min 2)>");
			return data.get(0);
		}						
		offset=offsetXCorr;		// Reading cross correlation offset 
		Syncing.stamp=false;
		Log.i("cleaning", "number of tracks: "+data.size());
		//GraphicRender r=new GraphicRender();
		// creating the arrays to contain the tracks 
		for(int i=0;i<data.size();i++){
			amplitude=getShortsArray(data.get(i));
			WaveManipulation.getNormalizedAmplitudes(wh.getBitsPerSample());
		}
		normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		index=Syncing.selectionSyncronization(offset);	
		return AlgorithmWindows.algorithm(index[0],index[1]);
	}

	private static short[] getShortsArray(byte[] bs) {
		short[] result=new short[bs.length/2];
		for(int i=0,j=0;i<bs.length && j<result.length;i=i+2,j++){
			result[j]=ExtAudioRecorder.getShort(bs[i], bs[i+1]);
		}
		return result;
	}
}