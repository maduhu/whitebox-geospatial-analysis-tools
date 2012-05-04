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

import java.text.DecimalFormat;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class CrispnessIndex implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    public String getName() {
        return "CrispnessIndex";
    }

    public String getDescriptiveName() {
    	return "Crispness Index";
    }

    public String getToolDescription() {
    	return "Calculates the Crispness Index and is used to quantify how crisp "
                + "(or fuzzy) a probability image is.";
    }

    public String[] getToolbox() {
    	String[] ret = { "FuzzyTools" };
    	return ret;
    }

    
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
    
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    public boolean isActive() {
        return amIActive;
    }
    
    public void run() {
        amIActive = true;

        String inputHeader = null;
        
        int i = 0;
        int rows, cols;
        double imageTotal = 0;
        long imageN = 0;
        double imageAverage = 0;
        double imageTotalDeviation = 0;
        double crispness = 0;
        double z = 0;
        float progress = 0;
        int col, row;
        int a;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if (inputHeader == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");

            rows = image.getNumberRows();
            cols = image.getNumberColumns();
            double noData = image.getNoDataValue();
            
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z != noData) {
                        if (z < 0 || z > 1) {
                            showFeedback("This tool should only be used with "
                                    + "membership probability images containing "
                                    + "values that range from 0 to 1.");
                            break;
                        }
                        imageTotal += z;
                        imageN++;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating image average:", (int) progress);
            }

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z != noData) {
                        imageTotalDeviation += (z - imageAverage) * 
                                (z - imageAverage);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating the image total squared deviation:", (int) progress);
            }
            
            image.close();

            double denominator;
            denominator = Math.pow(imageTotal * (1 - imageAverage), 2) + 
                    Math.pow(imageAverage, 2) * (imageN - imageTotal);
            crispness = imageTotalDeviation / denominator;

            DecimalFormat df;
            df = new DecimalFormat("0.000");
            
            String retstr = "CRISPNESS INDEX";
            retstr = retstr + "\nInput image:\t" + inputHeader;
            retstr = retstr + "\nCrispness (C):\t" + df.format(crispness);

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
