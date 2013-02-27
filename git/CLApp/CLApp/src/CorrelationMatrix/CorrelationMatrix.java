package CorrelationMatrix;

import java.text.DecimalFormat;

public class CorrelationMatrix {
	public CorrelationCell[][] matrix;
	public int dimension;
	
	CorrelationMatrix(){
	}
	public CorrelationMatrix(int i){
		matrix=new CorrelationCell[i][i];
		dimension=i;
	}
	//function to compute maximum row
	public int maxRow(){
		float max=0, sum=0;
		int indice=0;
		
		for(int i=0;i<dimension;i++){
			sum=0;
			for(int j=0;j<dimension;j++){
				sum+=matrix[i][j].correlation;
			}
			if(max<sum){
				max=sum;
				indice=i;
			}
		}
		
		return indice;
	}
	//function to draw the matrix
	public void stamp(){
		DecimalFormat f = new DecimalFormat("###0.00000");
		System.out.print("In the cells: correlation, delay.\n\n");
		for(int i=0; i<dimension;i++){
			System.out.print("| ");
			for(int j=0; j<dimension; j++){
				System.out.print("\t"+f.format(matrix[i][j].correlation)+", "+matrix[i][j].delay+"\t|");
			}
			System.out.println();
		}
		System.out.println("\n");
	}
}
