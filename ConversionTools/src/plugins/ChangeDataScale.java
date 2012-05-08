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

import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ChangeDataScale implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    @Override
    public String getName() {
        return "ChangeDataScale";
    }

    @Override
    public String getDescriptiveName() {
    	return "Change Data Scale";
    }

    @Override
    public String getToolDescription() {
    	return "Converts the data scale used for default raster visualization.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ConversionTools" };
    	return ret;
    }

    
    @Override
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
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     * @return a boolean describing whether or not the plugin is actively being used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }
    
    @Override
    public void run() {
        amIActive = true;

        String inputHeader = null;
        String inputFilesString = null;
        WhiteboxRaster.DataScale dataScale = WhiteboxRaster.DataScale.CONTINUOUS;
        String[] imageFiles;
        int numImages = 0;
        int i;
        int progress;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputFilesString = args[i];
            } else if (i == 1) {
                if (args[i].toLowerCase().contains("bool")) {
                    dataScale = WhiteboxRaster.DataScale.BOOLEAN;
                } else if (args[i].toLowerCase().contains("cat")) {
                    dataScale = WhiteboxRaster.DataScale.CATEGORICAL;
                } else if (args[i].toLowerCase().contains("con")) {
                    dataScale = WhiteboxRaster.DataScale.CONTINUOUS;
                } else if (args[i].toLowerCase().contains("rgb")) {
                    dataScale = WhiteboxRaster.DataScale.RGB;
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if (inputFilesString == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            imageFiles = inputFilesString.split(";");
            numImages = imageFiles.length;
            for (i = 0; i < numImages; i++) {
                progress = (int)(100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                
                inputHeader = imageFiles[i];
                WhiteboxRaster wbr = new WhiteboxRaster(inputHeader, "r");
                if (dataScale == WhiteboxRaster.DataScale.RGB 
                        && !wbr.getDataType().equals("float")) {
                    showFeedback("Data scale RGB is only compatible with data type 'float'. "
                            + "This tool will not execute");
                    return;
                }
                wbr.setDataScale(dataScale);
                wbr.writeHeaderFile();
                wbr.close();
            }
            
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
