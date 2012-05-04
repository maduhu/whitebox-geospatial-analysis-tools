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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;

import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.text.DecimalFormat;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class KappaIndex implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "KappaIndex";
    }

    @Override
    public String getDescriptiveName() {
        return "Kappa Index of Agreement";
    }

    @Override
    public String getToolDescription() {
        return "Calculates the Kappa index of agreement on two categorical images.";
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
        String outputFile = null;
        int minClass;
        int maxClass;
        int numClasses;
        int i1, i2;
        int[][] contingency;
        double[] data1, data2;
        double z1, z2;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputHeader1 = args[0];
        inputHeader2 = args[1];
        outputFile = args[2];
        
        // check to see that the inputHeader1 and outputFile are not null.
        if (inputHeader1 == null || inputHeader2 == null) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            int row, col;
            float progress = 0;

            WhiteboxRaster image1 = new WhiteboxRaster(inputHeader1, "r");
            int rows = image1.getNumberRows();
            int cols = image1.getNumberColumns();
            double noData1 = image1.getNoDataValue();

            WhiteboxRaster image2 = new WhiteboxRaster(inputHeader2, "r");
            if (rows != image2.getNumberRows() || cols != image2.getNumberColumns()) {
                showFeedback("The input images must have the same dimensions (rows and columns).");
                return;
            }
            double noData2 = image2.getNoDataValue();

            minClass = (int)(Math.min(image1.getMinimumValue(), image2.getMinimumValue()));
            maxClass = (int)(Math.max(image1.getMaximumValue(), image2.getMaximumValue()));
            numClasses = maxClass - minClass + 1;

            contingency = new int[numClasses][numClasses];

            for (row = 0; row < rows; row++) {
                data1 = image1.getRowValues(row);
                data2 = image2.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    z1 = data1[col];
                    z2 = data2[col];
                    if (z1 != noData1 && z2 != noData2) {
                        i1 = (int)(z1 - minClass);
                        i2 = (int)(z2 - minClass);
                        contingency[i1][i2]++;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }
            
            image1.close();
            image2.close();

            double agreements = 0;
            double expectedFrequency = 0;
            double N = 0;
            double rowTotal = 0;
            double colTotal = 0;
            double kappa = 0;
            double overallAccuracy = 0;

            for (int a = 0; a <= numClasses - 1; a++) {
                agreements += contingency[a][a];
                for (int b = 0; b <= numClasses - 1; b++) {
                    N += contingency[a][b];
                }
            }

            for (int a = 0; a <= numClasses - 1; a++) {
                rowTotal = 0;
                colTotal = 0;
                for (int b = 0; b <= numClasses - 1; b++) {
                    colTotal += contingency[a][b];
                    rowTotal += contingency[b][a];
                    }
                    expectedFrequency += (colTotal * rowTotal) / N;
            }

            kappa = (agreements - expectedFrequency) / (N - expectedFrequency);
            overallAccuracy = agreements / N;

            File file = new File(outputFile);
            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
            try {
                fw = new FileWriter(file, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);
                
                String str;

                str = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
                out.println(str);
                str = "<head>";
                out.println(str);
                str = "<meta content=\"text/html; charset=iso-8859-1\" http-equiv=\"content-type\"><title>Kappa Index of Agreement Output</title>";
                out.println(str);
                str = "</head>";
                out.println(str);
                str = "<body><h1>Kappa Index of Agreement</h1>";
                out.println(str);
                str = "<b>Input Images:</b> <br><br><b>Classification Image:</b> " + inputHeader1 + "<br><b>Reference Image:</b> " + inputHeader2 + "<br>";
                out.println(str);
                str = "<br><b>Contingency Table:</b><br>";
                out.println(str);
                str = "<br><table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">";
                out.println(str);
                str = "<tr>";
                out.println(str);
                str = "<th colspan=\"2\" rowspan=\"2\"></th><th colspan=\"" + numClasses + "\">Class Image</th><th rowspan=\"2\">Row<br>Totals</th>";
                out.println(str);
                str = "</tr>";
                out.println(str);
                str = "<tr>";
                out.println(str);
                for (int a = 0; a <= numClasses - 1; a++) {
                    str = "<th>" + (minClass + a) + "</th>";
                    out.println(str);
                
                }
                str = "</tr>";
                out.println(str);
                for (int a = 0; a <= numClasses - 1; a++) {
                    if (a == 0) {
                        str = "<tr><th rowspan=\"" + numClasses + "\">Ref<br>Image</th> <th>" + (minClass + a) + "</th>";
                        out.println(str);
                
                    } else {
                        str = "<tr><th>" + (minClass + a) + "</th>";
                        out.println(str);
                
                    }
                    rowTotal = 0;
                    for (int b = 0; b <= numClasses - 1; b++) {
                        rowTotal += contingency[a][b];
                        str = "<td>" + contingency[a][b] + "</td>";
                        out.println(str);
                
                    }
                    str = "<td>" + rowTotal + "</td>";
                    out.println(str);
                
                    str = "</tr>";
                    out.println(str);
                
                }
                str = "<tr>";
                out.println(str);
                str = "<th colspan=\"2\">Col<br>Totals</th>";
                out.println(str);
                for (int a = 0; a <= numClasses - 1; a++) {
                    colTotal = 0;
                    for (int b = 0; b <= numClasses - 1; b++) {
                        colTotal += contingency[b][a];
                    }
                    str = "<td>" + colTotal + "</td>";
                    out.println(str);
                
                }

                str = "<td><b>N</b>=" + N + "</td></tr>";
                out.println(str);
                str = "</table>";
                out.println(str);
                str = "<br><b>Class Accuracy Statistics:</b><br><br>";
                out.println(str);
                str = "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">";
                out.println(str);
                str = "<tr><td><b>Class</b></td><td><b>User's<br>Accuracy</b></td><td><b>Producer's<br>Accuracy</b></td></tr>";
                out.println(str);
                
                DecimalFormat df = new DecimalFormat("0.00%");
                DecimalFormat df2 = new DecimalFormat("0.000");
                for (int a = 0; a <= numClasses - 1; a++) {
                    rowTotal = 0;
                    colTotal = 0;
                    for (int b = 0; b <= numClasses - 1; b++) {
                        colTotal += contingency[a][b];
                        rowTotal += contingency[b][a];
                    }
                    str = "<tr><td>" + a + "</td><td>" + df.format(contingency[a][a] / colTotal)
                            + "</td><td>" + df.format(contingency[a][a] / rowTotal) + "</td></tr>";
                    out.println(str);
                
                }

                str = "</table>";
                out.println(str);
                str = "<br>Note: User's accuracy refers to the proportion of cells correctly assigned to a class (i.e. the number of cells correctly classified for a category divided by the row total in the contingency table). "
                        + "Producer's accuracy is a measure of how much of the land in each category was classified correctly (i.e. the number of cells correctly classified for a category divided by the column total in the contingency table).<br>";
                out.println(str);
                str = "<br><b>Overall Accuracy</b> = " + df.format(overallAccuracy);
                out.println(str);
                str = "<br><br><b>Kappa</b> = " + df2.format(kappa);
                out.println(str);
                str = "</body>";
                out.println(str);
                
            } catch (java.io.IOException e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                if (out != null || bw != null) {
                    out.flush();
                    out.close();
                }

            }
            returnData(outputFile);

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
