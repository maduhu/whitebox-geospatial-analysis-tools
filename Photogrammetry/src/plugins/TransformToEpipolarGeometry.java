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

import static java.lang.Math.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
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

/**
 *
 * @author johnlindsay
 */
public class TransformToEpipolarGeometry {

    WhiteboxRaster rightImage;
    WhiteboxRaster leftImage;

    public static void main(String[] args) {
        TransformToEpipolarGeometry epiGeom = new TransformToEpipolarGeometry();
        epiGeom.run();
    }

    private void run() {

        try {
            // variables
            int a, b, c, i, j, k, n, r;
            int row, col;
            int progress, oldProgress;
            double x, y, z, newX, newY;
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

            // left image
            //String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus_C6430-74072-L9_253_Blue_clipped.dep";
            String leftImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 253.dep";

            // right image
            //String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus_C6430-74072-L9_254_Blue_clipped.dep";
            String rightImageName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 254.dep";

            // left image fiducials
            String leftFiducialsName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 253 fiducials.shp";

            // right image fiducials
            String rightFiducialsName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 254 fiducials.shp";

            // left image tie points
            String leftTiePointsName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 253 tie points.shp";

            // right image tie points
            String rightTiePointsName = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 254 tie points.shp";


            String leftOutputImageHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 253 epipolar.dep";
            String rightOutputImageHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/GuelphCampus 254 epipolar.dep";

            DBFField[] fields = new DBFField[4];
            fields[0] = new DBFField();
            fields[0].setName("FID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setDecimalCount(4);
            fields[0].setFieldLength(10);

            fields[1] = new DBFField();
            fields[1].setName("r");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setDecimalCount(4);
            fields[1].setFieldLength(10);

            fields[2] = new DBFField();
            fields[2].setName("d");
            fields[2].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[2].setDecimalCount(4);
            fields[2].setFieldLength(10);

            fields[3] = new DBFField();
            fields[3].setName("DIR");
            fields[3].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[3].setDecimalCount(4);
            fields[3].setFieldLength(10);

            // read the input data

            leftImage = new WhiteboxRaster(leftImageName, "r");
            leftImage.setForceAllDataInMemory(true);
            int nRowsLeft = leftImage.getNumberRows();
            int nColsLeft = leftImage.getNumberColumns();
            leftNodata = leftImage.getNoDataValue();

            rightImage = new WhiteboxRaster(rightImageName, "r");
            rightImage.setForceAllDataInMemory(true);
            int nRowsRight = rightImage.getNumberRows();
            int nColsRight = rightImage.getNumberColumns();
            rightNodata = rightImage.getNoDataValue();

            leftTiePoints = new ShapeFile(leftTiePointsName);
            if (leftTiePoints.getShapeType().getBaseType() != ShapeType.POINT) {
                throw new Exception("Tie points file must be of a POINT shape type.");
            }

            rightTiePoints = new ShapeFile(rightTiePointsName);
            if (rightTiePoints.getShapeType().getBaseType() != ShapeType.POINT) {
                throw new Exception("Tie points file must be of a POINT shape type.");
            }

            leftFiducials = new ShapeFile(leftFiducialsName);
            if (leftFiducials.getShapeType().getBaseType() != ShapeType.POINT) {
                throw new Exception("Fiducial points file must be of a POINT shape type.");
            }

            rightFiducials = new ShapeFile(rightFiducialsName);
            if (rightFiducials.getShapeType().getBaseType() != ShapeType.POINT) {
                throw new Exception("Fiducial points file must be of a POINT shape type.");
            }


            // make sure that the tie points files have the same number of points
            int numTiePoints = leftTiePoints.getNumberOfRecords();
            if (rightTiePoints.getNumberOfRecords() != numTiePoints) {
                throw new Exception("The input tie points files must contain the same number of features.");
            }

            // perform the initial tie point transformation
            for (r = 0; r < numTiePoints; r++) {
                double[][] leftPoint = leftTiePoints.getRecord(r).getGeometry().getPoints();
                double[][] rightPoint = rightTiePoints.getRecord(r).getGeometry().getPoints();

                leftTiePointsList.add(new XYPoint(leftPoint[0][0], leftPoint[0][1]));
                rightTiePointsList.add(new XYPoint(rightPoint[0][0], rightPoint[0][1]));
            }

            PolynomialLeastSquares2DFitting plsFit = new PolynomialLeastSquares2DFitting(
                    leftTiePointsList, rightTiePointsList, 1);

            double rmse = plsFit.getOverallRMSE();
            System.out.println("\nRMSE: " + rmse);


            // find the two principal points
            XYPoint leftPP = findPrincipalPoint(leftFiducials);
            XYPoint rightPP = findPrincipalPoint(rightFiducials);

            XYPoint leftCPP = plsFit.getBackwardCoordinates(rightPP);
            XYPoint rightCPP = plsFit.getForwardCoordinates(leftPP);

//            String leftOutputHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 253 pp and cpp.shp";
//
//            DBFField[] fields = new DBFField[1];
//            fields[0] = new DBFField();
//            fields[0].setName("FID");
//            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
//            fields[0].setDecimalCount(0);
//            fields[0].setFieldLength(10);
//            ShapeFile leftOutput = new ShapeFile(leftOutputHeader, ShapeType.POINT, fields);
//
//            outputPoint = new whitebox.geospatialfiles.shapefile.Point(leftPP.x, leftPP.y);
//            rowData = new Object[1];
//            rowData[0] = new Double(1);
//            leftOutput.addRecord(outputPoint, rowData);
//
//            outputPoint = new whitebox.geospatialfiles.shapefile.Point(leftCPP.x, leftCPP.y);
//            rowData = new Object[1];
//            rowData[0] = new Double(2);
//            leftOutput.addRecord(outputPoint, rowData);
//            
//            leftOutput.write();


//            String rightOutputHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus 254 pp and cpp.shp";
//
//            ShapeFile rightOutput = new ShapeFile(rightOutputHeader, ShapeType.POINT, fields);
//
//            outputPoint = new whitebox.geospatialfiles.shapefile.Point(rightPP.x, rightPP.y);
//            rowData = new Object[1];
//            rowData[0] = new Double(1);
//            rightOutput.addRecord(outputPoint, rowData);
//
//            outputPoint = new whitebox.geospatialfiles.shapefile.Point(rightCPP.x, rightCPP.y);
//            rowData = new Object[1];
//            rowData[0] = new Double(2);
//            rightOutput.addRecord(outputPoint, rowData);
//            
//            rightOutput.write();


            double flightlineAngle = -atan2((leftCPP.y - leftPP.y), (leftCPP.x - leftPP.x));
            double centerX = leftPP.x;
            double centerY = leftPP.y;
            newX = centerX + (leftCPP.x - centerX) * cos(flightlineAngle) - (leftCPP.y - centerY) * sin(flightlineAngle);
            newY = centerY + (leftCPP.x - centerX) * sin(flightlineAngle) + (leftCPP.y - centerY) * cos(flightlineAngle);


            String flightLineHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/temp1.shp";

            ShapeFile flightLine = new ShapeFile(flightLineHeader, ShapeType.POLYLINE, fields);

            PointsList points = new PointsList();
            

            points.addPoint(leftPP.x, leftPP.y);
            points.addPoint(newX, newY);
            int[] parts = {0};
            PolyLine poly = new PolyLine(parts, points.getPointsArray());
            rowData = new Object[1];
            rowData[0] = new Double(1);
            flightLine.addRecord(poly, rowData);

            
            flightLine.write();






            // left output image
            north = leftImage.getNorth();
            south = leftImage.getSouth();
            east = leftImage.getEast();
            west = leftImage.getWest();

            newNorth = Double.NEGATIVE_INFINITY;
            newSouth = Double.POSITIVE_INFINITY;
            newEast = Double.NEGATIVE_INFINITY;
            newWest = Double.POSITIVE_INFINITY;

            // rotate the corner points
            x = east;
            y = north;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            x = east;
            y = south;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            x = west;
            y = north;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            x = west;
            y = south;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            // figure out the rows and columns
            int nRowsLeftOut = (int) (round((newNorth - newSouth) / leftImage.getCellSizeY()));
            int nColsLeftOut = (int) (round((newEast - newWest) / leftImage.getCellSizeX()));

//            String leftOutputImageHeader = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus left epipolar output.dep";
            WhiteboxRaster leftOutputImage = new WhiteboxRaster(
                    leftOutputImageHeader, newNorth, newSouth, newEast, newWest,
                    nRowsLeftOut, nColsLeftOut, leftImage.getDataScale(),
                    leftImage.getDataType(), leftNodata, leftNodata);

            oldProgress = -1;
            for (row = 0; row < nRowsLeftOut; row++) {
                for (col = 0; col < nColsLeftOut; col++) {
                    x = leftOutputImage.getXCoordinateFromColumn(col);
                    y = leftOutputImage.getYCoordinateFromRow(row);

                    newX = centerX + x * cos(-flightlineAngle) - y * sin(-flightlineAngle);
                    newY = centerY + x * sin(-flightlineAngle) + y * cos(-flightlineAngle);

                    r = leftImage.getRowFromYCoordinate(newY);
                    c = leftImage.getColumnFromXCoordinate(newX);
                    z = leftImage.getValue(r, c);
                    leftOutputImage.setValue(row, col, z);
                }
                progress = (int) (100f * row / (nRowsLeftOut - 1));
                if (progress > oldProgress) {
                    System.out.println(progress + "%");
                    oldProgress = progress;
                }
            }

            leftOutputImage.close();






            // right output image
            north = rightImage.getNorth();
            south = rightImage.getSouth();
            east = rightImage.getEast();
            west = rightImage.getWest();

            newNorth = Double.NEGATIVE_INFINITY;
            newSouth = Double.POSITIVE_INFINITY;
            newEast = Double.NEGATIVE_INFINITY;
            newWest = Double.POSITIVE_INFINITY;

            // translate and rotate the corner points
            xyPoint = plsFit.getBackwardCoordinates(east, north);
            x = xyPoint.x;
            y = xyPoint.y;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            xyPoint = plsFit.getBackwardCoordinates(east, south);
            x = xyPoint.x;
            y = xyPoint.y;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            xyPoint = plsFit.getBackwardCoordinates(west, north);
            x = xyPoint.x;
            y = xyPoint.y;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            xyPoint = plsFit.getBackwardCoordinates(west, south);
            x = xyPoint.x;
            y = xyPoint.y;
            newX = (x - centerX) * cos(flightlineAngle) - (y - centerY) * sin(flightlineAngle);
            newY = (x - centerX) * sin(flightlineAngle) + (y - centerY) * cos(flightlineAngle);

            if (newY > newNorth) {
                newNorth = newY;
            }
            if (newY < newSouth) {
                newSouth = newY;
            }
            if (newX > newEast) {
                newEast = newX;
            }
            if (newX < newWest) {
                newWest = newX;
            }

            // figure out the rows and columns; the cell resolution should match the left image
            int nRowsRightOut = (int) (round((newNorth - newSouth) / leftImage.getCellSizeY()));
            int nColsRightOut = (int) (round((newEast - newWest) / leftImage.getCellSizeX()));

            leftImage.close();

            WhiteboxRaster rightOutputImage = new WhiteboxRaster(
                    rightOutputImageHeader, newNorth, newSouth, newEast, newWest,
                    nRowsRightOut, nColsRightOut, rightImage.getDataScale(),
                    rightImage.getDataType(), rightNodata, rightNodata);

            oldProgress = -1;
            for (row = 0; row < nRowsRightOut; row++) {
                for (col = 0; col < nColsRightOut; col++) {
                    x = rightOutputImage.getXCoordinateFromColumn(col);
                    y = rightOutputImage.getYCoordinateFromRow(row);

                    newX = centerX + x * cos(-flightlineAngle) - y * sin(-flightlineAngle);
                    newY = centerY + x * sin(-flightlineAngle) + y * cos(-flightlineAngle);

                    xyPoint = plsFit.getForwardCoordinates(newX, newY);

                    r = rightImage.getRowFromYCoordinate(xyPoint.y);
                    c = rightImage.getColumnFromXCoordinate(xyPoint.x);
                    z = rightImage.getValue(r, c);
                    rightOutputImage.setValue(row, col, z);
                }
                progress = (int) (100f * row / (nRowsRightOut - 1));
                if (progress > oldProgress) {
                    System.out.println(progress + "%");
                    oldProgress = progress;
                }
            }

            rightImage.close();

            rightOutputImage.close();





//            // open the output images again, this time in read mode and forcing them into memory
//
//            String matchedPointsLeftFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus matched points left3.shp";
//            String matchedPointsRightFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus matched points right3.shp";
//            String matchedPointsLineFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/campus matched points line3.shp";
//
//            ShapeFile matchedPointsLeft = new ShapeFile(matchedPointsLeftFile, ShapeType.POINT, fields);
//            ShapeFile matchedPointsRight = new ShapeFile(matchedPointsRightFile, ShapeType.POINT, fields);
//
//            ShapeFile matchedPointsLine = new ShapeFile(matchedPointsLineFile, ShapeType.POLYLINE, fields);
//
//
//            int searchWindowX = 180;
//            int searchWindowY = 10;
//            int searchGridSize = 100;
//            int kernelSize = 10;
//            int centerRowLeft, centerColLeft, centerRowRight, centerColRight;
//            double zLeft, zRight, d, direction, M, Q;
//            double stdDev, imageRange, imageRangeTarget;
//            int numMatchesFound = 0;
//            int numMatchesChecked = 0;
//            double edgeBuffer = 20;
//            boolean flag;
//            double templateMean, sampleMean, templateMin;
//            int searchWindowNCols = searchWindowX * 2 + kernelSize * 2;
//
//            leftImage = new WhiteboxRaster(leftOutputImageHeader, "r");
//            leftImage.setForceAllDataInMemory(true);
//            nRowsLeft = leftImage.getNumberRows();
//            nColsLeft = leftImage.getNumberColumns();
//            imageRange = leftImage.getMaximumValue() - leftImage.getMinimumValue();
//            imageRangeTarget = imageRange * 0.02; // there should be 2% of the overall range in variation within a kernel.
//
//            rightImage = new WhiteboxRaster(rightOutputImageHeader, "r");
//            rightImage.setForceAllDataInMemory(true);
//            nRowsRight = rightImage.getNumberRows();
//            nColsRight = rightImage.getNumberColumns();
//
//
////            String integralImageFile = leftImage.getHeaderFile().replace(".dep", "_integral.dep");
////            WhiteboxRaster integralImage = new WhiteboxRaster(integralImageFile, "rw",
////                    leftImage.getHeaderFile(), leftImage.getDataType(), 0);
////            oldProgress = -1;
////            double z2, sumOfCurrentRow;
////            for (r = 0; r < nRowsLeft; r++) {
////                sumOfCurrentRow = 0;
////                for (c = 0; c < nColsLeft; c++) {
////                    z = leftImage.getValue(r, c);
////                    if (z != leftNodata) {
////                        sumOfCurrentRow += z;
////                        z2 = integralImage.getValue(r - 1, c);
////                        if (z2 != leftNodata) {
////                            integralImage.setValue(r, c, sumOfCurrentRow + z2);
////                        } else {
////                            integralImage.setValue(r, c, sumOfCurrentRow);
////                        }
////                    } else {
////                        integralImage.setValue(r, c, leftNodata);
////                    }
////                }
////            }
////
////            integralImage.close();
//
//            oldProgress = -1;
//            for (r = 0; r < nRowsLeft; r += searchGridSize) {
//                for (c = 0; c < nColsLeft; c += searchGridSize) {
//                    zLeft = leftImage.getValue(r, c);
//                    if (zLeft != leftNodata) {
//                        // what is the row and column of this cell in the right image?
//                        x = leftImage.getXCoordinateFromColumn(c);
//                        y = leftImage.getYCoordinateFromRow(r);
//
//                        col = rightImage.getColumnFromXCoordinate(x);
//                        row = rightImage.getRowFromYCoordinate(y);
//
//                        if (col >= edgeBuffer && col < (nColsRight - edgeBuffer) && row >= edgeBuffer && row < (nRowsRight - edgeBuffer)) {
//                            numMatchesChecked++;
//
//                            double[][] template;
////                            kernelSize = 10;
////                            flag = true;
////                            do {
//
//                            template = new double[kernelSize * 2 + 1][kernelSize * 2 + 1];
////                                M = 0;
////                                Q = 0;
////                                n = 0;
////                                stdDev = 0;
//                            templateMin = Double.POSITIVE_INFINITY;
//                            for (a = -kernelSize; a <= kernelSize; a++) {
//                                for (b = -kernelSize; b <= kernelSize; b++) {
////                                        template[a + kernelSize][b + kernelSize] = leftImage.getValue(r + a, c + b);
//                                    z = leftImage.getValue(r + a, c + b);
//                                    template[a + kernelSize][b + kernelSize] = z;
//                                    if (z != leftNodata && z < templateMin) {
//                                        templateMin = z;
//                                    }
////                                        if (z != leftNodata) {
////                                            if (n > 0) {
////                                                M = M + (z - M) / (n + 1);
////                                                Q = Q + (n * (z - M) * (z - M)) / (n + 1);
////                                            } else {
////                                                M = z;
////                                                Q = 0;
////                                            }
////                                            n++;
////                                        }
//                                }
//                            }
//
////                                if (n > 1) {
////                                    stdDev = sqrt(Q / (n - 1));
////                                }
////                                if (stdDev >= imageRangeTarget) {
////                                    flag = false;
////                                } else {
////                                    kernelSize += 10;
////                                }
////                            } while (flag);
//                            double metricValue = 0;
//                            int bestRow = 0, bestCol = 0;
//                            for (i = -searchWindowY; i <= searchWindowY; i++) {
//                                double[] sumXCol = new double[searchWindowNCols];
//                                double[] sumXXCol = new double[searchWindowNCols];
//                                double[] sumYCol = new double[searchWindowNCols];
//                                double[] sumYYCol = new double[searchWindowNCols];
//                                double[] sumXYCol = new double[searchWindowNCols];
//                                int[] nCol = new int[searchWindowNCols];
//
////                                j = 0;
////                                for (a = -searchWindowX - kernelSize; a <= searchWindowX + kernelSize; a++) {
////                                    for (b = -kernelSize; b <= kernelSize; b++) {
////                                        
////                                    }
////                                    j++;
////                                }
//
//                                for (j = -searchWindowX; j <= searchWindowX; j++) {
////                                    centerRowLeft = r + i;
////                                    centerColLeft = c + j;
//
//                                    centerRowRight = row + i;
//                                    centerColRight = col + j;
//
//                                    double totalTemplate = 0, totalSample = 0;
//                                    double totalTS = 0, totalTSqr = 0, totalSSqr = 0;
//                                    double numerator = 0;
//                                    double denom1 = 0, denom2 = 0;
//                                    n = 0;
//                                    for (a = -kernelSize; a <= kernelSize; a++) {
//                                        for (b = -kernelSize; b <= kernelSize; b++) {
//                                            zLeft = template[a + kernelSize][b + kernelSize];
//                                            zRight = rightImage.getValue(centerRowRight + a, centerColRight + b);
//                                            if (zLeft != leftNodata && zRight != rightNodata) {
//                                                zLeft -= templateMin;
//                                                zRight -= templateMin;
//                                                n++;
//                                                totalTemplate += zLeft;
//                                                totalSample += zRight;
//                                                totalTS += zLeft * zRight;
//                                                totalTSqr += zLeft * zLeft;
//                                                totalSSqr += zRight * zRight;
//                                            }
//                                        }
//                                    }
////                                    double meanTemplate = totalTemplate / n;
////                                    double meanSample = totalSample / n;
////                                    for (a = -kernelSize; a <= kernelSize; a++) {
////                                        for (b = -kernelSize; b <= kernelSize; b++) {
////                                            zLeft = template[a + kernelSize][b + kernelSize];
////                                            zRight = rightImage.getValue(centerRowRight + a, centerColRight + b);
////                                            if (zLeft != leftNodata && zRight != rightNodata) {
////                                                numerator += (zLeft - meanTemplate) * (zRight - meanSample);
////                                                denom1 += (zLeft - meanTemplate) * (zLeft - meanTemplate);
////                                                denom2 += (zRight - meanSample) * (zRight - meanSample);
////                                            }
////                                        }
////                                    }
//
//
////                                    for (a = -kernelSize; a <= kernelSize; a++) {
////                                        for (b = -kernelSize; b <= kernelSize; b++) {
////                                            zLeft = template[a + kernelSize][b + kernelSize];
////                                            zRight = rightImage.getValue(centerRowRight + a, centerColRight + b);
////                                            if (zLeft != leftNodata && zRight != rightNodata) {
////                                                numerator += abs(zLeft - zRight);// zLeft * zRight;
////                                                n++;
////                                                denom1 += zLeft * zLeft;
////                                                denom2 += zRight * zRight;
////                                            }
////                                        }
////                                    }
//
//                                    if (n > 12) { //denom1 > 0 & denom2 > 0) {
//                                        numerator = (n * totalTS - totalTemplate * totalSample);
//                                        //z = (n) * totalTSqr - totalTemplate * totalTemplate;
//                                        denom1 = sqrt(n * totalTSqr - totalTemplate * totalTemplate);
//                                        denom2 = sqrt(n * totalSSqr - totalSample * totalSample);
//                                        double metric = numerator / (denom1 * denom2);
//                                        //double metric = numerator / (sqrt(denom1 * denom2));
//                                        if (metric > metricValue) {
//                                            metricValue = metric;
//                                            bestRow = i;
//                                            bestCol = j;
//                                        }
//                                    }
//                                }
//                            }
//                            if (metricValue > 0.90) {
//                                numMatchesFound++;
//
//                                d = abs(x - rightImage.getXCoordinateFromColumn(col + bestCol));
//                                newX = rightImage.getXCoordinateFromColumn(col + bestCol);
//                                newY = rightImage.getYCoordinateFromRow(row + bestRow);
//
//                                direction = atan2((newY - y), (newX - x));
//                                rowData = new Object[4];
//                                rowData[0] = new Double(kernelSize);
//                                rowData[1] = new Double(metricValue);
//                                rowData[2] = new Double(d);
//                                rowData[3] = new Double(direction);
//
//                                outputPoint = new whitebox.geospatialfiles.shapefile.Point(x, y);
//                                matchedPointsLeft.addRecord(outputPoint, rowData);
//
//                                outputPoint = new whitebox.geospatialfiles.shapefile.Point(newX, newY);
//                                matchedPointsRight.addRecord(outputPoint, rowData);
//
//                                PointsList points = new PointsList();
//                                points.addPoint(x, y);
//                                points.addPoint(newX, newY);
//                                int[] parts = {0};
//                                PolyLine poly = new PolyLine(parts, points.getPointsArray());
//                                matchedPointsLine.addRecord(poly, rowData);
//
//
//                            }
//                            //System.out.println(metricValue + " " + (r + bestRow) + " " + (c + bestCol));
//                        }
//                    }
//                }
//                progress = (int) (100f * r / (nRowsLeft - 1));
//                if (progress > oldProgress) {
//                    System.out.println(progress + "% complete, Matched Points: " + numMatchesFound + " (" + (100f * numMatchesFound / numMatchesChecked) + "%)");
//                    oldProgress = progress;
//                }
//            }
//
//            matchedPointsLeft.write();
//            matchedPointsRight.write();
//            matchedPointsLine.write();
//
//            leftImage.close();
//            rightImage.close();

            System.out.println("Done!");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private XYPoint findPrincipalPoint(ShapeFile fiducials) {
        XYPoint pp = null;
        try {
            int r, n;
            ArrayList<XYPoint> fiducialMarks = new ArrayList<>();
            for (r = 0; r < fiducials.getNumberOfRecords(); r++) {
                double[][] point = fiducials.getRecord(r).getGeometry().getPoints();
                for (int i = 0; i < point.length; i++) {
                    fiducialMarks.add(new XYPoint(point[i][0], point[i][1]));
                }
            }

            int numMarks = fiducialMarks.size();

            if (numMarks == 8) {

                double psi = 0;
                double x, y;
                double DegreeToRad = Math.PI / 180;
                double[] axes = new double[2];
                double[][] axesEndPoints = new double[4][2];
                double newXAxis = 0;
                double newYAxis = 0;
                double longAxis;
                double shortAxis;
                final double rightAngle = Math.toRadians(90);
                double[] newBoundingBox = new double[4];
                double slope;
                double boxCentreX, boxCentreY;
                double[][] verticesRotated = new double[8][2];
                double east = Double.NEGATIVE_INFINITY;
                double west = Double.POSITIVE_INFINITY;
                double north = Double.NEGATIVE_INFINITY;
                double south = Double.POSITIVE_INFINITY;
                XYPoint pt;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    if (pt.x > east) {
                        east = pt.x;
                    }
                    if (pt.x < west) {
                        west = pt.x;
                    }
                    if (pt.y > north) {
                        north = pt.y;
                    }
                    if (pt.y < south) {
                        south = pt.y;
                    }
                }

                double midX = west + (east - west) / 2.0;
                double midY = south + (north - south) / 2.0;

                axes[0] = 9999999;
                axes[1] = 9999999;
                slope = 0;
                boxCentreX = 0;
                boxCentreY = 0;
                // Rotate the edge cells in 0.5 degree increments.
                for (int m = 0; m <= 180; m++) {
                    psi = -m * 0.5 * DegreeToRad; // rotation in clockwise direction
                    // Rotate each edge cell in the array by m degrees.
                    for (int i = 0; i < 8; i++) {
                        pt = fiducialMarks.get(i);
                        x = pt.x - midX;
                        y = pt.y - midY;
                        verticesRotated[i][0] = (x * Math.cos(psi)) - (y * Math.sin(psi));
                        verticesRotated[i][1] = (x * Math.sin(psi)) + (y * Math.cos(psi));
                    }
                    // calculate the minimum bounding box in this coordinate 
                    // system and see if it is less
                    newBoundingBox[0] = Double.MAX_VALUE; // west
                    newBoundingBox[1] = Double.MIN_VALUE; // east
                    newBoundingBox[2] = Double.MAX_VALUE; // north
                    newBoundingBox[3] = Double.MIN_VALUE; // south
                    for (n = 0; n < 8; n++) {
                        x = verticesRotated[n][0];
                        y = verticesRotated[n][1];
                        if (x < newBoundingBox[0]) {
                            newBoundingBox[0] = x;
                        }
                        if (x > newBoundingBox[1]) {
                            newBoundingBox[1] = x;
                        }
                        if (y < newBoundingBox[2]) {
                            newBoundingBox[2] = y;
                        }
                        if (y > newBoundingBox[3]) {
                            newBoundingBox[3] = y;
                        }
                    }
                    newXAxis = newBoundingBox[1] - newBoundingBox[0];
                    newYAxis = newBoundingBox[3] - newBoundingBox[2];

                    if ((newXAxis * newYAxis) < (axes[0] * axes[1])) { // minimize the area of the bounding box.
                        axes[0] = newXAxis;
                        axes[1] = newYAxis;

                        if (axes[0] > axes[1]) {
                            slope = -psi;
                        } else {
                            slope = -(rightAngle + psi);
                        }
                        x = newBoundingBox[0] + newXAxis / 2;
                        y = newBoundingBox[2] + newYAxis / 2;
                        boxCentreX = midX + (x * Math.cos(-psi)) - (y * Math.sin(-psi));
                        boxCentreY = midY + (x * Math.sin(-psi)) + (y * Math.cos(-psi));
                    }
                }
                longAxis = Math.max(axes[0], axes[1]);
                shortAxis = Math.min(axes[0], axes[1]);

                axesEndPoints[0][0] = boxCentreX + longAxis / 2.0 * Math.cos(slope);
                axesEndPoints[0][1] = boxCentreY + longAxis / 2.0 * Math.sin(slope);
                axesEndPoints[1][0] = boxCentreX - longAxis / 2.0 * Math.cos(slope);
                axesEndPoints[1][1] = boxCentreY - longAxis / 2.0 * Math.sin(slope);

                axesEndPoints[2][0] = boxCentreX + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                axesEndPoints[2][1] = boxCentreY + shortAxis / 2.0 * Math.sin(rightAngle + slope);
                axesEndPoints[3][0] = boxCentreX - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                axesEndPoints[3][1] = boxCentreY - shortAxis / 2.0 * Math.sin(rightAngle + slope);


                // find the nearest point to each of the axes end points
                double dist;
                XYPoint p1 = new XYPoint();
                XYPoint p2 = new XYPoint();
                XYPoint p3 = new XYPoint();
                XYPoint p4 = new XYPoint();
                XYPoint p5 = new XYPoint();
                XYPoint p6 = new XYPoint();
                XYPoint p7 = new XYPoint();
                XYPoint p8 = new XYPoint();

                double minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (axesEndPoints[0][0] - x) * (axesEndPoints[0][0] - x) + (axesEndPoints[0][1] - y) * (axesEndPoints[0][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p1 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (axesEndPoints[1][0] - x) * (axesEndPoints[1][0] - x) + (axesEndPoints[1][1] - y) * (axesEndPoints[1][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p2 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (axesEndPoints[2][0] - x) * (axesEndPoints[2][0] - x) + (axesEndPoints[2][1] - y) * (axesEndPoints[2][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p3 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (axesEndPoints[3][0] - x) * (axesEndPoints[3][0] - x) + (axesEndPoints[3][1] - y) * (axesEndPoints[3][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p4 = pt;
                    }
                }

                double[][] cornerPoints = new double[4][2];
                cornerPoints[0][0] = axesEndPoints[0][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                cornerPoints[0][1] = axesEndPoints[0][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);

                cornerPoints[1][0] = axesEndPoints[0][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                cornerPoints[1][1] = axesEndPoints[0][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);

                cornerPoints[2][0] = axesEndPoints[1][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                cornerPoints[2][1] = axesEndPoints[1][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);

                cornerPoints[3][0] = axesEndPoints[1][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                cornerPoints[3][1] = axesEndPoints[1][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);


                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (cornerPoints[0][0] - x) * (cornerPoints[0][0] - x) + (cornerPoints[0][1] - y) * (cornerPoints[0][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p5 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (cornerPoints[2][0] - x) * (cornerPoints[2][0] - x) + (cornerPoints[2][1] - y) * (cornerPoints[2][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p6 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (cornerPoints[1][0] - x) * (cornerPoints[1][0] - x) + (cornerPoints[1][1] - y) * (cornerPoints[1][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p7 = pt;
                    }
                }

                minDist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 8; i++) {
                    pt = fiducialMarks.get(i);
                    x = pt.x;
                    y = pt.y;

                    dist = (cornerPoints[3][0] - x) * (cornerPoints[3][0] - x) + (cornerPoints[3][1] - y) * (cornerPoints[3][1] - y);
                    if (dist < minDist) {
                        minDist = dist;
                        p8 = pt;
                    }
                }

                // intersection 1
                XYPoint intersection = new XYPoint();

                double denominator = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x);

                if (denominator != 0) {
                    double xNumerator = (p1.x * p2.y - p1.y * p2.x) * (p3.x - p4.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x);
                    double yNumerator = (p1.x * p2.y - p1.y * p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x);
                    intersection.x = xNumerator / denominator;
                    intersection.y = yNumerator / denominator;
                } else {
                    throw new Exception("Something is wrong with the fiducial marks. Fiducial lines do not intersect");
                }

                XYPoint intersection2 = new XYPoint();

                denominator = (p5.x - p6.x) * (p7.y - p8.y) - (p5.y - p6.y) * (p7.x - p8.x);

                if (denominator != 0) {
                    double xNumerator = (p5.x * p6.y - p5.y * p6.x) * (p7.x - p8.x) - (p5.x - p6.x) * (p7.x * p8.y - p7.y * p8.x);
                    double yNumerator = (p5.x * p6.y - p5.y * p6.x) * (p7.y - p8.y) - (p5.y - p6.y) * (p7.x * p8.y - p7.y * p8.x);
                    intersection2.x = xNumerator / denominator;
                    intersection2.y = yNumerator / denominator;
                } else {
                    throw new Exception("Something is wrong with the fiducial marks. Fiducial lines do not intersect");
                }

                pp = new XYPoint((intersection.x + intersection2.x) / 2, (intersection.y + intersection2.y) / 2);

            } else if (numMarks == 4) {
                // are the fiducials arranged by the diagonal corners or the centres of sides?
                XYPoint p1 = fiducialMarks.get(0);
                XYPoint p2 = new XYPoint();
                XYPoint pt;
                double dist;
                double maxDist = 0;
                int k = 0;
                for (int a = 1; a < 4; a++) {
                    pt = fiducialMarks.get(a);
                    dist = Math.sqrt((pt.x - p1.x) * (pt.x - p1.x) + (pt.y - p1.y) * (pt.y - p1.y));
                    if (dist > maxDist) {
                        maxDist = dist;
                        p2 = pt;
                        k = a;
                    }
                }

                int i = 0, j = 0;
                switch (k) {
                    case 1:
                        i = 2;
                        j = 3;
                        break;

                    case 2:
                        i = 1;
                        j = 3;
                        break;

                    case 3:
                        i = 1;
                        j = 2;
                        break;
                }

                XYPoint p3 = fiducialMarks.get(i);
                XYPoint p4 = fiducialMarks.get(j);

                XYPoint intersection = new XYPoint();

                double denominator = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x);

                if (denominator != 0) {
                    double xNumerator = (p1.x * p2.y - p1.y * p2.x) * (p3.x - p4.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x);
                    double yNumerator = (p1.x * p2.y - p1.y * p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x);
                    intersection.x = xNumerator / denominator;
                    intersection.y = yNumerator / denominator;
                } else {
                    throw new Exception("Something is wrong with the fiducial marks. Fiducial lines do not intersect");
                }

                pp = new XYPoint(intersection.x, intersection.y);

            } else {
                throw new Exception("An unexpected number of fiducial marks were detected. There should be either 4 or 8 fiducial marks.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return pp;
        }
    }
}
