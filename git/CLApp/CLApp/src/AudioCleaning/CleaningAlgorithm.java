package AudioCleaning;

import java.io.IOException;
import java.util.ArrayList;
import java.text.DecimalFormat;
import com.musicg.wave.Wave;
import com.musicg.graphic.*;

public class CleaningAlgorithm{
	static float INF=Float.MAX_VALUE;
	public static String name;				//name for the saves
	static boolean WINDOW=false;	//boolean for the choice between WindowedAlgorithm or simple Algorithm
	public static short[] amplitude;		//amplitudes in short values
	public static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	public static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	static float[][] hToPlotW;		//matrix with the h value (windows mode)
	//Computes the error, one window at time
	public static void errorTrackWindow(ArrayList<float[][]> tracks, float[][] finalTrack, int[] bestCombo){
		Wave render;
		GraphicRender r=new GraphicRender();
		//File remove;
		int windows=tracks.get(0).length;
		int winLen=tracks.get(0)[0].length;
		int lastWin=tracks.get(0)[windows-1].length;
		float[] temp=new float[windows*(winLen-1)+lastWin];
		int multiplier=0;
		
		for(int i=0;i<tracks.size();i++){
			for(int j=0;j<windows;j++){
				for(int z=0;z<tracks.get(i)[j].length;z++){
					temp[multiplier+z]=(((bestCombo[j]+1)*tracks.get(i)[j][z])-finalTrack[j][z])/(bestCombo[j]+1);
				}
				multiplier+=winLen;
			}
			WaveManipulation.save(i+"-errorWind.wav",WaveManipulation.convertFloatsToDoubles(temp));
			render=new Wave(i+"-errorWind.wav");
			
			r.renderWaveform(render, name+(i)+"error-file-window"+windows+".jpg");
			//remove=new File("errore.wav");
			//remove.delete();
			render=null;
			multiplier=0;
			temp=new float[windows*(winLen-1)+lastWin];
		}
	}
	
	public static void main(String[] args) throws IOException{
		Wave input;
		int waveHead, index;
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		// Input verification
		if(args.length<5){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <head file output name> <windows enable (true or false)> <offset crosscorrelation> <path audiofile (min 2)>");
			return;
		}
		name=args[0]; 							// Reading file name
		WINDOW=Boolean.parseBoolean(args[1]);	// Reading windows choice
		offset=Integer.parseInt(args[2]);		// Reading cross correlation offset 
		Syncing.stamp=false;
		System.out.println("Tracks: "+(args.length-3));
		GraphicRender r=new GraphicRender();
		// creating the arrays to contain the tracks 
		for(int i=3;i<args.length;i++){
			input=new  Wave(args[i]);
			r.renderWaveform(input, args[i]+".jpg");
			waveHead=input.getWaveHeader().getBitsPerSample();
			amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		index=Syncing.selectionSyncronization(offset);	
		// windowed algorithm
		if(WINDOW){
			AlgorithmWindows.algorithm(index);
			//windowedAlgorithm(index);
		}
		else{ // basic algorithm
			System.out.println("Windows deactivated.\n");
			Algorithm.algorithm(index);			
		}
	}
}