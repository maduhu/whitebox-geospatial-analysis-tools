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
public class NumInflowingNeighbours implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "NumInflowingNeighbours";
    }

    @Override
    public String getDescriptiveName() {
    	return "Number of Inflowing Neighbours";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the number of inflowing neighbours to each grid cell.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "HydroTools" };
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
        double numDownslopeNeighbours;
        double z, z2;
        int i;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        String pntrType = null;

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
                pntrType = args[i].toLowerCase();
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
                    
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.INTEGER, noData);
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            output.setPreferredPalette("spectrum_black_background.pal");
            
            
            if ((pntrType.equals("d8") || (pntrType.equals("rho8")))) {
                // calculate the number of inflowing neighbours to each grid cell 
                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        if (pntr.getValue(row, col) != noData) {
                            z = 0;
                            for (i = 0; i < 8; i++) {
                                if (pntr.getValue(row + dY[i], col + dX[i])
                                        == inflowingVals[i]) { z++; }
                            }
                            output.setValue(row, col, z);
                        } else {
                            output.setValue(row, col, noData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
            } else if (pntrType.equals("dinf")) {
                double flowDir;
                double[] startFD = new double[]{180, 225, 270, 315, 0, 45, 90, 135};
                double[] endFD = new double[]{270, 315, 360, 45, 90, 135, 180, 225};
                int c;

                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        flowDir = pntr.getValue(row, col);
                        if (flowDir != noData) {
                            c = 0;
                            for (i = 0; i < 8; i++) {
                                flowDir = pntr.getValue(row + dY[i], col + dX[i]);
                                if (flowDir != noData) {
                                    if (i != 3) {
                                        if (flowDir > startFD[i]
                                                && flowDir < endFD[i]) {
                                            c++;
                                        }
                                    } else {
                                        if (flowDir > startFD[i]
                                                || flowDir < endFD[i]) {
                                            c++;
                                        }
                                    }
                                }
                            }
                            output.setValue(row, col, c);
                        } else {
                            output.setValue(row, col, noData);
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