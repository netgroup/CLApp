package AudioCleaning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.JavaPlot.Key;
import com.panayotis.gnuplot.layout.AutoGraphLayout;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.ImageTerminal;

public class PlotH {
	
	public static void main(String[] args){
		float[][] test=new float[2][4];
		for(int i=0;i<test.length;i++){
			for(int j=0;j<test[i].length;j++){
				test[i][j]= (float) ((Math.random()*1000)-500)%30;
			}
		}
        
        Plotting((float[][]) test);
	}
	
	public static double min(float[][] h, int l){
		double min=Double.MAX_VALUE;
		
		for(int j=0;j<h[l].length;j++){
			if(min>h[l][j])
				min=h[l][j];
		}
		
		return min;
	}
	
	public static double max(float[][] h, int l){
		double max=Double.MIN_VALUE;
		for(int j=0;j<h[l].length;j++){
			if(h[l][j]<0){
				if(max<(h[l][j]*(-1))){
					max=(h[l][j]*(-1));
				}
			}
			else{
				if(max<h[l][j])
					max=h[l][j];
			}
		}
		
		return max;
	}
	
	public static void Plotting(float[][] h){
		double min, max;
		JavaPlot plot=new JavaPlot();
		AutoGraphLayout auto=new AutoGraphLayout();
		auto.setDrawFirst(AutoGraphLayout.ROWSFIRST);
		auto.setOrientation(AutoGraphLayout.DOWNWARDS);
		auto.setColumns(0);
		auto.setRows(h.length);
		PlotStyle plotStyle=new PlotStyle();
		File file;
		ImageTerminal png = new ImageTerminal();
		
		float[][] temp=new float[h[0].length][2];
		
		for(int j=0;j<h.length;j++){
			for(int i=0;i<h[j].length;i++){
				temp[i][0]=i;
				temp[i][1]=h[j][i];
			}
			file = new File(j+"-plot.png");
		    try {
		        file.createNewFile();
		        png.processOutput(new FileInputStream(file));
		    } catch (FileNotFoundException ex) {
		        System.err.print(ex);
		    } catch (IOException ex) {
		        System.err.print(ex);
		    }
		    
		    plot.setTerminal(png);
			
			max=max(h,j);
			DataSetPlot data=new DataSetPlot(temp);
			plot.getPage().setLayout(auto);
			plotStyle.setStyle(Style.LINES);
			plot.setKey(Key.OFF);
			plot.setTitle("Track "+j);
			plot.getAxis("y").setBoundaries(((-1)*max)-1, max+1);
			plot.getAxis("x").setLabel("windows");
			plot.getAxis("x").setBoundaries(0, h[0].length-1);
			data.setPlotStyle(plotStyle);
			plot.addPlot(data);
			plot.plot();
			try {
		        ImageIO.write(png.getImage(), "png", file);
		    } catch (IOException ex) {
		        System.err.print(ex);
		    }
			plot=new JavaPlot();
		}
				
	}
}
