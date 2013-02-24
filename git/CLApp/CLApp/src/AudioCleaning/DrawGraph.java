package AudioCleaning;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

public class DrawGraph {
	
	public static void mixImage(String[] files, String name) throws IOException{
		BufferedImage dest;
		BufferedImage single;
		WritableRaster wr;
		File f;
		for(int i=0;i<files.length;i++){
			f=new File(files[i]);
			single = ImageIO.read(f);
			if(i==0){
				dest=new BufferedImage(single.getWidth(), single.getHeight()*files.length,single.getType());
				wr=dest.getRaster();
			}
			
			
		}
	}
	
	public static void createImage(float[] fs, String name){
		CleaningAlgorithm.save("temp.wav", CleaningAlgorithm.convertFloatsToDoubles(fs));
		Wave w=new Wave("temp.wav");
		File f;
		GraphicRender r=new GraphicRender();
		r.renderWaveform(w, name+".jpg");
		f=new File("temp.wav");
		f.delete();
	}

}
