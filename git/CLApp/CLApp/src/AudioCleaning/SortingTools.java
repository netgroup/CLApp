package AudioCleaning;

/**
 * Contains some sorting functions
 * @author Daniele De Angelis
 *
 */
public class SortingTools {

	static float INF=Float.MAX_VALUE;

	/**
	 * Computes the minimum value in a column of a bidimensional array of floats
	 * @param factors
	 * 		Bidimensional array
	 * @param j
	 * 		Column index
	 * @return
	 * 		Min value
	 */
	public static int min(float[][] factors, int j){
		int k=0;
		float min=INF;
		for(int i=0; i<factors.length;i++){
			if(min>factors[i][j]){
				min=factors[i][j];
				k=i;
			}
		}
		return k;
	}

	/**
	 * Sorts the classification done in the algorithm ranking phase
	 * for a specific window
	 * @param factors
	 * 		Classification
	 * @param k
	 * 		Window index
	 */
	public static void trackSort(Ranking[][] factors, int k) {
		Ranking temp;
		int length=factors.length;
		boolean swap=true;
		while(swap){
			swap=false;
			for(int i=1;i<length;i++){
				if((factors[i-1][k].sigma>factors[i][k].sigma && factors[i][k].sigma!=0) || factors[i-1][k].sigma==0){
					temp=factors[i-1][k];
					factors[i-1][k]=factors[i][k];
					factors[i][k]=temp;
					swap=true;
				}			
			}
			length--;
		}
	}	
}