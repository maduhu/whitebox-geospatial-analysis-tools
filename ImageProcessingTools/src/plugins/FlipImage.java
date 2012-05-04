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
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class FlipImage implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    public String getName() {
        return "FlipImage";
    }

    public String getDescriptiveName() {
    	return "Flip Image";
    }

    public String getToolDescription() {
    	return "Reflects an image in the vertical or horizontal axis.";
    }

    public String[] getToolbox() {
    	String[] ret = { "ImageProc" };
    	return ret;
    }

    
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

        String inputHeader = null;
        String outputHeader = null;
        int col;
        int row;
        int numCols;
        int numRows;
        int i;
        int progress;
        String reflectionAxis = "vertical";
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                if (args[i].toLowerCase().contains("v")) {
                    reflectionAxis = "vertical";
                } else if (args[i].toLowerCase().contains("ho")) {
                    reflectionAxis = "horizontal";
                } else if (args[i].toLowerCase().contains("b")) {
                    reflectionAxis = "both";
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            numRows = image.getNumberRows();
            numCols = image.getNumberColumns();
            int rowsLessOne = numRows - 1;
            int colsLessOne = numCols - 1;
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            
            double[] data;
            if (reflectionAxis.equals("vertical")) {
                for (row = 0; row < numRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < numCols; col++) {
                        output.setValue(rowsLessOne - row, col, data[col]);
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (numRows - 1));
                    updateProgress("Finding patch min row and columns:", progress);
                }
            } else if (reflectionAxis.equals("horizontal")) {
                for (row = 0; row < numRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < numCols; col++) {
                        output.setValue(row, colsLessOne - col, data[col]);
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (numRows - 1));
                    updateProgress("Finding patch min row and columns:", progress);
                }
            } else if (reflectionAxis.equals("both")) {
                for (row = 0; row < numRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < numCols; col++) {
                        output.setValue(rowsLessOne - row, colsLessOne - col, data[col]);
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (numRows - 1));
                    updateProgress("Finding patch min row and columns:", progress);
                }
            }
            
            output.setDisplayMinimum(image.getDisplayMinimum());
            output.setDisplayMaximum(image.getDisplayMaximum());
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            image.close();
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

            
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
