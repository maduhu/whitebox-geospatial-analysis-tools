package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class DifferenceFromMeanElevation implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "DifferenceFromMeanElevation";
    }

    @Override
    public String getDescriptiveName() {
    	return "Difference From Mean Elevation";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates the difference between the elevation in a grid cell "
                + "and the mean elevation of its neighbourhood.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "ElevResiduals" };
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
        int progress = 0;
        int a;
        int filterSize = 3;
        double n;
        double sum;
        double average;
        int dX[];
        int dY[];
        int midPoint;
        int numPixelsInFilter;
        boolean filterRounded = true;
        double[] filterShape;
        boolean reflectAtBorders = true;
        double centreValue = 0;
        double neighbourhoodDist = 0;
    
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
                neighbourhoodDist = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            DEM.isReflectedAtEdges = reflectAtBorders;

            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            double noData = DEM.getNoDataValue();

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("grey.pal");
            
            filterSize = (int)(neighbourhoodDist / ((DEM.getCellSizeX() + DEM.getCellSizeY()) / 2));
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (Math.floor(filterSize / 2d) == (filterSize / 2d)) {
                filterSize++;
            }
            
            numPixelsInFilter = filterSize * filterSize;
            dX = new int[numPixelsInFilter];
            dY = new int[numPixelsInFilter];
            filterShape = new double[numPixelsInFilter];

            //fill the filter DX and DY values
            midPoint = (int)Math.floor(filterSize / 2);
            //see which pixels in the filter lie within the largest ellipse 
            //that fits in the filter box 
            double aSqr = midPoint * midPoint;
            double bSqr = midPoint * midPoint;
            a = 0;
            for (row = 0; row < filterSize; row++) {
                for (col = 0; col < filterSize; col++) {
                    dX[a] = col - midPoint;
                    dY[a] = row - midPoint;
                    z = (dX[a] * dX[a]) / aSqr + (dY[a] * dY[a]) / bSqr;
                    if (z > 1) {
                        filterShape[a] = 0;
                    } else {
                        filterShape[a] = 1;
                    }
                    a++;
                }
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    centreValue = DEM.getValue(row, col);
                    if (centreValue != noData) {
                        n = 0;
                        sum = 0;
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                            if ((x != midPoint) && (y != midPoint)) {
                                z = DEM.getValue(y, x);
                                if (z != noData) {
                                    n += filterShape[a];
                                    sum += z * filterShape[a];
                                }
                            }
                        }
                        
                        z = centreValue - sum / n;
                        output.setValue(row, col, z);
                    } else {
                        output.setValue(row, col, noData);
                    }

                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            DEM.close();
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