/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.stats;

import java.util.ArrayList;
import org.apache.commons.math3.linear.*;
import whitebox.structures.XYPoint;

/**
 *
 * @author johnlindsay
 */
public class PolynomialLeastSquares2DFitting {

    private int polyOrder = 1;
    private double[] forwardRegressCoeffX;
    private double[] forwardRegressCoeffY;
    private double[] backRegressCoeffX;
    private double[] backRegressCoeffY;
    private int numCoefficients;
    private double[] xCoords1;
    private double[] yCoords1;
    private double[] xCoords2;
    private double[] yCoords2;
    private double[] residualsXY;
    //private boolean[] useGCP;
    private double xMin1;
    private double yMin1;
    private double xMin2;
    private double yMin2;
    private double overallRMSE = 0.0;

    public PolynomialLeastSquares2DFitting() {
    }

    public PolynomialLeastSquares2DFitting(double[] X1, double[] Y1,
            double[] X2, double[] Y2, int polyOrder) {
        this.polyOrder = polyOrder;

        addData(X1, Y1, X2, Y2);
    }

    public PolynomialLeastSquares2DFitting(ArrayList<XYPoint> data1,
            ArrayList<XYPoint> data2, int polyOrder) {
        this.polyOrder = polyOrder;

        double[] X1 = new double[data1.size()];
        double[] Y1 = new double[data1.size()];
        double[] X2 = new double[data2.size()];
        double[] Y2 = new double[data2.size()];

        int i = 0;
        for (XYPoint xy : data1) {
            X1[i] = xy.x;
            Y1[i] = xy.y;
            i++;
        }

        i = 0;
        for (XYPoint xy : data2) {
            X2[i] = xy.x;
            Y2[i] = xy.y;
            i++;
        }

        addData(X1, Y1, X2, Y2);
    }

    // properties
    public int getPolyOrder() {
        return polyOrder;
    }

    public void setPolyOrder(int polyOrder) {
        if (polyOrder < 1) {
            polyOrder = 1;
        }
        if (polyOrder > 10) {
            polyOrder = 10;
        }
        this.polyOrder = polyOrder;
    }

    public double[] getForwardRegressCoeffX() {
        return forwardRegressCoeffX;
    }

    public double[] getForwardRegressCoeffY() {
        return forwardRegressCoeffY;
    }

    public double[] getBackRegressCoeffX() {
        return backRegressCoeffX;
    }

    public double[] getBackRegressCoeffY() {
        return backRegressCoeffY;
    }

    public double[] getResidualsXY() {
        return residualsXY;
    }

    public double getOverallRMSE() {
        return overallRMSE;
    }

    // methods
    public void addData(ArrayList<XYPoint> data1, ArrayList<XYPoint> data2) {
        double[] X1 = new double[data1.size()];
        double[] Y1 = new double[data1.size()];
        double[] X2 = new double[data2.size()];
        double[] Y2 = new double[data2.size()];

        int i = 0;
        for (XYPoint xy : data1) {
            X1[i] = xy.x;
            Y1[i] = xy.y;
            i++;
        }

        i = 0;
        for (XYPoint xy : data2) {
            X2[i] = xy.x;
            Y2[i] = xy.y;
            i++;
        }

        addData(X1, Y1, X2, Y2);
    }

    public void addData(double[] X1, double[] Y1, double[] X2, double[] Y2) {
        int n = X1.length;
        if (Y1.length != n || X2.length != n || Y2.length != n) {
            return;
        }
        xCoords1 = new double[n];
        yCoords1 = new double[n];
        xCoords2 = new double[n];
        yCoords2 = new double[n];

        System.arraycopy(X1, 0, xCoords1, 0, n);
        System.arraycopy(Y1, 0, yCoords1, 0, n);
        System.arraycopy(X2, 0, xCoords2, 0, n);
        System.arraycopy(Y2, 0, yCoords2, 0, n);

        xMin1 = Double.POSITIVE_INFINITY;
        yMin1 = Double.POSITIVE_INFINITY;
        xMin2 = Double.POSITIVE_INFINITY;
        yMin2 = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (X1[i] < xMin1) {
                xMin1 = X1[i];
            }
            if (Y1[i] < yMin1) {
                yMin1 = Y1[i];
            }
            if (X2[i] < xMin2) {
                xMin2 = X2[i];
            }
            if (Y2[i] < yMin2) {
                yMin2 = Y2[i];
            }
        }

        calculateEquations();
    }

    public void calculateEquations() {
        try {
            int m, i, j, k;

            int n = xCoords2.length;

            // How many coefficients are there?
            numCoefficients = 0;

            for (j = 0; j <= polyOrder; j++) {
                for (k = 0; k <= (polyOrder - j); k++) {
                    numCoefficients++;
                }
            }

//            for (i = 0; i < n; i++) {
//                xCoords1[i] -= xMin1;
//                yCoords1[i] -= yMin1;
//                xCoords2[i] -= xMin2;
//                yCoords2[i] -= yMin2;
//            }

            // Solve the forward transformation equations
            double[][] forwardCoefficientMatrix = new double[n][numCoefficients];
            for (i = 0; i < n; i++) {
                m = 0;
                for (j = 0; j <= polyOrder; j++) {
                    for (k = 0; k <= (polyOrder - j); k++) {
                        forwardCoefficientMatrix[i][m] = Math.pow(xCoords1[i], j) * Math.pow(yCoords1[i], k);
                        m++;
                    }
                }
            }

            RealMatrix coefficients =
                    new Array2DRowRealMatrix(forwardCoefficientMatrix, false);
            //DecompositionSolver solver = new SingularValueDecomposition(coefficients).getSolver();
            DecompositionSolver solver = new QRDecomposition(coefficients).getSolver();

            // do the x-coordinate first
            RealVector constants = new ArrayRealVector(xCoords2, false);
            RealVector solution = solver.solve(constants);
            forwardRegressCoeffX = new double[n];
            for (int a = 0; a < numCoefficients; a++) {
                forwardRegressCoeffX[a] = solution.getEntry(a);
            }

            double[] residualsX = new double[n];
            double SSresidX = 0;
            for (i = 0; i < n; i++) {
                double yHat = 0.0;
                for (j = 0; j < numCoefficients; j++) {
                    yHat += forwardCoefficientMatrix[i][j] * forwardRegressCoeffX[j];
                }
                residualsX[i] = xCoords2[i] - yHat;
                SSresidX += residualsX[i] * residualsX[i];
            }

            double sumX = 0;
            double SSx = 0;
            for (i = 0; i < n; i++) {
                SSx += xCoords2[i] * xCoords2[i];
                sumX += xCoords2[i];
            }
            double varianceX = (SSx - (sumX * sumX) / n) / n;
            double SStotalX = (n - 1) * varianceX;
            double rsqX = 1 - SSresidX / SStotalX;

            //System.out.println("x-coordinate r-square: " + rsqX);


            // now the y-coordinate 
            constants = new ArrayRealVector(yCoords2, false);
            solution = solver.solve(constants);
            forwardRegressCoeffY = new double[numCoefficients];
            for (int a = 0; a < numCoefficients; a++) {
                forwardRegressCoeffY[a] = solution.getEntry(a);
            }

            double[] residualsY = new double[n];
            residualsXY = new double[n];
            double SSresidY = 0;
            for (i = 0; i < n; i++) {
                double yHat = 0.0;
                for (j = 0; j < numCoefficients; j++) {
                    yHat += forwardCoefficientMatrix[i][j] * forwardRegressCoeffY[j];
                }
                residualsY[i] = yCoords2[i] - yHat;
                SSresidY += residualsY[i] * residualsY[i];
                residualsXY[i] = Math.sqrt(residualsX[i] * residualsX[i]
                        + residualsY[i] * residualsY[i]);
            }



            double sumY = 0;
            double sumR = 0;
            double SSy = 0;
            double SSr = 0;
            for (i = 0; i < n; i++) {
                SSy += yCoords2[i] * yCoords2[i];
                SSr += residualsXY[i] * residualsXY[i];
                sumY += yCoords2[i];
                sumR += residualsXY[i];
            }
            double varianceY = (SSy - (sumY * sumY) / n) / n;
            double varianceResiduals = (SSr - (sumR * sumR) / n) / n;
            double SStotalY = (n - 1) * varianceY;
            double rsqY = 1 - SSresidY / SStotalY;
            overallRMSE = Math.sqrt(varianceResiduals);

            //System.out.println("y-coordinate r-square: " + rsqY);

//            // Print the residuals.
//            System.out.println("\nResiduals:");
//            for (i = 0; i < n; i++) {
//                System.out.println("Point " + (i + 1) + "\t" + residualsX[i]
//                        + "\t" + residualsY[i] + "\t" + residualsXY[i]);
//            }


            // Solve the backward transformation equations
            double[][] backCoefficientMatrix = new double[n][numCoefficients];
            for (i = 0; i < n; i++) {
                m = 0;
                for (j = 0; j <= polyOrder; j++) {
                    for (k = 0; k <= (polyOrder - j); k++) {
                        backCoefficientMatrix[i][m] = Math.pow(xCoords2[i], j) * Math.pow(yCoords2[i], k);
                        m++;
                    }
                }
            }

            coefficients = new Array2DRowRealMatrix(backCoefficientMatrix, false);
            //DecompositionSolver solver = new SingularValueDecomposition(coefficients).getSolver();
            solver = new QRDecomposition(coefficients).getSolver();

            // do the x-coordinate first
            constants = new ArrayRealVector(xCoords1, false);
            solution = solver.solve(constants);
            backRegressCoeffX = new double[numCoefficients];
            for (int a = 0; a < numCoefficients; a++) {
                backRegressCoeffX[a] = solution.getEntry(a);
            }

            // now the y-coordinate 
            constants = new ArrayRealVector(yCoords1, false);
            solution = solver.solve(constants);
            backRegressCoeffY = new double[n];
            for (int a = 0; a < numCoefficients; a++) {
                backRegressCoeffY[a] = solution.getEntry(a);
            }
        } catch (Exception e) {
//            showFeedback("Error in ImageRectificationDialog.calculateEquations: "
//                    + e.getMessage());
        }
    }

    public XYPoint getForwardCoordinates(double x, double y) {
        XYPoint ret;
        int j, k, m;
        double x_transformed = 0; //mapXMin;
        double y_transformed = 0; //mapYMin;
        double term;
        m = 0;
        for (j = 0; j <= polyOrder; j++) {
            for (k = 0; k <= (polyOrder - j); k++) {
                term = Math.pow(x, j) * Math.pow(y, k);
                x_transformed += term * forwardRegressCoeffX[m];
                y_transformed += term * forwardRegressCoeffY[m];
                m++;
            }
        }

        ret = new XYPoint(x_transformed, y_transformed);

        return ret;
    }

    public XYPoint getBackwardCoordinates(double x, double y) {
        XYPoint ret;
        int j, k, m;
        double x_transformed = 0; //imageXMin;
        double y_transformed = 0; //imageYMin;
        double term;
        m = 0;
        for (j = 0; j <= polyOrder; j++) {
            for (k = 0; k <= (polyOrder - j); k++) {
                term = Math.pow(x, j) * Math.pow(y, k);
                x_transformed += term * backRegressCoeffX[m];
                y_transformed += term * backRegressCoeffY[m];
                m++;
            }
        }

        ret = new XYPoint(x_transformed, y_transformed);

        return ret;
    }
}
