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
package plugins;

import org.apache.commons.math3.linear.*;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTZ;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterBase;
import whitebox.structures.XYPoint;

/**
 *
 * @author johnlindsay
 */
public class TiePointTransformation implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;
    private double[] forwardRegressCoeffX;
    private double[] forwardRegressCoeffY;
    private double[] backRegressCoeffX;
    private double[] backRegressCoeffY;
    private int polyOrder;
    private int numCoefficients;
    private double image1MinX;
    private double image1MinY;
    private double image2MinX;
    private double image2MinY;

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "ElongationRatio";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Elongation Ratio";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "The ratio between the difference "
                + "in the long and short axis of the minimum bounding box for "
                + "each polygon to the sum of the long and short axis.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"PatchShapeTools"};
        return ret;
    }

    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the
     * class that the plugin will send all feedback messages, progress updates,
     * and return objects.
     *
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }

    /**
     * Used to communicate a return object from a plugin tool to the main
     * Whitebox user-interface.
     *
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.println("Progress: " + progress + "%");
        }
    }

    /**
     * Sets the arguments (parameters) used by the plugin.
     *
     * @param args
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     *
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     *
     * @return a boolean describing whether or not the plugin is actively being
     * used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        try {
            int m, i, j, k;

            String inputGCPFile1 = args[0];
            String inputRasterFile1 = args[1];
            String inputGCPFile2 = args[2];
            String outputRasterFile = args[3];
            polyOrder = Integer.parseInt(args[4]);

            ShapeFile GCPs1 = new ShapeFile(inputGCPFile1);
            ShapeFile GCPs2 = new ShapeFile(inputGCPFile2);

            int n = GCPs1.getNumberOfRecords();
            if (n != GCPs2.getNumberOfRecords()) {
                System.err.println("Shapefiles must have the same number of GCPs.");
                return;
            }
            if (GCPs1.getShapeType().getBaseType() != ShapeType.POINT
                    || GCPs2.getShapeType().getBaseType() != ShapeType.POINT) {
                System.err.println("Shapefiles must be of a Point ShapeType.");
                return;
            }
            
            image1MinX = GCPs1.getxMin();
            image1MinY = GCPs1.getyMin();
            image2MinX = GCPs2.getxMin();
            image2MinY = GCPs2.getyMin();


            // Read the GCP data 
            double[] GCPs1X = new double[n];
            double[] GCPs1Y = new double[n];
            double[] GCPs2X = new double[n];
            double[] GCPs2Y = new double[n];

            i = 0;
            for (ShapeFileRecord record : GCPs1.records) {
                double[][] vertices = new double[1][1];
                ShapeType shapeType = record.getShapeType();
                switch (shapeType) {
                    case POINT:
                        whitebox.geospatialfiles.shapefile.Point recPoint =
                                (whitebox.geospatialfiles.shapefile.Point) (record.getGeometry());
                        vertices = recPoint.getPoints();
                        break;
                    case POINTZ:
                        PointZ recPointZ = (PointZ) (record.getGeometry());
                        vertices = recPointZ.getPoints();
                        break;
                    case POINTM:
                        PointM recPointM = (PointM) (record.getGeometry());
                        vertices = recPointM.getPoints();
                        break;
                    default:
                        System.err.println("Shapefiles must be of Point ShapeType");
                        return;
                }

                GCPs1X[i] = vertices[0][0]; // - image1MinX;
                GCPs1Y[i] = vertices[0][1]; // - image1MinY;

                i++;
            }

            i = 0;
            for (ShapeFileRecord record : GCPs2.records) {
                double[][] vertices = new double[1][1];
                ShapeType shapeType = record.getShapeType();
                switch (shapeType) {
                    case POINT:
                        whitebox.geospatialfiles.shapefile.Point recPoint =
                                (whitebox.geospatialfiles.shapefile.Point) (record.getGeometry());
                        vertices = recPoint.getPoints();
                        break;
                    case POINTZ:
                        PointZ recPointZ = (PointZ) (record.getGeometry());
                        vertices = recPointZ.getPoints();
                        break;
                    case POINTM:
                        PointM recPointM = (PointM) (record.getGeometry());
                        vertices = recPointM.getPoints();
                        break;
                    default:
                        System.err.println("Shapefiles must be of Point ShapeType");
                        return;
                }

                GCPs2X[i] = vertices[0][0]; // - image2MinX;
                GCPs2Y[i] = vertices[0][1]; // - image2MinY;

                i++;
            }

            calculateEquations(GCPs1X, GCPs1Y, GCPs2X, GCPs2Y);

//            boolean[] useGCP = new boolean[n];
//            for (i = 0; i < n; i++) {
//                useGCP[i] = true;
//            }
//
//            useGCP[2] = false;
////            useGCP[14] = false;
////            useGCP[18] = false;
//
//            int newN = 0;
//            for (i = 0; i < n; i++) {
//                if (useGCP[i]) {
//                    newN++;
//                }
//            }
//
//            double[] X1 = new double[newN];
//            double[] Y1 = new double[newN];
//            double[] X2 = new double[newN];
//            double[] Y2 = new double[newN];
//
//            j = 0;
//            for (i = 0; i < n; i++) {
//                if (useGCP[i]) {
//                    X1[j] = GCPs1X[i];
//                    Y1[j] = GCPs1Y[i];
//                    X2[j] = GCPs2X[i];
//                    Y2[j] = GCPs2Y[i];
//                    j++;
//                }
//            }
//
//            System.out.println("\n");
//
//            calculateEquations(X1, Y1, X2, Y2);
            
//            WhiteboxRaster input1 = new WhiteboxRaster(inputRasterFile1, "r");
            WhiteboxRaster input2 = new WhiteboxRaster(inputRasterFile1, "r");
            
            
            double image2North = input2.getNorth();
            double image2South = input2.getSouth();
            double image2West = input2.getWest();
            double image2East = input2.getEast();
            XYPoint topLeftCorner = getForwardCoordinates(image2West, image2North);
            XYPoint topRightCorner = getForwardCoordinates(image2East, image2North);
            XYPoint bottomLeftCorner = getForwardCoordinates(image2West, image2South);
            XYPoint bottomRightCorner = getForwardCoordinates(image2East, image2South);
            
            double outputNorth = Double.NEGATIVE_INFINITY;
            double outputSouth = Double.POSITIVE_INFINITY;
            double outputEast = Double.NEGATIVE_INFINITY;
            double outputWest = Double.POSITIVE_INFINITY;
            
            if (topLeftCorner.y > outputNorth) { outputNorth = topLeftCorner.y; }
            if (topLeftCorner.y < outputSouth) { outputSouth = topLeftCorner.y; }
            if (topLeftCorner.x > outputEast) { outputEast = topLeftCorner.x; }
            if (topLeftCorner.x < outputWest) { outputWest = topLeftCorner.x; }
            
            if (topRightCorner.y > outputNorth) { outputNorth = topRightCorner.y; }
            if (topRightCorner.y < outputSouth) { outputSouth = topRightCorner.y; }
            if (topRightCorner.x > outputEast) { outputEast = topRightCorner.x; }
            if (topRightCorner.x < outputWest) { outputWest = topRightCorner.x; }
            
            if (bottomLeftCorner.y > outputNorth) { outputNorth = bottomLeftCorner.y; }
            if (bottomLeftCorner.y < outputSouth) { outputSouth = bottomLeftCorner.y; }
            if (bottomLeftCorner.x > outputEast) { outputEast = bottomLeftCorner.x; }
            if (bottomLeftCorner.x < outputWest) { outputWest = bottomLeftCorner.x; }
            
            if (bottomRightCorner.y > outputNorth) { outputNorth = bottomRightCorner.y; }
            if (bottomRightCorner.y < outputSouth) { outputSouth = bottomRightCorner.y; }
            if (bottomRightCorner.x > outputEast) { outputEast = bottomRightCorner.x; }
            if (bottomRightCorner.x < outputWest) { outputWest = bottomRightCorner.x; }
            
            double nsRange = outputNorth - outputSouth;
            double ewRange = outputEast - outputWest;
            
            int nRows = input2.getNumberRows(); //(int)(nsRange / input2.getCellSizeY());
            int nCols = input2.getNumberColumns(); //(int)(ewRange / input2.getCellSizeX());
            
            WhiteboxRaster output = new WhiteboxRaster(outputRasterFile, outputNorth, 
                    outputSouth, outputEast, outputWest, nRows, nCols, input2.getDataScale(), 
                    input2.getDataType(), input2.getNoDataValue(), input2.getNoDataValue());
            
            
            double outputX, outputY;
            double inputX, inputY;
            int inputCol, inputRow;
            XYPoint point;
            double z;
            int oldProgress = -1;
            int progress;
            for (int row = 0; row < nRows; row++) {
                for (int col = 0; col < nCols; col++) {
                    outputX = output.getXCoordinateFromColumn(col);
                    outputY = output.getYCoordinateFromRow(row);
                    
                    // back transform them into image 2 coordinates.
                    point = getBackwardCoordinates(outputX, outputY);
                    
                    inputX = point.x;
                    inputY = point.y;
                    
                    inputCol = input2.getColumnFromXCoordinate(inputX);
                    inputRow = input2.getRowFromYCoordinate(inputY);
                    
                    z = input2.getValue(inputRow, inputCol);
                    
                    output.setValue(row, col, z);
                }
                if (cancelOp) { cancelOperation(); return; }
                progress = (int) (100f * row / (nRows - 1));
                if (progress != oldProgress) {
                    System.out.println(progress + "%");
                    oldProgress = progress;
                }
            }
            
            output.close();
            

        } catch (Exception e) {
            showFeedback(e.getMessage());
            showFeedback(e.getCause().toString());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }

    public void calculateEquations(double[] X1, double[] Y1,
            double[] X2, double[] Y2) {
        int m, i, j, k, p;

        int n = X1.length;

        // How many coefficients are there?
        numCoefficients = 0;

        for (j = 0; j <= polyOrder; j++) {
            for (k = 0; k <= (polyOrder - j); k++) {
                numCoefficients++;
            }
        }

        // Solve the forward transformation equations
        double[][] forwardCoefficientMatrix = new double[n][numCoefficients];
        for (i = 0; i < n; i++) {
            m = 0;
            for (j = 0; j <= polyOrder; j++) {
                for (k = 0; k <= (polyOrder - j); k++) {
                    forwardCoefficientMatrix[i][m] = Math.pow(X2[i], j) * Math.pow(Y2[i], k);
                    m++;
                }
            }
        }

        RealMatrix coefficients =
                new Array2DRowRealMatrix(forwardCoefficientMatrix, false);
        //DecompositionSolver solver = new SingularValueDecomposition(coefficients).getSolver();
        DecompositionSolver solver = new QRDecomposition(coefficients).getSolver();

        // do the x-coordinate first
        RealVector constants = new ArrayRealVector(X1, false);
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
            residualsX[i] = X1[i] - yHat;
            SSresidX += residualsX[i] * residualsX[i];
        }

        double sumX = 0;
        double SSx = 0;
        for (i = 0; i < n; i++) {
            SSx += X1[i] * X1[i];
            sumX += X1[i];
        }
        double varianceX = (SSx - (sumX * sumX) / n) / n;
        double SStotalX = (n - 1) * varianceX;
        double rsqX = 1 - SSresidX / SStotalX;

        System.out.println("x-coordinate r-square: " + rsqX);


        // now the y-coordinate 
        constants = new ArrayRealVector(Y1, false);
        solution = solver.solve(constants);
        forwardRegressCoeffY = new double[n];
        for (int a = 0; a < numCoefficients; a++) {
            forwardRegressCoeffY[a] = solution.getEntry(a);
        }

        double[] residualsY = new double[n];
        double[] combinedResidual = new double[n];
        double SSresidY = 0;
        for (i = 0; i < n; i++) {
            double yHat = 0.0;
            for (j = 0; j < numCoefficients; j++) {
                yHat += forwardCoefficientMatrix[i][j] * forwardRegressCoeffY[j];
            }
            residualsY[i] = Y1[i] - yHat;
            SSresidY += residualsY[i] * residualsY[i];
            combinedResidual[i] = Math.sqrt(residualsX[i] * residualsX[i]
                    + residualsY[i] * residualsY[i]);
        }



        double sumY = 0;
        double sumR = 0;
        double SSy = 0;
        double SSr = 0;
        for (i = 0; i < n; i++) {
            SSy += Y1[i] * Y1[i];
            SSr += combinedResidual[i] * combinedResidual[i];
            sumY += Y1[i];
            sumR += combinedResidual[i];
        }
        double varianceY = (SSy - (sumY * sumY) / n) / n;
        double varianceResiduals = (SSr - (sumR * sumR) / n) / n;
        double SStotalY = (n - 1) * varianceY;
        double rsqY = 1 - SSresidY / SStotalY;
        double residualsStdDev = Math.sqrt(varianceResiduals);

        System.out.println("y-coordinate r-square: " + rsqY);

        // Print the residuals.
        System.out.println("\nResiduals:");
        for (i = 0; i < n; i++) {
            System.out.println("Point " + (i + 1) + "\t" + residualsX[i]
                    + "\t" + residualsY[i] + "\t" + combinedResidual[i]
                    + "\t" + (combinedResidual[i] / residualsStdDev));
        }


        // Solve the backward transformation equations
        double[][] backCoefficientMatrix = new double[n][numCoefficients];
        for (i = 0; i < n; i++) {
            m = 0;
            for (j = 0; j <= polyOrder; j++) {
                for (k = 0; k <= (polyOrder - j); k++) {
                    backCoefficientMatrix[i][m] = Math.pow(X1[i], j) * Math.pow(Y1[i], k);
                    m++;
                }
            }
        }

        coefficients = new Array2DRowRealMatrix(backCoefficientMatrix, false);
        //DecompositionSolver solver = new SingularValueDecomposition(coefficients).getSolver();
        solver = new QRDecomposition(coefficients).getSolver();

        // do the x-coordinate first
        constants = new ArrayRealVector(X2, false);
        solution = solver.solve(constants);
        backRegressCoeffX = new double[n];
        for (int a = 0; a < numCoefficients; a++) {
            backRegressCoeffX[a] = solution.getEntry(a);
        }

        // now the y-coordinate 
        constants = new ArrayRealVector(Y2, false);
        solution = solver.solve(constants);
        backRegressCoeffY = new double[n];
        for (int a = 0; a < numCoefficients; a++) {
            backRegressCoeffY[a] = solution.getEntry(a);
        }
    }

    private XYPoint getForwardCoordinates(double x, double y) {
        XYPoint ret;
        int j, k, m;
        double x_transformed = 0;
        double y_transformed = 0;
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
    
    private XYPoint getBackwardCoordinates(double x, double y) {
        XYPoint ret;
        int j, k, m;
        double x_transformed = 0;
        double y_transformed = 0;
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

    //This method is only used during testing.
    public static void main(String[] args) {
        try {
            int polyOrder = 4;
//            String inputGCPFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/tiepoints 15-16 image 15.shp";
//            String inputRasterFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/A19411_15_Blue.dep";
//            String inputGCPFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/tiepoints 15-16 image 16.shp";
//            String inputRasterFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/A19411_16_Blue.dep";
//            String outputRasterFile = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/16 registered.dep";
            
//            String inputGCPFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/tiepoints final image 15-16.shp";
//            String inputRasterFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/tmp6.dep";
//            String inputGCPFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/tiepoints final image 17.shp";
//            String inputRasterFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/17 adjusted.dep";
//            String outputRasterFile = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/17 registered.dep";

//            String inputGCPFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/image 15 GCPs map.shp";
//            String inputRasterFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/A19411_15_Blue.dep";
//            String inputGCPFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/image 15 GCPs.shp";
//            String outputRasterFile = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/15 registered to map1.dep";
            
            String inputGCPFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/image 16 GCPs map.shp";
            String inputRasterFile1 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/A19411_16_Blue.dep";
            String inputGCPFile2 = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/image 16 GCPs.shp";
            String outputRasterFile = "/Users/johnlindsay/Documents/Data/Guelph Photomosaic/16 registered to map1.dep";
            
            
            args = new String[5];
            args[0] = inputGCPFile1;
            args[1] = inputRasterFile1;
            args[2] = inputGCPFile2;
            args[3] = outputRasterFile;
            args[4] = String.valueOf(polyOrder);

            TiePointTransformation tpt = new TiePointTransformation();
            tpt.setArgs(args);
            tpt.run();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
