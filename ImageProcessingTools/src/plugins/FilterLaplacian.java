package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class FilterLaplacian implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FilterLaplacian";
    }

    @Override
    public String getDescriptiveName() {
    	return "Laplacian Filter";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a Laplacian filter on an image.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "Filters" };
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
        int row, col, x, y;
        double z;
        float progress = 0;
        int a;
        double sum;
        int[] dX;
        int[] dY;
        double[] weights;
        int numPixelsInFilter;
        boolean reflectAtBorders = true;
        double centreValue;
        String filterSize = "3 x 3 (1)";
    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                filterSize = args[i].toLowerCase();
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster inputFile = new WhiteboxRaster(inputHeader, "r");
            inputFile.isReflectedAtEdges = reflectAtBorders;

            int rows = inputFile.getNumberRows();
            int cols = inputFile.getNumberColumns();
            double noData = inputFile.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette("grey.pal");
            
            if (filterSize.equals("3 x 3 (1)")) {
                weights = new double[]{0, -1, 0, -1, 4, -1, 0, -1, 0};
                dX = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
                dY = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
            } else if (filterSize.equals("3 x 3 (2)")) {
                weights = new double[]{0, -1, 0, -1, 5, -1, 0, -1, 0};
                dX = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
                dY = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
            } else if (filterSize.equals("3 x 3 (3)")) {
                weights = new double[]{-1, -1, -1, -1, 8, -1, -1, -1, -1};
                dX = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
                dY = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
            } else if (filterSize.equals("3 x 3 (4)")) {
                weights = new double[]{1, -2, 1, -2, 4, -2, 1, -2, 1};
                dX = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
                dY = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
            } else if (filterSize.equals("5 x 5 (1)")) {
                weights = new double[]{0, 0, -1, 0, 0, 0, -1, -2, -1, 0, -1, -2, 
                    17, -2, -1, 0, -1, -2, -1, 0, 0, 0, -1, 0, 0};
                dX = new int[]{-2, -1, 0, 1, 2, -2, -1, 0, 1, 2, -2, -1, 0, 1, 
                    2, -2, -1, 0, 1, 2, -2, -1, 0, 1, 2};
                dY = new int[]{-2, -2, -2, -2, -2, -1, -1, -1, -1, -1, 0, 0, 0, 
                    0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2};
            } else { // 5 x 5 (2)
                weights = new double[]{0, 0, -1, 0, 0, 0, -1, -2, -1, 0, -1, -2, 
                    16, -2, -1, 0, -1, -2, -1, 0, 0, 0, -1, 0, 0};
                dX = new int[]{-2, -1, 0, 1, 2, -2, -1, 0, 1, 2, -2, -1, 0, 1, 
                    2, -2, -1, 0, 1, 2, -2, -1, 0, 1, 2};
                dY = new int[]{-2, -2, -2, -2, -2, -1, -1, -1, -1, -1, 0, 0, 0, 
                    0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2};
            }

            numPixelsInFilter = dX.length;
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    centreValue = inputFile.getValue(row, col);
                    if (centreValue != noData) {
                        sum = 0;
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                            z = inputFile.getValue(y, x);
                            if (z == noData) { z = centreValue; }
                            sum += z * weights[a];
                        }
                        outputFile.setValue(row, col, sum);

                    } else {
                        outputFile.setValue(row, col, noData);
                    }

                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());
            
            inputFile.close();
            outputFile.close();
            

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