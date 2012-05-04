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
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.TDistribution;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class CrossTabulation implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "CrossTabulation";
    }

    @Override
    public String getDescriptiveName() {
        return "Cross Tabulation";
    }

    @Override
    public String getToolDescription() {
        return "Performs a cross-tabulation on two categorical images.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"StatisticalTools", "ChangeDetection"};
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

        String inputHeader1 = null;
        String inputHeader2 = null;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputHeader1 = args[0];
        inputHeader2 = args[1];

        // check to see that the inputHeader1 and outputHeader are not null.
        if (inputHeader1 == null || inputHeader2 == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            int row, col;
            double x, y;
            float progress = 0;

            WhiteboxRaster image1 = new WhiteboxRaster(inputHeader1, "r");
            int rows = image1.getNumberRows();
            int cols = image1.getNumberColumns();
            double noData1 = image1.getNoDataValue();
            int image1Min = (int) image1.getMinimumValue();
            int image1Max = (int) image1.getMaximumValue();
            int image1Range = image1Max - image1Min + 1;

            WhiteboxRaster image2 = new WhiteboxRaster(inputHeader2, "r");
            if (rows != image2.getNumberRows() || cols != image2.getNumberColumns()) {
                showFeedback("The input images must have the same dimensions (rows and columns).");
                return;
            }
            double noData2 = image2.getNoDataValue();
            int image2Min = (int) image2.getMinimumValue();
            int image2Max = (int) image2.getMaximumValue();
            int image2Range = image2Max - image2Min + 1;

            long[][] contingencyTable = new long[image1Range][image2Range];

            double[] data1, data2;
            for (row = 0; row < rows; row++) {
                data1 = image1.getRowValues(row);
                data2 = image2.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    x = data1[col];
                    y = data2[col];
                    if (x != noData1 && y != noData2) {
                        contingencyTable[(int)(x - image1Min)][(int)(y - image2Min)]++;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            DecimalFormat df = new DecimalFormat("###,###,###,###");
            String retstr = null;
            retstr = "CROSS-TABULATION REPORT\n\n";
            retstr += "Input Image 1 (X):\t\t" + image1.getShortHeaderFile() + "\n";
            retstr += "Input Image 2 (Y):\t\t" + image2.getShortHeaderFile() + "\n\n";

            String contingency = "\t\tImage 1\nImage 2";
            for (int a = 0; a < image1Range; a++) {
                contingency += "\t" + (a + image1Min);
            }
            contingency += "\n";
            for (int b = 0; b < image2Range; b++) {
                contingency += (b + image2Min);
                for (int a = 0; a < image1Range; a++) {
                    contingency += "\t" + df.format(contingencyTable[a][b]);
                }
                contingency += "\n";
            }

            retstr += contingency;
            
            returnData(retstr);

            image1.close();
            image2.close();

        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        ImageRegression ir = new ImageRegression();
//        args = new String[3];
//        args[0] = "/Users/johnlindsay/Documents/Data/LandsatData/band1.dep";
//        args[1] = "/Users/johnlindsay/Documents/Data/LandsatData/band2_cropped.dep";
//        args[2]= "not specified";
//        
//        ir.setArgs(args);
//        ir.run();
//        
//    }
}
