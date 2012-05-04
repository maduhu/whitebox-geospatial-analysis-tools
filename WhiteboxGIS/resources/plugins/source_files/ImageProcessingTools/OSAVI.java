package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class OSAVI implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "OSAVI";
    }

    @Override
    public String getDescriptiveName() {
    	return "Optimised Soil-Adjusted Vegetation Index (OSAVI)";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the OSAVI from near-infrared and red imagery.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "VegetationIndices" };
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
        
        String NIRHeader = null;
        String RedHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        double[] NIRVal;
        double[] redVal;
        float progress = 0;
        int a;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                NIRHeader = args[i];
            } else if (i == 1) {
                RedHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((NIRHeader == null) || (RedHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster NIR = new WhiteboxRaster(NIRHeader, "r");
            
            int rows = NIR.getNumberRows();
            int cols = NIR.getNumberColumns();
            double noData = NIR.getNoDataValue();

            WhiteboxRaster red = new WhiteboxRaster(RedHeader, "r");
            
            if (rows != red.getNumberRows() || cols != red.getNumberColumns()) {
                showFeedback("The two input images must have the same number of rows and columns.");
                return;
            }
            
            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", 
                    NIRHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette(NIR.getPreferredPalette());
            
            for (row = 0; row < rows; row++) {
                NIRVal = NIR.getRowValues(row);
                redVal = red.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (NIRVal[col] != noData && redVal[col] != noData) {
                        if ((NIRVal[col] + redVal[col]) != 0) {
                            outputFile.setValue(row, col, 
                                (NIRVal[col] - redVal[col]) / (NIRVal[col] + redVal[col] + 0.16)); 
                        } else {
                            outputFile.setValue(row, col, noData);
                        }
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
            
            NIR.close();
            red.close();
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