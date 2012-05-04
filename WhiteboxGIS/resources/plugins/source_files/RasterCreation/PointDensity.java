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

import java.util.Date;
import java.util.List;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.structures.KdTree;
import whitebox.structures.XYPoint;
import java.io.*;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PointDensity implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    @Override
    public String getName() {
        return "PointDensity";
    }

    @Override
    public String getDescriptiveName() {
        return "Point Density";
    }

    @Override
    public String getToolDescription() {
        return "Interpolates the spatial pattern of point density from x,y,z data.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"Interpolation"};
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
        /* The problem with this algorithm is that the implementation of the
         * k-d tree used here only allows you to return the n-closest neighbours
         * and does not allow you to search for points that are within a specified
         * distance. This will introduce an inaccuracy in point density estimates.
         */
        amIActive = true;

        String inputFilesString = null;
        String[] pointFiles;
        String outputHeader = null;
        int row, col;
        int nrows, ncols;
        double x, y, z;
        int i;
        int progress = 0;
        int numPointsToUse = 10;
        int numPoints = 0;
        int lineNum = 0;
        int nlines = 0;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double north, south, east, west;
        double resolution = 1;
        String delimiter = " ";
        boolean firstLineHeader = false;
        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        List<KdTree.Entry<XYPoint>> results;
        double noData = -32768;
        
        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        inputFilesString = args[0];
        firstLineHeader = Boolean.parseBoolean(args[1]);
        outputHeader = args[2];
        resolution = Double.parseDouble(args[3]);
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString.length() <= 0) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            pointFiles = inputFilesString.split(";");
            int numPointFiles = pointFiles.length;
            
            updateProgress("Counting the number of points:", 0);
            numPoints = 0;
            for (i = 0; i < numPointFiles; i++) {
                nlines = countLinesInFile(pointFiles[i]);
                if (firstLineHeader) {
                    numPoints += nlines - 1;
                } else {
                    numPoints += nlines;
                }
            }
            
            if (numPoints < numPointsToUse) { numPointsToUse = numPoints; }
            
            KdTree<XYPoint> pointsTree = new KdTree.SqrEuclid<XYPoint>(2, numPoints);
            
            nlines = 0;
            for (i = 0; i < numPointFiles; i++) {
                DataInputStream in = null;
                BufferedReader br = null;
                try {
                    // Open the file that is the first command line parameter
                    FileInputStream fstream = new FileInputStream(pointFiles[i]);
                    // Get the object of DataInputStream
                    in = new DataInputStream(fstream);

                    br = new BufferedReader(new InputStreamReader(in));

                    String line;
                    String[] str;
                    lineNum = 1;
                    //Read File Line By Line
                    while ((line = br.readLine()) != null) {
                        str = line.split(delimiter);
                        if (str.length <= 1) {
                            delimiter = "\t";
                            str = line.split(delimiter);
                            if (str.length <= 1) {
                                delimiter = " ";
                                str = line.split(delimiter);
                                if (str.length <= 1) {
                                    delimiter = ",";
                                    str = line.split(delimiter);
                                }
                            }
                        }
                        if ((lineNum > 1 || !firstLineHeader) && (str.length >= 3)) {
                            x = Double.parseDouble(str[0]);
                            y = Double.parseDouble(str[1]);
                            XYPoint pnt = new XYPoint(x, y);
                            double[] entry = {y, x};
                            pointsTree.addPoint(entry, pnt);
                            if (x < minX) {
                                minX = x;
                            }
                            if (x > maxX) {
                                maxX = x;
                            }
                            if (y < minY) {
                                minY = y;
                            }
                            if (y > maxY) {
                                maxY = y;
                            }
                        }
                        lineNum++;
                        nlines++;
                        progress = (int) (100d * nlines / numPoints);
                        updateProgress("Reading point data:", progress);
                    }
                    //Close the input stream
                    in.close();
                    br.close();

                } catch (java.io.IOException e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    try {
                        if (in != null || br != null) {
                            in.close();
                            br.close();
                        }
                    } catch (java.io.IOException ex) {
                    }

                }
            }
            
            // What are north, south, east, and west and how many rows and 
            // columns should there be?
            
            west = minX - 0.5 * resolution;
            north = maxY + 0.5 * resolution;
            nrows = (int)(Math.ceil((north - minY) / resolution));
            ncols = (int)(Math.ceil((maxX - west) / resolution));
            south = north - nrows * resolution;
            east = west + ncols * resolution;
            
            // create the whitebox header file.
            fw = new FileWriter(outputHeader, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
            out.println(str1);
            str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
            out.println(str1);
            str1 = "North:\t" + Double.toString(north);
            out.println(str1);
            str1 = "South:\t" + Double.toString(south);
            out.println(str1);
            str1 = "East:\t" + Double.toString(east);
            out.println(str1);
            str1 = "West:\t" + Double.toString(west);
            out.println(str1);
            str1 = "Cols:\t" + Integer.toString(ncols);
            out.println(str1);
            str1 = "Rows:\t" + Integer.toString(nrows);
            out.println(str1);
            str1 = "Data Type:\t" + "float";
            out.println(str1);
            str1 = "Z Units:\t" + "not specified";
            out.println(str1);
            str1 = "XY Units:\t" + "not specified";
            out.println(str1);
            str1 = "Projection:\t" + "not specified";
            out.println(str1);
            str1 = "Data Scale:\tcontinuous";
            out.println(str1);
            str1 = "Preferred Palette:\t" + "spectrum.pal";
            out.println(str1);
            str1 = "NoData:\t" + noData;
            out.println(str1);
            if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
            } else {
                str1 = "Byte Order:\t" + "BIG_ENDIAN";
            }
            out.println(str1);
            
            out.close();
            
            // Create the whitebox raster object.
            WhiteboxRaster image = new WhiteboxRaster(outputHeader, "rw");

            double northing, easting;
            double halfResolution = resolution / 2;
            double area = 0;
            for (row = 0; row < nrows; row++) {
                for (col = 0; col < ncols; col++) {
                    easting = (col * resolution) + (west + halfResolution);
                    northing = (north - halfResolution) - (row * resolution);
                    double[] entry = {northing, easting};
                    results = pointsTree.nearestNeighbor(entry, numPointsToUse, true);
                    // find the area over which the points are spread
                    minX = Double.POSITIVE_INFINITY;
                    maxX = Double.NEGATIVE_INFINITY;
                    minY = Double.POSITIVE_INFINITY;
                    maxY = Double.NEGATIVE_INFINITY;
                    for (i = 0; i < results.size(); i++) {
                        x = results.get(i).value.x;
                        y = results.get(i).value.y;
                        if (x < minX) { minX = x; }
                        if (x > maxX) { maxX = x; }
                        if (y < minY) { minY = y; }
                        if (y > maxY) { maxY = y; }
                    }
                    area = (maxX - minX) * (maxY - minY);
                    if (area > 0) {
                        image.setValue(row, col, area / numPointsToUse);
                    } else {
                        image.setValue(row, col, noData);
                    }
                    
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (nrows - 1));
                updateProgress("Interpolating point data:", progress);
            }

            image.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            image.addMetadataEntry("Created on " + new Date());

            image.close();

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
    
    public int countLinesInFile(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return count;
        } finally {
            is.close();
        }
    }
    
}