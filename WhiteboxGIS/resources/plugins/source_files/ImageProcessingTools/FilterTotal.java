package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class FilterTotal implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FilterTotal";
    }

    @Override
    public String getDescriptiveName() {
    	return "Total Filter";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a total filter on an image.";
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
        double total;
        float progress = 0;
        int a;
        int filterSizeX = 3;
        int filterSizeY = 3;
        int dX[];
        int dY[];
        int midPointX;
        int midPointY;
        int numPixelsInFilter;
        boolean filterRounded = false;
        double[] filterShape;
        boolean reflectAtBorders = false;
    
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
                filterSizeX = Integer.parseInt(args[i]);
            } else if (i == 3) {
                filterSizeY = Integer.parseInt(args[i]);
            } else if (i == 4) {
                filterRounded = Boolean.parseBoolean(args[i]);
            } else if (i == 5) {
                reflectAtBorders = Boolean.parseBoolean(args[i]);
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
            outputFile.setPreferredPalette(inputFile.getPreferredPalette());
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (Math.floor(filterSizeX / 2d) == (filterSizeX / 2d)) {
                showFeedback("Filter dimensions must be odd numbers. The specified filter x-dimension" + 
                        " has been modified.");
                
                filterSizeX++;
            }
            if (Math.floor(filterSizeY / 2d) == (filterSizeY / 2d)) {
                showFeedback("Filter dimensions must be odd numbers. The specified filter y-dimension" + 
                        " has been modified.");
                filterSizeY++;
            }

            numPixelsInFilter = filterSizeX * filterSizeY;
            dX = new int[numPixelsInFilter];
            dY = new int[numPixelsInFilter];
            filterShape = new double[numPixelsInFilter];

            //fill the filter DX and DY values
            midPointX = (int)Math.floor(filterSizeX / 2);
            midPointY = (int)Math.floor(filterSizeY / 2);
            if (!filterRounded) {
                a = 0;
                for (row = 0; row < filterSizeY; row++) {
                    for (col = 0; col < filterSizeX; col++) {
                        dX[a] = col - midPointX;
                        dY[a] = row - midPointY;
                        filterShape[a] = 1;
                        a++;
                     }
                }
            } else {
                //see which pixels in the filter lie within the largest ellipse 
                //that fits in the filter box 
                double aSqr = midPointX * midPointX;
                double bSqr = midPointY * midPointY;
                a = 0;
                for (row = 0; row < filterSizeY; row++) {
                    for (col = 0; col < filterSizeX; col++) {
                        dX[a] = col - midPointX;
                        dY[a] = row - midPointY;
                        z = (dX[a] * dX[a]) / aSqr + (dY[a] * dY[a]) / bSqr;
                        if (z > 1) {
                            filterShape[a] = 0;
                        } else {
                            filterShape[a] = 1;
                        }
                        a++;
                    }
                }
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        total = 0;
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                            z = inputFile.getValue(y, x);
                            if (z != noData && filterShape[a] == 1) {
                                total += z;
                            }
                        }
                        
                        outputFile.setValue(row,col, total);       
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