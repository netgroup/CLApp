package AudioCleaning;
import java.io.*;

import com.musicg.wave.Wave;

/**
 * Class containing function to write txt file containing value to graph
 * @author Daniele De Angelis
 *
 */
public class DataFile {
	/**
	 * File creator for index-data
	 * @param track
	 * 		Data
	 * @param name
	 * 		File name
	 * @throws IOException
	 */
	public static void fileCreate(float[] track, String name) throws IOException{
		File data=new File(name+".data");
		data.createNewFile();
		FileWriter flow=new FileWriter(data);
		
		for(int i=0;i<track.length;i++){
			flow.append(Integer.toString(i)+"\t");
			flow.append(Float.toString(track[i])+"\n");
		}
		flow.close();	
	}
	
	/**
	 * File creator for index-data (integer version)
	 * @param bestCombo
	 * 		Data
	 * @param name
	 * 		File name
	 * @throws IOException
	 */
	public static void fileCreate(int[] bestCombo, String name) throws IOException{
		File data=new File(name+".data");
		data.createNewFile();
		FileWriter flow=new FileWriter(data);
		
		for(int i=0;i<bestCombo.length;i++){
			flow.append(Integer.toString(i)+"\t");
			flow.append(Integer.toString(bestCombo[i]+1)+"\n");
		}
		flow.close();	
	}
	
	
	/**
	 * File creator for time-data
	 * @param track
	 * 		Data
	 * @param name
	 * 		File name
	 * @throws IOException
	 */
	public static void fileCreate(double[] track, String name) throws IOException{
		File data=new File(name+".dat");
		double time;
		data.createNewFile();
		FileWriter flow=new FileWriter(data);
		
		for(int i=0;i<track.length;i++){
			time=(i/44100.0);
			flow.append(Double.toString(time)+" ");
			flow.append(Double.toString(track[i])+"\n");
		}
		
	}
	
	public static void main(String[] args) throws IOException{
		if(args.length<1){
			System.err.println("Errore: pochi argomenti");
			return;
		}
		Wave input=new  Wave(args[0]);
		double[] track = input.getNormalizedAmplitudes();
		fileCreate(track,"prova");
	}

}
