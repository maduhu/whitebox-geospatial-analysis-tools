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
public class JensonSnapPourPoints implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
        
    @Override
    public String getName() {
        return "JensonSnapPourPoints";
    }

    @Override
    public String getDescriptiveName() {
    	return "Jenson Snap Pour Points";
    }

    @Override
    public String getToolDescription() {
    	return "Moves outlet points used to specify points of interest in a "
                + "watershedding operation onto the stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "WatershedTools" };
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
        
        WhiteboxRaster outlets;
        WhiteboxRaster streams;
        WhiteboxRaster output;
        String outputHeader = null;
        String streamsHeader = null;
        String outletHeader = null;
        int rows = 0;
        int cols = 0;
        int row, col;
        double noData = -32768;
        double gridRes = 0;
        int i;
        float progress = 0;
        double z;
        double maxZ;
        int x, y;
        int minX = 0;
        int minY = 0;
        double minDist = 0;
        double snapDistance = 0;
        int snapDistInt = 0;
        double outletID;
    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                outletHeader = args[i];
            } else if (i == 1) {
                streamsHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            } else if (i == 3) {
                snapDistance = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((outletHeader == null) || (streamsHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            outlets = new WhiteboxRaster(outletHeader, "r");
            rows = outlets.getNumberRows();
            cols = outlets.getNumberColumns();
            noData = outlets.getNoDataValue();
            gridRes = (outlets.getCellSizeX() + outlets.getCellSizeY()) / 2;

            streams = new WhiteboxRaster(streamsHeader, "r");
            
            if (streams.getNumberColumns() != cols || streams.getNumberRows() != rows) {
                showFeedback("The input files must have the same dimensions.");
                return;
            }
            
            output = new WhiteboxRaster(outputHeader, "rw", outletHeader, 
                    WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette(outlets.getPreferredPalette());
            output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
            
            //convert the snapdistance to units of grid cells
            snapDistInt = (int)(snapDistance / gridRes);
            if (snapDistInt < 1) { snapDistInt = 1; }

            double[] data;
            
            for (row = 0; row < rows; row++) {
                data = outlets.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    outletID = data[col];
                    if (outletID != 0 && outletID != noData) {
                        minDist = 99999999;
                        minX = col;
                        minY = row;
                        for (x = col - snapDistInt; x <= col + snapDistInt; x++) {
                            for (y = row - snapDistInt; y <= row + snapDistInt; y++) {
                                z = streams.getValue(y, x);
                                if (z > 0) {
                                    //calculate the distance
                                    z = (x - col) * (x - col) + (y - row) * (y - row);
                                    if (z < minDist) {
                                        minDist = z;
                                        minX = x;
                                        minY = y;
                                    }
                                }
                            }
                        }
                        output.setValue(minY, minX, outletID);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            outlets.close();
            streams.close();
            output.close();
            
            // returning a header file string displays the DEM.
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