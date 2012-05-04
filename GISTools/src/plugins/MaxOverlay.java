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
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.util.Date;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MaxOverlay implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "MaxOverlay";
    }

    @Override
    public String getDescriptiveName() {
    	return "Max Overlay";
    }

    @Override
    public String getToolDescription() {
    	return "Evaluates the maximum value for each grid cell from a group of input rasters.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "OverlayTools" };
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
        
        String outputHeader = null;
        WhiteboxRaster image = null;
        WhiteboxRaster output = null;
        int cols = 0;
        int rows = 0;
        double imageNoData = -32768;
        double outputNoData = -32768;
        int numImages;
        double z;
        int progress = 0;
        int col, row;
        int a, i;
        String inputFilesString = null;
        String[] imageFiles;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        // read the input parameters
        inputFilesString = args[0];
        outputHeader = args[1];
        
        imageFiles = inputFilesString.split(";");

        numImages = imageFiles.length;
        
        // check to see that the inputHeader and outputHeader are not null.
        if (numImages < 2) {
            showFeedback("At least two images must be specified.");
            return;
        }

        try {
            double[] data;
            // check that each of the input images has the same number of rows and columns
            // and calculate the image averages.
            updateProgress("Calculating min values:", 0);
            for (a = 0; a < numImages; a++) {
                image = new WhiteboxRaster(imageFiles[a], "r");
                imageNoData = image.getNoDataValue();
                String label = "Loop " + String.valueOf(a + 1) + " of " + String.valueOf(numImages) + ":";
                    
                if (a == 0) {
                    rows = image.getNumberRows();
                    cols = image.getNumberColumns();
                    outputNoData = imageNoData;
                    output = new WhiteboxRaster(outputHeader, "rw", imageFiles[0], 
                            WhiteboxRaster.DataType.FLOAT, outputNoData);
                    output.setPreferredPalette(image.getPreferredPalette());
                } else {
                    if (image.getNumberColumns() != cols || 
                            image.getNumberRows() != rows) {
                        showFeedback("All input images must have the same dimensions (rows and columns).");
                        return;
                    }
                }
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if (data[col] != imageNoData) {
                            z = output.getValue(row, col);
                            if (z != outputNoData) {
                                if (data[col] > z) {
                                    output.setValue(row, col, data[col]);
                                }
                            } else {
                                output.setValue(row, col, data[col]);
                            }
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (rows - 1));
                    updateProgress(label, progress);
                }
                image.close();
                progress = a / (numImages - 1) * 100;
                updateProgress("Calculating image average:", (int)progress);
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
}
