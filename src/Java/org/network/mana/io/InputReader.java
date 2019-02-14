package Java.org.network.mana.io;

import Java.org.network.mana.base_components.neurons.InputNeurons;
import Java.org.network.mana.utils.Utils;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class InputReader {

    public static void readInputs(InputNeurons inp, String _filename) {

        Scanner sc = null;
        try {
            int noNeu;
            if(_filename.contains(".mat")) {
                MatFileReader mfr = new MatFileReader(_filename);
                MLCell asdf = (MLCell) mfr.getContent().get("asdf");
                if(asdf == null) {
                    throw new IOException("Cell array containing calcSpikeResponses times in mat-file must be named \"asdf\"");
                }
                ArrayList<MLArray> mlSpkT = asdf.cells();
                noNeu = mlSpkT.size()-2;
                inp.init(noNeu);
                for(int ii=0; ii<noNeu; ++ii) {
                    inp.getSpk_times()[ii] = new double[((MLDouble) mlSpkT.get(ii)).getSize()];
                    double[][] temp = ((MLDouble) mlSpkT.get(ii)).getArray();
                    int sz1 = temp.length;
                    int sz2 = temp[0].length;
                    if (sz2>sz1) {
                        for (int jj = 0; jj < inp.getSpk_times()[ii].length; ++jj) {
                            inp.getSpk_times()[ii][jj] = temp[0][jj];
                        }
                    } else {
                        for (int jj = 0; jj < inp.getSpk_times()[ii].length; ++jj) {
                            inp.getSpk_times()[ii][jj] = temp[jj][0];
                        }
                    }
                }
            } else {
                sc = new Scanner(new FileReader(_filename));
                noNeu = sc.nextInt();
                inp.init(noNeu);
                for(int ii=0; ii<noNeu; ++ii) {
                    Scanner lineSc = new Scanner(sc.nextLine());
                    ArrayList<Double> times = new ArrayList<Double>();
                    while(lineSc.hasNext()) {
                        times.add(lineSc.nextDouble());
                    }
                    inp.getSpk_times()[ii] = Utils.getDoubleArr(times);
                    lineSc.close();
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("Could not fine input file... exiting...");
            System.exit(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("Exiting...");
            System.exit(0);
        } catch (ClassCastException e) {
            e.printStackTrace();
            System.err.println("asdf was not a cell... asdf format must use cell array. Alternatively, calcSpikeResponses times must be doubles.");
            System.exit(0);
        } finally {
            if(sc!=null) {
                sc.close();
            }
        }
    }

}
