package CorrelationMatrix;

import java.text.DecimalFormat;
/**
 * Object correlation matrix
 * @author Daniele De Angelis
 *
 */
public class CorrelationMatrix {
	public CorrelationCell[][] matrix;
	public int dimension;
	public Integer maxRow=null;
	
	/**
	 * Constructor
	 */
	CorrelationMatrix(){
	}
	/**
	 * Constructor
	 * @param i
	 * 		Dimension ixi
	 */
	public CorrelationMatrix(int i){
		matrix=new CorrelationCell[i][i];
		dimension=i;
	}
	
	/**
	 * Return the maximum row index
	 * @return
	 * 		Index
	 */
	public int maxRow(){
		float max=0, sum=0;
		int indice=0;
		
		if(maxRow!=null){
			return maxRow;
		}
		
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
		maxRow=indice;
		return indice;
	}
	
	
	/**
	 * Return the second maximum row index
	 * @return
	 * 		Index
	 */
	public int secondMaxRow(){
		float max=0, sum=0;
		int indice=0;
		
		if(maxRow!=null){
			maxRow();
		}
		
		for(int i=0;i<dimension;i++){
			if(i!=maxRow){
				sum=0;
				for(int j=0;j<dimension;j++){
					sum+=matrix[i][j].correlation;
				}
				if(max<sum){
					max=sum;
					indice=i;
				}
			}
		}
		return indice;
	}
	
	/**
	 * Function that print the matrix on the command line
	 */
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
