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
import java.util.List;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.structures.KdTree;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class FillMissingDataHoles implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FillMissingDataHoles";
    }

    @Override
    public String getDescriptiveName() {
    	return "Fill Missing Data Holes";
    }

    @Override
    public String getToolDescription() {
    	return "Fills NoData holes in an image or DEM by linear interpolation.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ImageEnhancement", "TerrainAnalysis", "LidarTools" };
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
        int row, col;
        int progress = 0;
        double z, val;
        int i;
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        outputHeader = args[1];
        

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            image.isReflectedAtEdges = true;
            int rows = image.getNumberRows();
            int cols = image.getNumberColumns();
            double noData = image.getNoDataValue();
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            
            // Find all cells that border with no-data cells. 
            int k = 0;
            boolean neighboursNoData = false;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z != noData) {
                        neighboursNoData = false;
                        for (i = 0; i < 8; i++) {
                            if (image.getValue(row + dY[i], col + dX[i]) == noData) {
                                neighboursNoData = true;
                                break;
                            }
                        }
                        if (neighboursNoData) {
                            k++;
                            //double[] entry = { row, col };
                            //tree.addPoint(entry, z);
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 3: ", progress);
            }
            
            KdTree<Double> tree = new KdTree.SqrEuclid<Double>(2, k);
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z != noData) {
                        neighboursNoData = false;
                        for (i = 0; i < 8; i++) {
                            if (image.getValue(row + dY[i], col + dX[i]) == noData) {
                                neighboursNoData = true;
                                break;
                            }
                        }
                        if (neighboursNoData) {
                            double[] entry = { row, col };
                            tree.addPoint(entry, z);
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 3: ", progress);
            }

            List<KdTree.Entry<Double>> results;
            //double[] entry;
            double sumWeights;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z == noData) {
                        double[] entry = { row, col };
                        results = tree.nearestNeighbor(entry, 6, true);
                        sumWeights = 0;
                        for (i = 0; i < results.size(); i++) {
                            sumWeights += 1 / (results.get(i).distance);
                        }
                        val = 0;
                        for (i = 0; i < results.size(); i++) {
                            val += (1 / (results.get(i).distance)) / sumWeights * results.get(i).value;
                        }
                        output.setValue(row, col, val);
                    } else {
                        output.setValue(row, col, z);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 3 of 3: ", progress);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
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