package AudioCleaning;

import java.util.ArrayList;

import com.musicg.wave.Wave;

public class TestCovariance {
	
	static boolean windows;
	
	public static void main(String[] args){
		Wave input;
		int waveHead;
		CleaningAlgorithm.normalizedAmplitudes=new ArrayList<float[]>();
		CleaningAlgorithm.amplitudeReady=new ArrayList<float[]>();
		int offset;
		
		if(args.length<4){
			System.err.println("Usage: java -jar cleaningAlgorithm.jar <name> <offset crosscorrelation> <flag windows> <path audiofile (min 2)>");
			return;
		}
		CleaningAlgorithm.name=args[0];
		offset=Integer.parseInt(args[1]);
		//windows=Boolean.parseBoolean(args[2]);
		
		for(int i=2;i<args.length;i++){
			input=new  Wave(args[i]);
			waveHead=input.getWaveHeader().getBitsPerSample();
			CleaningAlgorithm.amplitude=input.getSampleAmplitudes();
			CleaningAlgorithm.getNormalizedAmplitudes(waveHead);
			
		}
		CleaningAlgorithm.normalizedAmplitudes.trimToSize();
		CleaningAlgorithm.zeroPadding();
		CleaningAlgorithm.selectionSyncronization(offset);
		
		float variance=CleaningAlgorithm.variance(CleaningAlgorithm.amplitudeReady.get(0));
		System.out.println("Variance: "+variance);
		
		float covariance=CleaningAlgorithm.covariance(CleaningAlgorithm.amplitudeReady.get(0),CleaningAlgorithm.amplitudeReady.get(1));
		System.out.println("Covariance: "+covariance);
		
		float[][] track=new float[2][];
		track[0]=CleaningAlgorithm.amplitudeReady.get(0);
		track[1]=CleaningAlgorithm.amplitudeReady.get(1);
		float[] product = new float[track[0].length];
		for(int i=0;i<track[0].length;i++){
			product[i]=track[0][i]*track[1][i];
		}
		float media = avg(product);
		media-=(avg(track[0])*avg(track[1]));
		
		System.out.println("E[XY]-E[X]E[Y]: "+media);
	}
	
	static float avg(float[] a){
		float sum=0;
		for(int i=0;i<a.length;i++){
			sum+=a[i];
		}
		sum/=a.length;
		return sum;
	}

}
