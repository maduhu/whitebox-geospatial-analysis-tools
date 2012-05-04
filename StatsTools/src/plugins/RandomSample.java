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
import java.util.Random;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class RandomSample implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "RandomSample";
    }

    @Override
    public String getDescriptiveName() {
    	return "Random Sample";
    }

    @Override
    public String getToolDescription() {
    	return "Creates an image containing randomly located sample grid cells.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "StatisticalTools" };
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
    	
        WhiteboxRaster image;
        WhiteboxRaster output;
        int cols, rows;
        double imageTotal = 0;
        long imageN = 0;
        double imageAverage = 0;
        double imageTotalDeviation = 0;
        double stdDeviation = 0;
        int progress = 0;
        int col, row;
        int i;
        int numSamplePoints = 0;
                
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
                numSamplePoints = Integer.parseInt(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            image = new WhiteboxRaster(inputHeader, "r");
            rows = image.getNumberRows();
            cols = image.getNumberColumns();
            
            if (rows * cols < numSamplePoints) {
                showFeedback("The number of samples cannot exceed the number of cells.");
                return;
            }

            output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            output.setPreferredPalette("qual.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);

            image.close();
            
            Random generator = new Random();
            
            i = 0;
            do {
                row = generator.nextInt(rows);
                col = generator.nextInt(cols);
                if (output.getValue(row, col) == 0) {
                    i++;
                    output.setValue(row, col, i);
                    progress = (int)(100f * i / numSamplePoints);
                    updateProgress(progress);
                }
            } while (i < numSamplePoints);
            
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
