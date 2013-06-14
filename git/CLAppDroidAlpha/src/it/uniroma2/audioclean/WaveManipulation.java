package it.uniroma2.audioclean;

import it.uniroma2.audioclean.tools.SortingTools;
import it.uniroma2.audioclean.tools.Statistical;

public class WaveManipulation {

	public static final int SAMPLE_RATE = 44100;
	public static double[] convertFloatsToDoubles(float[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    double[] output = new double[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = input[i];
	    }
	    return output;
	}

	/* converting array from short to double */
	
	public static void getNormalizedAmplitudes(int waveHeader) {
		boolean signed=true; 
		// usually 8bit is unsigned
		if (waveHeader==8){
			signed=false;
		}
		int numSamples = CleaningAlgorithm.amplitude.length;
		int maxAmplitude = 1 << (waveHeader - 1);
		
		if (!signed){	// one more bit for unsigned value
			maxAmplitude<<=1;
		}
		
		CleaningAlgorithm.normalizedAmplitudes.add(new float[numSamples]);
		for (int i = 0; i < numSamples; i++) {
			CleaningAlgorithm.normalizedAmplitudes.get(CleaningAlgorithm.normalizedAmplitudes.size()-1)[i] = (float) CleaningAlgorithm.amplitude[i] / maxAmplitude;
		}
	}

	/**
	 * Save the double array as a sound file (using .wav or .au format).
	 * Fonte: http://introcs.cs.princeton.edu/java/stdlib/StdAudio.java.html
	 */
	/*public static void save(String filename, double[] input) {
	
	    // assumes 44,100 samples per second
	    // use 16-bit audio, mono, signed PCM, little Endian
	    AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
	    byte[] data = new byte[2 * input.length];
	    for (int i = 0; i < input.length; i++) {
	        int temp = (short) (input[i] * MAX_16_BIT);
	        data[2*i + 0] = (byte) temp;
	        data[2*i + 1] = (byte) (temp >> 8);
	    }
	
	    // now save the file
	    try {
	        ByteArrayInputStream bais = new ByteArrayInputStream(data);
	        AudioInputStream ais = new AudioInputStream(bais, format, input.length);
	        if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
	            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
	        }
	        else if (filename.endsWith(".au") || filename.endsWith(".AU")) {
	            AudioSystem.write(ais, AudioFileFormat.Type.AU, new File(filename));
	        }
	        else {
	            throw new RuntimeException("File format not supported: " + filename);
	        }
	    }
	    catch (Exception e) {
	        System.out.println(e);
	        System.exit(1);
	    }
	}
	*/
	public static void amplitudeNormalization(float[][] f){
		float alpha=(float)0.7;
		double amplitudeN=0.0, amplitudeP=0.0, pwravgW=0.0;
		
		amplitudeP=Statistical.RMS(f[0]);
		for(int i=1;i<f.length;i++){
			pwravgW=Statistical.RMS(f[i]);
			amplitudeN=alpha*(pwravgW)+(1-alpha)*amplitudeP;
			for(int j=0;j<f[i].length;j++){
				f[i][j]*=amplitudeN/pwravgW;
			}
			amplitudeP=amplitudeN;
		}
	}
	
	//TODO insert those function and test it!
	@SuppressWarnings("unused")
	private static float alphaValue(int length){
		for(int i=5;i>0;i--){
			if(length>Math.pow(10, i)){
				return (float) (((float) i+4)/10.0); 
			}
		}
		return (float) 1.0;
	}
	
	public static void normalization0dbwithMem(float[][] track){
		float alpha=(float) 0.8, gammaOld=(float) 0.0, gammaNow, peak, maxAmpl=(float) 0.8;
		
		for(int i=0;i<track.length;i++){
			//alpha=alphaValue(track[i].length);
			peak=SortingTools.peak(track[i]);
			gammaNow=(maxAmpl/peak)*alpha+gammaOld*(1-alpha);
			for(int j=0;j<track[i].length;j++){
				track[i][j]*=gammaNow;
			}
			gammaOld=gammaNow;
		}
	}
	
	public static void normalizationMinus0dot2db(float[] track){
		float alpha=(float) 0.8, gammaOld=(float) 0.0, gammaNow, peak, maxAmpl=(float) 0.8;
		int counter=0, numWind=0;
		float[] temp=new float[SAMPLE_RATE];
		
		for(int i=0;i<track.length;i++){
			if(counter<SAMPLE_RATE){
				temp[counter]=track[i];
				counter++;
			}
			else{	
				peak=SortingTools.peak(temp);
				gammaNow=(maxAmpl/peak)*alpha+gammaOld*(1-alpha);
				for(int j=numWind;j<(track.length+numWind);j++){
					track[i]*=gammaNow;
				}
				numWind++;
				counter=0;
				gammaOld=gammaNow;
				i--;
			}
		}
	}

	public static int computeNumWindows(short[] tr, double n, int byteRate){
		if(tr.length%(byteRate/n)==0)
			return (int) ((int) tr.length/(byteRate/n));
		else
			return (int) (tr.length/(byteRate/n))+1;
	}

	public static short[][] windowsCreation(short[] tr,int NumWindows, int windowsLenght){
		int leftovers = tr.length;
		int index=0;
		short[][] insert=new short[NumWindows][];
		
		
		index=0;
		leftovers = tr.length;
		insert=new short[NumWindows][];
		while(leftovers!=0){
			if(index!=NumWindows-1){
				insert[index]=new short[windowsLenght];
				for(int i=0;i<windowsLenght;i++){
					insert[index][i]=tr[index*windowsLenght+i];
				}
				index++;
				leftovers-=windowsLenght;
			}
			else{
				insert[index] = new short[leftovers];
				for(int i=0;i<leftovers;i++){
					insert[index][i]=tr[index*windowsLenght+i];
				}
				index++;
				leftovers=0;
			}
		}
		
		return insert;
	}

}
