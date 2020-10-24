import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a template for a plugin that does not require one image
 * (be it that it does not require any, or that it lets the user
 * choose more than one image in a dialog).
 */
public class Remove_Islands implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param arg can be specified in plugins.config
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */

	private boolean px_array[][][];
	private boolean done_array[][][];
	private long progress;
	private long total;
	List<List> islands = new ArrayList<List>();
	
	public int tailProcess(List<int[]> island, List<int[]> new_island){
		boolean stop = true;
		List<int[]> tmp_island = new ArrayList<int[]>();

		for (int i = 0; i < new_island.size(); i++){
			int x = new_island.get(i)[0];
			int y = new_island.get(i)[1];
			int z = new_island.get(i)[2];
			
			int tmp[] = {x, y, z};
			island.add(tmp);
			
			if ((x - 1 >= 0) && px_array[x - 1][y][z] && !done_array[x - 1][y][z]){
				done_array[x - 1][y][z] = true;
				int tmp_px[] = {x - 1, y, z};
				tmp_island.add(tmp_px);
				stop = false;
			}
			if ((x + 1 < px_array.length) && px_array[x + 1][y][z] && !done_array[x + 1][y][z]){
				done_array[x + 1][y][z] = true;
				int tmp_px[] = {x + 1, y, z};
				tmp_island.add(tmp_px);
				stop = false;
			}
			
			if ((y - 1 >= 0) && px_array[x][y - 1][z] && !done_array[x][y - 1][z]){
				done_array[x][y - 1][z] = true;
				int tmp_px[] = {x, y - 1, z};
				tmp_island.add(tmp_px);
				stop = false;
			}
			if ((y + 1 < px_array[0].length) && px_array[x][y + 1][z] && !done_array[x][y + 1][z]){
				done_array[x][y + 1][z] = true;
				int tmp_px[] = {x, y + 1, z};
				tmp_island.add(tmp_px);
				stop = false;
			}
			
			if ((z - 1 >= 0) && px_array[x][y][z - 1] && !done_array[x][y][z - 1]){
				done_array[x][y][z - 1] = true;
				int tmp_px[] = {x, y, z - 1};
				tmp_island.add(tmp_px);
				stop = false;
			}
			if ((z + 1 < px_array[0][0].length) && px_array[x][y][z + 1] && !done_array[x][y][z + 1]){
				done_array[x][y][z + 1] = true;
				int tmp_px[] = {x, y, z + 1};
				tmp_island.add(tmp_px);
				stop = false;
			}
		}

		if (stop){
			return 0;
		}

		island.addAll(tmp_island);

		progress += tmp_island.size();
		IJ.showProgress((double)progress / total);
		
		return tailProcess(island, tmp_island);
		
	}
	
	@Override
	public void run(String arg) {
		IJ.log("");
		
		ImagePlus imp = IJ.getImage();
		ImageProcessor ip = imp.getProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		int stacks = imp.getStackSize();
//		int stacks = 200; // For debug

		long true_count = 0;

		total = (long)stacks;

		px_array = new boolean[width][height][stacks];
		done_array = new boolean[width][height][stacks];

		// Make image array
		progress = 0;
		IJ.showProgress((double)progress / total);
		for (int z = 0; z < stacks; z++){
			imp.setSlice(z + 1);
			for (int y = 0; y < height; y++){
				for (int x = 0; x < width; x++){
					int value = imp.getPixel(x, y)[0];
					if (value == 0){
						px_array[x][y][z] = false;
					}else{
						px_array[x][y][z] = true;
						true_count += 1;
					}
					
					done_array[x][y][z] = false;
				}
			}
			progress += 1;
			IJ.showProgress((double)progress / total);
		}

		// Get islands
		total = true_count;
		progress = 0;
		
		IJ.showProgress((double)progress / total);
		for (int z = 0; z < stacks; z++){
			for (int y = 0; y < height; y++){
				for (int x = 0; x < width; x++){
					if (px_array[x][y][z] && !done_array[x][y][z]){
						List<int[]> island = new ArrayList<int[]>();
						List<int[]> tmp_island = new ArrayList<int[]>();
						int[] tmp_px = {x, y, z};
						tmp_island.add(tmp_px);
						tailProcess(island, tmp_island);
						islands.add(island);
					}
				}
			}
		}

		IJ.log(islands.size() + " islands found");

		// Calculate size of the islands and count for each size
		HashMap<Integer, Integer> islands_count = new HashMap<Integer, Integer>();
		for (int i = 0; i < islands.size(); i++){
			Integer island_size = islands.get(i).size();
			Integer cnt = islands_count.get(island_size);
			if (null == cnt){
				islands_count.put(island_size, 1);
			}else{
				islands_count.put(island_size, cnt + 1);
			}
		}

		// Sort counts and show
		Map<Integer, Integer> treeMap = new TreeMap<Integer, Integer>(islands_count);
	    for (Integer key : treeMap.keySet()) {
	      IJ.log("Size:\t" + key + "\t Count:\t" + islands_count.get(key));
	    }
		

		// Set threshold for excluding small islands
		//double threshold = IJ.getNumber("Set threshold (minimum size of islands included)", 10000);
		int threshold = 10000;
		
		// Overwrite the image
		progress = 0;
		total = islands.size();
		IJ.showProgress((double)progress / total);
		for (int i = 0; i < islands.size(); i++){
			List<int[]> island = islands.get(i);
			Integer island_size = island.size();
			if (island_size < threshold){
				for (int j = 0; j < island.size(); j++){
					int[] px = island.get(j);
					imp.setSlice(px[2] + 1);
					ip.set(px[0], px[1], 0);
				}
			}
			progress += 1;
			IJ.showProgress((double)progress / total);
		}
		imp.updateAndDraw();
	}
}

