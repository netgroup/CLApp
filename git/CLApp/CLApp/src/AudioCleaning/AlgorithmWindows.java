package AudioCleaning;

import java.io.IOException;
import java.util.ArrayList;

public class AlgorithmWindows {
	public static final int SAMPLE_RATE = 44100;
	public static final int secForWindows = 1;
	public static float[][] hToPlotW;

	public static int computeNumWindows(double n){
		if(CleaningAlgorithm.amplitudeReady.get(0).length%(n*SAMPLE_RATE)==0)
			return (int) ((int) CleaningAlgorithm.amplitudeReady.get(0).length/(n*SAMPLE_RATE));
		else
			return (int) (CleaningAlgorithm.amplitudeReady.get(0).length/(n*SAMPLE_RATE))+1;
	}
	
	public static void windowsCreation(ArrayList<float[][]> tracks, int NumWindows, int windowsLenght, int lastWindowLenght){
		int leftovers = CleaningAlgorithm.amplitudeReady.get(0).length;
		int numTracks = CleaningAlgorithm.amplitudeReady.size(), index=0, trackID=0;
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
	}
	
	public static void normalization(ArrayList<float[][]> tracks, int index){
		int numWindows=tracks.get(0).length;
		int indexFirst;
		int bestSigma[] = new int[numWindows];
		float c1[] = new float[numWindows], c2[] = new float[numWindows];
		float hValue[][]=new float[tracks.size()][numWindows];
		float[][] sigma=new float[tracks.size()-1][numWindows];
		
		if(index!=0){
			indexFirst=0;
			for(int i=0;i<numWindows;i++){
				c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(0)[i]);
				hValue[0][i]=(float) 1.0;
			}
		}
		else{
			indexFirst=1;
			for(int i=0;i<numWindows;i++){
				c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(1)[i]);
				hValue[1][i]=(float) 1.0;
			}
		}
		for(int i=0;i<tracks.size();i++){
			if(i!= index && i != indexFirst){
				for(int j=0;j<numWindows;j++){
					c1[j]=Statistical.covariance(tracks.get(index)[j], tracks.get(i)[j]);
					hValue[i][j]=(float) c2[j]/c1[j];
				}
			}
		}
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
		for(int i=0;i<numWindows;i++){
			bestSigma[i]=SortingTools.min(sigma,i);
			if(bestSigma[i]>=index)
				bestSigma[i]++;
		}
		for(int i=0;i<numWindows;i++){
			c1[i]=Statistical.covariance(tracks.get(bestSigma[i])[i], tracks.get(index)[i]);
			hValue[index][i]=c2[i]/c1[i];
		}
		for(int i=0;i<numWindows;i++){
			for(int j=0;j<tracks.get(index)[i].length;j++){
				tracks.get(index)[i][j]*=hValue[index][i];
			}
		}
		hToPlotW=hValue;
	}
	
	public static Ranking[][] ranking(ArrayList<float[][]> tracks){
		int numWindows=tracks.get(0).length;
		int numTracks=tracks.size();
		Ranking[][] ranked=new Ranking[numTracks][numWindows];
		
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
	
	public static float[][] powerAverage(ArrayList<float[][]> tracks, Ranking[][] rank){
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
			for(int i=0;i<numWindows;i++)
				pwg[counter][i]=Statistical.variance(temp,i);
			for(int i=0;i<numWindows;i++)
				for(int j=0;j<temp[i].length;j++)
					temp[i][j]*=counter+1;
			counter++;
		}
		return pwg;
	}
	
	public static float[][] combine(ArrayList<float[][]> tracks, Ranking[][] rank,int[] best){
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
	
	public static float[] mergingWindows(float[][] track){
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
	
	public static void algorithm(int index) throws IOException{
		int numWindows=computeNumWindows(secForWindows), windowsLenght=secForWindows*SAMPLE_RATE, lastWindowLenght=0;
		ArrayList<float[][]> tracks;
		Ranking[][] rank;
		float[][] pwg;
		float[][] combinedTrack;
		float[] combinedWithoutWindows;
		float[] dataToStamp;
		float RMSETot;
		int[] bestCombo=new int[numWindows];
		
		tracks = new ArrayList<float[][]>();
		/* Warning: windowsCreation deletes CleaningAlgorithm.amplitudeReady at the end! */
		windowsCreation(tracks,numWindows,windowsLenght,lastWindowLenght);
		System.out.println("Number of windows: "+numWindows+"\n");
		hToPlotW=new float[tracks.size()][numWindows];
		normalization(tracks,index);
		rank=ranking(tracks);
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
		for(int i=0;i<bestCombo.length;i++){
			for(int j=tracks.size()-1;j>bestCombo[i];j--){
				hToPlotW[rank[j][i].pos][i]=(float) 0.0;
			}
		}
		for(int i=0;i<tracks.size();i++){
			DataFile.fileCreate(hToPlotW[i], "hplot"+i);
		}
		combinedTrack=combine(tracks,rank,bestCombo);
		CleaningAlgorithm.errorTrackWindow(tracks,combinedTrack, bestCombo);
		WaveManipulation.amplitudeNormalization(combinedTrack);
		/* Warning: normalizedRMSE and normalizedRMSETotW could change the values of tracks */
		for(int i=0;i<tracks.size();i++){
			dataToStamp=new float[numWindows];
			for(int j=0;j<numWindows;j++){
				dataToStamp[j]=(float) Statistical.normalizedRMSE(combinedTrack[j],tracks.get(i)[j]);
			}
			DataFile.fileCreate(dataToStamp, "dataRMSE"+i);
		}
		System.out.println("\nRMSE between the initial and final track (full track)");
		for(int i=0;i<tracks.size();i++){
			RMSETot=(float) Statistical.normalizedRMSETotW(combinedTrack, tracks.get(i));
			System.out.println("Track"+i+": "+RMSETot);
		}
		/* TODO
		CleaningAlgorithm.errorTrackWindow(tracks,combinedTrack, bestCombo);
		*/
		/* Warning: mergingWindows deletes combinedTrack at the end! */
		combinedWithoutWindows=mergingWindows(combinedTrack);
		WaveManipulation.save(CleaningAlgorithm.name+"("+numWindows+").wav", WaveManipulation.convertFloatsToDoubles(combinedWithoutWindows));
		System.out.println("Conversion completed!");
	}

}

