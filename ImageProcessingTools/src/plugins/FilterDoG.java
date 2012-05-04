package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class FilterDoG implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FilterDoG";
    }

    @Override
    public String getDescriptiveName() {
    	return "Difference-of-Gaussian Filter";
    }

    @Override
    public String getToolDescription() {
    	return "Performs a Difference-of-Gaussian filter on an image.";
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
        int filterSize1 = 3;
        int filterSize2 = 3;
        double n;
        double sum;
        int[] dX1;
        int[] dX2;
        int[] dY1;
        int[] dY2;
        double[] weights1;
        double[] weights2;
        int midPoint;
        int numPixelsInFilter1;
        int numPixelsInFilter2;
        boolean reflectAtBorders = false;
        double sigma1 = 0;
        double sigma2 = 0;
        double recipRoot2PiTimesSigma1;
        double recipRoot2PiTimesSigma2;
        double twoSigmaSqr1;
        double twoSigmaSqr2;
        double zN, zFinal_1, zFinal_2;
    
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
                sigma1 = Double.parseDouble(args[i]);
            } else if (i == 3) {
                sigma2 = Double.parseDouble(args[i]);
            } else if (i == 4) {
                reflectAtBorders = Boolean.parseBoolean(args[i]);
            }
        }

        if (sigma1 < 0.5) {
            sigma1 = 0.5;
        } else if (sigma1 > 20) {
            sigma1 = 20;
        }
        
        if (sigma2 < 0.5) {
            sigma2 = 0.5;
        } else if (sigma2 > 20) {
            sigma2 = 20;
        }
        
        if (sigma1 == sigma2) {
            showFeedback("The two standard deviations cannot be equal.");
            return;
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
            
            recipRoot2PiTimesSigma1 = 1 / (Math.sqrt(2 * Math.PI) * sigma1);
            twoSigmaSqr1 = 2 * sigma1 * sigma1;

            recipRoot2PiTimesSigma2 = 1 / (Math.sqrt(2 * Math.PI) * sigma2);
            twoSigmaSqr2 = 2 * sigma2 * sigma2;

            //figure out the size of the filter
            double weight;
            for (int i = 0; i <= 250; i++) {
                weight = recipRoot2PiTimesSigma1 * Math.exp(-1 * (i * i) / twoSigmaSqr1);
                if (weight <= 0.001) {
                    filterSize1 = i * 2 + 1;
                    break;
                }
            }
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (filterSize1 % 2 == 0) {
                filterSize1++;
            }

            if (filterSize1 < 3) { filterSize1 = 3; }

            numPixelsInFilter1 = filterSize1 * filterSize1;
            dX1 = new int[numPixelsInFilter1];
            dY1 = new int[numPixelsInFilter1];
            weights1 = new double[numPixelsInFilter1];
            
            //fill the filter DX and DY values and the distance-weights
            midPoint = (int)Math.floor(filterSize1 / 2) + 1;
            a = 0;
            for (row = 0; row < filterSize1; row++) {
                for (col = 0; col < filterSize1; col++) {
                    x = col - midPoint;
                    y = row - midPoint;
                    dX1[a] = x;
                    dY1[a] = y;
                    weight = recipRoot2PiTimesSigma1 * Math.exp(-1 * (x * x + y * y) / twoSigmaSqr1);
                    weights1[a] = weight;
                    a++;
                }
            }
            
            
            //figure out the size of the filter
            for (int i = 0; i <= 250; i++) {
                weight = recipRoot2PiTimesSigma2 * Math.exp(-1 * (i * i) / twoSigmaSqr2);
                if (weight <= 0.001) {
                    filterSize2 = i * 2 + 1;
                    break;
                }
            }
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (filterSize2 % 2 == 0) {
                filterSize2++;
            }

            if (filterSize2 < 3) { filterSize2 = 3; }

            numPixelsInFilter2 = filterSize2 * filterSize2;
            dX2 = new int[numPixelsInFilter2];
            dY2 = new int[numPixelsInFilter2];
            weights2 = new double[numPixelsInFilter2];
            
            //fill the filter DX and DY values and the distance-weights
            midPoint = (int)Math.floor(filterSize2 / 2) + 1;
            a = 0;
            for (row = 0; row < filterSize2; row++) {
                for (col = 0; col < filterSize2; col++) {
                    x = col - midPoint;
                    y = row - midPoint;
                    dX2[a] = x;
                    dY2[a] = y;
                    weight = recipRoot2PiTimesSigma2 * Math.exp(-1 * (x * x + y * y) / twoSigmaSqr2);
                    weights2[a] = weight;
                    a++;
                }
            }
            
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        sum = 0;
                        zFinal_1 = 0;
                        for (a = 0; a < numPixelsInFilter1; a++) {
                            x = col + dX1[a];
                            y = row + dY1[a];
                            zN = inputFile.getValue(y, x);
                            if (zN != noData) {
                                sum += weights1[a];
                                zFinal_1 += weights1[a] * zN;
                            }
                        }
                        zFinal_1 = zFinal_1 / sum;
                        
                        sum = 0;
                        zFinal_2 = 0;
                        for (a = 0; a < numPixelsInFilter2; a++) {
                            x = col + dX2[a];
                            y = row + dY2[a];
                            zN = inputFile.getValue(y, x);
                            if (zN != noData) {
                                sum += weights2[a];
                                zFinal_2 += weights2[a] * zN;
                            }
                        }
                        zFinal_2 = zFinal_2 / sum;
                        
                        outputFile.setValue(row, col, zFinal_1 - zFinal_2);
                        
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