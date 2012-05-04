package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class RemoveSpurs implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "RemoveSpurs";
    }

    @Override
    public String getDescriptiveName() {
    	return "Remove Spurs (prunning)";
    }

    @Override
    public String getToolDescription() {
    	return "Removes the spurs (prunning operation) from a Boolean image.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "MathematicalMorphology" };
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
        double z;
        int progress = 0;
        int i, a;
        long counter = 0;
        int loopNum = 0;
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[][] elements = { {0, 1, 4, 5, 6, 7}, {0, 1, 2, 5, 6, 7}, 
            {0, 1, 2, 3, 6, 7}, {0, 1, 2, 3, 4, 7}, 
            {0, 1, 2, 3, 4, 5}, {1, 2, 3, 4, 5, 6}, 
            {2, 3, 4, 5, 6, 7}, {0, 3, 4, 5, 6, 7} };
        double[] neighbours = new double[8];
        boolean patternMatch = false;
        int numIterations = 10;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        outputHeader = args[1];
        numIterations = Integer.parseInt(args[2]);
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int nRows = image.getNumberRows();
            int nCols = image.getNumberColumns();
            double noData = image.getNoDataValue();

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("black_white.pal");
            
            // copy the input image into the output.
            double[] data = null;
            for (row = 0; row < nRows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < nCols; col++) {
                    if (data[col] > 0) {
                        output.setValue(row, col, 1);
                    } else if (data[col] == noData) {
                        output.setValue(row, col, noData);
                    } else {
                        output.setValue(row, col, 0);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (nRows - 1));
                updateProgress(progress);
            }
            
            image.close();
            output.flush();
            
            for (int k = 0; k < numIterations; k++) {
                loopNum++;
                updateProgress("Loop Number " + loopNum + ":", 0);
                counter = 0;
                for (row = 0; row < nRows; row++) {
                    for (col = 0; col < nCols; col++) {
                        z = output.getValue(row, col);
                        if (z == 1 && z != noData) {
                            // fill the neighbours array
                            for (i = 0; i < 8; i++) {
                                neighbours[i] = output.getValue(row + dY[i], col + dX[i]);
                            }
                            
                            for (a = 0; a < 8; a++) {
                                // scan through element
                                patternMatch = true;
                                for (i = 0; i < elements[a].length; i++) {
                                    if (neighbours[elements[a][i]] != 0) {
                                        patternMatch = false;
                                    }
                                }
                                if (patternMatch) {
                                    output.setValue(row, col, 0);
                                    counter++;
                                }
                            }
                        }

                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (nRows - 1));
                    updateProgress(progress);
                }
                if (counter == 0) { break; }
            }


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