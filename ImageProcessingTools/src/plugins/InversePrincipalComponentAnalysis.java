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
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale;

import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class InversePrincipalComponentAnalysis implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "InversePrincipalComponentAnalysis";
    }

    @Override
    public String getDescriptiveName() {
        return "Inverse Principal Component Analysis";
    }

    @Override
    public String getToolDescription() {
        return "Performs an inverse principal component analysis on PCA components.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"ImageTransformations"};
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

        String inputFilesString = null;
        String[] imageFiles = null;
        String outputName = null;
        String workingDirectory = null;
        WhiteboxRasterInfo[] components = null;
        WhiteboxRaster ouptut = null;
        int nCols = 0;
        int nRows = 0;
        double z;
        int numImages = 0;
        int numFiles, numComponents;
        int progress = 0;
        int col, row;
        int a, i, j;
        double[] imageAverages;
        double[] imageTotals;
        double[] imageNumPixels;
        double[][] data;
        double[] noDataValues;
        String pathSep = File.separator;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        // read the input parameters
        inputFilesString = args[0];
        outputName = args[1];
        if (outputName.toLowerCase().contains(".dep")) {
            outputName = outputName.replace(".dep", "");
        }
        
        
        try {
            // deal with the input images
            imageFiles = inputFilesString.split(";");
            numFiles = imageFiles.length;
            components = new WhiteboxRasterInfo[numFiles];
            
            imageAverages = new double[numFiles];
            imageTotals = new double[numFiles];
            imageNumPixels = new double[numFiles];
            noDataValues = new double[numFiles];
            data = new double[numFiles][];
            
            int[] componentNumbers = new int[numFiles];
            
            double[][] eigenvectors = null;
            
            //ArrayList<double[]> eigenvectors = new ArrayList<double[]>();
            
            for (i = 0; i < numFiles; i++) {
                boolean isComponent = false;
                components[i] = new WhiteboxRasterInfo(imageFiles[i]);
                noDataValues[i] = components[i].getNoDataValue();
                if (i == 0) {
                    nCols = components[i].getNumberColumns();
                    nRows = components[i].getNumberRows();
                    File file = new File(imageFiles[i]);
                    workingDirectory = file.getParent();
                } else {
                    if (components[i].getNumberColumns() != nCols
                            || components[i].getNumberRows() != nRows) {
                        showFeedback("All input components must have the same dimensions (rows and columns).");
                        return;
                    }
                }
                ArrayList<String> metadata = components[i].getMetadata();
                for (String entry: metadata) {
                    if (entry.toLowerCase().contains("principal component num")) {
                        componentNumbers[i] = Integer.parseInt(entry.replace("Principal Component Num.:", "").trim()) - 1; // zero based
                    }
                    if (entry.toLowerCase().contains("eigenvector")) {
                        String[] eigenStr = entry.replace("Eigenvector:", "").
                                replace("[", "").replace("]", "").split(",");
                        if (i == 0) {
                            numImages = eigenStr.length;
                            eigenvectors = new double[numImages][numImages];
                        }
                        for (a = 0; a < numImages; a++) {
                            eigenvectors[componentNumbers[i]][a] = Double.parseDouble(eigenStr[a].trim());
                        }
                        isComponent = true;
                    }
                }
                if (!isComponent) {
                    showFeedback("At least one of the input images does not appear to have been created by the PrincipalComponentAnalysis tool. " + 
                            "This tool will not operate correctly.");
                    return;
                }
            }
            
            int[] componentPointers = new int[numImages];
            for (i = 0; i < numFiles; i++) {
                componentPointers[componentNumbers[i]] = i;
            }
            
            for (j = 0; j < numImages; j++) {
                    // now set up the output image
                    String outputHeader = workingDirectory + pathSep + outputName + (j + 1) + ".dep";
                    if (new File(outputHeader).exists()) {
                        (new File(outputHeader)).delete();
                    }
                    if (new File(outputHeader.replace(".dep", ".tas")).exists()) {
                        (new File(outputHeader.replace(".dep", ".tas"))).delete();
                    }
                    WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                            imageFiles[0], WhiteboxRaster.DataType.FLOAT, 0);
                    output.setDataScale(DataScale.CONTINUOUS);

                    for (row = 0; row < nRows; row++) {
                        for (i = 0; i < numFiles; i++) {
                            data[i] = components[i].getRowValues(row);
                        }
                        for (col = 0; col < nCols; col++) {
                            if (data[0][col] != noDataValues[0]) {
                                z = 0;
                                for (i = 0; i < numFiles; i++) {
                                    z += data[i][col] * eigenvectors[componentPointers[i]][j];
                                }
                                output.setValue(row, col, z);
                            } else {
                                output.setValue(row, col, noDataValues[0]);
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (nRows - 1));
                        updateProgress("Creating image " + (j + 1) + ":", progress);
                    }

                    output.addMetadataEntry("Created by the "
                            + getDescriptiveName() + " tool.");
                    output.addMetadataEntry("Created on " + new Date());
                    output.close();
            }
            
            for (i = 0; i < numFiles; i++) {
                components[i].close();
            }
            
            String outputHeader = workingDirectory + pathSep + outputName + "1.dep";
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
    
    public static class PrincipalComponent implements Comparable<PrincipalComponent> {

        public double eigenValue;
        public double[] eigenVector;

        public PrincipalComponent(double eigenValue, double[] eigenVector) {
            this.eigenValue = eigenValue;
            this.eigenVector = eigenVector;
        }

        @Override
        public int compareTo(PrincipalComponent o) {
            int ret = 0;
            if (eigenValue > o.eigenValue) {
                ret = -1;
            } else if (eigenValue < o.eigenValue) {
                ret = 1;
            }
            return ret;
        }

        @Override
        public String toString() {
            String ret = "Principle Component, eigenvalue: " + eigenValue + ", eigenvector: [";
            for (int i = 0; i < eigenVector.length; i++) {
                ret += eigenVector[i] + ", ";
            }
            ret += "]";
            return ret;
        }
    }
    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        InversePrincipalComponentAnalysis ipca = new InversePrincipalComponentAnalysis();
//        args = new String[2];
//        args[0] = "/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp1.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp3.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp2.dep";
//        //args[0] = "/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp1.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp3.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp2.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp4.dep;/Users/johnlindsay/Documents/Data/LandsatData/PCA_comp5.dep";
//        args[1] = "invPCA_band";
//        
//        ipca.setArgs(args);
//        ipca.run();
//        
//    }
}
