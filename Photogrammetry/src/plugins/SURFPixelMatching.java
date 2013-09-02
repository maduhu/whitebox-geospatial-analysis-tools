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

import jopensurf.*;
import static java.lang.Math.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.PointsList;
import whitebox.geospatialfiles.shapefile.PolyLine;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.structures.XYPoint;
import whitebox.stats.PolynomialLeastSquares2DFitting;
import whitebox.structures.KdTree;
import whitebox.structures.RowPriorityGridCell;
import photogrammetry.Normalize2DHomogeneousPoints;

/**
 *
 * @author johnlindsay
 */
public class SURFPixelMatching {

    public static void main(String[] args) {
        SURFPixelMatching surf = new SURFPixelMatching();
        surf.run();
    }

    private void run() {

        try {
            // variables
            int a, b, c, i, j, k, n, r;
            int row, col;
            int progress, oldProgress;
            double x, y, z, newX, newY;
            double x2, y2;
            double north, south, east, west;
            double newNorth, newSouth, newEast, newWest;
            double rightNodata;
            double leftNodata;
            Object[] rowData;
            whitebox.geospatialfiles.shapefile.Point outputPoint;
            ShapeFile rightTiePoints;
            ShapeFile leftTiePoints;
            ShapeFile rightFiducials;
            ShapeFile leftFiducials;
            XYPoint xyPoint;
            ArrayList<XYPoint> leftTiePointsList = new ArrayList<>();
            ArrayList<XYPoint> rightTiePointsList = new ArrayList<>();

            float balanceValue = 0.81f;
            float threshold = 0.004f;
            int octaves = 4;
            double maxAllowableRMSE = 1.0;
            double matchingThreshold = 0.6;


            // left image
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus_C6430-74072-L9_253_Blue_clipped.dep";
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-82_Blue.dep";
            String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 253.dep";
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 253 epipolar.dep";
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-82_Blue low res.dep";
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/tmp1.dep";

            // right image
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus_C6430-74072-L9_254_Blue_clipped.dep";
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-83_Blue.dep";
            String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 254.dep";
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 254 epipolar.dep";
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-83_Blue low res.dep";
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 253.dep";


            String leftOutputHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/tmp5.shp";

            String rightOutputHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/tmp5_2.shp";


            DBFField[] fields = new DBFField[5];
            fields[0] = new DBFField();
            fields[0].setName("FID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setDecimalCount(4);
            fields[0].setFieldLength(10);

            fields[1] = new DBFField();
            fields[1].setName("ORIENT");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setDecimalCount(4);
            fields[1].setFieldLength(10);

            fields[2] = new DBFField();
            fields[2].setName("SCALE");
            fields[2].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[2].setDecimalCount(4);
            fields[2].setFieldLength(10);

            fields[3] = new DBFField();
            fields[3].setName("LAPLAC");
            fields[3].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[3].setDecimalCount(4);
            fields[3].setFieldLength(10);

            fields[4] = new DBFField();
            fields[4].setName("RESID");
            fields[4].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[4].setDecimalCount(4);
            fields[4].setFieldLength(10);

            // read the input data

            WhiteboxRaster leftImage = new WhiteboxRaster(leftImageName, "r");
//            leftImage.setForceAllDataInMemory(true);
            int nRowsLeft = leftImage.getNumberRows();
            int nColsLeft = leftImage.getNumberColumns();
            leftNodata = leftImage.getNoDataValue();

            WhiteboxRaster rightImage = new WhiteboxRaster(rightImageName, "r");
            //rightImage.setForceAllDataInMemory(true);
//            int nRowsRight = rightImage.getNumberRows();
//            int nColsRight = rightImage.getNumberColumns();
            rightNodata = rightImage.getNoDataValue();

//            ArrayList<InterestPoint> interest_points;
//            double threshold = 600;
//            double balanceValue = 0.9;
//            int octaves = 4;
//            ISURFfactory mySURF = SURF.createInstance(leftImage, balanceValue, threshold, octaves);
//            IDetector detector = mySURF.createDetector();
//            interest_points = detector.generateInterestPoints();
//            System.out.println("Interest points generated");
//            IDescriptor descriptor = mySURF.createDescriptor(interest_points);
//            descriptor.generateAllDescriptors();



            System.out.println("Performing SURF analysis on left image...");
            Surf leftSurf = new Surf(leftImage, balanceValue, threshold, octaves);
//            if (leftSurf.getNumberOfPoints() > 500000) {
//                System.out.println("Number of points exceeds limit, reset threshold: " + leftSurf.getNumberOfPoints());
//                return;
//            }
            if (leftSurf.getNumberOfPoints() == 0) {
                System.out.println("Number of points equals zero, reset threshold: " + leftSurf.getNumberOfPoints());
                return;
            }

            System.out.println("Performing SURF analysis on right image...");
            Surf rightSurf = new Surf(rightImage, balanceValue, threshold, octaves);
            if (rightSurf.getNumberOfPoints() == 0) {
                System.out.println("Number of points equals zero, reset threshold: " + leftSurf.getNumberOfPoints());
                return;
            }

            System.out.println("Matching points of interest...");
            Map<SURFInterestPoint, SURFInterestPoint> matchingPoints =
                    leftSurf.getMatchingPoints(rightSurf, matchingThreshold, false);

            int numTiePoints = matchingPoints.size();
            if (numTiePoints < 3) {
                System.err.println("The number of potential tie points is less than 3. Adjust your threshold parameters and retry.");
                return;
            }
            System.out.println(numTiePoints + " potential tie points located");
            System.out.println("Trimming outlier tie points...");


            boolean flag;
            do {
                flag = false;
                leftTiePointsList.clear();
                rightTiePointsList.clear();
                i = 0;
                for (SURFInterestPoint point : matchingPoints.keySet()) {
                    x = point.getX();
                    y = point.getY();

                    SURFInterestPoint target = matchingPoints.get(point);
                    x2 = target.getX();
                    y2 = target.getY();

                    leftTiePointsList.add(new XYPoint(x, y));
                    rightTiePointsList.add(new XYPoint(x2, y2));


                    i++;
                }

                PolynomialLeastSquares2DFitting overallFit = new PolynomialLeastSquares2DFitting(
                        leftTiePointsList, rightTiePointsList, 1);

                double maxDist = 0;
                SURFInterestPoint mostInfluentialPoint = null;
                i = 0;
                for (SURFInterestPoint point : matchingPoints.keySet()) {
                    leftTiePointsList.clear();
                    rightTiePointsList.clear();
                    for (SURFInterestPoint point2 : matchingPoints.keySet()) {
                        if (point2 != point) {
                            x = point2.getX();
                            y = point2.getY();

                            SURFInterestPoint target = matchingPoints.get(point2);
                            x2 = target.getX();
                            y2 = target.getY();

                            leftTiePointsList.add(new XYPoint(x, y));
                            rightTiePointsList.add(new XYPoint(x2, y2));
                        }
                    }
                    PolynomialLeastSquares2DFitting newFit = new PolynomialLeastSquares2DFitting(
                            leftTiePointsList, rightTiePointsList, 1);

                    x = point.getX();
                    y = point.getY();
                    XYPoint pt1 = overallFit.getForwardCoordinates(x, y);
                    XYPoint pt2 = newFit.getForwardCoordinates(x, y);
                    double dist = pt1.getSquareDistance(pt2);
                    if (dist > maxDist) {
                        maxDist = dist;
                        mostInfluentialPoint = point;
                    }
                }

                if (maxDist > 10 && mostInfluentialPoint != null) {
                    matchingPoints.remove(mostInfluentialPoint);
                    flag = true;
                }
                System.out.println(maxDist);
            } while (flag);

            int numPoints = matchingPoints.size();

            // create homogeneous points matrices
            double[][] leftPoints = new double[3][numPoints];
            double[][] rightPoints = new double[3][numPoints];

            i = 0;
            for (SURFInterestPoint point : matchingPoints.keySet()) {
                leftPoints[0][i] = point.getX();
                leftPoints[1][i] = point.getY();
                leftPoints[2][i] = 1;
                
                SURFInterestPoint target = matchingPoints.get(point);
                
                rightPoints[0][i] = target.getX();
                rightPoints[1][i] = target.getY();
                rightPoints[2][i] = 1;
                i++;
            }
            
            double[][] normalizedLeftPoints = Normalize2DHomogeneousPoints.normalize(leftPoints);
            RealMatrix Tl = MatrixUtils.createRealMatrix(Normalize2DHomogeneousPoints.T);
            double[][] normalizedRightPoints = Normalize2DHomogeneousPoints.normalize(rightPoints);
            RealMatrix Tr = MatrixUtils.createRealMatrix(Normalize2DHomogeneousPoints.T);
            
            RealMatrix pnts1 = MatrixUtils.createRealMatrix(normalizedLeftPoints);
            RealMatrix pnts2 = MatrixUtils.createRealMatrix(normalizedRightPoints);
            
            RealMatrix A =  MatrixUtils.createRealMatrix(buildA(normalizedLeftPoints, normalizedRightPoints));
            
            //RealMatrix ata = A.transpose().multiply(A);
            
            SingularValueDecomposition svd = new SingularValueDecomposition(A);
            
            RealMatrix V = svd.getV();
            RealVector V_smallestSingularValue = V.getColumnVector(8);
            RealMatrix F = MatrixUtils.createRealMatrix(3, 3);
            for (i = 0; i < 9; i++) {
                F.setEntry(i / 3, i % 3, V_smallestSingularValue.getEntry(i));
            }
            
            for (i = 0; i < V.getRowDimension(); i++) {
                System.out.println(V.getRowVector(i).toString());
            }
            
            SingularValueDecomposition svd2 = new SingularValueDecomposition(F);
            RealMatrix U = svd2.getU();
            RealMatrix S = svd2.getS();
            V = svd2.getV();
            RealMatrix m = MatrixUtils.createRealMatrix(new double[][]{{S.getEntry(1, 1), 0, 0}, {0, S.getEntry(2, 2), 0}, {0, 0, 0}});
            F = U.multiply(m).multiply(V).transpose();
            
            // Denormalise
            F = Tr.transpose().multiply(F).multiply(Tl);
            for (i = 0; i < F.getRowDimension(); i++) {
                System.out.println(F.getRowVector(i).toString());
            }
            
            svd2 = new SingularValueDecomposition(F);
            //[U,D,V] = svd(F,0);
            RealMatrix e1 = svd2.getV().getColumnMatrix(2); //hnormalise(svd2.getV(:,3));
            RealMatrix e2 = svd2.getU().getColumnMatrix(2); //e2 = hnormalise(U(:,3));
            
            e1.setEntry(0, 0, (e1.getEntry(0, 0) / e1.getEntry(2, 0)));
            e1.setEntry(1, 0, (e1.getEntry(1, 0) / e1.getEntry(2, 0)));
            e1.setEntry(2, 0, 1);
            
            e2.setEntry(0, 0, (e2.getEntry(0, 0) / e2.getEntry(2, 0)));
            e2.setEntry(1, 0, (e2.getEntry(1, 0) / e2.getEntry(2, 0)));
            e2.setEntry(2, 0, 1);
            
            
            System.out.println("");
            
//                boolean[] removeTiePoint = new boolean[numTiePoints];
//                double[] residuals = null;
//                double[] residualsOrientation = null;
//                boolean flag;
//                do {
//                    // perform the initial tie point transformation
//                    leftTiePointsList.clear();
//                    rightTiePointsList.clear();
//                    int numPointsIncluded = 0;
//                    i = 0;
//                    for (SURFInterestPoint point : matchingPoints.keySet()) {
//                        if (i < numTiePoints && !removeTiePoint[i]) {
//                            x = point.getX();
//                            y = point.getY();
//
//                            SURFInterestPoint target = matchingPoints.get(point);
//                            x2 = target.getX();
//                            y2 = target.getY();
//
//                            leftTiePointsList.add(new XYPoint(x, y));
//                            rightTiePointsList.add(new XYPoint(x2, y2));
//
//                            numPointsIncluded++;
//                        }
//                        i++;
//                    }
//
//                    PolynomialLeastSquares2DFitting plsFit = new PolynomialLeastSquares2DFitting(
//                            leftTiePointsList, rightTiePointsList, 1);
//
//                    double rmse = plsFit.getOverallRMSE();
//                    System.out.println("RMSE: " + rmse + " with " + numPointsIncluded + " points included.");
//
//                    flag = false;
//                    residuals = plsFit.getResidualsXY();
//                    residualsOrientation = plsFit.getResidualsOrientation();
//                    if (rmse > maxAllowableRMSE) {
//                        i = 0;
//                        for (k = 0; k < numTiePoints; k++) {
//                            if (!removeTiePoint[k]) {
//                                if (residuals[i] > 3 * rmse) {
//                                    removeTiePoint[k] = true;
//                                    flag = true;
//                                }
//                                i++;
//                            }
//                        }
//                    }
//                } while (flag);
//
//                i = 0;
//                for (k = 0; k < numTiePoints; k++) {
//                    if (!removeTiePoint[k]) {
//                        i++;
//                    }
//                }
                System.out.println(numPoints + " tie points remain.");

                System.out.println("Outputing tie point files...");

                ShapeFile leftOutput = new ShapeFile(leftOutputHeader, ShapeType.POINT, fields);

                ShapeFile rightOutput = new ShapeFile(rightOutputHeader, ShapeType.POINT, fields);



                i = 0;
                k = 0;
                for (SURFInterestPoint point : matchingPoints.keySet()) {
//                    if (i < numTiePoints && !removeTiePoint[i]) {
                        x = leftImage.getXCoordinateFromColumn((int) point.getX());
                        y = leftImage.getYCoordinateFromRow((int) point.getY());

                        SURFInterestPoint target = matchingPoints.get(point);
                        x2 = rightImage.getXCoordinateFromColumn((int) target.getX());
                        y2 = rightImage.getYCoordinateFromRow((int) target.getY());

                        outputPoint = new whitebox.geospatialfiles.shapefile.Point(x, y);
                        rowData = new Object[5];
                        rowData[0] = new Double(k + 1);
                        rowData[1] = new Double(point.getOrientation());
                        rowData[2] = new Double(point.getScale());
                        rowData[3] = new Double(point.getLaplacian());
                        rowData[4] = new Double(0); //residuals[k]);
                        leftOutput.addRecord(outputPoint, rowData);

                        outputPoint = new whitebox.geospatialfiles.shapefile.Point(x2, y2);
                        rowData = new Object[5];
                        rowData[0] = new Double(k + 1);
                        rowData[1] = new Double(target.getOrientation());
                        rowData[2] = new Double(target.getScale());
                        rowData[3] = new Double(target.getLaplacian());
                        rowData[4] = new Double(0); //residuals[k]);
                        rightOutput.addRecord(outputPoint, rowData);

                        k++;
//                    }
                    i++;
                }

                leftOutput.write();
                rightOutput.write();

                System.out.println("Done!");

            }   catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private double[][] buildA(double[][] points1, double[][] points2) {
        int numPoints = points1[0].length;
        double[][] result = new double[numPoints][9];
        for (int i = 0; i < numPoints; i++) {
            double x1i = points1[0][i];
            double x2i = points2[0][i];
            double y1i = points1[1][i];
            double y2i = points2[1][i];

            double[] row = new double[]{x1i * x2i, y1i * x2i, x2i, x1i * y2i, y1i * y2i, y2i,
                x1i, y1i, 1};
            System.arraycopy(row, 0, result[i], 0, 9);
        }
        return result;
    }
}
