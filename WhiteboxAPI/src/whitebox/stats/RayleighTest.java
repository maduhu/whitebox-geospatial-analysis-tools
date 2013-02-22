/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.stats;

/**
 *
 * @author johnlindsay
 */
public class RayleighTest {
    public enum AngularUnit {
        DEGREE, RADIANS;
    }
    double[] directionData;
    double[] magnitudeData;
    boolean axialData = false;
    double pvalue = -1.0;
    double resultantVectorLength = -1.0;
    double resultantDirection = -9999.0;
    int n = 0;
    double rayleighsR = -1.0;
    double zStat = -1.0;
    AngularUnit au = AngularUnit.RADIANS;
    boolean transformationToRadianComplete = false;
    boolean transformationAxialComplete = false;
    
    final double twoPi = Math.PI * 2;
    
    // constructors
    public RayleighTest() {
        // zero-parameter constructor
    }
    
    public RayleighTest(double[] directionData, AngularUnit au, boolean axialData) {
        this.axialData = axialData;
        this.directionData = directionData.clone();
        this.n = directionData.length;
        this.au = au;
        this.magnitudeData = new double[n];
        for (int a = 0; a < n; a++) {
            this.magnitudeData[a] = 1;
        }
    }
    
    public RayleighTest(double[] directionData, AngularUnit au, boolean axialData, double[] magnitudeData) {
        this.axialData = axialData;
        this.directionData = directionData.clone();
        this.n = directionData.length;
        this.au = au;
        this.magnitudeData = magnitudeData.clone();
    }
    
    // properties
    public double getPvalue() throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        if (pvalue < 0) {
            calculatePValue();
        }
        return pvalue;
    }

    public double getResultantVectorLength() throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        return resultantVectorLength;
    }

    public double getResultantDirection() throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        return resultantDirection;
    }

    public double getRayleighsR() throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        if (pvalue < 0) {
            calculatePValue();
        }
        return rayleighsR;
    }

    public double getzStat() throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        if (pvalue < 0) {
            calculatePValue();
        }
        return zStat;
    }

    public int getN() {
        return n;
    }
    
    public double[] getDirectionData() {
        return directionData;
    }

    public void setDirectionData(double[] directionData) {
        this.directionData = directionData.clone();
        this.n = directionData.length;
    }

    public double[] getMagnitudeData() {
        return magnitudeData;
    }

    public void setMagnitudeData(double[] magnitudeData) {
        this.magnitudeData = magnitudeData.clone();
    }

    public boolean isAxialData() {
        return axialData;
    }

    public void setAxialData(boolean axialData) {
        this.axialData = axialData;
    }

    public AngularUnit getAngularUnit() {
        return au;
    }

    public void setAngularUnit(AngularUnit au) {
        this.au = au;
    }
    
    public String getTestOutput(double alpha) throws Exception {
        if (resultantVectorLength < 0) {
            calculateResultantVector();
        }
        if (pvalue < 0) {
            calculatePValue();
        }
        if (pvalue <= alpha) {
            return "The null hypothesis was rejected (alpha = " + alpha + "); the data are not distributed uniformly around the circle.";
        }
        return "The null hypothesis, that the data are distributed uniformly around the circle, could not be rejected (alpha = " + alpha + "). ";
    }
    
    
    // methods
    private void transformationAxial() {
        if (this.axialData) {
            for (int a = 0; a < n; a++) {
                this.directionData[a] = (2 * directionData[a]) % twoPi;
            }
            transformationAxialComplete = true;
        }
    }
    
    private void transformationToRadians() {
        if (au == AngularUnit.DEGREE) {
            for (int a = 0; a < n; a++) {
                this.directionData[a] = Math.toRadians(this.directionData[a]);
            }
            transformationToRadianComplete = true;
        }
    }
    
    private void calculateResultantVector() throws Exception {
        if (directionData == null) { throw new Exception("Error: direction data not specified."); }
        if (magnitudeData == null) { throw new Exception("Error: magnitude data not specified."); }
        if (directionData.length != magnitudeData.length) { throw new Exception("Error: direction and magnitude arrays must be the same size."); }
        
        if (au == AngularUnit.DEGREE) { transformationToRadians(); }
        if (axialData) { transformationAxial(); }
        
        double xComponent = 0;
        double yComponent = 0;
        double sumWeights = 0;
        for (int a = 0; a < n; a++) {
            xComponent += magnitudeData[a] * Math.cos(directionData[a]);
            yComponent += magnitudeData[a] * Math.sin(directionData[a]);
            sumWeights += magnitudeData[a];
        }
        resultantVectorLength = Math.sqrt(xComponent * xComponent + yComponent * yComponent) / sumWeights;
        resultantDirection = Math.atan2(yComponent, xComponent);
        if (au == AngularUnit.DEGREE) {
            resultantDirection = Math.toDegrees(resultantDirection);
        }
        if (this.axialData) {
            resultantDirection = resultantDirection / 2.0;
        }
        
    }
    
    private void calculatePValue() throws Exception {
        if (directionData == null) { throw new Exception("Error: direction data not specified."); }
        if (magnitudeData == null) { throw new Exception("Error: magnitude data not specified."); }
        if (directionData.length != magnitudeData.length) { throw new Exception("Error: direction and magnitude arrays must be the same size."); }
        if (resultantVectorLength < 0) { calculateResultantVector(); }
        
        rayleighsR = resultantVectorLength * n;
        zStat = rayleighsR * rayleighsR / n;
        pvalue = Math.exp(Math.sqrt(1 + 4 * n + 4 * (n * n - rayleighsR * rayleighsR)) - (1 + 2 * n));
    }
    
    public static void main(String[] args) {
        try {
            /* This is used for testing purposes. */
            //double[] testData = {150, 180, 210};
            double[] testData = {150, 0, 210}; // axially equivalent to the above array
            //double[] testData = {15, 45, 75};
            //double[] testData = {66, 75, 86, 88, 88, 93, 97, 101, 118, 130};
            //double[] testData = { 120, 180, 240 };
            RayleighTest rt = new RayleighTest(testData, AngularUnit.DEGREE, true);
            System.out.println("Resultant direction: " + rt.getResultantDirection());
            System.out.println("Resultant length: " + rt.getResultantVectorLength());
            System.out.println("N = " + rt.getN());
            System.out.println("Rayleigh's R: " + rt.getRayleighsR());
            System.out.println("p-value: " + rt.getPvalue());
            System.out.println(rt.getTestOutput(0.05));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
