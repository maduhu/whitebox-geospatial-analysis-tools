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
public class FlowAccumFD8 implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    WhiteboxRaster DEM;
    WhiteboxRaster output;
    WhiteboxRaster tmpGrid;
    double threshold = 0;
    double noData = -32768;
    int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
    int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
    double power = 1;
    double gridRes;
        
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "FlowAccumFD8";
    }

    @Override
    public String getDescriptiveName() {
    	return "FD8 Flow Accumulation";
    }

    @Override
    public String getToolDescription() {
    	return "Performs an FD8 flow accumulation operation on a "
                + "specified digital elevation model (DEM).";
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
        double numInNeighbours;
        boolean flag = false;
        boolean logTransform = false;
        String outputType = null;
        
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
                power = Double.parseDouble(args[i]);
                if (power > 10) { power = 10; }
            } else if (i == 3) {
                outputType = args[i].toLowerCase();
            } else if (i == 4) {
                logTransform = Boolean.parseBoolean(args[i]);
            } else if (i == 5) {
                if (!args[i].toLowerCase().contains("not specified")) {
                    threshold = Double.parseDouble(args[i]);
                } else {
                    threshold = -9999;
                }
                
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            DEM = new WhiteboxRaster(inputHeader, "r");
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            noData = DEM.getNoDataValue();
            gridRes = DEM.getCellSizeX();
                    
            output = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.FLOAT, 1);
            output.setPreferredPalette("blueyellow.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            output.setZUnits("dimensionless");
            
            tmpGrid = new WhiteboxRaster(outputHeader.replace(".dep", 
                    "_temp.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            tmpGrid.isTemporaryFile = true;
            
            // Calculate the number of inflowing neighbours to each cell.
            updateProgress("Loop 1 of 3:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = DEM.getValue(row, col);
                    if (z != noData) {
                        numInNeighbours = 0;
                        for (i = 0; i < 8; i++) {
                            if (DEM.getValue(row + dY[i], col + dX[i]) > z) { 
                                numInNeighbours++; 
                            }
                        }
                        tmpGrid.setValue(row, col, numInNeighbours);
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
                        FD8Accum(row, col);
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
                        z = DEM.getValue(row, col);
                        if (z != noData) {
                            output.setValue(row, col,
                                    output.getValue(row, col) * gridRes);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress("Loop 3 of 3:", (int) progress);
                }
            } else if (outputType.equals("total catchment area")) {
                double gridCellArea = gridRes * gridRes;
                for (row = 0; row < rows; row++) {
                    for (col = 0; col < cols; col++) {
                        z = output.getValue(row, col);
                        if (z != noData) {
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
                        z = output.getValue(row, col);
                        if (z != noData) {
                            output.setValue(row, col,
                                    Math.log(output.getValue(row, col)));
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
            
            DEM.close();
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
    
    private void FD8Accum(int row, int col) {
        double flowAccumVal = output.getValue(row, col);
        double proportion = 0;
        double totalRelief = 0;
        int a, b;
        byte c;
        double z1 = DEM.getValue(row, col);
        double z2;
        double z;

        tmpGrid.setValue(row, col, -1);

        if (threshold > flowAccumVal || threshold == -9999) {
            for (c = 0; c < 8; c++) {
                a = col + dX[c];
                b = row + dY[c];
                z2 = DEM.getValue(b, a);
                if (z1 > z2 && z2 != noData) {
                    totalRelief += Math.pow((z1 - z2), power);
                }
            }

            for (c = 0; c < 8; c++) {
                a = col + dX[c];
                b = row + dY[c];
                z2 = DEM.getValue(b, a);
                if (z1 > z2 && z2 != noData) {
                    proportion = Math.pow((z1 - z2), power) / totalRelief;
                    z = output.getValue(b, a);
                    output.setValue(b, a, z +  flowAccumVal * proportion);
                    tmpGrid.setValue(b, a, tmpGrid.getValue(b, a) - 1);
                    if (tmpGrid.getValue(b, a) == 0) {
                        FD8Accum(b, a);
                    }
                }
            }
        } else { //use a D8 method
            //find the the steepest downslope neighbour
            double slope;
            double maxSlope = -999999999;
            double diagGridRes = gridRes * Math.sqrt(2);
            double dist = diagGridRes;
            double flowDir = 255;

            for (c = 0; c < 8; c++) {
                a = col + dX[c];
                b = row + dY[c];
                z2 = DEM.getValue(b, a);
                if (z1 > z2 && z2 != noData) {
                    slope = (z1 - z2) / dist;
                    if (slope > maxSlope) {
                        maxSlope = slope;
                        flowDir = c;
                    }
                }

                if (dist == gridRes) {
                    dist = diagGridRes;
                } else {
                    dist = gridRes;
                }
            }

            for (c = 0; c < 8; c++) {
                a = col + dX[c];
                b = row + dY[c];
                z2 = DEM.getValue(b, a);
                if (z1 > z2 && z2 != noData) {
                    if (c == flowDir) {
                        proportion = 1;
                    } else {
                        proportion = 0;
                    }
                    output.setValue(b, a, output.getValue(b, a) + 
                            flowAccumVal * proportion);
                    tmpGrid.setValue(b, a, tmpGrid.getValue(b, a) - 1);
                    if (tmpGrid.getValue(b, a) == 0) {
                        FD8Accum(b, a);
                    }
                }
            }
        }
    }
}