package AudioCleaning;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Class containing all the functions that change something in the wave data
 * @author Daniele De Angelis
 *
 */
public class WaveManipulation {

	public static final int SAMPLE_RATE = 44100;
	private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767
	
	/**
	 * Converts data typed like floats in doubles
	 * @param input
	 * 		Array of floats
	 * @return
	 * 		Array of double
	 */
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

	/**
	 * Convert data typed like shorts to floats
	 * @param waveHeader
	 * 		Value of bits per sample
	 */
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
	 * @author http://introcs.cs.princeton.edHardDriveu/java/stdlib/StdAudio.java.html
	 * @param filename
	 * 		Name to save the file on the HD
	 * @param input
	 * 		Data expressed in doubles
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
	
	/**
	 * Application of the moving exponential average to smooth the audio between
	 * the windows
	 * @param f
	 * 		Windowed track
	 */
	public static void amplitudeNormalization(float[][] f){
		float alpha=alphaValue(20);
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
	
	/**
	 * Compute the alpha value for the moving average
	 * @param n
	 * 		Number of windows to keep in memory
	 * @return
	 * 		Alpha value
	 */
	private static float alphaValue(int n){
		return (float) (2.0/(n+1.0));
	}

	/**
	 * Function that computes the number of windows with a specified divisor
	 * @param n
	 * 		Value to divide the sample rate
	 * @return
	 * 		Number of windows
	 */
	public static int computeNumWindows(double n){
		if(CleaningAlgorithm.amplitudeReady.get(0).length%(SAMPLE_RATE/n)==0)
			return (int) ((int) CleaningAlgorithm.amplitudeReady.get(0).length/(SAMPLE_RATE/n));
		else
			return (int) (CleaningAlgorithm.amplitudeReady.get(0).length/(SAMPLE_RATE/n))+1;
	}

	/**
	 * Function that creates the windowed tracks data structure
	 * @param tracks
	 * 		Data structure where insert the windowed tracks
	 * @param NumWindows
	 * 		Number of windows
	 * @param windowsLenght
	 * 		Generic window size
	 * @return
	 * 		Last window size
	 */
	public static Integer windowsCreation(ArrayList<float[][]> tracks, int NumWindows, int windowsLenght){
		int leftovers = CleaningAlgorithm.amplitudeReady.get(0).length;
		int numTracks = CleaningAlgorithm.amplitudeReady.size(), index=0, trackID=0;
		int lastWindowLenght = 0;
		float[][] insert=new float[NumWindows][];
		
		while(trackID<numTracks){
			index=0;
			leftovers = CleaningAlgorithm.amplitudeReady.get(0).length;
			insert=new float[NumWindows][];
			//Continue up to leftovers is 0
			while(leftovers!=0){
				//If it isn't the last window
				if(index!=NumWindows-1){
					insert[index]=new float[windowsLenght];
					for(int i=0;i<windowsLenght;i++){
						insert[index][i]=CleaningAlgorithm.amplitudeReady.get(0)[index*windowsLenght+i];
					}
					index++;
					leftovers-=windowsLenght;
				}
				//If it is the last window
				else{
					insert[index] = new float[leftovers];
					for(int i=0;i<leftovers;i++){
						insert[index][i]=CleaningAlgorithm.amplitudeReady.get(0)[index*windowsLenght+i];
					}
					index++;
					lastWindowLenght = leftovers;
					leftovers=0;
				}
			}
			tracks.add(insert);
			CleaningAlgorithm.amplitudeReady.remove(0);
			trackID++;
		}
		tracks.trimToSize();
		return lastWindowLenght;
	}
}
