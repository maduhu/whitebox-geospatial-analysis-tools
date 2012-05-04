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
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class TestForNormality implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "TestForNormality";
    }

    @Override
    public String getDescriptiveName() {
    	return "KS Test For Normality";
    }

    @Override
    public String getToolDescription() {
    	return "Evaluates whether the values in a raster are normally distributed.";
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
        boolean useSampleBool = false;
        int sampleSize = 0;
    	
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        if (args[1].toLowerCase().equals("not specified")) {
            useSampleBool = false;
            sampleSize = 0;
        } else {
            useSampleBool = true;
            sampleSize = Integer.parseInt(args[1]);
        }

        // check to see that the inputHeader and outputHeader are not null.
       if (inputHeader == null) {
           showFeedback("One or more of the input parameters have not been set properly.");
           return;
       }

        try {
            int row, col;
            double z;
            float progress = 0;
            
            WhiteboxRaster inputFile = new WhiteboxRaster(inputHeader, "r");
            
            int rows = inputFile.getNumberRows();
            int cols = inputFile.getNumberColumns();
            
            double noData = inputFile.getNoDataValue();

            int numBins = 10000;
            double minValue = inputFile.getMinimumValue();
            double maxValue = inputFile.getMaximumValue();
            double binSize = (maxValue - minValue) / numBins;
            long[] histogram = new long[numBins];
            int binNum;
            int numBinsLessOne = numBins - 1;
            double[] data;
            double total, mean, stdDev, N, totalDeviation, Dmax;
            total = 0;
            N = 0;
            totalDeviation = 0;
            updateProgress("Calculating CDF:", 0);
            
            if (!useSampleBool) {
                for (row = 0; row < rows; row++) {
                    data = inputFile.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        z = data[col];
                        if (z != noData) {
                            binNum = (int) ((z - minValue) / binSize);
                            if (binNum > numBinsLessOne) {
                                binNum = numBinsLessOne;
                            }
                            histogram[binNum]++;
                            total += z;
                            N++;
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress("Calculating CDF:", (int) progress);
                }

                mean = total / N;

                for (row = 0; row < rows; row++) {
                    data = inputFile.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        z = data[col];
                        if (z != noData) {
                            totalDeviation += (z - mean)
                                    * (z - mean);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress("Calculating CDF:", (int) progress);
                }
            } else {
                double[] sample = new double[sampleSize];
                minValue = Float.POSITIVE_INFINITY;
                maxValue = Float.NEGATIVE_INFINITY;
                Random generator = new Random();
                int[][] rowsAndColumns = new int[sampleSize][2];
                int sampleNumber = 0;
                while (sampleNumber < sampleSize) {
                    rowsAndColumns[sampleNumber][0] = generator.nextInt(rows);
                    rowsAndColumns[sampleNumber][1] = generator.nextInt(cols);
                    sampleNumber++;
//                    z = inputFile.getValue(row, col);
//                    if (z != noData) {
//                        sample[sampleNumber] = z;
//                        total += z;
//                        N++;
//                        if (z < minValue) { minValue = z; }
//                        if (z > maxValue) { maxValue = z; }
//                        sampleNumber++;
//                    }
//                    progress = (float) (100f * sampleNumber / (sampleSize - 1));
//                    updateProgress("Calculating CPD:", (int) progress);
                }
                
                Arrays.sort(rowsAndColumns, new Comparator<int[]>() {

                    @Override
                    public int compare(final int[] entry1, final int[] entry2) {
                        final int int1 = entry1[0];
                        final int int2 = entry2[0];
                        return Integer.valueOf(int1).compareTo(int2);
                    }
                });

                for (int i = 0; i < sampleSize; i++) {
                    row = rowsAndColumns[i][0];
                    col = rowsAndColumns[i][1];
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        sample[i] = z;
                        total += z;
                        N++;
                        if (z < minValue) { minValue = z; }
                        if (z > maxValue) { maxValue = z; }
                    }
                    progress = (float) (100f * sampleNumber / (sampleSize - 1));
                    updateProgress("Calculating CDF:", (int) progress);
                }
                mean = total / N;
                binSize = (maxValue - minValue) / numBins;
                
                for (int i = 0; i < sampleSize; i++) {
                    totalDeviation += (sample[i] - mean) * (sample[i] - mean);
                    binNum = (int) ((sample[i] - minValue) / binSize);
                    if (binNum > (numBins - 1)) { binNum = numBins - 1; }
                    histogram[binNum]++;
                }
            }

            stdDev = Math.sqrt(totalDeviation / (N - 1));

            double[] cdf = new double[numBins];
            cdf[0] = histogram[0]; 
            for (int i = 1; i < numBins; i++) {
                cdf[i] = cdf[i - 1] + histogram[i];
            }
            histogram = null;
            for (int i = 0; i < numBins; i++) {
                cdf[i] = cdf[i] / N;
            }
            
            double[] normalDist = new double[numBins];
            double SDroot2PI = stdDev * Math.sqrt(2 * Math.PI);
            double twoSDsqr = 2 * stdDev * stdDev;
            for (int i = 0; i < numBins; i++) {
                z = minValue + i * binSize;
                normalDist[i] = 1 / SDroot2PI * Math.exp((-(z - mean) * (z - mean)) / twoSDsqr);
            }
            for (int i = 1; i < numBins; i++) {
                normalDist[i] = normalDist[i - 1] + normalDist[i];
            }
            
            for (int i = 0; i < numBins; i++) {
                normalDist[i] = normalDist[i] / normalDist[numBins - 1];
            }
            
            // calculate the critical statistic, Dmax
            Dmax = 0;
            for (int i = 0; i < numBins; i++) {
                z = Math.abs(cdf[i] - normalDist[i]);
                if (z > Dmax) {
                    Dmax = z;
                }
            }
            
            // calculate p-value
            double s = N * Dmax * Dmax;
            double pValue = 2 * Math.exp(-(2.000071 + 0.331 / Math.sqrt(N) + 1.409 / N) * s);
            
            DecimalFormat df;
            df = new DecimalFormat("0.000");
            
            String retstr = null;
            retstr = "Kolmogorov–Smirnov (K-S) Test for Normality:\n\n";
            retstr = retstr + "Input image:\t\t" + inputFile.getShortHeaderFile() + "\n";
            retstr = retstr + "Sample Size (N):\t" + N + "\n";
            retstr = retstr + "Test Statistic (Dmax):\t" + df.format(Dmax) + "\n";
            if (pValue > 0.001) {
                retstr = retstr + "Significance (p-value):\t" + df.format(pValue) + "\n";
            } else {
                retstr = retstr + "Significance (p-value):\t<0.001\n\n";
            }
            String result;
            if (pValue < 0.05) {
                result = "The test rejects the null hypothesis that the values come from a normal distribution.\n";
            } else {
                result = "The test fails to reject the null hypothesis that the values come from a normal distribution.\n";
            }
            String caveat = "Caveat: Given a sufficiently large sample, extremely small and non-notable differences can be found to be statistically significant, \nand statistical significance says nothing about the practical significance of a difference.\n";
            
            retstr += result + caveat;
            
            returnData(retstr);
            
            

            inputFile.close();
            
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
