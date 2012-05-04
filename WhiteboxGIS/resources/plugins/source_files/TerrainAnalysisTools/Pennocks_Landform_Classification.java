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
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Pennocks_Landform_Classification implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "Pennocks_Landform_Classification";
    }

    @Override
    public String getDescriptiveName() {
        return "Pennock's Landform Classification";
    }

    @Override
    public String getToolDescription() {
        return "Classifies hillslope zones based on Slope, Profile Curvature, "
                + "and Plan Curvature. Zones are based upon Pennock`s (1987) "
                + "interpretation of Ruhe`s (1960) landform classes. These "
                + "classes are; Level ground, Divergent and Convergent Backslopes, "
                + "Divergent and Convergent Shoulders, and Divergent and "
                + "Convergent Foreslopes.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"LandformClass"};
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
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
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
        int progress = 0;
        double z;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double zFactor = 0;
        double slopeThreshold = 0;
        double profCurvThreshold = 0;
        double planCurvThreshold = 0;
        double radToDeg = 180 / Math.PI;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                zFactor = Double.parseDouble(args[i]);
            } else if (i == 3) {
                slopeThreshold = Double.parseDouble(args[i]);
            } else if (i == 4) {
                profCurvThreshold = Double.parseDouble(args[i]);
            } else if (i == 5) {
                planCurvThreshold = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            double noData = DEM.getNoDataValue();

            double gridResX = DEM.getCellSizeX();
            double gridResY = DEM.getCellSizeY();
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = new double[]{diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY};

            double Zx, Zy, Zxx, Zyy, Zxy, p, Zx2, q, Zy2;
            double fx, fy;
            double gridResTimes2 = gridResX * 2;
            double gridResSquared = gridResX * gridResX;
            double fourTimesGridResSquared = gridResSquared * 4;
            double planCurv, profCurv, slope;
            double eightGridRes = 8 * gridResX;
            double[] N = new double[8];
                   
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    inputHeader, WhiteboxRaster.DataType.FLOAT, -999);
            output.setPreferredPalette("landclass.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = DEM.getValue(row, col);
                    if (z != noData) {
                        z = z * zFactor;
                        //get the neighbouring cell Z values
                        for (c = 0; c < 8; c++) {
                            N[c] = DEM.getValue(row + dY[c], col + dX[c]);
                            if (N[c] != noData) {
                                N[c] = N[c] * zFactor;
                            } else {
                                N[c] = z;
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
                            //eqn for slope
                            fy = (N[6] - N[4] + 2 * (N[7] - N[3]) + N[0] - N[2]) / eightGridRes;
                            fx = (N[2] - N[4] + 2 * (N[1] - N[5]) + N[0] - N[6]) / eightGridRes;
                            slope = Math.atan(Math.sqrt(fx * fx + fy * fy));
                            slope = slope * radToDeg;
                            //Plan curve calc
                            planCurv = -1 * (Zxx * Zy2 - 2 * Zxy * Zx * Zy + Zyy * Zx2) / Math.pow(p, 1.5);
                            planCurv = (planCurv * radToDeg);
                            //Profile curve calc
                            profCurv = -1 * (Zxx * Zx2 + 2 * Zxy * Zx * Zy + Zyy * Zy2) / Math.pow(p * q, 1.5);
                            profCurv = (profCurv * radToDeg);

                            if (profCurv < -profCurvThreshold && planCurv <= -planCurvThreshold & slope > slopeThreshold) {
                                //Convergent Footslope
                                output.setValue(row, col, 1);

                            } else if (profCurv < -profCurvThreshold && planCurv > planCurvThreshold && slope > slopeThreshold) {
                                //Divergent Footslope
                                output.setValue(row, col, 2);

                            } else if (profCurv > profCurvThreshold && planCurv <= planCurvThreshold && slope > slopeThreshold) {
                                //Convergent Shoulder
                                output.setValue(row, col, 3);

                            } else if (profCurv > profCurvThreshold && planCurv > planCurvThreshold && slope > slopeThreshold) {
                                //Divergent Shoulder
                                output.setValue(row, col, 4);

                            } else if (profCurv >= -profCurvThreshold && profCurv < profCurvThreshold && slope > slopeThreshold && planCurv <= -planCurvThreshold) {
                                //Convergent Backslope
                                output.setValue(row, col, 5);

                            } else if (profCurv >= -profCurvThreshold && profCurv < profCurvThreshold && slope > slopeThreshold && planCurv > planCurvThreshold) {
                                //Divergent Backslope
                                output.setValue(row, col, 6);

                            } else if (slope <= slopeThreshold) {
                                //Level
                                output.setValue(row, col, 7);

                            } else {
                                output.setValue(row, col, noData);
                            }
                        } else {
                            output.setValue(row, col, noData);
                        }
                    }
                    
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress(progress);
            }


            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            DEM.close();
            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);
            
            String retstr = "LANDFORM CLASSIFICATION KEY\n";
            retstr += "\nValue:\tClass";
            retstr += "\n1\tConvergent Footslope";
            retstr += "\n2\tDivergent Footslope";
            retstr += "\n3\tConvergent Shoulder";
            retstr += "\n4\tDivergent Shoulder";
            retstr += "\n5\tConvergent Backslope";
            retstr += "\n6\tDivergent Backslope";
            retstr += "\n7\tLevel";
            returnData(retstr);

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