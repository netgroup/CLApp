package AudioCleaning;

import java.util.ArrayList;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class TestXref {
	
	static float INF=Float.MAX_VALUE;
	public static final int SAMPLE_RATE = 44100;
	private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767

	static boolean stamp=true;
	static boolean windows=false;
	
	public static void main(String[] args){
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
		
		for(int i=3;i<args.length;i++){
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			CleaningAlgorithm.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		CleaningAlgorithm.zeroPadding();
		int index=CleaningAlgorithm.selectionSyncronization(offset);
		
		if(!windows){
			Ranking[] ranked;
		
			CleaningAlgorithm.normalization(index);
			System.out.println("Variance: "+CleaningAlgorithm.variance(CleaningAlgorithm.amplitudeReady.get(0)));
			// sorting track by their variance
			ranked=CleaningAlgorithm.ranking();
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
			int wind=12;
			Ranking[][] ranked;
			int interval=(CleaningAlgorithm.amplitudeReady.get(0).length/wind), lastWind=0;
			System.out.println("Windows activated.\n#windows: "+windows+"; window length: "+interval);
			ArrayList<ArrayList<float[]>> windowed=new ArrayList<ArrayList<float[]>>();
			
			CleaningAlgorithm.windowsCreation(windowed, wind, interval, lastWind);
			
			CleaningAlgorithm.normalizationWindows(windowed,wind,index);
			// sorting track by their variance
			ranked=CleaningAlgorithm.rankingWindows(windowed);
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
