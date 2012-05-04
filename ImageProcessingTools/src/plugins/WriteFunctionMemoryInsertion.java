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

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class WriteFunctionMemoryInsertion implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "WriteFunctionMemoryInsertion";
    }

    @Override
    public String getDescriptiveName() {
        return "Write Function Memory Insertion";
    }

    @Override
    public String getToolDescription() {
        return "Performs a write function memory insertion for change detection.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"ChangeDetection"};
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

        String inputHeaderRed = null;
        String inputHeaderGreen = null;
        String inputHeaderBlue = null;
        String outputHeader = null;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputHeaderBlue = args[0];
        inputHeaderGreen = args[1];
        inputHeaderRed = args[2];
        outputHeader = args[3];

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeaderRed == null) || (inputHeaderGreen == null)
                || (inputHeaderBlue == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            int row, col;
            double redVal, greenVal, blueVal;
            double redRange, greenRange, blueRange;
            double redMin, greenMin, blueMin;
            int r, g, b;
            double z;
            float progress = 0;
            
            if (inputHeaderRed.toLowerCase().contains("not specified")) {
                inputHeaderRed = inputHeaderGreen;
                inputHeaderGreen = inputHeaderBlue;
            }

            WhiteboxRasterInfo red = new WhiteboxRasterInfo(inputHeaderRed);

            int rows = red.getNumberRows();
            int cols = red.getNumberColumns();

            WhiteboxRasterInfo green = new WhiteboxRasterInfo(inputHeaderGreen);
            if ((green.getNumberRows() != rows) || (green.getNumberColumns() != cols)) {
                showFeedback("All input images must have the same dimensions.");
                return;
            }
            
            double noData = red.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw",
                    inputHeaderRed, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette("rgb.pal");
            outputFile.setDataScale(WhiteboxRaster.DataScale.RGB);

            if (!inputHeaderGreen.equals(inputHeaderBlue)) {
                WhiteboxRasterInfo blue = new WhiteboxRasterInfo(inputHeaderBlue);
                if ((blue.getNumberRows() != rows) || (blue.getNumberColumns() != cols)) {
                    showFeedback("All input images must have the same dimensions.");
                    return;
                }

                redMin = red.getDisplayMinimum();
                greenMin = green.getDisplayMinimum();
                blueMin = blue.getDisplayMinimum();

                redRange = red.getDisplayMaximum() - redMin;
                greenRange = green.getDisplayMaximum() - greenMin;
                blueRange = blue.getDisplayMaximum() - blueMin;

                double[] dataRed, dataGreen, dataBlue;
                for (row = 0; row < rows; row++) {
                    dataRed = red.getRowValues(row);
                    dataGreen = green.getRowValues(row);
                    dataBlue = blue.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        redVal = dataRed[col];
                        greenVal = dataGreen[col];
                        blueVal = dataBlue[col];
                        if ((redVal != noData) && (greenVal != noData) && (blueVal != noData)) {
                            r = (int) ((redVal - redMin) / redRange * 255);
                            if (r < 0) {
                                r = 0;
                            }
                            if (r > 255) {
                                r = 255;
                            }
                            g = (int) ((greenVal - greenMin) / greenRange * 255);
                            if (g < 0) {
                                g = 0;
                            }
                            if (g > 255) {
                                g = 255;
                            }
                            b = (int) ((blueVal - blueMin) / blueRange * 255);
                            if (b < 0) {
                                b = 0;
                            }
                            if (b > 255) {
                                b = 255;
                            }
                            z = (double) ((255 << 24) | (b << 16) | (g << 8) | r);
                            outputFile.setValue(row, col, z);
                        } else {
                            outputFile.setValue(row, col, noData);
                        }

                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                red.close();
                green.close();
                blue.close();
            } else {
                redMin = red.getDisplayMinimum();
                greenMin = green.getDisplayMinimum();
                blueMin = greenMin;

                redRange = red.getDisplayMaximum() - redMin;
                greenRange = green.getDisplayMaximum() - greenMin;
                blueRange = greenRange;

                double[] dataRed, dataGreen;
                for (row = 0; row < rows; row++) {
                    dataRed = red.getRowValues(row);
                    dataGreen = green.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        redVal = dataRed[col];
                        greenVal = dataGreen[col];
                        blueVal = dataGreen[col];
                        if ((redVal != noData) && (greenVal != noData) && (blueVal != noData)) {
                            r = (int) ((redVal - redMin) / redRange * 255);
                            if (r < 0) {
                                r = 0;
                            }
                            if (r > 255) {
                                r = 255;
                            }
                            g = (int) ((greenVal - greenMin) / greenRange * 255);
                            if (g < 0) {
                                g = 0;
                            }
                            if (g > 255) {
                                g = 255;
                            }
                            b = (int) ((blueVal - blueMin) / blueRange * 255);
                            if (b < 0) {
                                b = 0;
                            }
                            if (b > 255) {
                                b = 255;
                            }
                            z = (double) ((255 << 24) | (b << 16) | (g << 8) | r);
                            outputFile.setValue(row, col, z);
                        } else {
                            outputFile.setValue(row, col, noData);
                        }

                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                red.close();
                green.close();
            }

            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());

            outputFile.close();
            
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
