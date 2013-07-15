package it.uniroma2.audioclean;

import java.util.ArrayList;

public class WaveManipulation {

	private static final double MAX_16_BIT = Short.MAX_VALUE;
	
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
		
		CleaningAlgorithm.normalizedAmplitudes.add(new float[numSamples]);
		for (int i = 0; i < numSamples; i++) {
			CleaningAlgorithm.normalizedAmplitudes.get(CleaningAlgorithm.normalizedAmplitudes.size()-1)[i] = (float) CleaningAlgorithm.amplitude[i] / maxAmplitude;
		}
	}

	/**
	 * Converts an array of floats into one of bytes
	 * @param input
	 * 		Array of floats
	 * @return
	 * 		Array of bytes
	 */
	public static byte[] fromFloatsToByte(float[] input){
		byte[] data = new byte[2 * input.length];
	    for (int i = 0; i < input.length; i++) {
	        int temp = (short) (input[i] * MAX_16_BIT);
	        data[2*i + 0] = (byte) temp;
	        data[2*i + 1] = (byte) (temp >> 8);
	    }
	    return data;
	}

	/**
	 * Function that computes the number of windows with a specified divisor
	 * @param n
	 * 		Value to divide the sample rate
	 * @return
	 * 		Number of windows
	 */
	public static int computeNumWindows(double n){
		if(CleaningAlgorithm.amplitudeReady.get(0).length%(CleaningAlgorithm.SAMPLE_RATE/n)==0)
			return (int) ((int) CleaningAlgorithm.amplitudeReady.get(0).length/(CleaningAlgorithm.SAMPLE_RATE/n));
		else
			return (int) (CleaningAlgorithm.amplitudeReady.get(0).length/(CleaningAlgorithm.SAMPLE_RATE/n))+1;
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
			while(leftovers!=0){
				if(index!=NumWindows-1){
					insert[index]=new float[windowsLenght];
					for(int i=0;i<windowsLenght;i++){
						insert[index][i]=CleaningAlgorithm.amplitudeReady.get(0)[index*windowsLenght+i];
					}
					index++;
					leftovers-=windowsLenght;
				}
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
