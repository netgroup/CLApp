package Test;

import java.io.IOException;
import java.util.ArrayList;

import AudioCleaning.*;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class TestXref {
	
	static float INF=Float.MAX_VALUE;
	public static final int SAMPLE_RATE = 44100;
	static boolean stamp=true;
	static boolean windows=false;
	
	public static void main(String[] args) throws IOException{
		Wave input;
		int waveHead;
		CleaningAlgorithm.normalizedAmplitudes=new ArrayList<float[]>();
		CleaningAlgorithm.amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		if(args.length<5){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <name> <offset crosscorrelation> <flag windows> <path audiofile (min 2)>");
			return;
		}
		CleaningAlgorithm.name=args[0];
		offset=Integer.parseInt(args[1]);
		windows=Boolean.parseBoolean(args[2]);
		
		Syncing.stamp=false;
		
		for(int i=3;i<args.length;i++){
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			WaveManipulation.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		Syncing.zeroPadding();
		int[] index=Syncing.selectionSyncronization(offset);
		
		if(!windows){
			Ranking[] ranked;
		
			Algorithm.normalization(index[0]);
			System.out.println("Variance: "+Statistical.variance(CleaningAlgorithm.amplitudeReady.get(0)));
			// sorting track by their variance
			ranked=Algorithm.ranking();
			/*
			Wave renders;
			int i=0, tmp;
			GraphicRender r=new GraphicRender();
			while(i<CleaningAlgorithm.amplitudeReady.size()){
				tmp=ranked[i].pos;
				CleaningAlgorithm.save((tmp+1)+CleaningAlgorithm.name+"normalized-at"+i+".wav", CleaningAlgorithm.convertFloatsToDoubles(CleaningAlgorithm.amplitudeReady.get(tmp)));
				//amplitudeReady.remove(tmp);
				renders=new Wave((tmp+1)+CleaningAlgorithm.name+"normalized-at"+i+".wav");
				r.renderWaveform(renders, ((tmp+1)+CleaningAlgorithm.name+"normalized-at"+i+".wav-render.jpg"));
				i++;
			}
			System.out.println("Ranking completed!");
		
			float poweravg[]=CleaningAlgorithm.poweravg(ranked);
		
			// find lowest poweravg
			int k=CleaningAlgorithm.min(poweravg);
		
			System.out.print("Combination with lowest poweravg: ");
			for(i=0; i<=k; i++){
				System.out.print("track "+(ranked[i].pos+1));
				if(i<k-1){
					System.out.print(" + ");
				}
			}
			System.out.println();*/
			return;
		}
		else{
			int wind=AlgorithmWindows.computeNumWindows(1);
			Ranking[][] ranked;
			int interval=(CleaningAlgorithm.amplitudeReady.get(0).length/wind), lastWind=0;
			System.out.println("Windows activated.\n#windows: "+windows+"; window length: "+interval);
			ArrayList<float[][]> windowed=new ArrayList<float[][]>();
			
			lastWind=AlgorithmWindows.windowsCreation(windowed, wind, interval);
			
			AlgorithmWindows.normalization(windowed,index[0],index[1]);
			// sorting track by their variance
			ranked=AlgorithmWindows.ranking(windowed);
			/*
		
			Wave renders;
			int i=0;
			GraphicRender r=new GraphicRender();
			
			while(i<windowed.size()){
				CleaningAlgorithm.save(CleaningAlgorithm.name+(i+1)+"-normalized.wav", CleaningAlgorithm.convertFloatsToDoubles(CleaningAlgorithm.fromWindowedToNormal(windowed.get(i),wind,interval,lastWind)));
				//amplitudeReady.remove(tmp);
				renders=new Wave(CleaningAlgorithm.name+(i+1)+"-normalized.wav");
				r.renderWaveform(renders, CleaningAlgorithm.name+(i+1)+"-normalized.wav-render.jpg");
				i++;
			}*/
			
			System.out.println("Ranking completed!");
			
			/*
		
			float poweravg[]=CleaningAlgorithm.poweravg(ranked);
		
			// find the lowest poweravg
			int k=CleaningAlgorithm.min(poweravg);
		
			System.out.print("Combination with lowest poweravg: ");
			for(i=0; i<=k; i++){
				System.out.print("track "+(ranked[i].pos+1));
				if(i<k-1){
					System.out.print(" + ");
				}
			}
			System.out.println();
			return;
			*/
		}
	}

}
