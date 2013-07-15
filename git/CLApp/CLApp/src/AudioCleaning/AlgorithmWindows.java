package AudioCleaning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

/**
 * The class containing the other phases not computed early
 * @author Daniele  De Angelis
 *
 */

public class AlgorithmWindows {
	private static final int SAMPLE_RATE = 44100; 
	private static double sampleRateDivider = 0.1;
	private static float[][] hToPlotW; //Bi-dimensional array for h values plotting

	/**
	 * Setting up divider to create windows
	 * @param n
	 * 			value from input
	 */
	protected static void setDivider(double n){
		sampleRateDivider=n;
	}
	
	/**
	 * Function that executes the normalization phase
	 * @param tracks
	 * 		Audio tracks
	 * @param index
	 * 		Index of the first best track after synchronization
	 * @param secondIndex
	 * 		Index of the second best track after synchronization
	 * @throws IOException
	 */
	private static void normalization(ArrayList<float[][]> tracks, int index, int secondIndex) throws IOException{
		int numWindows=tracks.get(0).length;
		int bestSigma[] = new int[numWindows];
		float c1[] = new float[numWindows], c2[] = new float[numWindows];
		float hValue[][]=new float[tracks.size()][numWindows];
		float[][] sigma=new float[tracks.size()-1][numWindows];
		
		//Computing covariance between first and second best tracks
		System.out.println("Covariance by windows trackRef"+index+"VStrack"+secondIndex);
		for(int i=0;i<numWindows;i++){
			c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(1)[i]);
			hValue[secondIndex][i]=(float) 1.0;
			System.out.println("Windows"+i+": "+c2[i]+", h->"+hValue[secondIndex][i]);
		}
		System.out.println();
		DataFile.fileCreate(c2, "Cov"+secondIndex);
		
		//Computing the covariances of the other tracks and their multiplication values
		for(int i=0;i<tracks.size();i++){
			if(i!= index && i != secondIndex){
				System.out.println("Covariance by windows trackRef"+index+"VStrack"+i);
				for(int j=0;j<numWindows;j++){
					c1[j]=Statistical.covariance(tracks.get(index)[j], tracks.get(i)[j]);
					if(c1[j]==0)
						hValue[i][j]=(float)0.0;
					else if(c2[j]==0)
						hValue[i][j]=(float)1.0;
					else
						hValue[i][j]=(float) c2[j]/c1[j];
					System.out.println("Windows"+j+": "+c1[j]+", h->"+hValue[i][j]+", Cov to reach: "+c2[j]);
				}
				System.out.println();
				DataFile.fileCreate(c1, "Cov"+i);
			}
		}
		//Ranking the tracks already normalized to make a classification of them noise
		for(int i=0;i<tracks.size();i++){
			if(i != index){
				for(int j=0;j<numWindows;j++){
					for(int z=0;z<tracks.get(i)[j].length;z++){
						tracks.get(i)[j][z]*=hValue[i][j];
					}
					if(i<index)
						sigma[i][j]=Statistical.variance(tracks.get(i)[j]);
					else
						sigma[i-1][j]=Statistical.variance(tracks.get(i)[j]);	
				}
			}
		}
		System.out.println("Best ref by windows");
		for(int i=0;i<numWindows;i++){
			bestSigma[i]=SortingTools.min(sigma,i);
			if(bestSigma[i]>=index)
				bestSigma[i]++;
			System.out.println("Window"+i+" -> track"+bestSigma[i]);
		}
		//Picking the best normalized track for every window, 
		//it computes the covariance between them and the first best track of the synchronization
		System.out.println("\nCovariance by windows bestRefVStrackRef"+index);
		for(int i=0;i<numWindows;i++){
			c1[i]=Statistical.covariance(tracks.get(bestSigma[i])[i], tracks.get(index)[i]);
			if(c1[i]==0)
				hValue[index][i]=(float)0.0;
			else if(c2[i]==0)
				hValue[index][i]=(float)1.0;
			else
				hValue[index][i]=c2[i]/c1[i];
			System.out.println("Windows"+i+" (ref:"+bestSigma[i]+"): "+c1[i]+", h->"+hValue[index][i]+", Cov to reach: "+c2[i]);
		}
		System.out.println();
		DataFile.fileCreate(c1, "Cov"+index);
		for(int i=0;i<numWindows;i++){
			for(int j=0;j<tracks.get(index)[i].length;j++){
				tracks.get(index)[i][j]*=hValue[index][i];
			}
		}
		hToPlotW=hValue;
	}
	
	/**
	 * Function that classifies all the tracks for their noise value in each window
	 * @param tracks
	 * 		Audio tracks windowed
	 * @return
	 * 		Bidimensional array containing the classification
	 */
	private static Ranking[][] ranking(ArrayList<float[][]> tracks){
		int numWindows=tracks.get(0).length;
		int numTracks=tracks.size();
		Ranking[][] ranked=new Ranking[numTracks][numWindows];
		
		for(int i=0;i<numTracks;i++){
			for(int j=0;j<numWindows;j++){
				//Computing variance of each track's window
				ranked[i][j]=new Ranking();
				ranked[i][j].sigma=Statistical.variance(tracks.get(i)[j]);
				ranked[i][j].pos=i;
			}
		}
		//Sorting the values computed
		for(int i=0;i<numWindows;i++){
			SortingTools.trackSort(ranked, i);
		}
		return ranked;
	}
	
	/**
	 * Function to compute the noise at each window with all the combinations
	 * @param tracks
	 * 		Audio tracks
	 * @param rank
	 * 		Classification of them by lowest noise
	 * @return
	 * 		An array containing the values for each window at each combination
	 */
	private static float[][] powerAverage(ArrayList<float[][]> tracks, Ranking[][] rank){
		int numTracks = tracks.size();
		int numWindows = tracks.get(0).length;
		float[][] pwg = new float[numTracks][numWindows];
		float[][] temp = new float[numWindows][tracks.get(0)[0].length];
		temp[numWindows-1]= new float[tracks.get(0)[numWindows-1].length]; 
		int counter = 0, trackSelected;
		
		while(counter<numTracks){
			for(int i=0;i<numWindows;i++){
				trackSelected=rank[counter][i].pos;
				for(int j=0;j<temp[i].length;j++){
					temp[i][j]+=tracks.get(trackSelected)[i][j];
					temp[i][j]/=counter+1;
				}
			}
			//Noise computed with the variance
			for(int i=0;i<numWindows;i++)
				pwg[counter][i]=Statistical.variance(temp,i);
			for(int i=0;i<numWindows;i++)
				for(int j=0;j<temp[i].length;j++)
					temp[i][j]*=counter+1;
			counter++;
		}
		return pwg;
	}
	
	/**
	 * Function to compute the final windows after the best combinations were established
	 * @param tracks
	 * 		ArrayList with Bi-dimensional tracks array
	 * @param rank
	 * 		Bi-dimensional array for the classification
	 * @param best
	 * 		Best combination by window
	 * @return
	 * 		Final track
	 */
	private static float[][] combine(ArrayList<float[][]> tracks, Ranking[][] rank,int[] best){
		int numWindows=best.length;
		int counter=0, window=0, index;
		float[][] combined= new float[numWindows][];
		
		while(window<numWindows){
			counter=0;
			combined[window]=new float[tracks.get(0)[window].length];
			while(counter<=best[window]){
				index=rank[counter][window].pos;
				for(int i=0;i<tracks.get(index)[window].length;i++){
					combined[window][i]+=tracks.get(index)[window][i];
				}
				counter++;
			}
			for(int i=0;i<combined[window].length;i++){
				combined[window][i]/=counter;
			}
			window++;
		}
		return combined;
	}
	
	/**
	 * Function to recompute values of the tracks as they were before the normalization phase
	 * @param tracks
	 * 		Tracks normalized
	 */
	private static void denormalizeTrack(ArrayList<float[][]> tracks) {
		for(int i=0;i<tracks.size();i++){
			for(int j=0;j<tracks.get(i).length;j++){
				for(int h=0;h<tracks.get(i)[j].length;h++){
					if(hToPlotW[i][j]!=0)
						tracks.get(i)[j][h]/=hToPlotW[i][j];
				}
			}
		}
	}

	/**
	 * To merge each windows to create an unique track
	 * @param track
	 * 		Bi-dimensional array of windows
	 * @return
	 * 		Merged track
	 */
	private static float[] mergingWindows(float[][] track){
		int numWindows=track.length;
		int length=0, pointer=0;
		float[] finalTrack;
		
		for(int i=0;i<numWindows;i++){
			length+=track[i].length;
		}
		finalTrack=new float[length];
		for(int i=0;i<numWindows;i++){
			for(int j=0;j<track[i].length;j++){
				finalTrack[j+pointer]=track[i][j];
			}
			pointer+=track[i].length;
		}
		track=null;
		return finalTrack;
	}
	
	/**
	 * Function to continue the audio cleaning. It contains:
	 * 		-the windows generation phase
	 * 		-the normalization phase
	 * 		-the combination phase
	 * @param index
	 * 			First track selected by synchronization
	 * @param secondIndex
	 * 			Second track selected by synchronization
	 * @throws IOException
	 */
	
	public static void algorithm(int index, int secondIndex) throws IOException{
		int numWindows, windowsLenght;
		//Computing the windows number also by checking the boolean value of WINDOW
		if(CleaningAlgorithm.WINDOW){
			numWindows=WaveManipulation.computeNumWindows(sampleRateDivider); 
			windowsLenght=(int) (SAMPLE_RATE/sampleRateDivider); 
		}
		else{
			numWindows=1; 
			windowsLenght=CleaningAlgorithm.amplitudeReady.get(0).length; 
		}
		Integer lastWindowLenght=0;
		ArrayList<float[][]> tracks;
		Ranking[][] rank;
		float[][] pwg;
		float[][] combinedTrack;
		float[] combinedWithoutWindows;
		float[] dataToStamp;
		float RMSETot;
		int[] bestCombo=new int[numWindows];
		GraphicRender r=new GraphicRender();
		
		tracks = new ArrayList<float[][]>();
		/* Warning: windowsCreation deletes CleaningAlgorithm.amplitudeReady at the end! */
		lastWindowLenght=WaveManipulation.windowsCreation(tracks,numWindows,windowsLenght);
		System.out.println("Number of windows: "+numWindows+", length: "+windowsLenght+", last: "+lastWindowLenght+"\n");
		hToPlotW=new float[tracks.size()][numWindows];
		//Normalization phase
		normalization(tracks,index, secondIndex);
		//Ranking phase
		rank=ranking(tracks);
		//Checking the best combination by window
		pwg=powerAverage(tracks,rank);
		for(int i=0;i<numWindows;i++)
			bestCombo[i]=SortingTools.min(pwg,i);
		System.out.println("Best combine by windows:");
		for(int i=0;i<bestCombo.length;i++){
			System.out.print("Windows "+i+": ");
			for(int j=0;j<=bestCombo[i];j++){
				System.out.print("track"+rank[j][i].pos+" ");
			}
			System.out.println();
		}
		DataFile.fileCreate(bestCombo, "bestCombo");
		//Combination phase
		combinedTrack=combine(tracks,rank,bestCombo);
		denormalizeTrack(tracks);
		//Saving h values to plot
		for(int i=0;i<bestCombo.length;i++){
			for(int j=tracks.size()-1;j>bestCombo[i];j--){
				hToPlotW[rank[j][i].pos][i]=(float) 0.0;
			}
		}
		for(int i=0;i<tracks.size();i++){
			DataFile.fileCreate(hToPlotW[i], "hplot"+i);
		}
		errorTrackWindow(tracks,combinedTrack, bestCombo);
		//Computing and saving RMSE values by window
		for(int i=0;i<tracks.size();i++){
			dataToStamp=new float[numWindows];
			System.out.println("\nRMSE between final and initial track"+i+"(by window)");
			for(int j=0;j<numWindows;j++){
				dataToStamp[j]=(float) Statistical.normalizedRMSE(combinedTrack[j],tracks.get(i)[j]);
				System.out.println("Window"+j+": "+dataToStamp[j]);
			}
			DataFile.fileCreate(dataToStamp, "dataRMSE"+i);
		}
		//Computing total RMSE values
		System.out.println("\nRMSE between final and initial track (full track)");
		for(int i=0;i<tracks.size();i++){
			RMSETot=(float) Statistical.normalizedRMSETotW(combinedTrack, tracks.get(i));
			System.out.println("Track"+i+": "+RMSETot);
		}
		/* Warning: mergingWindows deletes combinedTrack at the end! */
		//Smoothing windows phase
		WaveManipulation.amplitudeNormalization(combinedTrack);
		//Saving the output file
		combinedWithoutWindows=mergingWindows(combinedTrack);
		WaveManipulation.save(CleaningAlgorithm.name+"("+numWindows+").wav", WaveManipulation.convertFloatsToDoubles(combinedWithoutWindows));
		Wave toRender=new Wave(CleaningAlgorithm.name+"("+numWindows+").wav");
		r.renderWaveform(toRender, CleaningAlgorithm.name+"("+numWindows+").wav.jpg");
		System.out.println("Conversion completed!");
	}

	/**
	 * Function to compute the odds between the input records and the output data
	 * @param tracks
	 * 		Bi-dimensional array containing the input tracks after their normalization
	 * @param finalTrack
	 * 		Final track computed by algorithm
	 * @param bestCombo
	 * 		Array containing the number of tracks used by window
	 */
	public static void errorTrackWindow(ArrayList<float[][]> tracks, float[][] finalTrack, int[] bestCombo){
		Wave render;
		GraphicRender r=new GraphicRender();
		File remove;
		int windows=tracks.get(0).length;
		float[][] temp=new float[windows][];
		
		for(int i=0;i<tracks.size();i++){
			//Denormalizing tracks one by one and computing the odds
			for(int j=0;j<windows;j++){
				double rms_final = Statistical.avg_mod(finalTrack[j]);
				double rms_track_i = Statistical.avg_mod(tracks.get(i)[j]);
				temp[j]=new float[tracks.get(i)[j].length];
				for(int z=0;z<tracks.get(i)[j].length;z++){
					temp[j][z] =(float) (tracks.get(i)[j][z] * rms_final /rms_track_i - finalTrack[j][z]);
				}
			}
			WaveManipulation.save(i+"-errorWind.wav",WaveManipulation.convertFloatsToDoubles(AlgorithmWindows.mergingWindows(temp)));
			render=new Wave(i+"-errorWind.wav");
			
			r.renderWaveform(render, CleaningAlgorithm.name+(i)+"error-file-window"+windows+".jpg");
			remove=new File("errore.wav");
			remove.delete();
			render=null;
			temp=new float[windows][];
		}
	}

}

