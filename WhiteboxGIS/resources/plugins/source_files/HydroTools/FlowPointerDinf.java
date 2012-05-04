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
public class FlowPointerDinf implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FlowPointerDinf";
    }

    @Override
    public String getDescriptiveName() {
    	return "D-infinity Flow Pointer";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a D-infinity flow direction (pointer) operation on a specified DEM.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "FlowPointers" };
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
        int i, a;
        //int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        //int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double dist;
        double gridRes;
        double diagGridRes;
        double maxSlope;
        double flowDir = 0;
        double e0;
        double af;
        double ac;
        double e1, r, s1, s2, s, e2;
        
        int[] acVals = new int[]{0, 1, 1, 2, 2, 3, 3, 4};
        int[] afVals = new int[]{1, -1, 1, -1, 1, -1, 1, -1};

        int[] e1Col = new int[]{1, 0, 0, -1, -1, 0, 0, 1};
        int[] e1Row = new int[]{0, -1, -1, 0, 0, 1, 1, 0};

        int[] e2Col = new int[]{1, 1, -1, -1, -1, -1, 1, 1};
        int[] e2Row = new int[]{-1, -1, -1, -1, 1, 1, 1, 1};

        double atanof1 = Math.atan(1);

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            double noData = DEM.getNoDataValue();
            gridRes = DEM.getCellSizeX();
            diagGridRes = gridRes * Math.sqrt(2);
                    
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("circular_bw.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            output.setZUnits("degrees");
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    e0 = DEM.getValue(row, col);
                    if (e0 != noData) {
                        maxSlope = -9999999;
                        flowDir = 360;
                        for (a = 0; a < 8; a++) {
                            ac = acVals[a];
                            af = afVals[a];
                            e1 = DEM.getValue(row + e1Row[a], col + e1Col[a]);
                            e2 = DEM.getValue(row + e2Row[a], col + e2Col[a]);
                            if (e1 != noData && e2 != noData) {
                                if (e0 > e1 && e0 > e2) {
                                    s1 = (e0 - e1) / gridRes;
                                    if (s1 == 0) { s1 = 0.00001; }
                                    s2 = (e1 - e2) / gridRes;
                                    r = Math.atan(s2 / s1);
                                    s = Math.sqrt(s1 * s1 + s2 * s2);
                                    if (s1 < 0 && s2 < 0) { s = -1 * s; }
                                    if (s1 < 0 && s2 == 0) { s = -1 * s; }
                                    if (s1 == 0 && s2 < 0) { s = -1 * s; }
                                    if (s1 == 0.001 && s2 < 0) { s = -1 * s; }
                                    if (r < 0 || r > atanof1) {
                                        if (r < 0) {
                                            r = 0;
                                            s = s1;
                                        } else {
                                            r = atanof1;
                                            s = (e0 - e2) / diagGridRes;
                                        }
                                    }
                                    if (s >= maxSlope && s != 0.00001) {
                                        maxSlope = s;
                                        flowDir = af * r + ac * (Math.PI / 2);
                                    }
                                } else if (e0 > e1 || e0 > e2) {
                                    if (e0 > e1) {
                                        r = 0;
                                        s = (e0 - e1) / gridRes;
                                    } else {
                                        r = atanof1;
                                        s = (e0 - e2) / diagGridRes;
                                    }
                                    if (s >= maxSlope && s != 0.00001) {
                                        maxSlope = s;
                                        flowDir = af * r + ac * (Math.PI / 2);
                                    }
                                }
                            }
                        }
                        if (maxSlope <= 0) {
                            output.setValue(row, col, -1);
                        } else {
                            flowDir = Math.round((flowDir * (180 / Math.PI)) * 10) / 10;
                            flowDir = 360 - flowDir + 90;
                            if (flowDir > 360) { flowDir = flowDir - 360; }
                            output.setValue(row, col, flowDir);
                        }
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

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            DEM.close();
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