
package whitebox.utilities;

/**
 *
 * @author johnlindsay
 */
public final class AxialData {
    static final double twoPi = Math.PI * 2;
    
    static public double rationalizeAxialAngle(double angle) {
        // this appears more complex than necessary because
        // Java's mod function is actually a remainder function
        // and doesn't handle the case of a negative value well.
        return (((angle * 2.0) % twoPi + twoPi) % twoPi) / 2.0;
    }
    
    static public double angularDifferenceInAxes(double angle1, double angle2) {
        angle1 = rationalizeAxialAngle(angle1);
        angle2 = rationalizeAxialAngle(angle2);
        return  Math.abs(angle1 - angle2);
    }
}
