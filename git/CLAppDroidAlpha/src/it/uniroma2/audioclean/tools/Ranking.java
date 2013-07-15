package it.uniroma2.audioclean.tools;

/**
 * Ranking object
 * @author Daniele De Angelis
 *
 */
public class Ranking {
	public int pos;
	public double sigma;
	
	/**
	 * Constructor
	 *
	 */
	public Ranking(){
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
