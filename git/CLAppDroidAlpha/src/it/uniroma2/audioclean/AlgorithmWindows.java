package it.uniroma2.audioclean;

import it.uniroma2.audioclean.tools.Ranking;
import it.uniroma2.audioclean.tools.SortingTools;
import it.uniroma2.audioclean.tools.Statistical;
import it.uniroma2.clappdroidalpha.CleanTask;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

/**
 * Class containing the remaining part of the algorithm after CleaningAlgorithm
 * @author Daniele De Angelis
 *
 */
public class AlgorithmWindows {
	public static final double sampleRateDivider = 100;
	private static float[][] hToPlotW;

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
		
		//Computing the covariance between the best two tracks
		System.out.println("Covariance by windows trackRef"+index+"VStrack"+secondIndex);
		for(int i=0;i<numWindows;i++){
			c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(1)[i]);
			hValue[secondIndex][i]=(float) 1.0;
			System.out.println("Windows"+i+": "+c2[i]+", h->"+hValue[secondIndex][i]);
		}
		System.out.println();
		
		//Computes all the other covariance with the first
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
			}
		}
		//For every windows the best tracks is selected computing the variance
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
		//Computes the covariance for every windows between the best tracks from the
		//classification and the first best track after the synchronization
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
		
		//Computes the variance of every tracks and classifies them
		for(int i=0;i<numTracks;i++){
			for(int j=0;j<numWindows;j++){
				ranked[i][j]=new Ranking();
				ranked[i][j].sigma=Statistical.variance(tracks.get(i)[j]);
				ranked[i][j].pos=i;
			}
		}
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
		
		//Computing the variance of all the combination
		while(counter<numTracks){
			for(int i=0;i<numWindows;i++){
				trackSelected=rank[counter][i].pos;
				for(int j=0;j<temp[i].length;j++){
					temp[i][j]+=tracks.get(trackSelected)[i][j];
					temp[i][j]/=counter+1;
				}
			}
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
	 * Main function that manages all the remaining phases
	 * @param ct
	 * 		Caller thread
	 * @param index
	 * 		First best track index
	 * @param secondIndex
	 * 		Second best track index
	 * @return
	 * 		Final track
	 * @throws IOException
	 */
	public static float[] algorithm(CleanTask ct,int index, int secondIndex) throws IOException{
		int numWindows, windowsLenght;
		ArrayList<float[][]> tracks;
		Ranking[][] rank;
		float[][] pwg;
		float[][] combinedTrack;
		int[] bestCombo;
		//Computes the number of windows and creates the windowed tracks
		if(CleaningAlgorithm.WINDOW){
			numWindows=WaveManipulation.computeNumWindows(sampleRateDivider); 
			windowsLenght=(int) (CleaningAlgorithm.SAMPLE_RATE/sampleRateDivider); 
		}
		else{
			numWindows=1; 
			windowsLenght=CleaningAlgorithm.amplitudeReady.get(0).length; 
		}
		bestCombo=new int[numWindows];
		tracks = new ArrayList<float[][]>();
		WaveManipulation.windowsCreation(tracks,numWindows,windowsLenght);
		hToPlotW=new float[tracks.size()][numWindows];
		//Normalization phase
		normalization(tracks,index, secondIndex);
		//Making a classification of the tracks
		rank=ranking(tracks);
		//Computing all the combination to choose the best
		pwg=powerAverage(tracks,rank);
		for(int i=0;i<numWindows;i++)
			bestCombo[i]=SortingTools.min(pwg,i);
		//Combining the best combination
		combinedTrack=combine(tracks,rank,bestCombo);
		//Denormalizing the tracks for the RMSE computation
		denormalizeTrack(tracks);
		//Computes the RMSE and sends it to the caller CleanTask
		double RMSETot=(float) Statistical.normalizedRMSETotW(combinedTrack, tracks.get(tracks.size()-1));
		System.out.println("Gaining: "+RMSETot);
		ct.RMSE=RMSETot;
		//Merges the windows and return
		float[] floatArray=mergingWindows(combinedTrack);
		Log.i("clean", "Conversion completed!");
		return floatArray;
	}

}

