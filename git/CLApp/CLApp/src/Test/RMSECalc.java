package Test;

import java.util.ArrayList;

import AudioCleaning.*;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class RMSECalc {
	
	public static void main(String[] args){
		Wave input;
		int waveHead;
		CleaningAlgorithm.normalizedAmplitudes=new ArrayList<float[]>();
		CleaningAlgorithm.amplitudeReady=new ArrayList<float[]>();
		int offset;
		Syncing.stamp=false;
		
		if(args.length<3){
			System.out.println("Usage: java -jar nomeprogramma.jar <offset crosscorrelation> <clean file> <final file>");
			return;
		}
		//CleaningAlgorithm.name=args[0];
		offset=Integer.parseInt(args[0]);
		
		
		for(int i=1;i<args.length;i++){
			System.out.println(args[i]);
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		Syncing.selectionSyncronization(offset);
		System.out.println("Syncing completed!");
		System.out.println("RMSE clean track VS cleaned track: "+Statistical.RMSE(CleaningAlgorithm.amplitudeReady.get(0), CleaningAlgorithm.amplitudeReady.get(1)));
		
		return;
	}

}
