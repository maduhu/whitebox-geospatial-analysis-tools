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
public class Reclass implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "Reclass";
    }

    @Override
    public String getDescriptiveName() {
    	return "Reclass";
    }

    @Override
    public String getToolDescription() {
    	return "Assigns grid cells in a raster image new values based on "
                + "user-defined ranges.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ReclassTools" };
    	return ret;
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
    
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
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
        int row, col;
        double noData;
        int progress;
        int i;
        int numReclassRanges;
        int numReclassRangesMinusOne;
        String[] reclassRangeStr = null;
        double[][] reclassRange;
        boolean blnAssignMode = false;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        outputHeader = args[1];
        reclassRangeStr = args[2].split("\t");
        if (reclassRangeStr[2].toLowerCase().equals("not specified")) { blnAssignMode = true; }
        
        // check to see that the inputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int rows = image.getNumberRows();
            int cols = image.getNumberColumns();
            double[] data;
            noData = image.getNoDataValue();
            
            //How many rows should there be in the ReclassRange array?
            //There are three numbers in each range: New Value, From Value, To Just Less Than Value
            numReclassRanges = reclassRangeStr.length / 3;
            numReclassRangesMinusOne = numReclassRanges - 1;
            reclassRange = new double[3][numReclassRanges];
            i = 0;
            for (int b = 0; b < reclassRangeStr.length; b++) {
                if (!reclassRangeStr[b].toLowerCase().equals("not specified")) {
                    if (!reclassRangeStr[b].toLowerCase().equals("nodata")) {
                        reclassRange[i][b / 3] = Double.parseDouble(reclassRangeStr[b]);
                    } else {
                        reclassRange[i][b / 3] = noData;
                    }
                } else {
                    reclassRange[i][b / 3] = 0;
                }
                i++;
                if (i == 3) {
                    i = 0;
                }
            }

            if (numReclassRanges == 0) {
                showFeedback("There is an error with the reclass ranges.");
                return;
            }

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette(image.getPreferredPalette());


            if (blnAssignMode) {
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        for (i = 0; i < numReclassRanges; i++) {
                            if (data[col] == reclassRange[1][i]) {
                                output.setValue(row, col, reclassRange[0][i]);
                                break;
                            }
                            if (i == numReclassRangesMinusOne) { //z was not in the reclass ranges; output value equals input value
                                output.setValue(row, col, data[col]);
                            }
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (rows - 1));
                    updateProgress(progress);
                }

            } else {
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        for (i = 0; i < numReclassRanges; i++) {
                            if (data[col] >= reclassRange[1][i] && data[col] < reclassRange[2][i]) {
                                output.setValue(row, col, reclassRange[0][i]);
                                break;
                            }
                            if (i == numReclassRangesMinusOne) { //z was not in the reclass ranges; output value equals input value
                                output.setValue(row, col, data[col]);
                            }
                        }

                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (rows - 1));
                    updateProgress(progress);
                }

            }


            image.close();

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
