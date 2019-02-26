package Java.org.network.mana.base_components.sparse;

import java.util.Comparator;

public final class SrcTarPair {

	public final int src;
	public final int tar;

	public SrcTarPair(final int _src, final int _tar) {
		this.src = _src;
		this.tar = _tar;
	}

	public SrcTarPair(final int _src, final int _tar, double[] values) {
		this.src = _src;
		this.tar = _tar;
	}
	
	@Override
	public int hashCode() {
		return hashCodeGen(src, tar);
	}

	public static  int hashCodeGen(int src, int tar) {
		return 100003 * src + 777743 * tar;
	}

	@Override
	public boolean equals(final Object p) {
		if(!this.getClass().equals(p.getClass())) {
			return false;
		}
		return src == ((SrcTarPair)p).src && tar == ((SrcTarPair)p).tar;
	}

	public static Comparator<SrcTarPair> getComparator() {
		return (a, b) -> {
			if(a.tar < b.tar) {
				return -1;
			} else if (a.tar > b.tar) {
				return 1;
			} else {
				if(a.src < b.src) {
					return -1;
				} else if(a.src > b.src) {
					return 1;
				} else {
					return 0;
				}
			}
		};
	}
	
}
