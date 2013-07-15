package AudioCleaning;

/**
 * Ranking object
 * @author Daniele De Angelis
 *
 */
public class Ranking {
	int pos;
	double sigma;
	
	/**
	 * Constructor
	 *
	 */
	Ranking(){
		this.pos=0;
		this.sigma= 0.0;
	}
	/**
	 * Constructor
	 * @param pos
	 * 		Track index in his data structure
	 * @param h
	 * 		Value to sort
	 */
	Ranking(int pos, double h){
		this.pos=pos;
		this.sigma=h;
	}
}
