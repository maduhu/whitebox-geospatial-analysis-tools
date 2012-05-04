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
 * @author johnlindsay
 */
public class WetnessIndex implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "WetnessIndex";
    }

    @Override
    public String getDescriptiveName() {
    	return "Wetness Index";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the topographic wetness index, Ln(A / tan(Beta)).";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "SecondaryTerrainAttributes" };
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
        
        String areaHeader = null;
        String slopeHeader = null;
        String outputHeader = null;
        int i;
        int progress;
        int row, col;
        double z;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                areaHeader = args[i];
            } else if (i == 1) {
                slopeHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((areaHeader == null) || (slopeHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster slopeImage = new WhiteboxRaster(slopeHeader, "r");
            int rows = slopeImage.getNumberRows();
            int cols = slopeImage.getNumberColumns();
            double slopeNoData = slopeImage.getNoDataValue();
            
            WhiteboxRaster areaImage = new WhiteboxRaster(areaHeader, "r");
            if (areaImage.getNumberRows() != rows || areaImage.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            double areaNoData = areaImage.getNoDataValue();
                
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", areaHeader, 
                    WhiteboxRaster.DataType.FLOAT, areaNoData);
            output.setPreferredPalette("blueyellow.pal");

            double[] area;
            double[] slope;
            for (row = 0; row < rows; row++) {
                area = areaImage.getRowValues(row);
                slope = slopeImage.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (area[row] != areaNoData && slope[row] != slopeNoData) {
                        if (slope[row] != 0) {
                            z = Math.log(((area[row]) / (Math.tan(Math.toRadians(slope[row])))));
                        } else {
                            z = areaNoData;
                        }
                        output.setValue(row, col, z);
                    } else {
                        output.setValue(row, col, areaNoData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int)(100f * row / (rows - 1));
                updateProgress(progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            slopeImage.close();
            areaImage.close();
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
