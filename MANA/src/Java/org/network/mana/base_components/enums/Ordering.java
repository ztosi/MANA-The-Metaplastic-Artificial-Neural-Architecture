package Java.org.network.mana.base_components.enums;

import Java.org.network.mana.utils.SrcTarDataPack;

import java.util.Comparator;

public enum Ordering {
    SOURCE, TARGET;
    /**
     * Provides the appropriate comparator for sorting a COO as tuples in a list
     * appropriate to the ordering (source or target major). Tuples must be:
     * {src#, tar#, vals...}
     * @return
     */
    public static Comparator<SrcTarDataPack> orderTypeTupleComp(Ordering ordering) {
        if (ordering == Ordering.TARGET) {
            return (SrcTarDataPack a, SrcTarDataPack b) ->
            {
                if (a.coo.tar < b.coo.tar) {
                    return -1;
                } else if (a.coo.tar > b.coo.tar) {
                    return 1;
                } else {
                    if (a.coo.src < b.coo.src)
                        return -1;
                    else if (a.coo.src > b.coo.src)
                        return 1;
                    else
                        return 0;
                }
            };
        } else {
            return (SrcTarDataPack a,SrcTarDataPack b) ->
            {
                if (a.coo.src < b.coo.src) {
                    return -1;
                } else if (a.coo.src > b.coo.src) {
                    return 1;
                } else {
                    if (a.coo.tar < b.coo.tar)
                        return -1;
                    else if (a.coo.tar > b.coo.tar)
                        return 1;
                    else
                        return 0;
                }
            };
        }
    }
}
