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
public class NewRasterFromBase implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "NewRasterFromBase";
    }

    @Override
    public String getDescriptiveName() {
        return "New Raster From Base";
    }

    @Override
    public String getToolDescription() {
        return "Creates a new raster using a base image.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"RasterCreation"};
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
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
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
        WhiteboxRaster image = null;
        float progress = 0;
        int i;
        double constantValue = 0;
        WhiteboxRaster.DataType dataType = WhiteboxRaster.DataType.FLOAT;
        double noData = 0;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
                if (inputHeader == null) {
                    showFeedback("One or more of the input parameters have not been set properly.");
                    return;
                }
                image = new WhiteboxRaster(inputHeader, "r");
                noData = image.getNoDataValue();
            } else if (i == 1) {
                outputHeader = args[i];
                if (outputHeader == null) {
                    showFeedback("One or more of the input parameters have not been set properly.");
                    return;
                }
            } else if (i == 2) {
                try {
                    if (args[i].toLowerCase().contains("nodata") || args[i].toLowerCase().contains("no data")) {
                        constantValue = noData;
                    } else {
                        constantValue = Double.parseDouble(args[i]);
                    }
                } catch (Exception e) {
                    constantValue = noData;
                }
            } else if (i == 3) {
                if (args[i].toLowerCase().contains("double")) {
                    dataType = WhiteboxRaster.DataType.DOUBLE;
                } else if (args[i].toLowerCase().contains("float")) {
                    dataType = WhiteboxRaster.DataType.FLOAT;
                } else if (args[i].toLowerCase().contains("int")) {
                    dataType = WhiteboxRaster.DataType.INTEGER;
                } else if (args[i].toLowerCase().contains("byte")) {
                    dataType = WhiteboxRaster.DataType.BYTE;
                    if (constantValue > 127 || constantValue < -128) {
                        noData = -128;
                    }
                }
            }
        }

        try {
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    inputHeader, dataType, constantValue);
            output.setNoDataValue(noData);
            output.createNewDataFile();
            
            output.setPreferredPalette(image.getPreferredPalette());
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            image.close();
            output.close();

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