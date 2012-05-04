package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class FilterGaussian implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FilterGaussian";
    }

    @Override
    public String getDescriptiveName() {
    	return "Gaussian Filter";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a Gaussian filter on an image.";
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
        int filterSize = 3;
        double n;
        double sum;
        int[] dX;
        int[] dY;
        double[] weights;
        int midPoint;
        int numPixelsInFilter;
        boolean reflectAtBorders = false;
        double sigmaD = 0;
        double recipRoot2PiTimesSigmaD;
        double twoSigmaSqrD;
        double zN, zFinal;
    
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
                sigmaD = Double.parseDouble(args[i]);
            } else if (i == 3) {
                reflectAtBorders = Boolean.parseBoolean(args[i]);
            }
        }

        if (sigmaD < 0.5) {
            sigmaD = 0.5;
        } else if (sigmaD > 20) {
            sigmaD = 20;
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
            outputFile.setPreferredPalette(inputFile.getPreferredPalette());
            
            
            recipRoot2PiTimesSigmaD = 1 / (Math.sqrt(2 * Math.PI) * sigmaD);
            twoSigmaSqrD = 2 * sigmaD * sigmaD;

            //figure out the size of the filter
            double weight;
            for (int i = 0; i <= 250; i++) {
                weight = recipRoot2PiTimesSigmaD * Math.exp(-1 * (i * i) / twoSigmaSqrD);
                if (weight <= 0.001) {
                    filterSize = i * 2 + 1;
                    break;
                }
            }
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (filterSize % 2 == 0) {
                filterSize++;
            }

            if (filterSize < 3) { filterSize = 3; }

            numPixelsInFilter = filterSize * filterSize;
            dX = new int[numPixelsInFilter];
            dY = new int[numPixelsInFilter];
            weights = new double[numPixelsInFilter];
            
            //fill the filter DX and DY values and the distance-weights
            midPoint = (int)Math.floor(filterSize / 2) + 1;
            a = 0;
            for (row = 0; row < filterSize; row++) {
                for (col = 0; col < filterSize; col++) {
                    x = col - midPoint;
                    y = row - midPoint;
                    dX[a] = x;
                    dY[a] = y;
                    weight = recipRoot2PiTimesSigmaD * Math.exp(-1 * (x * x + y * y) / twoSigmaSqrD);
                    weights[a] = weight;
                    a++;
                }
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        sum = 0;
                        zFinal = 0;
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                            zN = inputFile.getValue(y, x);
                            if (zN != noData) {
                                sum += weights[a];
                                zFinal += weights[a] * zN;
                            }
                        }
                        outputFile.setValue(row, col, zFinal / sum);
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