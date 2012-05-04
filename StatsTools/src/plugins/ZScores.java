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
import java.text.DecimalFormat;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ZScores implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ZScores";
    }

    @Override
    public String getDescriptiveName() {
    	return "Z-Scores";
    }

    @Override
    public String getToolDescription() {
    	return "Converts the values of an image to z-scores.";
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
        float progress = 0;
        int col, row;
        int i;
                
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
            image = new WhiteboxRaster(inputHeader, "r");
            rows = image.getNumberRows();
            cols = image.getNumberColumns();
            double noData = image.getNoDataValue();

            if (image.getDataScale() == WhiteboxRaster.DataScale.BOOLEAN || 
                    image.getDataScale() == WhiteboxRaster.DataScale.CATEGORICAL ||
                    image.getDataScale() == WhiteboxRaster.DataScale.RGB) {
                showFeedback("This tool should only be used with data on a continuous scale.");
                return;
            }
            
            output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette(image.getPreferredPalette());

            updateProgress("Calculating image average:", 0);
            double[] data;
            for (row = 0; row < rows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] != noData) {
                        imageTotal += data[col];
                        imageN++;
                    }
                }
                if (cancelOp) { cancelOperation(); return; }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating image average:", (int)progress);
            }
            
            imageAverage = imageTotal / imageN;

            updateProgress("Calculating the standard deviation:", 0);
            for (row = 0; row < rows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] != noData) {
                        imageTotalDeviation += (data[col] - imageAverage) * 
                                (data[col] - imageAverage);
                    }
                }
                if (cancelOp) { cancelOperation(); return; }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating the standard deviation:", (int)progress);
            }

            stdDeviation = Math.sqrt(imageTotalDeviation / (imageN - 1));

            updateProgress("Calculating the z-scores:", 0);
            for (row = 0; row < rows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] != noData) {
                        output.setValue(row, col, (data[col] - imageAverage) / stdDeviation);
                    }
                }
                if (cancelOp) { cancelOperation(); return; }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating the z-scores:", (int)progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            
            DecimalFormat df;
            df = new DecimalFormat("0.000");
            
            String retstr = null;
            retstr = "CONVERT TO Z-SCORE\n";
            retstr = retstr + "Input image:\t\t" + image.getShortHeaderFile() + "\n";
            retstr = retstr + "Created image:\t\t" + output.getShortHeaderFile() + "\n";
            retstr = retstr + "Input image average:\t" + df.format(imageAverage) + "\n";
            retstr = retstr + "Input image std. dev.:\t" + df.format(stdDeviation) + "\n";
            retstr = retstr + "N:\t\t"+ imageN;
            returnData(retstr);
            
            image.close();
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
