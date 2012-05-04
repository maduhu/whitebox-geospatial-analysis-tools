/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale;
import java.util.Random;
import java.text.DecimalFormat;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class kMeansClassification implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "kMeansClassification";
    }

    @Override
    public String getDescriptiveName() {
        return "k-Means Classification";
    }

    @Override
    public String getToolDescription() {
        return "Performs a k-means classification on a multi-spectral dataset.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"ImageClass", "LandformClass"};
        return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    private int previousProgress = 0;
    private String previousProgressLabel = "";

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }

    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;

        String inputFilesString = null;
        String[] imageFiles = null;
        String outputHeader = null;
        WhiteboxRasterInfo[] images = null;
        WhiteboxRaster ouptut = null;
        int nCols = 0;
        int nRows = 0;
        double z;
        int numClasses;
        int numImages;
        int progress = 0;
        int col, row;
        int a, i, j;
        double[][] data;
        double noData = -32768;
        double[][] classCentres;
        double[][] imageMetaData;
        long[] numPixelsInEachClass;
        int maxIterations = 100;
        double dist, minDist;
        int whichClass;
        double minAdjustment = 10;
        byte initializationMode = 0; // maximum dispersion along diagonal
        long numCellsChanged = 0;
        long totalNumCells = 0;
        boolean totalNumCellsCounted = false;
        double percentChanged = 0;
        double percentChangedThreshold = 1.0;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        // read the input parameters
        inputFilesString = args[0];
        outputHeader = args[1];
        numClasses = Integer.parseInt(args[2]);
        maxIterations = Integer.parseInt(args[3]);
        percentChangedThreshold = Double.parseDouble(args[4]);
        if (args[5].toLowerCase().contains("random")) {
            initializationMode = 1; //random positioning 
        } else {
            initializationMode = 0; //maximum dispersion along multi-dimensional diagonal
        }

        try {
            // deal with the input images
            imageFiles = inputFilesString.split(";");
            numImages = imageFiles.length;
            images = new WhiteboxRasterInfo[numImages];
            imageMetaData = new double[numImages][3];
            for (i = 0; i < numImages; i++) {
                images[i] = new WhiteboxRasterInfo(imageFiles[i]);
                if (i == 0) {
                    nCols = images[i].getNumberColumns();
                    nRows = images[i].getNumberRows();
                    noData = images[i].getNoDataValue();
                } else {
                    if (images[i].getNumberColumns() != nCols
                            || images[i].getNumberRows() != nRows) {
                        showFeedback("All input images must have the same dimensions (rows and columns).");
                        return;
                    }
                }
                imageMetaData[i][0] = images[i].getNoDataValue();
                imageMetaData[i][1] = images[i].getMinimumValue();
                imageMetaData[i][2] = images[i].getMaximumValue();

            }

            data = new double[numImages][];
            numPixelsInEachClass = new long[numImages];

            // now set up the output image
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    imageFiles[0], WhiteboxRaster.DataType.INTEGER, 0);
            output.setDataScale(DataScale.CATEGORICAL);
            output.setPreferredPalette("qual.pal");

            if (initializationMode == 1) {
                // initialize the class centres randomly
                Random generator = new Random();
                double range;
                classCentres = new double[numClasses][numImages];
                for (a = 0; a < numClasses; a++) {
                    for (i = 0; i < numImages; i++) {
                        range = imageMetaData[i][2] - imageMetaData[i][1];
                        classCentres[a][i] = imageMetaData[i][1] + generator.nextDouble() * range;
                    }
                }
            } else {
                double range, spacing;
                classCentres = new double[numClasses][numImages];
                for (a = 0; a < numClasses; a++) {
                    for (i = 0; i < numImages; i++) {
                        range = imageMetaData[i][2] - imageMetaData[i][1];
                        spacing = range / numClasses;
                        classCentres[a][i] = imageMetaData[i][1] + spacing * a;
                    }
                }
            }

            j = 0;
            whichClass = 0;
            do {
                j++;
                // assign each pixel to a class
                updateProgress("Loop " + j, 1);
                double[][] classCentreData = new double[numClasses][numImages];
                numPixelsInEachClass = new long[numClasses];

                numCellsChanged = 0;
                for (row = 0; row < nRows; row++) {
                    for (i = 0; i < numImages; i++) {
                        data[i] = images[i].getRowValues(row);
                    }
                    for (col = 0; col < nCols; col++) {
                        if (data[0][col] != noData) {
                            if (!totalNumCellsCounted) {
                                totalNumCells++;
                            }
                            // calculate the squared distance to each of the centroids
                            // and assign the pixel the value of the nearest centroid.
                            minDist = Double.POSITIVE_INFINITY;
                            for (a = 0; a < numClasses; a++) {
                                dist = 0;
                                for (i = 0; i < numImages; i++) {
                                    dist += (data[i][col] - classCentres[a][i]) * (data[i][col] - classCentres[a][i]);
                                }
                                if (dist < minDist) {
                                    minDist = dist;
                                    whichClass = a;
                                }
                            }
                            z = output.getValue(row, col);
                            if ((int)z != whichClass) {
                                numCellsChanged++;
                            }
                            output.setValue(row, col, whichClass);

                            numPixelsInEachClass[whichClass]++;
                            for (i = 0; i < numImages; i++) {
                                classCentreData[whichClass][i] += data[i][col];
                            }
                        } else {
                            output.setValue(row, col, noData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (nRows - 1));
                    updateProgress("Loop " + j, progress);
                }
                totalNumCellsCounted = true;

                // Update the class centroids
                for (a = 0; a < numClasses; a++) {
                    if (numPixelsInEachClass[a] > 0) {
                        double[] newClassCentre = new double[numImages];
                        for (i = 0; i < numImages; i++) {
                            newClassCentre[i] = classCentreData[a][i] / numPixelsInEachClass[a];
                        }
                        for (i = 0; i < numImages; i++) {
                            classCentres[a][i] = newClassCentre[i];
                        }

                    }
                }

                percentChanged = (double)numCellsChanged / totalNumCells * 100;
            } while ((percentChanged > percentChangedThreshold) && (j < maxIterations));

            // prepare the report
            double[] totalDeviations = new double[numClasses];
            for (row = 0; row < nRows; row++) {
                for (i = 0; i < numImages; i++) {
                    data[i] = images[i].getRowValues(row);
                }
                for (col = 0; col < nCols; col++) {
                    if (data[0][col] != noData) {
                        whichClass = (int)(output.getValue(row, col));
                        dist = 0;
                        for (i = 0; i < numImages; i++) {
                            dist += (data[i][col] - classCentres[whichClass][i]) * (data[i][col] - classCentres[whichClass][i]);
                        }
                        totalDeviations[whichClass] += dist;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (nRows - 1));
                updateProgress("Loop " + j, progress);
            }
            
            double[] standardDeviations = new double[numClasses];
            for (a = 0; a < numClasses; a++) {
                standardDeviations[a] = Math.sqrt(totalDeviations[a] / (numPixelsInEachClass[a] - 1));
            }
            
            DecimalFormat df;
            df = new DecimalFormat("0.00");
            
            String retStr = "k-Means Classification Report\n\n";
            
            retStr += "     \tCentroid Vector\n"; 
            retStr += "     \t";
            for (i = 0; i < numImages; i++) {
                retStr += "Image" + (i + 1) + "\t";
            }
            retStr += "SD\tPixels\t% Area\n";
            for (a = 0; a < numClasses; a++) {
                String str = "";
                for (i = 0; i < numImages; i++) {
                    str += df.format(classCentres[a][i]) + "\t";
                }
                        
                retStr += "Cluster " + a + "\t" + str + df.format(standardDeviations[a]) + "\t" + numPixelsInEachClass[a] + "\t" + df.format((double)numPixelsInEachClass[a] / totalNumCells * 100) + "\n";
            }
            retStr += "\n";
            for (i = 0; i < numImages; i++) {
                retStr += "Image" + (i + 1) + " = " + images[i].getShortHeaderFile() + "\n";
            }
            
            retStr += "\nCluster Centroid Distance Analysis:\n";
            for (a = 0; a < numClasses; a++) {
                retStr += "\tClus. " + a;
            }
            retStr += "\n";
            //double[][] centroidDistances = new double[numClasses][numClasses];
            for (a = 0; a < numClasses; a++) {
                retStr += "Cluster " + a;
                for (int b = 0; b < numClasses; b++) {
                    if (b >= a) {
                        dist = 0;
                        for (i = 0; i < numImages; i++) {
                            dist += (classCentres[a][i] - classCentres[b][i]) * (classCentres[a][i] - classCentres[b][i]);
                        }
                        retStr += "\t" + df.format(Math.sqrt(dist));
                    } else {
                        retStr += "\t";
                    }
                }
                retStr += "\n";
            }
            
            returnData(retStr);

            Dendrogram plot = new Dendrogram(classCentres, numPixelsInEachClass);
            returnData(plot);
            
            for (i = 0; i < numImages; i++) {
                images[i].close();
            }
            
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        kMeansClassification kmc = new kMeansClassification();
//        args = new String[5];
//        args[0] = "/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/band1 clipped.dep;/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/band2 clipped.dep;/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/band3 clipped.dep;/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/band4 clipped.dep;/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/band5 clipped.dep;";
//        args[1] = "/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab1/Data/LE70180302002142EDC00/tmp1.dep";
//        args[2] = "10";
//        args[3] = "25";
//        args[4] = "3";
//        
//        kmc.setArgs(args);
//        kmc.run();
//        
//    }
}
