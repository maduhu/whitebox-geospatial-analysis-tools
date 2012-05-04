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

import java.awt.Color;
import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class HistogramEqualization implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    public String getName() {
        return "HistogramEqualization";
    }

    public String getDescriptiveName() {
    	return "Histogram Equalization";
    }

    public String getToolDescription() {
    	return "Performs a histogram equalization contrast enhancment on an image.";
    }

    public String[] getToolbox() {
    	String[] ret = { "ImageEnhancement" };
    	return ret;
    }

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
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
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
    
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    public boolean isActive() {
        return amIActive;
    }

    public void run() {
        amIActive = true;
        
        String outputHeader = null;
    	String inputFilesString = null;
        String outputFileSuffix = "_HistoEqual";
        WhiteboxRaster image = null;
        WhiteboxRaster output = null;
        int row, col;
        int rows = 0;
        int cols = 0;
        double z;
        float progress = 0;
        long numCells = 0;
        int i = 0;
        String[] imageFiles;
        int numImages = 0;
        double noData = -32768;
        int numBins = 1024;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFilesString = args[0];
        outputFileSuffix = args[1];
        numBins = Integer.parseInt(args[2]);
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString == null) || (outputFileSuffix == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        imageFiles = inputFilesString.split(";");

        numImages = imageFiles.length;

        try {
            
            for (i = 0; i < numImages; i++) {

                image = new WhiteboxRaster(imageFiles[i], "r");
                noData = image.getNoDataValue();
                rows = image.getNumberRows();
                cols = image.getNumberColumns();
                
                outputHeader = imageFiles[i].replace(".dep", outputFileSuffix + ".dep");
                output = new WhiteboxRaster(outputHeader, "rw", imageFiles[i], 
                        WhiteboxRaster.DataType.INTEGER, noData);
                output.setPreferredPalette(image.getPreferredPalette());

                double minValue = image.getMinimumValue();
                double maxValue = image.getMaximumValue();
                double binSize = (maxValue - minValue) / numBins;
                long[] histogram = new long[numBins];
                int binNum;
                int numBinsLessOne = numBins - 1;
                double[] data;

                if (image.getDataScale() != WhiteboxRaster.DataScale.RGB) {

                    updateProgress("Loop 1 of 2:", 0);
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != noData) {
                                numCells++;
                                binNum = (int) ((data[col] - minValue) / binSize);
                                if (binNum > numBinsLessOne) {
                                    binNum = numBinsLessOne;
                                }
                                histogram[binNum]++;
                            }

                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress("Loop 1 of 2:", (int) progress);
                    }

                    double[] cdf = new double[numBins];
                    cdf[0] = histogram[0];
                    for (int j = 1; j < numBins; j++) {
                        cdf[j] = cdf[j - 1] + histogram[j];
                    }
                    histogram = null;

                    updateProgress("Loop 2 of 2:", 0);
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != noData) {
                                binNum = (int) ((data[col] - minValue) / binSize);
                                if (binNum > numBinsLessOne) {
                                    binNum = numBinsLessOne;
                                }
                                z = Math.round((cdf[binNum] - cdf[0]) / (numCells - cdf[0]) * numBinsLessOne);
                                output.setValue(row, col, z);
                            }

                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress("Loop 2 of 2:", (int) progress);
                    }
                } else {
                    output = new WhiteboxRaster(outputHeader, "rw", imageFiles[i], 
                            WhiteboxRaster.DataType.FLOAT, noData);
                    output.setPreferredPalette(image.getPreferredPalette());
                    output.setDataScale(WhiteboxRaster.DataScale.RGB);

                    // convert the rgb to hsv and calculate the histogram for the 'v'
                    double h, s, v;
                    int a, r, g, b;
                    minValue = 99999999;
                    maxValue = -99999999;

                    float[] hsbvals = new float[3];
                    float[] rgbvals = new float[3];

                    updateProgress("Loop 1 of 3:", 0);
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != noData) {
                                r = (int) data[col] & 0xFF;
                                g = ((int) data[col] >> 8) & 0xFF;
                                b = ((int) data[col] >> 16) & 0xFF;

                                Color.RGBtoHSB(r, g, b, hsbvals);
                                v = hsbvals[2];

                                if (v < minValue) {
                                    minValue = v;
                                }
                                if (v > maxValue) {
                                    maxValue = v;
                                }

                            }

                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress("Loop 1 of 3:", (int) progress);
                    }

                    binSize = (maxValue - minValue) / numBins;
                    histogram = new long[numBins];
                    numBinsLessOne = numBins - 1;

                    updateProgress("Loop 2 of 3:", 0);
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != noData) {
                                r = (int) data[col] & 0xFF;
                                g = ((int) data[col] >> 8) & 0xFF;
                                b = ((int) data[col] >> 16) & 0xFF;

                                Color.RGBtoHSB(r, g, b, hsbvals);
                                v = hsbvals[2];

                                numCells++;
                                binNum = (int) ((v - minValue) / binSize);
                                if (binNum > numBinsLessOne) {
                                    binNum = numBinsLessOne;
                                }
                                histogram[binNum]++;
                            }

                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress("Loop 2 of 3:", (int) progress);
                    }

                    double[] cdf = new double[numBins];
                    cdf[0] = histogram[0];
                    for (int j = 1; j < numBins; j++) {
                        cdf[j] = cdf[j - 1] + histogram[j];
                    }
                    histogram = null;

                    updateProgress("Loop 3 of 3:", 0);
                    float val = 0;
                    int rgb = 0;
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != noData) {
                                z = data[col];
                                r = (int) data[col] & 0xFF;
                                g = ((int) data[col] >> 8) & 0xFF;
                                b = ((int) data[col] >> 16) & 0xFF;
                                a = ((int) data[col] >> 24) & 0xFF;
                                hsbvals = Color.RGBtoHSB(r, g, b, null);
                                v = hsbvals[2];

                                binNum = (int) ((v - minValue) / binSize);
                                if (binNum > numBinsLessOne) {
                                    binNum = numBinsLessOne;
                                }

                                val = (float) ((cdf[binNum] - cdf[0]) / (numCells - cdf[0]));

                                rgb = Color.HSBtoRGB(hsbvals[0], hsbvals[1], val);
                                r = (rgb >> 16) & 0xFF;
                                g = (rgb >> 8) & 0xFF;
                                b = rgb & 0xFF;

                                z = (double) ((a << 24) | (b << 16) | (g << 8) | r);

                                output.setValue(row, col, z);
                            }

                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress("Loop 3 of 3:", (int) progress);
                    }
                }
                output.addMetadataEntry("Created by the "
                        + getDescriptiveName() + " tool.");
                output.addMetadataEntry("Created on " + new Date());

                image.close();
                output.close();

                // returning a header file string displays the image.
                returnData(outputHeader);
            }
            
            
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}
