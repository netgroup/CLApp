package AudioCleaning;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class WaveManipulation {

	public static final int SAMPLE_RATE = 44100;
	private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767
	
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
		/* normalizedAmplitudes: tracks type float */
		CleaningAlgorithm.normalizedAmplitudes.add(new float[numSamples]);
		for (int i = 0; i < numSamples; i++) {
			CleaningAlgorithm.normalizedAmplitudes.get(CleaningAlgorithm.normalizedAmplitudes.size()-1)[i] = (float) CleaningAlgorithm.amplitude[i] / maxAmplitude;
		}
	}

	/**
	 * Save the double array as a sound file (using .wav or .au format).
	 * Fonte: http://introcs.cs.princeton.edu/java/stdlib/StdAudio.java.html
	 */
	public static void save(String filename, double[] input) {
	
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
	
	public static void amplitudeNormalization(float[][] f){
		float alpha=(float)0.9;
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

}
