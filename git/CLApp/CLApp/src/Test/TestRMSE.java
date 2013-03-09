package Test;

import AudioCleaning.Statistical;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveHeader;

public class TestRMSE {
	public static void main(String[] args){
		double[] first, last;
		float[] firstF,lastF;
		Wave input;
		
		if(args.length!=2){
			System.err.println("Usage: java -jar <jar name> <wave1> <wave2>;");
			return;
		}
		input=new Wave(args[0]);
		first=input.getNormalizedAmplitudes();
		input=new Wave(args[1]);
		last=input.getNormalizedAmplitudes();
		firstF=new float[first.length];
		lastF=new float[last.length];
		for(int i=0;i<first.length;i++){
			firstF[i]=(float)first[i];
		}
		for(int i=0;i<last.length;i++){
			lastF[i]=(float)last[i];
		}
		System.out.println(Statistical.normalizedRMSE(lastF, firstF));	
	}
}
