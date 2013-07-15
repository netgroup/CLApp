package AudioCleaning;

import java.io.IOException;
import java.util.ArrayList;
import com.musicg.wave.Wave;
import com.musicg.graphic.*;


/**
 * Starting point of the algorithm
 * @author Daniele De Angelis
 *
 */
public class CleaningAlgorithm{
	protected static String name;						//name for the saves
	protected static boolean WINDOW=true;			//boolean for the choice between WindowedAlgorithm or simple Algorithm
	protected static short[] amplitude;				//amplitudes in short values
	protected static ArrayList<float[]> normalizedAmplitudes; //amplitude in float values
	public static ArrayList<float[]> amplitudeReady;		 //amplitude in float values with same lenght
	
	/**
	 * Starting function of the program
	 * @param args
	 * 		Array of the input strings
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		Wave input;
		int waveHead;
		int[] index=new int[2];
		
		normalizedAmplitudes=new ArrayList<float[]>();
		amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		// Input verification
		if(args.length<6){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <head file output name> <windows enable (true or false)> <windows divider value> <offset crosscorrelation> <path audiofile (min 2)>");
			return;
		}
		name=args[0]; 							// Reading file name
		WINDOW=Boolean.parseBoolean(args[1]);	// Reading windows choice
		offset=Integer.parseInt(args[3]);		// Reading cross correlation offset 
		AlgorithmWindows.setDivider(Double.parseDouble(args[2]));  //Reading constant for windows creation
		Syncing.stamp=false;
		System.out.println("Tracks: "+(args.length-3));
		GraphicRender r=new GraphicRender();
		// creating the arrays to contain the tracks 
		for(int i=4;i<args.length;i++){
			input=new  Wave(args[i]);
			r.renderWaveform(input, args[i]+".jpg");
			waveHead=input.getWaveHeader().getBitsPerSample();
			amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		//Tracks setup and syncing
		normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		index=Syncing.selectionSynchronization(offset);	
		//Continuing algorithm
		if(WINDOW || !WINDOW){
			AlgorithmWindows.algorithm(index[0],index[1]);
		}
	}
}