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
import whitebox.geospatialfiles.WhiteboxRasterBase;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.utilities.BitOps;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class BranchLength implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "BranchLength";
    }

    @Override
    public String getDescriptiveName() {
        return "Branch Length";
    }

    @Override
    public String getToolDescription() {
        return "Calculates the branch length for each grid cell in a D8 flow direction (Pointer) grid.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"FlowpathTAs"};
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

        int row, col, x, y, x1, x2, y1, y2;
        int progress;
        double z;
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int maxFlowpathLength;
        double[][] flowpath1;
        double[][] flowpath2;
        double dist;
        double gridResX, gridResY, diagGridRes;
        double[] gridRes;
        final double lnOf2 = Math.log(2); //0.693147180559945;
        byte baseTestByte;
        byte testByte;
        int curPosFlowpath1, curPosFlowpath2;
        boolean flag1, flag2;
        int flowDir;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        // input parameters
        String pntrHeaderFile = args[0];
        String outputHeader = args[1];
        //String branchLengthType = args[2].toLowerCase();
        //boolean includeDiagBln = Boolean.parseBoolean(args[3]);

        //if (includeDiagBln) {
            //0 0 0
            //0   1
            //1 1 1
            baseTestByte = 30;
        //} else {
            //0 0 0
            //0   1
            //0 1 0
        //    baseTestByte = 10;
        //}

        // check to see that the inputHeader and outputHeader are not null.
        if ((pntrHeaderFile == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster pntr1 = new WhiteboxRaster(pntrHeaderFile, "r");
            int rows = pntr1.getNumberRows();
            int rowsLessOne = rows - 1;
            int cols = pntr1.getNumberColumns();
            int colsLessOne = cols - 1;
            double noData = pntr1.getNoDataValue();
            gridResX = pntr1.getCellSizeX();
            gridResY = pntr1.getCellSizeY();
            diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            gridRes = new double[]{diagGridRes, gridResX, diagGridRes, gridResY,
                diagGridRes, gridResX, diagGridRes, gridResY};

            maxFlowpathLength = (int) (2 * Math.sqrt(cols * cols + rows * rows));

            WhiteboxRaster pntr2 = new WhiteboxRaster(pntrHeaderFile, "r");
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    pntrHeaderFile, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("grey.pal");
            output.setDataScale(WhiteboxRasterBase.DataScale.CONTINUOUS);

            // Perform the Branch Length calculation
            updateProgress("Loop 1 of 2:", 1);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = pntr1.getValue(row, col);
                    if (z != noData) {
                        // test byte serves as a means of telling the 
                        // filter which neighbouring cells to evaluate Bmax for
                        testByte = baseTestByte;
                        if (row == rowsLessOne) {
                            //0 0 0
                            //0   1
                            //X X X
                            testByte = 2;
                        }
                        if (col == 0) {
                            //0 0 0
                            //0   1
                            //X 1 1
                            testByte = BitOps.clearBit(testByte, (byte) 4);
                        }
                        if (col == colsLessOne) {
                            //0 0 0
                            //0   X
                            //1 1 X
                            testByte = BitOps.clearBit(testByte, (byte) 1);
                            testByte = BitOps.clearBit(testByte, (byte) 2);
                        }
                        for (int c = 1; c < 5; c++) {
                            //see if it's already been tested
                            if (BitOps.checkBit(testByte, (byte) c)) {
                                //set the appropriate bits in this cells bit field high, as well as the pair's bit field
                                x = col + dX[c];
                                y = row + dY[c];

                                //traverse the flowpath initiating from a,b and from the neighbour in tandom and find the confluence cell, if any
                                flowpath1 = new double[3][maxFlowpathLength]; //first col is cell column, second is cell row, third is cumulative flowpath distance
                                flowpath2 = new double[3][maxFlowpathLength]; //first col is cell column, second is cell row, third is cumulative flowpath distance
                                x1 = col;
                                y1 = row;
                                x2 = col + dX[c];
                                y2 = row + dY[c];
                                curPosFlowpath1 = 0;
                                curPosFlowpath2 = 0;

                                flowpath1[0][curPosFlowpath1] = x1;
                                flowpath1[1][curPosFlowpath1] = y1;
                                flowpath1[2][curPosFlowpath1] = 0;
                                flowpath2[0][curPosFlowpath1] = x2;
                                flowpath2[1][curPosFlowpath1] = y2;
                                flowpath2[2][curPosFlowpath1] = 0;

                                flag1 = true;
                                flag2 = true;
                                do {
                                    //Flowpath 1
                                    if (flag1) {
                                        flowDir = (int) pntr1.getValue(y1, x1);
                                        if (flowDir > 0) {
                                            curPosFlowpath1++;

                                            //Convert the base 2 flow direction into a 0-7 flow direction
                                            flowDir = (int) (Math.log(flowDir) / lnOf2);

                                            dist = gridRes[flowDir];

                                            //move x,y to the cell that flowdir is pointing to
                                            x1 += dX[flowDir];
                                            y1 += dY[flowDir];

                                            //update the flowpath info
                                            flowpath1[0][curPosFlowpath1] = x1;
                                            flowpath1[1][curPosFlowpath1] = y1;
                                            flowpath1[2][curPosFlowpath1] = flowpath1[2][curPosFlowpath1 - 1] + dist;

                                            // see if this new point intersects with flowpath2 anywhere
                                            for (int d = curPosFlowpath2; d >= 0; d--) {
                                                if (flowpath2[0][d] == x1 && flowpath2[1][d] == y1) {
                                                    // A confluence has been found!
                                                    dist = flowpath1[2][curPosFlowpath1];
                                                    if (output.getValue(row, col) < dist) {
                                                        output.setValue(row, col, dist);
                                                    }
                                                    dist = flowpath2[2][d];
                                                    if (output.getValue(y, x) < dist) {
                                                        output.setValue(y, x, dist);
                                                    }
                                                    flag1 = false;
                                                    flag2 = false;
                                                    break;
                                                }
                                            }
                                        } else {
                                            flag1 = false;
                                            //either the edge of the grid or 
                                            //a pit has been encountered before 
                                            //the confluence 
                                            if (!flag2) {
                                                dist = flowpath1[2][curPosFlowpath1];
                                                if (output.getValue(row, col) < dist) {
                                                    output.setValue(row, col, dist);
                                                }
                                                dist = flowpath2[2][curPosFlowpath2];
                                                if (output.getValue(y, x) < dist) {
                                                    output.setValue(y, x, dist);
                                                }
//                                                        if (bBlnOutputEC Then
//                                                            OutputEC(a, b) = 1
//                                                            OutputEC(X, Y) = 1
//                                                        End If
                                                break;
                                            }
                                        }
                                    }

                                    //Flowpath 2
                                    if (flag2) {
                                        flowDir = (int) pntr2.getValue(y2, x2);
                                        if (flowDir > 0) {
                                            curPosFlowpath2 += 1;

                                            //Convert the base 2 flow direction into a 0-7 flow direction
                                            flowDir = (int) (Math.log(flowDir) / lnOf2);

                                            dist = gridRes[flowDir];

                                            //move x,y to the cell that flowdir is pointing to
                                            x2 += dX[flowDir];
                                            y2 += dY[flowDir];

                                            //update the flowpath info
                                            flowpath2[0][curPosFlowpath2] = x2;
                                            flowpath2[1][curPosFlowpath2] = y2;
                                            flowpath2[2][curPosFlowpath2] = flowpath2[2][curPosFlowpath2 - 1] + dist;

                                            //See if this new point intersects with flowpath1 anywhere
                                            for (int d = curPosFlowpath1; d >= 0; d--) {
                                                if (flowpath1[0][d] == x2 && flowpath1[1][d] == y2) {
                                                    //A confluence has been found!
                                                    dist = flowpath2[2][curPosFlowpath2];
                                                    if (output.getValue(y, x) < dist) {
                                                        output.setValue(y, x, dist);
                                                    }
                                                    dist = flowpath1[2][d];
                                                    if (output.getValue(row, col) < dist) {
                                                        output.setValue(row, col, dist);
                                                    }
                                                    flag1 = false;
                                                    flag2 = false;
                                                    break;
                                                }
                                            }
                                        } else {
                                            flag2 = false;
                                            //either the edge of the grid or a pit 
                                            //has been encountered before the confluence 
                                            if (!flag1) {
                                                dist = flowpath1[2][curPosFlowpath1];
                                                if (output.getValue(row, col) < dist) {
                                                    output.setValue(row, col, dist);
                                                }
                                                dist = flowpath2[2][curPosFlowpath2];
                                                if (output.getValue(y, x) < dist) {
                                                    output.setValue(y, x, dist);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } while (flag1 || flag2);
                            }
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", progress);
            }

            updateProgress("Loop 2 of 2:", 1);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (output.getValue(row, col) != noData) {
                        output.setValue(row, col, output.getValue(row, col) / 1000);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 2:", progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            pntr1.close();
            pntr2.close();
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

//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        BranchLength bl = new BranchLength();
//        args = new String[4];
//        //args[0] = "/Users/johnlindsay/Documents/Data/DEM filled flowDir.dep";
////        args[0] = "/Users/johnlindsay/Documents/Data/Rondeau D8 direction.dep";
////        args[1] = "/Users/johnlindsay/Documents/Data/Bmax.dep";
//        args[0] = "/Users/johnlindsay/Documents/tmp3.dep";
//        args[1] = "/Users/johnlindsay/Documents/Bmax.dep";
//        args[2] = "maximum";
//        args[3] = "true";
//
//        bl.setArgs(args);
//        bl.run();
//
//    }
}