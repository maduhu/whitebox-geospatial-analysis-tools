package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import java.util.ArrayList;

/**
 *
 * @author johnlindsay
 */
public class WeightedOverlay implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "WeightedOverlay";
    }

    @Override
    public String getDescriptiveName() {
    	return "Weighted Overlay (MCE)";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a weighted sum on multiple input raster images after "
                + "converting each image to a common scale.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "OverlayTools" };
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
        
        String outputHeader = null;
        WhiteboxRaster image = null;
        WhiteboxRaster output = null;
        int row, col;
        int nRows, nCols;
        double z;
        int progress = 0;
        int i, a;
        double noData = -32768;
        double outputNoData = -32768;
        String inputDataString = null;
        int numImages = 0;
        double sumOfWeights = 0;
        double d = 0;
        double weight = 0;
        double scaleMin = 0;
        double scaleMax = 1;
        double imageMin = 0;
        double imageMax = 1;
        double imageRange = 1;
        double[] data = null;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        String[] dataSplit = args[0].split(";");
        String[] constraints = args[1].split(";");
        outputHeader = args[2];
        String commonScale = args[3];
        if (commonScale.equals("0-1")){
            scaleMin = 0;
            scaleMax = 1;
        } else if (commonScale.equals("0-100")){
            scaleMin = 0;
            scaleMax = 100;
        } else if (commonScale.equals("0-255")){
            scaleMin = 0;
            scaleMax = 255;
        }
                        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputDataString == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            // first deal with the factors.
            
            ArrayList<Boolean> costFactor = new ArrayList<Boolean>();
            ArrayList<String> imageFiles = new ArrayList<String>();
            ArrayList<Double> weights = new ArrayList<Double>();
            for (a = 0; a < dataSplit.length; a += 3) {
                if (!dataSplit[a + 1].trim().equals("")
                        && !dataSplit[a].trim().toLowerCase().equals("not specified")) {
                    costFactor.add(Boolean.parseBoolean(dataSplit[a]));
                    imageFiles.add(dataSplit[a + 1].trim());
                    weights.add(Double.parseDouble(dataSplit[a + 2]));
                    sumOfWeights += weights.get(numImages);
                    numImages++;
                }
            }
            
            if (numImages < 2) {
                showFeedback("At least two factor images must be specified.");
                return;
            }

            for (a = 0; a < numImages; a++) {
                d = weights.get(a) / sumOfWeights;
                weights.set(a, d);
            }
            
            image = new WhiteboxRaster(imageFiles.get(0), "r");
            nRows = image.getNumberRows();
            nCols = image.getNumberColumns();
            outputNoData = image.getNoDataValue();
            
            output = new WhiteboxRaster(outputHeader, "rw", imageFiles.get(0), 
                        WhiteboxRaster.DataType.FLOAT, 0);
            
            for (i = 0; i < numImages; i++) {
                progress = (int)(100f * (i + 1) / numImages);
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                weight = weights.get(i);
                if (i > 0) {
                    image = new WhiteboxRaster(imageFiles.get(i), "r");
                    noData = image.getNoDataValue();
                    if (image.getNumberRows() != nRows || image.getNumberColumns() != nCols) {
                        showFeedback("All input images must have the same dimensions (rows and columns).");
                        return;
                    }
                }    
                imageMin = image.getMinimumValue();
                imageMax = image.getMaximumValue();
                imageRange = imageMax - imageMin;
                Boolean boolCost = costFactor.get(i);
                
                for (row = 0; row < nRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < nCols; col++) {
                        if (data[col] != noData) {
                            z = output.getValue(row, col);
                            if (z != outputNoData) {
                                if (!boolCost) {
                                    d = (data[col] - imageMin) / imageRange * scaleMax;
                                    output.setValue(row, col, z + d * weight);
                                } else {
                                    d = (1 - (data[col] - imageMin) / imageRange) * scaleMax;
                                    output.setValue(row, col, z + d * weight);
                                }
                            }
                        } else {
                            output.setValue(row, col, outputNoData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
                
                image.close();
                
            }
            
            // now deal with the constraints, if there are any.
            for (i = 0; i < constraints.length; i++) {
                image = new WhiteboxRaster(constraints[i].trim(), "r");
                noData = image.getNoDataValue();
                if (image.getNumberRows() != nRows || image.getNumberColumns() != nCols) {
                    showFeedback("All input images must have the same dimensions (rows and columns).");
                    return;
                }
                
                for (row = 0; row < nRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < nCols; col++) {
                        if (data[col] != noData && data[col] <= 0) {
                            if (output.getValue(row, col) != outputNoData) {
                                output.setValue(row, col, scaleMin);
                            }
                        } else {
                            output.setValue(row, col, outputNoData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
                
                image.close();
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