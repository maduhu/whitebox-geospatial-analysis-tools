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
public class FlowAccumDinf implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    WhiteboxRaster pointer;
    WhiteboxRaster output;
    WhiteboxRaster tmpGrid;
    double noData = -32768;
    int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
    int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
    double gridRes = 1;
    double[] startFD = new double[]{180, 225, 270, 315, 0, 45, 90, 135};
    double[] endFD = new double[]{270, 315, 360, 45, 90, 135, 180, 225};
    
    @Override
    public String getName() {
        return "FlowAccumDinf";
    }

    @Override
    public String getDescriptiveName() {
    	return "D-infinity Flow Accumulation";
    }

    @Override
    public String getToolDescription() {
    	return "Performs an D-infinity flow accumulation operation on a "
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
        int i, c;
        double numInNeighbours;
        boolean flag = false;
        boolean logTransform = false;
        String outputType = null;
        double flowDir;
        
        
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
            pointer = new WhiteboxRaster(inputHeader, "r");
            int rows = pointer.getNumberRows();
            int cols = pointer.getNumberColumns();
            noData = pointer.getNoDataValue();
            gridRes = pointer.getCellSizeX();
                    
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
                    flowDir = pointer.getValue(row, col);
                    if (flowDir != noData) {
                        i = 0;
                        for (c = 0; c < 8; c++) {
                            x = col + dX[c];
                            y = row + dY[c];
                            flowDir = pointer.getValue(y, x);
                            if (flowDir >= 0 && flowDir <= 360) {
                                if (c != 3) {
                                    if (flowDir > startFD[c] && flowDir < endFD[c]) { i++; }
                                } else {
                                    if (flowDir > startFD[c] || flowDir < endFD[c]) { i++; }
                                }
                            }
                        }
                        tmpGrid.setValue(row, col, i);
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
                        DinfAccum(row, col);
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
                        z = pointer.getValue(row, col);
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
            
            pointer.close();
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
    
    private void DinfAccum(int row, int col) {
        double flowAccumVal = output.getValue(row, col);
        double flowDir = pointer.getValue(row, col);
        double proportion1 = 0;
        double proportion2 = 0;
        int a1 = 0;
        int b1 = 0;
        int a2 = 0;
        int b2 = 0;

        tmpGrid.setValue(row, col, -1); // 'this ensures that you don't process this cell a second time.
        
        if (flowDir >= 0) {
            // find which two cells receive flow and the proportion to each
            if (flowDir >= 0 && flowDir < 45) {
                proportion1 = (45 - flowDir) / 45;
                a1 = col;
                b1 = row - 1;
                proportion2 = flowDir / 45;
                a2 = col + 1;
                b2 = row - 1;
            } else if (flowDir >= 45 && flowDir < 90) {
                proportion1 = (90 - flowDir) / 45;
                a1 = col + 1;
                b1 = row - 1;
                proportion2 = (flowDir - 45) / 45;
                a2 = col + 1;
                b2 = row;
            } else if (flowDir >= 90 && flowDir < 135) {
                proportion1 = (135 - flowDir) / 45;
                a1 = col + 1;
                b1 = row;
                proportion2 = (flowDir - 90) / 45;
                a2 = col + 1;
                b2 = row + 1;
            } else if (flowDir >= 135 && flowDir < 180) {
                proportion1 = (180 - flowDir) / 45;
                a1 = col + 1;
                b1 = row + 1;
                proportion2 = (flowDir - 135) / 45;
                a2 = col;
                b2 = row + 1;
            } else if (flowDir >= 180 && flowDir < 225) {
                proportion1 = (225 - flowDir) / 45;
                a1 = col;
                b1 = row + 1;
                proportion2 = (flowDir - 180) / 45;
                a2 = col - 1;
                b2 = row + 1;
            } else if (flowDir >= 225 && flowDir < 270) {
                proportion1 = (270 - flowDir) / 45;
                a1 = col - 1;
                b1 = row + 1;
                proportion2 = (flowDir - 225) / 45;
                a2 = col - 1;
                b2 = row;
            } else if (flowDir >= 270 && flowDir < 315) {
                proportion1 = (315 - flowDir) / 45;
                a1 = col - 1;
                b1 = row;
                proportion2 = (flowDir - 270) / 45;
                a2 = col - 1;
                b2 = row - 1;
            } else if (flowDir >= 315 && flowDir <= 360) {
                proportion1 = (360 - flowDir) / 45;
                a1 = col - 1;
                b1 = row - 1;
                proportion2 = (flowDir - 315) / 45;
                a2 = col;
                b2 = row - 1;
            }

            if (proportion1 > 0 && output.getValue(b1, a1) != noData) {
                output.setValue(b1, a1, output.getValue(b1, a1) + flowAccumVal * proportion1);
                tmpGrid.setValue(b1, a1, tmpGrid.getValue(b1, a1) - 1);
                if (tmpGrid.getValue(b1, a1) == 0) {
                    DinfAccum(b1, a1);
                }
            }
            if (proportion2 > 0 && output.getValue(b2, a2) != noData) {
                output.setValue(b2, a2, output.getValue(b2, a2) + flowAccumVal * proportion2);
                tmpGrid.setValue(b2, a2, tmpGrid.getValue(b2, a2) - 1);
                if (tmpGrid.getValue(b2, a2) == 0) {
                    DinfAccum(b2, a2);
                }
            }
        }
    }
}