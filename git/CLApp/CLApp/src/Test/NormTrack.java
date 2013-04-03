package Test;

import java.util.ArrayList;

import AudioCleaning.*;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class NormTrack {
	
	public static void main(String[] args){
		Wave input;
		int waveHead;
		CleaningAlgorithm.normalizedAmplitudes=new ArrayList<float[]>();
		CleaningAlgorithm.amplitudeReady=new ArrayList<float[]>();
		int offset;
		Syncing.stamp=false;
		
		if(args.length<1){
			System.out.println("Usage: java -jar nomeprogramma.jar <offset crosscorrelation> <clean file> <final file>");
			return;
		}
		//CleaningAlgorithm.name=args[0];
		//offset=Integer.parseInt(args[0]);
		
		
		for(int i=0;i<args.length;i++){
			System.out.println(args[i]);
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		/*Syncing.zeroPadding();
		Syncing.selectionSyncronization(offset);
		System.out.println("Syncing completed!");
		System.out.println("RMSE clean track VS cleaned track: "+Statistical.RMSE(CleaningAlgorithm.amplitudeReady.get(0), CleaningAlgorithm.amplitudeReady.get(1)));
		*/
		CleaningAlgorithm.amplitudeReady.add(CleaningAlgorithm.normalizedAmplitudes.get(0));
		CleaningAlgorithm.normalizedAmplitudes=null;
		int numWindows=AlgorithmWindows.computeNumWindows(0.1);
		ArrayList<float[][]> track=new ArrayList<float[][]>();
		AlgorithmWindows.windowsCreation(track, numWindows, 441000);
		WaveManipulation.amplitudeNormalization(track.get(0));
		WaveManipulation.save("Renorm.wav",WaveManipulation.convertFloatsToDoubles(AlgorithmWindows.mergingWindows(track.get(0))));
		GraphicRender r=new GraphicRender();
		Wave toRender=new Wave("Renorm.wav");
		r.renderWaveform(toRender, "Renorm.wav.jpg");
		return;
	}

}
