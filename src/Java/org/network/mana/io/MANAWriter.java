package Java.org.network.mana.io;

import Java.org.network.mana.base_components.sparse.WeightData;
import Java.org.network.mana.mana_components.MANA_Sector;
import Java.org.network.mana.mana_components.MANA_Unit;
import Java.org.network.mana.utils.Utils;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt32;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MANAWriter {

    /**
     * Prints relevant network data to a matlab .mat file which can be accessed with matlab.
     * @param outDir
     * @param outPrefix
     * @param time
     * @param dt
     */
    public static void printData2Matlab(final MANA_Unit unit, final String outDir, final String outPrefix,
                                 final double time, final double dt) {
        Map<String, double []> data = new HashMap<>();
        data.put("PrefFRs", new double[unit.getSize()]);
        data.put("EstFRs", new double[unit.getSize()]);
        data.put("Threshs", new double[unit.getSize()]);
        data.put("NormBaseExc", new double[unit.getSize()]);
        data.put("NormBaseInh", new double[unit.getSize()]);
        data.put("excSF", new double[unit.getSize()]);
        data.put("inhSF", new double[unit.getSize()]);

        data.put("x", new double[unit.getSize()]);
        data.put("y", new double[unit.getSize()]);
        data.put("z", new double[unit.getSize()]);
//        data.put("Positions", new double[unit.getSize()]);

        int i_offset = 0;
        for(String id : unit.sectors.keySet()) {
            MANA_Sector s = unit.sectors.get(id);
            System.arraycopy(s.target.prefFR, 0, data.get("PrefFRs"),
                    i_offset, s.getWidth());
            s.target.estFR.copyTo(data.get("EstFRs"), i_offset);
            System.arraycopy(s.target.getThresholds(), 0, data.get("Threshs"),
                    i_offset, s.getWidth());
            System.arraycopy(s.target.normValsExc, 0, data.get("NormBaseExc"),
                    i_offset, s.getWidth());
            System.arraycopy(s.target.normValsInh, 0, data.get("NormBaseInh"),
                    i_offset, s.getWidth());
            System.arraycopy(s.target.inh_sf, 0, data.get("inhSF"),
                    i_offset, s.getWidth());
            System.arraycopy(s.target.exc_sf, 0, data.get("excSF"),
                    i_offset, s.getWidth());
            System.arraycopy(s.target.getCoordinates(true)[0], 0, data.get("x"), i_offset, s.getWidth());
            System.arraycopy(s.target.getCoordinates(true)[1], 0, data.get("y"), i_offset, s.getWidth());
            System.arraycopy(s.target.getCoordinates(true)[2], 0, data.get("z"), i_offset, s.getWidth());
            i_offset+=s.getWidth();
        }
        List<MLArray> mlData = new ArrayList<MLArray>();
        for(String key : data.keySet()) {
            mlData.add(new MLDouble(key, data.get(key), 1));
        }
        WeightData wd = unit.getMatrix();
        Utils.addScalar(wd.srcInds, 1);
        Utils.addScalar(wd.tarInds, 1);

        mlData.add(new MLInt32("srcInds", wd.srcInds, 1));
        mlData.add(new MLInt32("tarInds", wd.tarInds, 1));
        mlData.add(new MLDouble("wtValues", wd.values, 1));

        MLCell asdfCell = new MLCell("asdf", new int[]{unit.getSize()+2, 1});
        collectSpikes(unit, time, dt);
        for(int ii=0, n=unit.getSize(); ii<n; ++ii) {
            asdfCell.set(new MLDouble("", Utils.getDoubleArr(unit.getAllSpikes().get(ii)), 1), ii);
        }
        asdfCell.set(new MLDouble("", new double[]{dt}, 1), unit.getSize());
        asdfCell.set(new MLDouble("", new double[] {unit.getSize(), time/dt}, 1), unit.getSize()+1);

        mlData.add(asdfCell);
        try {
            new MatFileWriter(outDir + File.separator + (int)(time+dt)/1000 + "_" + outPrefix + ".mat", mlData);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Flushes recorded spike data from each sector into a unit's allspikes field (which empties all the sector spike
     * data recorders). These are accumulated into allSpikes and previous data is left in tact there.
     * @param time
     * @param dt
     */
    public static void collectSpikes(MANA_Unit unit, double time, double dt) {
        int offset = 0;
        for(String id : unit.sectors.keySet()) {
            MANA_Sector sec = unit.sectors.get(id);
            List<ArrayList<Double>> spks = sec.spkDat.flushToASDFFormat(time, dt);
            for(int ii=0; ii<sec.getWidth(); ++ii) {
                unit.getAllSpikes().get(ii+offset).addAll(spks.get(ii));
            }
            offset += sec.getWidth();
        }
    }
}
