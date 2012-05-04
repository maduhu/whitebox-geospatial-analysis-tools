package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author johnlindsay
 */
public class TangentialCurv implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "TangentialCurv";
    }

    @Override
    public String getDescriptiveName() {
    	return "Tangential Curvature";
    }

    @Override
    public String getToolDescription() {
    	return "Calculates tangential curvature, in degrees per 100 horizontal units, for each grid cell in an input digital elevation model (DEM).";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "SurfDerivatives" };
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
        double zConvFactor = 1;
    	
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
                zConvFactor = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            int row, col;
            double z;
            double[] N = new double[8];
            float slope;
            float progress = 0;
            int[] Dy = {-1, 0, 1, 1, 1, 0, -1, -1};
            int[] Dx = {1, 1, 1, 0, -1, -1, -1, 0};
            final double radToDeg = 180 / Math.PI;
            double Zx, Zy, Zxx, Zyy, Zxy, Zx2, Zy2, p, q;
                        
            WhiteboxRaster inputFile = new WhiteboxRaster(inputHeader, "r");
            inputFile.isReflectedAtEdges = true;

            int rows = inputFile.getNumberRows();
            int cols = inputFile.getNumberColumns();
            double gridRes = inputFile.getCellSizeX();
            double gridResTimes2 = gridRes * 2;
            double eightGridRes = 8 * gridRes;
            double gridResSquared = gridRes * gridRes;
            double fourTimesGridResSquared = gridResSquared * 4;
            double curv;
            double noData = inputFile.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette("blue_white_red.pal");

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        for (int i = 0; i < 8; i++) {
                            N[i] = inputFile.getValue(row + Dy[i], col + Dx[i]);
                            if (N[i] != noData) {
                                N[i] = N[i] * zConvFactor;
                            } else {
                                N[i] = z * zConvFactor;
                            }
                        }
                        //calculate each of the terms
                        Zx = (N[1] - N[5]) / gridResTimes2;
                        Zy = (N[7] - N[3]) / gridResTimes2;
                        Zxx = (N[1] - 2 * z + N[5]) / gridResSquared;
                        Zyy = (N[7] - 2 * z + N[3]) / gridResSquared;
                        Zxy = (-N[6] + N[0] + N[4] - N[2]) / fourTimesGridResSquared;
                        Zx2 = Zx * Zx;
                        Zy2 = Zy * Zy;
                        p = Zx2 + Zy2;
                        q = p + 1;
                        if (p > 0) {
                            curv = (Zxx * Zy2 - 2 * Zxy * Zx * Zy + Zyy * Zx2) /( p * Math.pow(q, 1.5));
                            outputFile.setValue(row, col, curv * radToDeg * 100);
                        } else {
                            outputFile.setValue(row,col, noData);
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
            
            //outputFile.setDisplayMaximum(5.0);
            //outputFile.setDisplayMinimum(-5.0);
            

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
