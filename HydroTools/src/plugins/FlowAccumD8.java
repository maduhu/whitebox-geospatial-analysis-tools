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
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class FlowAccumD8 implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "FlowAccumD8";
    }

    @Override
    public String getDescriptiveName() {
    	return "D8 and Rho8 Flow Accumulation";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a D8 or Rho8 flow accumulation operation on a "
                + "specified flow pointer grid.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "FlowAccum" };
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
        
        String inputHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        float progress = 0;
        double slope;
        double z, z2;
        int i;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        double numInNeighbours;
        boolean flag = false;
        boolean logTransform = false;
        String outputType = null;
        double gridRes;
        double flowDir = 0;
        
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
                outputType = args[i].toLowerCase();
            } else if (i == 3) {
                logTransform = Boolean.parseBoolean(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster pntr = new WhiteboxRaster(inputHeader, "r");
            
            int rows = pntr.getNumberRows();
            int cols = pntr.getNumberColumns();
            double noData = pntr.getNoDataValue();
            gridRes = pntr.getCellSizeX();
                    
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.FLOAT, 1);
            output.setPreferredPalette("blueyellow.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            output.setZUnits("dimensionless");
            
            WhiteboxRaster tmpGrid = new WhiteboxRaster(outputHeader.replace(".dep", 
                    "_temp.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            tmpGrid.isTemporaryFile = true;
            
            updateProgress("Loop 1 of 3:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (pntr.getValue(row, col) != noData) {
                        z = 0;
                        for (i = 0; i < 8; i++) {
                            if (pntr.getValue(row + dY[i], col + dX[i]) ==
                                    inflowingVals[i]) { z++; }
                        }
                        tmpGrid.setValue(row, col, z);
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 3:", (int) progress);
            }

            updateProgress("Loop 2 of 3:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (tmpGrid.getValue(row, col) == 0) { //there are no 
                        //remaining inflowing neighbours, send it's current 
                        //flow accum val downslope
                        tmpGrid.setValue(row, col, -1);
                        flag = false;
                        x = col;
                        y = row;
                        do {
                            z = output.getValue(y, x); //z is the flow accum 
                            //value to send to it's neighbour
                            //find it's downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                i = (int) (Math.log(flowDir) / LnOf2);
                                x += dX[i];
                                y += dY[i];
                                //update the output grids
                                z2 = output.getValue(y, x);
                                output.setValue(y, x, z2 + z);
                                numInNeighbours = tmpGrid.getValue(y, x) - 1;
                                tmpGrid.setValue(y, x, numInNeighbours);
                                //see if you can progress further downslope
                                if (numInNeighbours == 0) {
                                    tmpGrid.setValue(y, x, -1);
                                    flag = true;
                                } else {
                                    flag = false;
                                }
                            } else {
                                flag = false;
                            }
                        } while (flag);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 3:", (int) progress);
            }
            
            updateProgress("Loop 3 of 3:", 0);
            if (outputType.equals("specific catchment area (sca)")) {
                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        flowDir = pntr.getValue(row, col);
                        if (flowDir != noData) {
                            output.setValue(row, col,
                                    output.getValue(row, col) * gridRes);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
            } else if (outputType.equals("total catchment area")) {
                double gridCellArea = gridRes * gridRes;
                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        flowDir = output.getValue(row, col);
                        if (flowDir != noData) {
                            output.setValue(row, col,
                                    output.getValue(row, col) * gridCellArea);
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
            
            if (logTransform) {
                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        flowDir = output.getValue(row, col);
                        if (flowDir != noData) {
                            output.setValue(row, col,
                                    Math.log(output.getValue(row, col)));
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            pntr.close();
            tmpGrid.close();
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