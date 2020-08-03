package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class MNISTLoader {


    public HashMap<Integer, ArrayList<MNISTImage>> mnistData = new HashMap<>();
    {
        for(int ii=0; ii<10; ++ii) {
            mnistData.put(ii, new ArrayList<>());
        }
    }

    public MNISTLoader(String imageFilename, String labelFilename) {

        try {
            byte[] inputStream = Files.readAllBytes(Paths.get(imageFilename));
            int rows = inputStream[11];
            int cols = inputStream[15];
            int numPx = rows*cols;
            byte[] labelStream = Files.readAllBytes(Paths.get(labelFilename));
            int offset = 16;
            int ii = 8;
            for(;ii<60000; ++ii) {
                int label = labelStream[ii];
                mnistData.get(label).add(new MNISTImage(inputStream, offset, label));
                offset+=numPx;
            }

        } catch(IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }

    }


}
