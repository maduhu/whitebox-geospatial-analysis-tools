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
import java.util.ArrayList;
import java.util.List;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.structures.XYPoint;
import whitebox.stats.PolynomialLeastSquares2DFitting;
import whitebox.structures.KdTree;

/**
 *
 * @author johnlindsay
 */
public class LocateConjugatePrincipalPoint {
    // This method is only used during testing.

    public static void main(String[] args) {
        args = new String[2];

        long startTime = System.nanoTime();
        boolean conductFineSearch = false;
        int a, b, i, j, row, col, n, x, y;
        int referenceRadius;
        int refNeighbourhoodStart = 40;
        int refNeighbourhoodStep = 20;
        int maxNeighbourhoodSize = 200;
        epsilon = 1.2;
        int polyOrder = 2;
        int[][] xOffset;
        int[][] yOffset;
        int[] annulusX;
        int[] annulusY;
        double dist;
        ArrayList<Integer> annulusColumns = new ArrayList<>();
        ArrayList<Integer> annulusRows = new ArrayList<>();
        StringBuilder str;

        double[] referenceMeans;
        double[] referenceVariances;
        boolean[] coarsereferenceRings;
        double z;
        double M = 0, Q = 0;
        
        KdTree<Double> controlPointTree = new KdTree.SqrEuclid<>(2, new Integer(2000));

        try {

            String referenceFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-82_Blue.dep";
            //String ppFile1 = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-82 principal point.shp";
            String ppFile1 = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/test point3.shp";

            String transformedFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/Guelph_A19409-83_Blue.dep";
            //String outputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/pp mapped.shp";
            //String outputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/test point2 mapped.shp";
            String outputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/tmp2.shp";

            String referenceTiePoints = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/82 tie points.shp";
            String transformedTiePoints = "/Users/johnlindsay/Documents/Teaching/GEOG2420/airphotos/83 tie points.shp";


            DBFField[] fields = new DBFField[2];
            fields[0] = new DBFField();
            fields[0].setName("r1");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setDecimalCount(4);
            fields[0].setFieldLength(10);

            fields[1] = new DBFField();
            fields[1].setName("r2");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setDecimalCount(4);
            fields[1].setFieldLength(10);
            ShapeFile output = new ShapeFile(outputFile, ShapeType.POINT, fields);



            fields = new DBFField[2];
            fields[0] = new DBFField();
            fields[0].setName("r1");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setDecimalCount(4);
            fields[0].setFieldLength(10);

            fields[1] = new DBFField();
            fields[1].setName("r2");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setDecimalCount(4);
            fields[1].setFieldLength(10);
            ShapeFile output2 = new ShapeFile(outputFile.replace(".shp", "_2.shp"), ShapeType.POINT, fields);



            WhiteboxRaster referenceImage = new WhiteboxRaster(referenceFile, "r");
            referenceImage.setForceAllDataInMemory(true);
            int rows1 = referenceImage.getNumberRows();
            int cols1 = referenceImage.getNumberColumns();
            double noData1 = referenceImage.getNoDataValue();

            WhiteboxRaster transformedImage = new WhiteboxRaster(transformedFile, "r");
            transformedImage.setForceAllDataInMemory(true);

//            int rows2 = transformedImage.getNumberRows();
//            int cols2 = transformedImage.getNumberColumns();
            double noData2 = transformedImage.getNoDataValue();

            ShapeFile pp1 = new ShapeFile(ppFile1);
            if (pp1.getShapeType().getBaseType() != ShapeType.POINT
                    && pp1.getShapeType().getBaseType() != ShapeType.MULTIPOINT) {
                //showFeedback("The input shapefile must be of a 'POINT' or 'MULTIPOINT' data type.");
                return;
            }
            ShapeFileRecord record = pp1.getRecord(0);
            double[][] point; // = record.getGeometry().getPoints();

            ShapeFile refTiePoints = new ShapeFile(referenceTiePoints);
            if (refTiePoints.getShapeType().getBaseType() != ShapeType.POINT) {
                //showFeedback("The input shapefile must be of a 'POINT' or 'MULTIPOINT' data type.");
                return;
            }

            ShapeFile transTiePoints = new ShapeFile(transformedTiePoints);
            if (transTiePoints.getShapeType().getBaseType() != ShapeType.POINT) {
                //showFeedback("The input shapefile must be of a 'POINT' or 'MULTIPOINT' data type.");
                return;
            }

            int numTiePoints = refTiePoints.getNumberOfRecords();
            if (transTiePoints.getNumberOfRecords() != numTiePoints) {
                return;
            }

//            double[] refX = new double[numTiePoints];
//            double[] refY = new double[numTiePoints];
//            double[] transX = new double[numTiePoints];
//            double[] transY = new double[numTiePoints];

            ArrayList<XYPoint> tiePointsRef = new ArrayList<>();
            ArrayList<XYPoint> tiePointsTransform = new ArrayList<>();

            for (int r = 0; r < refTiePoints.getNumberOfRecords(); r++) {
                long startTimeLoop = System.nanoTime();

                double[][] refPoint = refTiePoints.getRecord(r).getGeometry().getPoints();

                int refCol = referenceImage.getColumnFromXCoordinate(refPoint[0][0]);
                int refRow = referenceImage.getRowFromYCoordinate(refPoint[0][1]);

                point = transTiePoints.getRecord(r).getGeometry().getPoints();
                int transCol = transformedImage.getColumnFromXCoordinate(point[0][0]);
                int transRow = transformedImage.getRowFromYCoordinate(point[0][1]);

                int referenceRadiusN = 0;
                referenceRadius = refNeighbourhoodStart;
                boolean flag = true;
                do {
                    xOffset = new int[referenceRadius + 1][];
                    yOffset = new int[referenceRadius + 1][];
                    for (i = 1; i <= referenceRadius; i++) {
                        // count the number of cells in the convolution annulus
                        n = 0;
                        annulusColumns.clear();
                        annulusRows.clear();
                        for (row = referenceRadius - i - 1; row <= referenceRadius + i + 1; row++) {
                            a = row - referenceRadius;
                            for (col = referenceRadius - i - 1; col <= referenceRadius + i + 1; col++) {
                                b = col - referenceRadius;
                                dist = Math.sqrt(a * a + b * b);
                                if (Math.abs(dist - i) < 0.5) {
                                    annulusRows.add(a);
                                    annulusColumns.add(b);
                                    n++;
                                }
                            }
                        }
                        annulusX = new int[n];
                        annulusY = new int[n];

                        for (a = 0; a < n; a++) {
                            annulusX[a] = annulusColumns.get(a);
                            annulusY[a] = annulusRows.get(a);
                        }

                        xOffset[i] = annulusX;
                        yOffset[i] = annulusY;

                    }

                    referenceMeans = new double[referenceRadius + 1];
                    referenceVariances = new double[referenceRadius + 1];
                    //double z;
                    double[][] filterData1 = new double[referenceRadius + 1][2];
                    double[][] filterData2 = new double[referenceRadius + 1][2];
                    double minValOfMean = Double.POSITIVE_INFINITY;
                    double maxValOfMean = Double.NEGATIVE_INFINITY;
                    double minValOfVariance = Double.POSITIVE_INFINITY;
                    double maxValOfVariance = Double.NEGATIVE_INFINITY;
                    //double M = 0, Q = 0;
                    for (i = 1; i <= referenceRadius; i++) {
                        double total = 0;
                        n = 0;
                        for (a = 0; a < xOffset[i].length - 1; a++) {
                            row = refRow + yOffset[i][a];
                            col = refCol + xOffset[i][a];
                            z = referenceImage.getValue(row, col);

                            if (z != noData1) {
                                total += z;
                                n++;
                                if (a > 0) {
                                    M = M + (z - M) / (a + 1);
                                    Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                } else {
                                    M = z;
                                    Q = 0;
                                }
                            }
                        }
                        referenceMeans[i] = total / n;
                        referenceVariances[i] = sqrt(Q / (n - 1));

                        if (referenceMeans[i] < minValOfMean) {
                            minValOfMean = referenceMeans[i];
                        }
                        if (referenceMeans[i] > maxValOfMean) {
                            maxValOfMean = referenceMeans[i];
                        }

                        if (referenceVariances[i] < minValOfVariance) {
                            minValOfVariance = referenceVariances[i];
                        }
                        if (referenceVariances[i] > maxValOfVariance) {
                            maxValOfVariance = referenceVariances[i];
                        }

                        filterData1[i][0] = i;
                        filterData1[i][1] = referenceMeans[i];
                        filterData2[i][0] = i;
                        filterData2[i][1] = referenceVariances[i];

//                System.out.println(i + "\t" + referenceMeans[i] + "\t" + referenceVariances[i]);
                    }

                    double valRange = maxValOfMean - minValOfMean;
//                    epsilon = 1.5; //valRange / 15;
                    double[][] newData1 = douglasPeuckerFilter(filterData1, 1, referenceMeans.length - 1);
                    valRange = maxValOfVariance - minValOfVariance;
                    //epsilon = 2; //valRange / 15;
                    double[][] newData2 = douglasPeuckerFilter(filterData2, 1, referenceVariances.length - 1);

                    referenceRadiusN = 0;
                    coarsereferenceRings = new boolean[referenceRadius + 1];
                    for (i = 0; i < newData1.length; i++) {
                        coarsereferenceRings[(int) newData1[i][0]] = true;
                        referenceRadiusN++;
                    }

                    for (i = 0; i < newData2.length; i++) {
                        if (coarsereferenceRings[(int) newData2[i][0]] == false) {
                            coarsereferenceRings[(int) newData2[i][0]] = true;
                            referenceRadiusN++;
                        }
                    }


//            System.out.println(referenceRadiusN);

//                    for (i = 1; i <= referenceRadius; i++) {
//                        if (coarsereferenceRings[i]) {
//                            System.out.println(i + "\t" + referenceMeans[i] + "\t" + referenceVariances[i]);
//                        }
//                    }

                    if (newData1.length > 8 && newData2.length > 8 && referenceRadiusN > 12) {
                        // there must be enough information on both the means data 
                        // and the variance data.
                        flag = false;
                    } else {
                        referenceRadius += refNeighbourhoodStep;
                        if (referenceRadius > maxNeighbourhoodSize) {
                            flag = false;
                        }
                    }
                } while (flag);
                
                if (referenceRadius < maxNeighbourhoodSize) {

                System.out.println("\n" + referenceRadius + "\t" + referenceRadiusN);

//            for (i = 0; i < newData1.length; i++) {
//                System.out.println(newData1[i][0] + "\t" + newData1[i][1]);
//            }
//
//            System.out.println("\n");
//
//            for (i = 0; i < newData2.length; i++) {
//                System.out.println(newData2[i][0] + "\t" + newData2[i][1]);
//            }



                double referenceMean = 0;
                double referenceVariance = 0;
                double referenceMeanDetailed = 0;
                double referenceVarianceDetailed = 0;

                for (a = 1; a < referenceRadius + 1; a++) {
                    referenceMeanDetailed += referenceMeans[a];
                    referenceVarianceDetailed += referenceVariances[a];
                    if (coarsereferenceRings[a]) {
                        referenceMean += referenceMeans[a];
                        referenceVariance += referenceVariances[a];
                    }
                }

                referenceMean = referenceMean / referenceRadiusN;
                referenceVariance = referenceVariance / referenceRadiusN;
                referenceMeanDetailed = referenceMeanDetailed / referenceRadius;
                referenceVarianceDetailed = referenceVarianceDetailed / referenceRadius;


                double[] referenceMeanDeviates = new double[referenceRadius + 1];
                double[] referenceVarianceDeviates = new double[referenceRadius + 1];
                double[] referenceMeanDeviatesDetailed = new double[referenceRadius + 1];
                double[] referenceVarianceDeviatesDetailed = new double[referenceRadius + 1];
                double sqrDev1 = 0;
                double sqrDev2 = 0;
                double sqrDev1Detailed = 0;
                double sqrDev2Detailed = 0;
                for (a = 1; a < referenceRadius + 1; a++) {
                    referenceMeanDeviatesDetailed[a] = referenceMeans[a] - referenceMeanDetailed;
                    referenceVarianceDeviatesDetailed[a] = referenceVariances[a] - referenceVarianceDetailed;
                    sqrDev1Detailed += (referenceMeans[a] - referenceMeanDetailed) * (referenceMeans[a] - referenceMean);
                    sqrDev2Detailed += (referenceVariances[a] - referenceVarianceDetailed) * (referenceVariances[a] - referenceVariance);

                    referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
                    referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;


                    if (coarsereferenceRings[a]) {
//                    referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
//                    referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;
                        sqrDev1 += (referenceMeans[a] - referenceMean) * (referenceMeans[a] - referenceMean);
                        sqrDev2 += (referenceVariances[a] - referenceVariance) * (referenceVariances[a] - referenceVariance);
                    }
                }

//                referenceImage.close();



                double maxCorrelations = 0;
                double maxCorrelation1 = 0;
                double maxCorrelation2 = 0;
                int maxCorrelationRow = -1, maxCorrelationCol = -1;

                int n1 = 0;
                int n2 = 0;

                int oldProgress = -1;
                int progress;
                int searchWindowSize = 30;
                for (int row2 = transRow - searchWindowSize; row2 <= transRow + searchWindowSize; row2++) { //5400; row2 < 6450; row2++) {
                    for (int col2 = transCol - searchWindowSize; col2 <= transCol + searchWindowSize; col2++) { //1500; col2 < 2500; col2++) {
                        n1++;
                        // find the means and variances of each annulus
                        double[] means = new double[referenceRadius + 1];
                        double[] variances = new double[referenceRadius + 1];
                        for (i = 1; i <= referenceRadius; i++) {
                            if (coarsereferenceRings[i]) {
                                double total = 0;
                                n = 0;
                                for (a = 0; a < xOffset[i].length - 1; a++) {
                                    row = row2 + yOffset[i][a];
                                    col = col2 + xOffset[i][a];
                                    z = transformedImage.getValue(row, col);
                                    if (z != noData2) {
                                        total += z;
                                        n++;
                                        if (a > 0) {
                                            M = M + (z - M) / (a + 1);
                                            Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                        } else {
                                            M = z;
                                            Q = 0;
                                        }
                                    }
                                }
                                means[i] = total / n;
                                variances[i] = sqrt(Q / (n - 1));
                            }
                        }

                        double sampleMean = 0;
                        double sampleVariance = 0;
                        for (a = 1; a < referenceRadius + 1; a++) {
                            if (coarsereferenceRings[a]) {
                                sampleMean += means[a];
                                sampleVariance += variances[a];
                            }
                        }
                        sampleMean = sampleMean / referenceRadiusN;
                        sampleVariance = sampleVariance / referenceRadiusN;


                        double sampleSqrdDev1 = 0;
                        double sampleSqrdDev2 = 0;

                        // correlate the reference and sample means and variances
                        double cov1 = 0;
                        double cov2 = 0;
                        for (a = 1; a < referenceRadius + 1; a++) {
                            if (coarsereferenceRings[a]) {
                                cov1 += (means[a] - sampleMean) * referenceMeanDeviates[a];
                                cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviates[a];
                                sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);
                            }
                        }


                        double r1 = cov1 / (Math.sqrt(sqrDev1 * sampleSqrdDev1));
                        double r2 = cov2 / (Math.sqrt(sqrDev2 * sampleSqrdDev2));

                        if (!conductFineSearch) {
                            if (r1 + r2 > maxCorrelations) {
                                maxCorrelations = r1 + r2;
                                maxCorrelation1 = r1;
                                maxCorrelation2 = r2;
                                maxCorrelationCol = col2;
                                maxCorrelationRow = row2;
                            }
                        } else {
                            if (r1 > 0.9 && r2 > 0.9) { // + r2 > 1.95) { // conduct a detailed correlation
                                n2++;


                                for (i = 1; i <= referenceRadius; i++) {
                                    if (!coarsereferenceRings[i]) {
                                        double total = 0;
                                        n = 0;
                                        for (a = 0; a < xOffset[i].length - 1; a++) {
                                            row = row2 + yOffset[i][a];
                                            col = col2 + xOffset[i][a];
                                            z = transformedImage.getValue(row, col);
                                            if (z != noData2) {
                                                total += z;
                                                n++;
                                                if (a > 0) {
                                                    M = M + (z - M) / (a + 1);
                                                    Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                                } else {
                                                    M = z;
                                                    Q = 0;
                                                }
                                            }
                                        }
                                        means[i] = total / n;
                                        variances[i] = sqrt(Q / (n - 1));
                                    }
                                }

                                sampleMean = 0;
                                sampleVariance = 0;
                                for (a = 1; a < referenceRadius + 1; a++) {
                                    sampleMean += means[a];
                                    sampleVariance += variances[a];

                                }
                                sampleMean = sampleMean / referenceRadius;
                                sampleVariance = sampleVariance / referenceRadius;

                                // correlate the reference and sample means and variances
                                cov1 = 0;
                                cov2 = 0;
                                sampleSqrdDev1 = 0;
                                sampleSqrdDev2 = 0;
                                for (a = 1; a < referenceRadius + 1; a++) {
                                    cov1 += (means[a] - sampleMean) * referenceMeanDeviatesDetailed[a];
                                    cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviatesDetailed[a];
                                    sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                    sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);

                                }

                                r1 = cov1 / (Math.sqrt(sqrDev1Detailed * sampleSqrdDev1));
                                r2 = cov2 / (Math.sqrt(sqrDev2Detailed * sampleSqrdDev2));


                                if (r1 + r2 > maxCorrelations) {
                                    maxCorrelations = r1 + r2;
                                    maxCorrelation1 = r1;
                                    maxCorrelation2 = r2;
                                    maxCorrelationCol = col2;
                                    maxCorrelationRow = row2;
                                }

                            }
                        }
                    }
//                    progress = (int) ((100.0 * row2) / rows2);
//                    if (progress > oldProgress) {
//                        System.out.println(progress + "% " + maxCorrelations);
//                        oldProgress = progress;
//                    }
                }

                if (maxCorrelationCol >= 0 && maxCorrelationRow >= 0) {

                    double x2 = transformedImage.getXCoordinateFromColumn(maxCorrelationCol);
                    double y2 = transformedImage.getYCoordinateFromRow(maxCorrelationRow);
                    whitebox.geospatialfiles.shapefile.Point PP = new whitebox.geospatialfiles.shapefile.Point(x2, y2);
                    Object[] rowData = new Object[2];
                    rowData[0] = new Double(maxCorrelation1);
                    rowData[1] = new Double(maxCorrelation2);
                    output.addRecord(PP, rowData);


                    PP = new whitebox.geospatialfiles.shapefile.Point(refPoint[0][0], refPoint[0][1]);
                    rowData = new Object[2];
                    rowData[0] = new Double(maxCorrelation1);
                    rowData[1] = new Double(maxCorrelation2);
                    output2.addRecord(PP, rowData);

                    
                    System.out.println("row: " + maxCorrelationRow + " , col: " + maxCorrelationCol + " , r: " + maxCorrelations);

                    long endTimeLoop = System.nanoTime();

                    double duration = (endTimeLoop - startTimeLoop) / 1000000000.0; // in seconds

                    str = new StringBuilder();
                    str.append("Duration: ");
                    if (duration > 86400) {
                        str.append((int) (Math.floor(duration / 86400))).append(" days, ");
                        duration = duration / 86400;
                    }
                    if (duration > 3600) {
                        str.append((int) (Math.floor(duration / 3600))).append(" hours, ");
                        duration = duration / 3600;
                    }
                    if (duration > 60) {
                        str.append((int) (Math.floor(duration / 60))).append(" minutes, ");
                        duration = duration / 60;
                    }
                    str.append(duration).append(" seconds");

                    System.out.println(str.toString());

                    if (conductFineSearch) {
                        System.out.println(n1 + " cells coarsely scanned with " + n2 + " (" + (100.0 * n2 / n1) + "%) cells finely scanned");
                    }

                    if (maxCorrelations > 1.9) {
                        tiePointsRef.add(new XYPoint(refPoint[0][0], refPoint[0][1]));
                        tiePointsTransform.add(new XYPoint(x2, y2));
                    }

                } else {
                    System.out.println("No suitable match could be located.");
                }

            }
            }

            numTiePoints = tiePointsRef.size();

            if (numTiePoints > 2) {
//                PolynomialLeastSquares2DFitting pls = new PolynomialLeastSquares2DFitting(tiePointsRef,
//                        tiePointsTransform, 1);
                List<KdTree.Entry<Double>> results;
                
                int newPolyOrder = polyOrder;
                if (newPolyOrder == 4 && tiePointsRef.size() < 15) {
                    newPolyOrder--;
                }
                if (newPolyOrder == 3 && tiePointsRef.size() < 10) {
                    newPolyOrder--;
                }
                if (newPolyOrder == 2 && tiePointsRef.size() < 6) {
                    newPolyOrder--;
                }
                
                numTiePoints = 0;
                for (XYPoint tie : tiePointsRef) {
                    double[] entry = {tie.x, tie.y};
                    controlPointTree.addPoint(entry, (double)numTiePoints);
                    numTiePoints++;
                }

                PolynomialLeastSquares2DFitting pls = new PolynomialLeastSquares2DFitting(
                        tiePointsRef, tiePointsTransform, newPolyOrder);

                double rmse = pls.getOverallRMSE();
                System.out.println("\nRMSE: " + rmse);

                double north = transformedImage.getNorth();
                double south = transformedImage.getSouth();
                double east = transformedImage.getEast();
                double west = transformedImage.getWest();

                int oldProgress = -1;
                int progress;
                for (int r = 0; r < rows1; r += 750) {
                    for (int c = 0; c < cols1; c += 750) {

                        double refXCoord = referenceImage.getXCoordinateFromColumn(c);
                        double refYCoord = referenceImage.getYCoordinateFromRow(r);
                        
                        double[] entry = {refXCoord, refYCoord};
                        int numNearestNeighbours = 12;
                        if (numTiePoints < 12) {
                            numNearestNeighbours = numTiePoints;
                        }
                        results = controlPointTree.nearestNeighbor(entry, numNearestNeighbours, true);
                        
                        j = results.size();
                        double[] X1 = new double[j];
                        double[] Y1 = new double[j];
                        double[] X2 = new double[j];
                        double[] Y2 = new double[j];
                        
                        for (int k = 0; k < j; k++) {
                            double val = results.get(k).value;
                            X1[k] = tiePointsRef.get((int)val).x;
                            Y1[k] = tiePointsRef.get((int)val).y;
                            X2[k] = tiePointsTransform.get((int)val).x;
                            Y2[k] = tiePointsTransform.get((int)val).y;
                            
                        }
                        
                        // calculate the scaling factor
                        
                        
                        pls = new PolynomialLeastSquares2DFitting(X1, Y1, X2, Y2, 1);
                        
                        XYPoint transCoords = pls.getForwardCoordinates(
                                refXCoord, refYCoord);

                        if (transCoords.x <= east && transCoords.x >= west
                                && transCoords.y >= south && transCoords.y <= north) {

                            int refCol = c; //referenceImage.getColumnFromXCoordinate(transCoords.x);
                            int refRow = r; //referenceImage.getRowFromYCoordinate(transCoords.y);

//                            point = transTiePoints.getRecord(r).getGeometry().getPoints();
                            int transCol = transformedImage.getColumnFromXCoordinate(transCoords.x);
                            int transRow = transformedImage.getRowFromYCoordinate(transCoords.y);

                            int referenceRadiusN = 0;
                            referenceRadius = refNeighbourhoodStart;
                            boolean flag = true;
                            do {
                                xOffset = new int[referenceRadius + 1][];
                                yOffset = new int[referenceRadius + 1][];
                                for (i = 1; i <= referenceRadius; i++) {
                                    // count the number of cells in the convolution annulus
                                    n = 0;
                                    annulusColumns.clear();
                                    annulusRows.clear();
                                    for (row = referenceRadius - i - 1; row <= referenceRadius + i + 1; row++) {
                                        a = row - referenceRadius;
                                        for (col = referenceRadius - i - 1; col <= referenceRadius + i + 1; col++) {
                                            b = col - referenceRadius;
                                            dist = Math.sqrt(a * a + b * b);
                                            if (Math.abs(dist - i) < 0.5) {
                                                annulusRows.add(a);
                                                annulusColumns.add(b);
                                                n++;
                                            }
                                        }
                                    }
                                    annulusX = new int[n];
                                    annulusY = new int[n];

                                    for (a = 0; a < n; a++) {
                                        annulusX[a] = annulusColumns.get(a);
                                        annulusY[a] = annulusRows.get(a);
                                    }

                                    xOffset[i] = annulusX;
                                    yOffset[i] = annulusY;

                                }

                                referenceMeans = new double[referenceRadius + 1];
                                referenceVariances = new double[referenceRadius + 1];
                                //double z;
                                double[][] filterData1 = new double[referenceRadius + 1][2];
                                double[][] filterData2 = new double[referenceRadius + 1][2];
                                double minValOfMean = Double.POSITIVE_INFINITY;
                                double maxValOfMean = Double.NEGATIVE_INFINITY;
                                double minValOfVariance = Double.POSITIVE_INFINITY;
                                double maxValOfVariance = Double.NEGATIVE_INFINITY;
                                //double M = 0, Q = 0;
                                for (i = 1; i <= referenceRadius; i++) {
                                    double total = 0;
                                    n = 0;
                                    for (a = 0; a < xOffset[i].length - 1; a++) {
                                        row = refRow + yOffset[i][a];
                                        col = refCol + xOffset[i][a];
                                        z = referenceImage.getValue(row, col);

                                        if (z != noData1) {
                                            total += z;
                                            n++;
                                            if (a > 0) {
                                                M = M + (z - M) / (a + 1);
                                                Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                            } else {
                                                M = z;
                                                Q = 0;
                                            }
                                        }
                                    }
                                    referenceMeans[i] = total / n;
                                    referenceVariances[i] = sqrt(Q / (n - 1));

                                    if (referenceMeans[i] < minValOfMean) {
                                        minValOfMean = referenceMeans[i];
                                    }
                                    if (referenceMeans[i] > maxValOfMean) {
                                        maxValOfMean = referenceMeans[i];
                                    }

                                    if (referenceVariances[i] < minValOfVariance) {
                                        minValOfVariance = referenceVariances[i];
                                    }
                                    if (referenceVariances[i] > maxValOfVariance) {
                                        maxValOfVariance = referenceVariances[i];
                                    }

                                    filterData1[i][0] = i;
                                    filterData1[i][1] = referenceMeans[i];
                                    filterData2[i][0] = i;
                                    filterData2[i][1] = referenceVariances[i];
                                }

                                double valRange = maxValOfMean - minValOfMean;
//                                epsilon = 3; //valRange / 15;
                                double[][] newData1 = douglasPeuckerFilter(filterData1, 1, referenceMeans.length - 1);
                                valRange = maxValOfVariance - minValOfVariance;
//                                epsilon = 3; //valRange / 15;
                                double[][] newData2 = douglasPeuckerFilter(filterData2, 1, referenceVariances.length - 1);

                                referenceRadiusN = 0;
                                coarsereferenceRings = new boolean[referenceRadius + 1];
                                for (i = 0; i < newData1.length; i++) {
                                    coarsereferenceRings[(int) newData1[i][0]] = true;
                                    referenceRadiusN++;
                                }

                                for (i = 0; i < newData2.length; i++) {
                                    if (coarsereferenceRings[(int) newData2[i][0]] == false) {
                                        coarsereferenceRings[(int) newData2[i][0]] = true;
                                        referenceRadiusN++;
                                    }
                                }

                                if (newData1.length > 8 && newData2.length > 8 && referenceRadiusN > 12) {
                                    // there must be enough information on both the means data 
                                    // and the variance data.
                                    flag = false;
                                } else {
                                    referenceRadius += refNeighbourhoodStep;
                                    if (referenceRadius > maxNeighbourhoodSize) {
                                        flag = false;
                                    }
                                }
                            } while (flag);

                            if (referenceRadius < maxNeighbourhoodSize) {
                                
                            
                            double referenceMean = 0;
                            double referenceVariance = 0;
                            double referenceMeanDetailed = 0;
                            double referenceVarianceDetailed = 0;

                            for (a = 1; a < referenceRadius + 1; a++) {
                                referenceMeanDetailed += referenceMeans[a];
                                referenceVarianceDetailed += referenceVariances[a];
                                if (coarsereferenceRings[a]) {
                                    referenceMean += referenceMeans[a];
                                    referenceVariance += referenceVariances[a];
                                }
                            }

                            referenceMean = referenceMean / referenceRadiusN;
                            referenceVariance = referenceVariance / referenceRadiusN;
                            referenceMeanDetailed = referenceMeanDetailed / referenceRadius;
                            referenceVarianceDetailed = referenceVarianceDetailed / referenceRadius;


                            double[] referenceMeanDeviates = new double[referenceRadius + 1];
                            double[] referenceVarianceDeviates = new double[referenceRadius + 1];
                            double[] referenceMeanDeviatesDetailed = new double[referenceRadius + 1];
                            double[] referenceVarianceDeviatesDetailed = new double[referenceRadius + 1];
                            double sqrDev1 = 0;
                            double sqrDev2 = 0;
                            double sqrDev1Detailed = 0;
                            double sqrDev2Detailed = 0;
                            for (a = 1; a < referenceRadius + 1; a++) {
                                referenceMeanDeviatesDetailed[a] = referenceMeans[a] - referenceMeanDetailed;
                                referenceVarianceDeviatesDetailed[a] = referenceVariances[a] - referenceVarianceDetailed;
                                sqrDev1Detailed += (referenceMeans[a] - referenceMeanDetailed) * (referenceMeans[a] - referenceMean);
                                sqrDev2Detailed += (referenceVariances[a] - referenceVarianceDetailed) * (referenceVariances[a] - referenceVariance);

                                referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
                                referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;


                                if (coarsereferenceRings[a]) {
//                    referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
//                    referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;
                                    sqrDev1 += (referenceMeans[a] - referenceMean) * (referenceMeans[a] - referenceMean);
                                    sqrDev2 += (referenceVariances[a] - referenceVariance) * (referenceVariances[a] - referenceVariance);
                                }
                            }

//                referenceImage.close();



                            double maxCorrelations = 0;
                            double maxCorrelation1 = 0;
                            double maxCorrelation2 = 0;
                            int maxCorrelationRow = -1, maxCorrelationCol = -1;

                            int n1 = 0;
                            int n2 = 0;

                            int searchWindowSize = 30; //(int) rmse * 5;
//                            if (searchWindowSize < 5) {
//                                searchWindowSize = 5;
//                            }
                            for (int row2 = transRow - searchWindowSize; row2 <= transRow + searchWindowSize; row2++) { //5400; row2 < 6450; row2++) {
                                for (int col2 = transCol - searchWindowSize; col2 <= transCol + searchWindowSize; col2++) { //1500; col2 < 2500; col2++) {
                                    n1++;
                                    // find the means and variances of each annulus
                                    double[] means = new double[referenceRadius + 1];
                                    double[] variances = new double[referenceRadius + 1];
                                    for (i = 1; i <= referenceRadius; i++) {
                                        if (coarsereferenceRings[i]) {
                                            double total = 0;
                                            n = 0;
                                            for (a = 0; a < xOffset[i].length - 1; a++) {
                                                row = row2 + yOffset[i][a];
                                                col = col2 + xOffset[i][a];
                                                z = transformedImage.getValue(row, col);
                                                if (z != noData2) {
                                                    total += z;
                                                    n++;
                                                    if (a > 0) {
                                                        M = M + (z - M) / (a + 1);
                                                        Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                                    } else {
                                                        M = z;
                                                        Q = 0;
                                                    }
                                                }
                                            }
                                            means[i] = total / n;
                                            variances[i] = sqrt(Q / (n - 1));
                                        }
                                    }

                                    double sampleMean = 0;
                                    double sampleVariance = 0;
                                    for (a = 1; a < referenceRadius + 1; a++) {
                                        if (coarsereferenceRings[a]) {
                                            sampleMean += means[a];
                                            sampleVariance += variances[a];
                                        }
                                    }
                                    sampleMean = sampleMean / referenceRadiusN;
                                    sampleVariance = sampleVariance / referenceRadiusN;


                                    double sampleSqrdDev1 = 0;
                                    double sampleSqrdDev2 = 0;

                                    // correlate the reference and sample means and variances
                                    double cov1 = 0;
                                    double cov2 = 0;
                                    for (a = 1; a < referenceRadius + 1; a++) {
                                        if (coarsereferenceRings[a]) {
                                            cov1 += (means[a] - sampleMean) * referenceMeanDeviates[a];
                                            cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviates[a];
                                            sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                            sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);
                                        }
                                    }


                                    double r1 = cov1 / (Math.sqrt(sqrDev1 * sampleSqrdDev1));
                                    double r2 = cov2 / (Math.sqrt(sqrDev2 * sampleSqrdDev2));

                                    if (!conductFineSearch) {
                                        if (r1 + r2 > maxCorrelations) {
                                            maxCorrelations = r1 + r2;
                                            maxCorrelation1 = r1;
                                            maxCorrelation2 = r2;
                                            maxCorrelationCol = col2;
                                            maxCorrelationRow = row2;
                                        }
                                    } else {
                                        if (r1 > 0.9 && r2 > 0.9) { // + r2 > 1.95) { // conduct a detailed correlation
                                            n2++;

                                            for (i = 1; i <= referenceRadius; i++) {
                                                if (!coarsereferenceRings[i]) {
                                                    double total = 0;
                                                    n = 0;
                                                    for (a = 0; a < xOffset[i].length - 1; a++) {
                                                        row = row2 + yOffset[i][a];
                                                        col = col2 + xOffset[i][a];
                                                        z = transformedImage.getValue(row, col);
                                                        if (z != noData2) {
                                                            total += z;
                                                            n++;
                                                            if (a > 0) {
                                                                M = M + (z - M) / (a + 1);
                                                                Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                                            } else {
                                                                M = z;
                                                                Q = 0;
                                                            }
                                                        }
                                                    }
                                                    means[i] = total / n;
                                                    variances[i] = sqrt(Q / (n - 1));
                                                }
                                            }

                                            sampleMean = 0;
                                            sampleVariance = 0;
                                            for (a = 1; a < referenceRadius + 1; a++) {
                                                sampleMean += means[a];
                                                sampleVariance += variances[a];

                                            }
                                            sampleMean = sampleMean / referenceRadius;
                                            sampleVariance = sampleVariance / referenceRadius;

                                            // correlate the reference and sample means and variances
                                            cov1 = 0;
                                            cov2 = 0;
                                            sampleSqrdDev1 = 0;
                                            sampleSqrdDev2 = 0;
                                            for (a = 1; a < referenceRadius + 1; a++) {
                                                cov1 += (means[a] - sampleMean) * referenceMeanDeviatesDetailed[a];
                                                cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviatesDetailed[a];
                                                sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                                sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);

                                            }

                                            r1 = cov1 / (Math.sqrt(sqrDev1Detailed * sampleSqrdDev1));
                                            r2 = cov2 / (Math.sqrt(sqrDev2Detailed * sampleSqrdDev2));


                                            if (r1 + r2 > maxCorrelations) {
                                                maxCorrelations = r1 + r2;
                                                maxCorrelation1 = r1;
                                                maxCorrelation2 = r2;
                                                maxCorrelationCol = col2;
                                                maxCorrelationRow = row2;
                                            }

                                        }
                                    }
                                }
                            }
                            if (maxCorrelations > 1.9) {
                                double x2 = transformedImage.getXCoordinateFromColumn(maxCorrelationCol);
                                double y2 = transformedImage.getYCoordinateFromRow(maxCorrelationRow);
                                whitebox.geospatialfiles.shapefile.Point PP = new whitebox.geospatialfiles.shapefile.Point(x2, y2);
                                Object[] rowData = new Object[2];
                                rowData[0] = new Double(maxCorrelation1);
                                rowData[1] = new Double(maxCorrelation2);
                                output.addRecord(PP, rowData);


                                PP = new whitebox.geospatialfiles.shapefile.Point(refXCoord, refYCoord);
                                rowData = new Object[2];
                                rowData[0] = new Double(maxCorrelation1);
                                rowData[1] = new Double(maxCorrelation2);
                                output2.addRecord(PP, rowData);
                            }

                            if (maxCorrelation1 > 0.95 && maxCorrelation2 > 0.95) {
                                double x2 = transformedImage.getXCoordinateFromColumn(maxCorrelationCol);
                                double y2 = transformedImage.getYCoordinateFromRow(maxCorrelationRow);
                                tiePointsRef.add(new XYPoint(refXCoord, refYCoord));
                                tiePointsTransform.add(new XYPoint(x2, y2));
                                entry = new double[]{refXCoord, refYCoord};
                                controlPointTree.addPoint(entry, (double)numTiePoints);
                                numTiePoints++;
                                newPolyOrder = polyOrder;
                                if (newPolyOrder == 4 && tiePointsRef.size() < 15) {
                                    newPolyOrder--;
                                }
                                if (newPolyOrder == 3 && tiePointsRef.size() < 10) {
                                    newPolyOrder--;
                                }
                                if (newPolyOrder == 2 && tiePointsRef.size() < 6) {
                                    newPolyOrder--;
                                }
//                                if (pls.getPolyOrder() != newPolyOrder) {
//                                    pls.setPolyOrder(newPolyOrder);
//                                }
//                                pls.addData(tiePointsRef, tiePointsTransform);
//                                rmse = pls.getOverallRMSE();
                                System.out.println("Num. tie points: " + tiePointsRef.size()); // + " , RMSE: " + rmse);
                            }
                        }
                        }
                    }
                    progress = (int) ((100.0 * r) / rows1);
                    if (progress > oldProgress) {
                        System.out.println(progress + "%");
                        oldProgress = progress;
                    }
                }










                int numLocatedPoints = tiePointsRef.size();
                double w1, w2;
                        
                oldProgress = -1;
                for (int r = 0; r < rows1; r += 100) {
                    for (int c = 0; c < cols1; c += 100) {

                        double refXCoord = referenceImage.getXCoordinateFromColumn(c);
                        double refYCoord = referenceImage.getYCoordinateFromRow(r);
                        
                        double[] entry = {refXCoord, refYCoord};
                        results = controlPointTree.nearestNeighbor(entry, 15, true);
                        
                        j = results.size();
                        double[] X1 = new double[j];
                        double[] Y1 = new double[j];
                        double[] X2 = new double[j];
                        double[] Y2 = new double[j];
                        
                        for (int k = 0; k < j; k++) {
                            double val = results.get(k).value;
                            X1[k] = tiePointsRef.get((int)val).x;
                            Y1[k] = tiePointsRef.get((int)val).y;
                            X2[k] = tiePointsTransform.get((int)val).x;
                            Y2[k] = tiePointsTransform.get((int)val).y;
                            
                        }
                        
                        pls = new PolynomialLeastSquares2DFitting(X1, Y1, X2, Y2, 1);
                        
                        XYPoint transCoords = pls.getForwardCoordinates(
                                refXCoord, refYCoord);

                        if (transCoords.x <= east && transCoords.x >= west
                                && transCoords.y >= south && transCoords.y <= north) {

                            int refCol = c; //referenceImage.getColumnFromXCoordinate(transCoords.x);
                            int refRow = r; //referenceImage.getRowFromYCoordinate(transCoords.y);

//                            point = transTiePoints.getRecord(r).getGeometry().getPoints();
                            int transCol = transformedImage.getColumnFromXCoordinate(transCoords.x);
                            int transRow = transformedImage.getRowFromYCoordinate(transCoords.y);

                            int referenceRadiusN = 0;
                            referenceRadius = refNeighbourhoodStart;
                            boolean flag = true;
                            do {
                                xOffset = new int[referenceRadius + 1][];
                                yOffset = new int[referenceRadius + 1][];
                                for (i = 1; i <= referenceRadius; i++) {
                                    // count the number of cells in the convolution annulus
                                    n = 0;
                                    annulusColumns.clear();
                                    annulusRows.clear();
                                    for (row = referenceRadius - i - 1; row <= referenceRadius + i + 1; row++) {
                                        a = row - referenceRadius;
                                        for (col = referenceRadius - i - 1; col <= referenceRadius + i + 1; col++) {
                                            b = col - referenceRadius;
                                            dist = Math.sqrt(a * a + b * b);
                                            if (Math.abs(dist - i) < 0.5) {
                                                annulusRows.add(a);
                                                annulusColumns.add(b);
                                                n++;
                                            }
                                        }
                                    }
                                    annulusX = new int[n];
                                    annulusY = new int[n];

                                    for (a = 0; a < n; a++) {
                                        annulusX[a] = annulusColumns.get(a);
                                        annulusY[a] = annulusRows.get(a);
                                    }

                                    xOffset[i] = annulusX;
                                    yOffset[i] = annulusY;

                                }

                                referenceMeans = new double[referenceRadius + 1];
                                referenceVariances = new double[referenceRadius + 1];
                                //double z;
                                double[][] filterData1 = new double[referenceRadius + 1][2];
                                double[][] filterData2 = new double[referenceRadius + 1][2];
                                double minValOfMean = Double.POSITIVE_INFINITY;
                                double maxValOfMean = Double.NEGATIVE_INFINITY;
                                double minValOfVariance = Double.POSITIVE_INFINITY;
                                double maxValOfVariance = Double.NEGATIVE_INFINITY;
                                //double M = 0, Q = 0;
                                for (i = 1; i <= referenceRadius; i++) {
                                    double total = 0;
                                    n = 0;
                                    for (a = 0; a < xOffset[i].length - 1; a++) {
                                        row = refRow + yOffset[i][a];
                                        col = refCol + xOffset[i][a];
                                        z = referenceImage.getValue(row, col);

                                        if (z != noData1) {
                                            total += z;
                                            n++;
                                            if (a > 0) {
                                                M = M + (z - M) / (a + 1);
                                                Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                            } else {
                                                M = z;
                                                Q = 0;
                                            }
                                        }
                                    }
                                    referenceMeans[i] = total / n;
                                    referenceVariances[i] = sqrt(Q / (n - 1));

                                    if (referenceMeans[i] < minValOfMean) {
                                        minValOfMean = referenceMeans[i];
                                    }
                                    if (referenceMeans[i] > maxValOfMean) {
                                        maxValOfMean = referenceMeans[i];
                                    }

                                    if (referenceVariances[i] < minValOfVariance) {
                                        minValOfVariance = referenceVariances[i];
                                    }
                                    if (referenceVariances[i] > maxValOfVariance) {
                                        maxValOfVariance = referenceVariances[i];
                                    }

                                    filterData1[i][0] = i;
                                    filterData1[i][1] = referenceMeans[i];
                                    filterData2[i][0] = i;
                                    filterData2[i][1] = referenceVariances[i];
                                }

                                double valRange = maxValOfMean - minValOfMean;
//                                epsilon = 3; //valRange / 15;
                                double[][] newData1 = douglasPeuckerFilter(filterData1, 1, referenceMeans.length - 1);
                                valRange = maxValOfVariance - minValOfVariance;
//                                epsilon = 3; //valRange / 15;
                                double[][] newData2 = douglasPeuckerFilter(filterData2, 1, referenceVariances.length - 1);

                                referenceRadiusN = 0;
                                coarsereferenceRings = new boolean[referenceRadius + 1];
                                for (i = 0; i < newData1.length; i++) {
                                    coarsereferenceRings[(int) newData1[i][0]] = true;
                                    referenceRadiusN++;
                                }

                                for (i = 0; i < newData2.length; i++) {
                                    if (coarsereferenceRings[(int) newData2[i][0]] == false) {
                                        coarsereferenceRings[(int) newData2[i][0]] = true;
                                        referenceRadiusN++;
                                    }
                                }

                                w1 = newData1.length / (newData1.length + newData2.length);
                                w2 = newData2.length / (newData1.length + newData2.length);
                                
                                if (newData1.length > 8 && newData2.length > 8 && referenceRadiusN > 12) {
                                    // there must be enough information on both the means data 
                                    // and the variance data.
                                    flag = false;
                                } else {
                                    referenceRadius += refNeighbourhoodStep;
                                    if (referenceRadius > maxNeighbourhoodSize) {
                                        flag = false;
                                    }
                                }
                            } while (flag);

                            if (referenceRadius < maxNeighbourhoodSize) {
                                
                            
                            double referenceMean = 0;
                            double referenceVariance = 0;
                            double referenceMeanDetailed = 0;
                            double referenceVarianceDetailed = 0;

                            for (a = 1; a < referenceRadius + 1; a++) {
                                referenceMeanDetailed += referenceMeans[a];
                                referenceVarianceDetailed += referenceVariances[a];
                                if (coarsereferenceRings[a]) {
                                    referenceMean += referenceMeans[a];
                                    referenceVariance += referenceVariances[a];
                                }
                            }

                            referenceMean = referenceMean / referenceRadiusN;
                            referenceVariance = referenceVariance / referenceRadiusN;
                            referenceMeanDetailed = referenceMeanDetailed / referenceRadius;
                            referenceVarianceDetailed = referenceVarianceDetailed / referenceRadius;


                            double[] referenceMeanDeviates = new double[referenceRadius + 1];
                            double[] referenceVarianceDeviates = new double[referenceRadius + 1];
                            double[] referenceMeanDeviatesDetailed = new double[referenceRadius + 1];
                            double[] referenceVarianceDeviatesDetailed = new double[referenceRadius + 1];
                            double sqrDev1 = 0;
                            double sqrDev2 = 0;
                            double sqrDev1Detailed = 0;
                            double sqrDev2Detailed = 0;
                            for (a = 1; a < referenceRadius + 1; a++) {
                                referenceMeanDeviatesDetailed[a] = referenceMeans[a] - referenceMeanDetailed;
                                referenceVarianceDeviatesDetailed[a] = referenceVariances[a] - referenceVarianceDetailed;
                                sqrDev1Detailed += (referenceMeans[a] - referenceMeanDetailed) * (referenceMeans[a] - referenceMean);
                                sqrDev2Detailed += (referenceVariances[a] - referenceVarianceDetailed) * (referenceVariances[a] - referenceVariance);

                                referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
                                referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;


                                if (coarsereferenceRings[a]) {
//                    referenceMeanDeviates[a] = referenceMeans[a] - referenceMean;
//                    referenceVarianceDeviates[a] = referenceVariances[a] - referenceVariance;
                                    sqrDev1 += (referenceMeans[a] - referenceMean) * (referenceMeans[a] - referenceMean);
                                    sqrDev2 += (referenceVariances[a] - referenceVariance) * (referenceVariances[a] - referenceVariance);
                                }
                            }

//                referenceImage.close();



                            double maxCorrelations = 0;
                            double maxCorrelation1 = 0;
                            double maxCorrelation2 = 0;
                            int maxCorrelationRow = -1, maxCorrelationCol = -1;

                            int n1 = 0;
                            int n2 = 0;

                            int searchWindowSize = 8;
                            for (int row2 = transRow - searchWindowSize; row2 <= transRow + searchWindowSize; row2++) { //5400; row2 < 6450; row2++) {
                                for (int col2 = transCol - searchWindowSize; col2 <= transCol + searchWindowSize; col2++) { //1500; col2 < 2500; col2++) {
                                    n1++;
                                    // find the means and variances of each annulus
                                    double[] means = new double[referenceRadius + 1];
                                    double[] variances = new double[referenceRadius + 1];
                                    for (i = 1; i <= referenceRadius; i++) {
                                        if (coarsereferenceRings[i]) {
                                            double total = 0;
                                            n = 0;
                                            for (a = 0; a < xOffset[i].length - 1; a++) {
                                                row = row2 + yOffset[i][a];
                                                col = col2 + xOffset[i][a];
                                                z = transformedImage.getValue(row, col);
                                                if (z != noData2) {
                                                    total += z;
                                                    n++;
                                                    if (a > 0) {
                                                        M = M + (z - M) / (a + 1);
                                                        Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                                    } else {
                                                        M = z;
                                                        Q = 0;
                                                    }
                                                }
                                            }
                                            means[i] = total / n;
                                            variances[i] = sqrt(Q / (n - 1));
                                        }
                                    }

                                    double sampleMean = 0;
                                    double sampleVariance = 0;
                                    for (a = 1; a < referenceRadius + 1; a++) {
                                        if (coarsereferenceRings[a]) {
                                            sampleMean += means[a];
                                            sampleVariance += variances[a];
                                        }
                                    }
                                    sampleMean = sampleMean / referenceRadiusN;
                                    sampleVariance = sampleVariance / referenceRadiusN;


                                    double sampleSqrdDev1 = 0;
                                    double sampleSqrdDev2 = 0;

                                    // correlate the reference and sample means and variances
                                    double cov1 = 0;
                                    double cov2 = 0;
                                    for (a = 1; a < referenceRadius + 1; a++) {
                                        if (coarsereferenceRings[a]) {
                                            cov1 += (means[a] - sampleMean) * referenceMeanDeviates[a];
                                            cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviates[a];
                                            sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                            sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);
                                        }
                                    }


                                    double r1 = cov1 / (Math.sqrt(sqrDev1 * sampleSqrdDev1));
                                    double r2 = cov2 / (Math.sqrt(sqrDev2 * sampleSqrdDev2));

                                    if (!conductFineSearch) {
                                        if (r1 + r2 > maxCorrelations) {
                                            maxCorrelations = r1 + r2;
                                            maxCorrelation1 = r1;
                                            maxCorrelation2 = r2;
                                            maxCorrelationCol = col2;
                                            maxCorrelationRow = row2;
                                        }
                                    } else {
                                        if (r1 > 0.9 && r2 > 0.9) { // + r2 > 1.95) { // conduct a detailed correlation
                                            n2++;

                                            for (i = 1; i <= referenceRadius; i++) {
                                                if (!coarsereferenceRings[i]) {
                                                    double total = 0;
                                                    n = 0;
                                                    for (a = 0; a < xOffset[i].length - 1; a++) {
                                                        row = row2 + yOffset[i][a];
                                                        col = col2 + xOffset[i][a];
                                                        z = transformedImage.getValue(row, col);
                                                        if (z != noData2) {
                                                            total += z;
                                                            n++;
                                                            if (a > 0) {
                                                                M = M + (z - M) / (a + 1);
                                                                Q = Q + (a * (z - M) * (z - M)) / (a + 1);
                                                            } else {
                                                                M = z;
                                                                Q = 0;
                                                            }
                                                        }
                                                    }
                                                    means[i] = total / n;
                                                    variances[i] = sqrt(Q / (n - 1));
                                                }
                                            }

                                            sampleMean = 0;
                                            sampleVariance = 0;
                                            for (a = 1; a < referenceRadius + 1; a++) {
                                                sampleMean += means[a];
                                                sampleVariance += variances[a];

                                            }
                                            sampleMean = sampleMean / referenceRadius;
                                            sampleVariance = sampleVariance / referenceRadius;

                                            // correlate the reference and sample means and variances
                                            cov1 = 0;
                                            cov2 = 0;
                                            sampleSqrdDev1 = 0;
                                            sampleSqrdDev2 = 0;
                                            for (a = 1; a < referenceRadius + 1; a++) {
                                                cov1 += (means[a] - sampleMean) * referenceMeanDeviatesDetailed[a];
                                                cov2 += (variances[a] - sampleVariance) * referenceVarianceDeviatesDetailed[a];
                                                sampleSqrdDev1 += (means[a] - sampleMean) * (means[a] - sampleMean);
                                                sampleSqrdDev2 += (variances[a] - sampleVariance) * (variances[a] - sampleVariance);

                                            }

                                            r1 = cov1 / (Math.sqrt(sqrDev1Detailed * sampleSqrdDev1));
                                            r2 = cov2 / (Math.sqrt(sqrDev2Detailed * sampleSqrdDev2));


                                            if (r1 + r2 > maxCorrelations) {
                                                maxCorrelations = r1 + r2;
                                                maxCorrelation1 = r1;
                                                maxCorrelation2 = r2;
                                                maxCorrelationCol = col2;
                                                maxCorrelationRow = row2;
                                            }

                                        }
                                    }
                                }
                            }
                            if (maxCorrelations > 1.9) {
                                double x2 = transformedImage.getXCoordinateFromColumn(maxCorrelationCol);
                                double y2 = transformedImage.getYCoordinateFromRow(maxCorrelationRow);
                                whitebox.geospatialfiles.shapefile.Point PP = new whitebox.geospatialfiles.shapefile.Point(x2, y2);
                                Object[] rowData = new Object[2];
                                rowData[0] = new Double(maxCorrelation1);
                                rowData[1] = new Double(maxCorrelation2);
                                output.addRecord(PP, rowData);


                                PP = new whitebox.geospatialfiles.shapefile.Point(refXCoord, refYCoord);
                                rowData = new Object[2];
                                rowData[0] = new Double(maxCorrelation1);
                                rowData[1] = new Double(maxCorrelation2);
                                output2.addRecord(PP, rowData);

                                numLocatedPoints++;
                                
                                
                                tiePointsRef.add(new XYPoint(refXCoord, refYCoord));
                                tiePointsTransform.add(new XYPoint(x2, y2));
                                entry = new double[]{refXCoord, refYCoord};
                                controlPointTree.addPoint(entry, (double)numTiePoints);
                                numTiePoints++;
                            }
                            
//                            if (maxCorrelation1 > 0.95 && maxCorrelation2 > 0.95) {
//                                double x2 = transformedImage.getXCoordinateFromColumn(maxCorrelationCol);
//                                double y2 = transformedImage.getYCoordinateFromRow(maxCorrelationRow);
//                                tiePointsRef.add(new XYPoint(refXCoord, refYCoord));
//                                tiePointsTransform.add(new XYPoint(x2, y2));
//                                entry = new double[]{refXCoord, refYCoord};
//                                controlPointTree.addPoint(entry, (double)numTiePoints);
//                                numTiePoints++;
//                            }

                        }
                        }
                    }
                    progress = (int) ((100.0 * r) / rows1);
                    if (progress > oldProgress) {
                        System.out.println(progress + "% (Num. of matched points: " + numLocatedPoints + ")");
                        oldProgress = progress;
                    }
                }






            } else {
                System.out.println("An insufficient number of tie point matches could be located.");
            }

            referenceImage.close();
            transformedImage.close();
            output.write();
            output2.write();

            System.out.println("\nOperation complete!");
            long endTime = System.nanoTime();

            double duration = (endTime - startTime) / 1000000000.0; // in seconds

            str = new StringBuilder();
            str.append("Duration: ");
            if (duration > 86400) {
                str.append((int) (Math.floor(duration / 86400))).append(" days, ");
                duration = duration / 86400;
            }
            if (duration > 3600) {
                str.append((int) (Math.floor(duration / 3600))).append(" hours, ");
                duration = duration / 3600;
            }
            if (duration > 60) {
                str.append((int) (Math.floor(duration / 60))).append(" minutes, ");
                duration = duration / 60;
            }
            str.append(duration).append(" seconds");

            System.out.println(str.toString());


        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    private static double epsilon = 4.0;

    private static double[][] douglasPeuckerFilter(double[][] points,
            int startIndex, int endIndex) {
        double dmax = 0;
        int idx = 0;
        double a = endIndex - startIndex;
        double b = points[endIndex][1] - points[startIndex][1];
        double c = -(b * startIndex - a * points[startIndex][1]);
        //double norm = sqrt(pow(a, 2) + pow(b, 2));
        double norm = sqrt(a * a + b * b);
        for (int i = startIndex + 1; i < endIndex; i++) {
            double distance = abs(b * i - a * points[i][1] + c) / norm;
            if (distance > dmax) {
                idx = i;
                dmax = distance;
            }
        }
        if (dmax >= epsilon) {
            double[][] recursiveResult1 = douglasPeuckerFilter(points,
                    startIndex, idx);
            double[][] recursiveResult2 = douglasPeuckerFilter(points,
                    idx, endIndex);
            double[][] result = new double[(recursiveResult1.length - 1)
                    + recursiveResult2.length][2];

            int result1Length = recursiveResult1.length;
            for (int i = 0; i < result1Length - 1; i++) {
                result[i][0] = recursiveResult1[i][0];
                result[i][1] = recursiveResult1[i][1];
            }
            for (int i = 0; i < recursiveResult2.length; i++) {
                result[result1Length + i - 1][0] = recursiveResult2[i][0];
                result[result1Length + i - 1][1] = recursiveResult2[i][1];
            }

//            for (int i = 0; i < result.length; i++) {
//                System.arraycopy(recursiveResult1[i], 0, result[i], 0,
//                        recursiveResult1.length - 1);
//                System.arraycopy(recursiveResult2[i], 0, result[i],
//                        recursiveResult1.length - 1, recursiveResult2.length);
//            }
            return result;
        } else {
            double[][] ret = {
                {points[startIndex][0], points[startIndex][1]},
                {points[endIndex][0], points[endIndex][1]}
            };
            return ret;
        }
    }
}
