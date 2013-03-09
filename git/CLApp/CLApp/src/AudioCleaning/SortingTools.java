package AudioCleaning;

public class SortingTools {

	static float INF=Float.MAX_VALUE;
	//Computing the minimum value in a float array
	static int min(float[] valori){
		int k=0;
		float min=INF;
		for(int i=0; i<valori.length;i++){
			if(min>valori[i]){
				min=valori[i];
				k=i;
			}
		}
		return k;
	}

	//compute minimum value in a column of a float matrix
	static int min(float[][] factors, int j){
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

	//compute minimum value like the previous function, with doubles matrix
	@SuppressWarnings("unused")
	static int min(double[][] factors, int j){
		int k=0;
		double min=INF;
		for(int i=0; i<factors.length;i++){
			if(min>factors[i][j]){
				min=factors[i][j];
				k=i;
			}
		}
		return k;
	}

	//Computes minimum trace with a Ranking Array
	@SuppressWarnings("unused")
	static int minTrack(Ranking[] factors){
		int min=0;
		double value=INF;
		for(int i=1;i<factors.length;i++){
			if(value>factors[i].sigma){
				value=factors[i].sigma;
				min=factors[i].pos;
			}
		}
		return min;
	}

	//computes minimum trace with a 2-D Ranking Array
	@SuppressWarnings("unused")
	static int minTrack(Ranking[][] factors, int j){
		int min=0;
		double  value=INF;
		for(int i=1;i<factors.length;i++){
			if(value>factors[i][j].sigma){
				value=factors[i][j].sigma;
				min=factors[i][j].pos;
			}
		}
		return min;
	}

	/* sorting tracks */
	static void trackSort(Ranking[] factors) {
		Ranking temp;
		int j=0;
		while(j<factors.length){
			for(int i=0,z=1;i<(factors.length-1-j) && z<(factors.length-j);i++,z++){
				if(factors[i].sigma>factors[z].sigma){
					temp=factors[i];
					factors[i]=factors[z];
					factors[z]=temp;
				}			
			}
			j++;
		}
		
	}

	/* sorting tracks with windows */
	static void trackSort(Ranking[][] factors, int k) {
		Ranking temp;
		int length=factors.length;
		boolean swap=true;
		while(swap){
			swap=false;
			for(int i=1;i<length;i++){
				if(factors[i-1][k].sigma>factors[i][k].sigma){
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