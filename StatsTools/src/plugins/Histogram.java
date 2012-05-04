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
public class Histogram implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "Histogram";
    }

    @Override
    public String getDescriptiveName() {
    	return "Histogram";
    }

    @Override
    public String getToolDescription() {
    	return "Produces a histogram from an input image.";
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
        WhiteboxRaster image;
        int cols, rows;
        double z;
        float progress = 0;
        int col, row;
        int a, i;
        double classSize = -9999;
        double startingClass = -9999;
        double endingClass = -9999;
        int numClasses = -9999;
        double[] histo;
        boolean blnCumulative = false;
        int classVal = 0;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                classSize = Double.parseDouble(args[i]);
            } else if (i == 2) {
                if(!args[i].toLowerCase().equals("not specified")) {
                    startingClass = Double.parseDouble(args[i]);
                }
            } else if (i == 3) {
                if(!args[i].toLowerCase().equals("not specified")) {
                    endingClass = Double.parseDouble(args[i]);
                }
            } else if (i == 4) {
                if (args[i].toLowerCase().equals("true")) { blnCumulative = true; }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if (inputHeader == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        if (classSize <= 0) {
            showFeedback("Class size must be larger than zero.");
            return;
        }
                    
        try {
            image = new WhiteboxRaster(inputHeader, "r");
            rows = image.getNumberRows();
            cols = image.getNumberColumns();
            double noData = image.getNoDataValue();

            if (startingClass == -9999) {
                startingClass = image.getMinimumValue();
            }
            if (endingClass == -9999) {
                endingClass = image.getMaximumValue();
            }

            double endingClassEndingVal = endingClass + classSize;

            numClasses = (int)((endingClassEndingVal - startingClass) / classSize);

            histo = new double[numClasses];

            updateProgress("Calculating histogram:", 0);
            double[] data;
            for (row = 0; row < rows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] != noData) {
                        // see what class this value is in
                        classVal = (int)(Math.floor((data[col] - startingClass) / classSize));
                        if (classVal < numClasses && classVal > 0) {
                            histo[classVal]++;
                        }
                    }
                }
                if (cancelOp) { cancelOperation(); return; }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating image average:", (int)progress);
            }
            
            image.close();
            
            if (blnCumulative) {
                for (a = 1; a < numClasses; a++) {
                    histo[a] = histo[a] + histo[a - 1];
                }
                for (a = 0; a < numClasses; a++) {
                    histo[a] = histo[a] / histo[numClasses - 1];
                }
            }


            String retstr = null;
            retstr = "HISTOGRAM\n";
            retstr = retstr + "Input image:\t" + image.getShortHeaderFile() + "\n";
            retstr = retstr + "Cumulative:\t" + Boolean.toString(blnCumulative) + "\n\n";
            retstr = retstr + "Bin\t" +  "Freq.\t" + "\n";
            if (!blnCumulative) {
                for (a = 0; a < numClasses; a++) {
                    z = a * classSize + startingClass;
                    retstr = retstr + z + "\t" + (int)(histo[a]) + "\n";
                }
            } else {
                DecimalFormat df = new DecimalFormat("0.0000");
                for (a = 0; a < numClasses; a++) {
                    z = a * classSize + startingClass;
                    retstr = retstr + z + "\t" + df.format(histo[a]) + "\n";
                }
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
