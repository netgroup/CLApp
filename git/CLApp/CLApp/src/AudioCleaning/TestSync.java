package AudioCleaning;

import java.util.ArrayList;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class TestSync {
	
	public static void main(String[] args){
		Wave input;
		int waveHead;
		CleaningAlgorithm.normalizedAmplitudes=new ArrayList<float[]>();
		CleaningAlgorithm.amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		if(args.length<3){
			System.out.println("Usage: java -jar nomeprogramma.jar <name> <offset crosscorrelation> <path audiofile (min 2)>");
			return;
		}
		CleaningAlgorithm.name=args[0];
		offset=Integer.parseInt(args[1]);
		
		
		for(int i=2;i<args.length;i++){
			System.out.println(args[i]);
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			CleaningAlgorithm.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		CleaningAlgorithm.zeroPadding();
		CleaningAlgorithm.selectionSyncronization(offset);
		
		Wave renders;
		int i=0;
		GraphicRender r=new GraphicRender();
		while(CleaningAlgorithm.amplitudeReady.size()>0){ 
			CleaningAlgorithm.save((i+1)+CleaningAlgorithm.name+"-syncronized.wav", CleaningAlgorithm.convertFloatsToDoubles(CleaningAlgorithm.amplitudeReady.get(0)));
			CleaningAlgorithm.amplitudeReady.remove(0);
			renders=new Wave((i+1)+CleaningAlgorithm.name+"-syncronized.wav");
			r.renderWaveform(renders, (i+1)+CleaningAlgorithm.name+"-syncronized.wav-render.jpg");
			i++;
		}
		System.out.println("Syncing completed!");
		return;
	}

}
