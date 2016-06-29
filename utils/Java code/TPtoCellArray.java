import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

public class TPtoCellArray {
	public static void main(String [] args) {
		ArrayList<double[]> modules = new ArrayList<double[]>();
		ArrayList<Double> sigs = new ArrayList<Double>();
		try(FileReader fr = new FileReader(args[0]);
			Scanner sc = new Scanner(fr);) {
			while(sc.hasNextLine()) {
				System.out.print(sc.next());
				sc.findInLine("size:");
				int size = sc.nextInt();
				System.out.println(" " + size);
				sc.findInLine("bs:");
				double sig = sc.nextDouble();
				sigs.add(sig);
				double [] mmems = new double[size];
				int ind = 0;
				sc.nextLine();
				while(sc.hasNextInt()) {
					mmems[ind++] = (double) sc.nextInt();
				}
				if (sc.hasNextLine()) sc.nextLine();
				modules.add(mmems);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		MLCell modCells = new MLCell("Modules", new int[]{modules.size(), 1});
		for (int i = 0; i < modules.size(); i++) {
			modCells.set(new MLDouble("Module" + i, modules.get(i), 1), i);
		}
		MLDouble sigVals = new MLDouble("sig_vals",sigs.toArray(new Double[sigs.size()]), 1);
		List<MLArray> col = new ArrayList<MLArray>();
		col.add(modCells);
		col.add(sigVals);
		try {
			new MatFileWriter().write("OSLOM_Modules.mat", col);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
