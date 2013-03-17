package AudioCleaning;

import java.io.IOException;
import java.util.ArrayList;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class AlgorithmWindows {
	public static final int SAMPLE_RATE = 44100;
	public static final double sampleRateDivider = 100;
	public static float[][] hToPlotW;

	public static int computeNumWindows(double n){
		if(CleaningAlgorithm.amplitudeReady.get(0).length%(SAMPLE_RATE/n)==0)
			return (int) ((int) CleaningAlgorithm.amplitudeReady.get(0).length/(SAMPLE_RATE/n));
		else
			return (int) (CleaningAlgorithm.amplitudeReady.get(0).length/(SAMPLE_RATE/n))+1;
	}
	
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
	
	public static void normalization(ArrayList<float[][]> tracks, int index, int secondIndex) throws IOException{
		int numWindows=tracks.get(0).length;
		int indexFirst;
		int bestSigma[] = new int[numWindows];
		float c1[] = new float[numWindows], c2[] = new float[numWindows];
		float hValue[][]=new float[tracks.size()][numWindows];
		float[][] sigma=new float[tracks.size()-1][numWindows];
		
		/*if(index!=0){
			indexFirst=0;
			System.out.println("Covariance by windows trackRef"+index+"VStrack"+indexFirst);
			for(int i=0;i<numWindows;i++){
				c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(0)[i]);
				hValue[0][i]=(float) 1.0;
				System.out.println("Windows"+i+": "+c2[i]+", h->"+hValue[0][i]);
			}
			System.out.println();
			DataFile.fileCreate(c2, "Cov"+indexFirst);
		}
		else{*/
			//indexFirst=1;
			System.out.println("Covariance by windows trackRef"+index+"VStrack"+secondIndex);
			for(int i=0;i<numWindows;i++){
				c2[i]=Statistical.covariance(tracks.get(index)[i], tracks.get(1)[i]);
				hValue[secondIndex][i]=(float) 1.0;
				System.out.println("Windows"+i+": "+c2[i]+", h->"+hValue[secondIndex][i]);
			}
			System.out.println();
			DataFile.fileCreate(c2, "Cov"+secondIndex);
		/*}*/
		for(int i=0;i<tracks.size();i++){
			if(i!= index && i != secondIndex){
				System.out.println("Covariance by windows trackRef"+index+"VStrack"+i);
				for(int j=0;j<numWindows;j++){
					c1[j]=Statistical.covariance(tracks.get(index)[j], tracks.get(i)[j]);
					hValue[i][j]=(float) c2[j]/c1[j];
					System.out.println("Windows"+j+": "+c1[j]+", h->"+hValue[i][j]);
				}
				System.out.println();
				DataFile.fileCreate(c1, "Cov"+i);
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
		System.out.println("Best ref by windows");
		for(int i=0;i<numWindows;i++){
			bestSigma[i]=SortingTools.min(sigma,i);
			if(bestSigma[i]>=index)
				bestSigma[i]++;
			System.out.println("Window"+i+" -> track"+bestSigma[i]);
		}
		System.out.println("\nCovariance by windows bestRefVStrackRef"+index);
		for(int i=0;i<numWindows;i++){
			c1[i]=Statistical.covariance(tracks.get(bestSigma[i])[i], tracks.get(index)[i]);
			hValue[index][i]=c2[i]/c1[i];
			System.out.println("Windows"+i+" (ref:"+bestSigma[i]+"): "+c1[i]+", h->"+hValue[index][i]);
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
	
	private static void denormalizeTrack(ArrayList<float[][]> tracks) {
		for(int i=0;i<tracks.size();i++){
			for(int j=0;j<tracks.get(i).length;j++){
				for(int h=0;h<tracks.get(i)[j].length;h++){
					tracks.get(i)[j][h]/=hToPlotW[i][j];
				}
			}
		}
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
	
	public static void algorithm(int index, int secondIndex) throws IOException{
		int numWindows=computeNumWindows(sampleRateDivider), windowsLenght=(int) (SAMPLE_RATE/sampleRateDivider); 
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
		lastWindowLenght=windowsCreation(tracks,numWindows,windowsLenght);
		System.out.println("Number of windows: "+numWindows+", length: "+windowsLenght+", last: "+lastWindowLenght+"\n");
		hToPlotW=new float[tracks.size()][numWindows];
		normalization(tracks,index, secondIndex);
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
		DataFile.fileCreate(bestCombo, "bestCombo");
		combinedTrack=combine(tracks,rank,bestCombo);
		denormalizeTrack(tracks);
		for(int i=0;i<bestCombo.length;i++){
			for(int j=tracks.size()-1;j>bestCombo[i];j--){
				hToPlotW[rank[j][i].pos][i]=(float) 0.0;
			}
		}
		for(int i=0;i<tracks.size();i++){
			DataFile.fileCreate(hToPlotW[i], "hplot"+i);
		}
		CleaningAlgorithm.errorTrackWindow(tracks,combinedTrack, bestCombo);
		/* (SOLVED) Warning: normalizedRMSE and normalizedRMSETotW could change the values of tracks */
		for(int i=0;i<tracks.size();i++){
			dataToStamp=new float[numWindows];
			System.out.println("\nRMSE between final and initial track"+i+"(by window)");
			for(int j=0;j<numWindows;j++){
				dataToStamp[j]=(float) Statistical.normalizedRMSE(combinedTrack[j],tracks.get(i)[j]);
				System.out.println("Window"+j+": "+dataToStamp[j]);
			}
			DataFile.fileCreate(dataToStamp, "dataRMSE"+i);
		}
		System.out.println("\nRMSE between final and initial track (full track)");
		for(int i=0;i<tracks.size();i++){
			RMSETot=(float) Statistical.normalizedRMSETotW(combinedTrack, tracks.get(i));
			System.out.println("Track"+i+": "+RMSETot);
		}
		/* TODO
		CleaningAlgorithm.errorTrackWindow(tracks,combinedTrack, bestCombo);
		*/
		/* Warning: mergingWindows deletes combinedTrack at the end! */
		//WaveManipulation.amplitudeNormalization(combinedTrack);
		combinedWithoutWindows=mergingWindows(combinedTrack);
		WaveManipulation.save(CleaningAlgorithm.name+"("+numWindows+").wav", WaveManipulation.convertFloatsToDoubles(combinedWithoutWindows));
		Wave toRender=new Wave(CleaningAlgorithm.name+"("+numWindows+").wav");
		r.renderWaveform(toRender, CleaningAlgorithm.name+"("+numWindows+").wav.jpg");
		System.out.println("Conversion completed!");
	}

}

