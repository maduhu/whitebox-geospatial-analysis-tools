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
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.text.DecimalFormat;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ImageStandardDeviation implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ImageStandardDeviation";
    }

    @Override
    public String getDescriptiveName() {
    	return "Image Standard Deviation";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the standard deviation of pixel values for an input image.";
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
        
        WhiteboxRaster image;
        WhiteboxRaster image2;
        int cols = 0;
        int rows = 0;
        double noData = -32768;
        int numImages;
        double z;
        float progress = 0;
        int col, row;
        int a, b, i;
        String inputFilesString = null;
        String[] imageFiles;
        double[] imageSDs;
        String[] shortNames = null;
        String[] units = null;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFilesString = args[0];
        
        imageFiles = inputFilesString.split(";");

        numImages = imageFiles.length;
        
        // check to see that the inputHeader and outputHeader are not null.
        if (numImages < 1) {
            showFeedback("At least one image must be specified for an image correlation.");
            return;
        }

        try {
            
            //initialize the image data arrays
            imageSDs = new double[numImages];
            shortNames = new String[numImages];
            units = new String[numImages];
            
            double[] data;
            // check that each of the input images has the same number of rows and columns
            // and calculate the image averages.
            for (a = 0; a < numImages; a++) {
                updateProgress("Image " + (a + 1) + ", Calculating image averages:", 1);
            
                image = new WhiteboxRaster(imageFiles[a], "r");
                noData = image.getNoDataValue();
                rows = image.getNumberRows();
                cols = image.getNumberColumns();
                shortNames[a] = image.getShortHeaderFile();
                if (!image.getZUnits().toLowerCase().equals("not specified")) {
                    units[a] = image.getZUnits();
                } else {
                    units[a] = "";
                }
                
                double imageTotal = 0;
                double imageN = 0;
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if (data[col] != noData) {
                            imageTotal += data[col];
                            imageN++;
                        }
                    }
                    if (cancelOp) { cancelOperation(); return; }
                }
                
                double imageAverage = imageTotal / imageN;
                double totalDeviation = 0;
                for (row = 0; row < rows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if (data[col] != noData) {
                            totalDeviation += (data[col] - imageAverage) * (data[col] - imageAverage);
                        }
                    }
                    if (cancelOp) { cancelOperation(); return; }
                }
                
                imageSDs[a] = Math.sqrt(totalDeviation / (imageN - 1));
                image.close();
                
                progress = (int)(100f * (a + 1) / numImages);
                updateProgress("Image " + (a + 1) + ", Calculating image average:", (int)progress);
            }

            String retstr = null;
            retstr = "IMAGE STANDARD DEVIATION\n";
            
            DecimalFormat df = new DecimalFormat("0.0000");
            
            for (a = 0; a < numImages; a++) {
                retstr = retstr + "\n" + shortNames[a] + "\t" + df.format(imageSDs[a]);
            }
            returnData(retstr);
            
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
