package functions;

public class UtilFunctions {

	public static double expLT0Approx(final double x) {
		if (x<-5) {
			return 0;
		}
		if(x==0) {
			return 1;
		}
		return (0.3877*x+1.959)/(x*x - 1.332 * x + 1.981);
	}
}
