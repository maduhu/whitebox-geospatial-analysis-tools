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
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ChangeDataType implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    @Override
    public String getName() {
        return "ChangeDataType";
    }

    @Override
    public String getDescriptiveName() {
    	return "Change Data Type";
    }

    @Override
    public String getToolDescription() {
    	return "Converts a raster image's data type.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ConversionTools" };
    	return ret;
    }

    
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.print("Progress: " + progress + "%");
        }
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

        String inputHeader = null;
        String outputHeader = null;
        String suffix = "_new";
        String inputFilesString = null;
        WhiteboxRaster.DataType dataType = WhiteboxRaster.DataType.FLOAT;
        String[] imageFiles;
        int numImages = 0;
        int i;
        int col, row;
        int progress;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputFilesString = args[i];
            } else if (i == 1) {
                suffix = args[i];
            } else if (i == 2) {
                if (args[i].toLowerCase().contains("double")) {
                    dataType = DataType.DOUBLE;
                } else if (args[i].toLowerCase().contains("float")) {
                    dataType = DataType.FLOAT;
                } else if (args[i].toLowerCase().contains("int")) {
                    dataType = DataType.INTEGER;
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if (inputFilesString == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            imageFiles = inputFilesString.split(";");
            numImages = imageFiles.length;
            for (i = 0; i < numImages; i++) {
                progress = (int)(100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                
                inputHeader = imageFiles[i];
                WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
                int rows = image.getNumberRows();
                int cols = image.getNumberColumns();
                double inputNoData = image.getNoDataValue();
                
                outputHeader = inputHeader.replace(".dep", suffix + ".dep");
                WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, dataType, inputNoData);
                double[] data = null;
                if (dataType == DataType.DOUBLE) { // any no data value will fit
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            output.setValue(row, col, data[col]);                            
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (rows - 1));
                        updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                    }
                } else if (dataType == DataType.FLOAT) {
                    double outputNoData = -32768;
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != inputNoData) {
                                output.setValue(row, col, data[col]);
                            } else {
                                output.setValue(row, col, outputNoData);
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (rows - 1));
                        updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                    }
                } else if (dataType == DataType.INTEGER) {
                    double outputNoData = -32768;
                    double z;
                    for (row = 0; row < rows; row++) {
                        data = image.getRowValues(row);
                        for (col = 0; col < cols; col++) {
                            if (data[col] != inputNoData) {
                                z = Math.round(data[col]);
                                output.setValue(row, col, (int)z);
                            } else {
                                output.setValue(row, col, outputNoData);
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (rows - 1));
                        updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                    }
                }
                
                image.close();
                output.close();
            }
            
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
}
