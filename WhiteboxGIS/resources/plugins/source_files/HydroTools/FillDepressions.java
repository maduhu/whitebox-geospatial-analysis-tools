package plugins;

import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import java.util.Date;
import java.util.PriorityQueue;
/**
 *
 * @author johnlindsay
 */
public class FillDepressions implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FillDepressions";
    }

    @Override
    public String getDescriptiveName() {
    	return "Fill Depressions";
    }

    @Override
    public String getToolDescription() {
    	return "This tool fills all of the depressions in a DEM using the Wang and Liu (2006) algorithm.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "DEMPreprocessing" };
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
        //Date strtime = new Date();

        String inputHeader = null;
        String outputHeader = null;
    	double SMALL_NUM = 0.0001d;
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
                    SMALL_NUM = Double.parseDouble(args[i]);
                }
    	}

        // check to see that the inputHeader and outputHeader are not null.
       if ((inputHeader == null) || (outputHeader == null)) {
           showFeedback("One or more of the input parameters have not been set properly.");
           return;
       }

        try {
            int row_n, col_n;
            int row, col;
            double z_n;
            long k = 0;
            GridCell gc = null;
            double z;
            int[] Dy = {-1, 0, 1, 1, 1, 0, -1, -1};
            int[] Dx = {1, 1, 1, 0, -1, -1, -1, 0};
            float progress = 0;
            boolean flag = false;
            
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int rows = image.getNumberRows();
            int cols = image.getNumberColumns();
            int numCells = 0;
            String preferredPalette = image.getPreferredPalette();
            
            double noData = image.getNoDataValue();

            double[][] output = new double[rows][cols];
            double[][] input = new double[rows + 2][cols + 2];
            for (row = 0; row < rows + 2; row++) {
                input[row][0] = noData;
                input[row][cols + 1] = noData;
            }
            
            for (col = 0; col < cols + 2; col++) {
                input[0][col] = noData;
                input[rows + 1][col] = noData;
            }
            
            double[] data;
            for (row = 0; row < rows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    output[row][col] = -999;
                    input[row + 1][col + 1] = data[col];
                }
            }
            image.close();
            data = new double[0];
            // initialize and fill the priority queue.
            updateProgress("Loop 1: ", 0);

            PriorityQueue<GridCell> queue = new PriorityQueue<GridCell>((2 * rows + 2 * cols) * 2);

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = input[row + 1][col + 1]; //me.getValue(row, col);
                    if (z != noData) {
                        numCells++;
                        flag = false;
                        for (int i = 0; i < 8; i++) {
                            row_n = row + Dy[i];
                            col_n = col + Dx[i];
                            z_n = input[row_n + 1][col_n + 1]; //me.getValue(row_n, col_n);
                            if (z_n == noData) {
                                // it's an edge cell.
                                flag = true;
                            }
                        }
                        if (flag) {
                            gc = new GridCell(row, col, z);
                            queue.add(gc);
                            //outputFile.setValue(row, col, z);
                            output[row][col] = z;
                        }
                    } else {
                        k++;
                        //outputFile.setValue(row, col, noData);
                        output[row][col] = noData;
                    }

                }
                progress = (float)(100f * row / rows);
                if (cancelOp) { cancelOperation(); return; }
                updateProgress("Loop 1: ", (int)progress);
            }

            // now fill!
            updateProgress("Loop 2: ", 0);

            double reportedProgress = 1;
            do {
                gc = queue.poll();
                row = gc.row;
                col = gc.col;
                z = gc.z;
                for (int i = 0; i < 8; i++) {
                    row_n = row + Dy[i];
                    col_n = col + Dx[i];
                    z_n = input[row_n + 1][col_n + 1]; //me.getValue(row_n, col_n);
                    //if ((z_n != noData) && (outputFile.getValue(row_n, col_n) == -999)) {
                    if ((z_n != noData) && (output[row_n][col_n] == -999)) {
                        if (z_n <= z) {
                            z_n = z + SMALL_NUM;
                        }
                        //outputFile.setValue(row_n, col_n, z_n);
                        output[row_n][col_n] = z_n;
                        gc = new GridCell(row_n, col_n, z_n);
                        queue.add(gc);
                    }
                }
                k++;
                progress = (float)(k * 100f / numCells);
                if (progress >= reportedProgress) {
                    if (cancelOp) { cancelOperation(); return; }
                    updateProgress("Loop 2: ", (int)progress);
                    reportedProgress++;
                }
            } while (queue.isEmpty() == false);

            
            updateProgress("Saving Data: ", 0);
            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.DOUBLE, -999);
            outputFile.setPreferredPalette(preferredPalette);
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    outputFile.setValue(row, col, output[row][col]);
                }
                progress = (float)(100f * row / rows);
                if (cancelOp) { cancelOperation(); return; }
                updateProgress("Saving Data: ", (int)progress);
            }
            
            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());

            //me.close();
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
    
    
    class GridCell implements Comparable<GridCell> {

        public int row;
        public int col;
        public double z;

        public GridCell(int Row, int Col, double Z) {
            row = Row;
            col = Col;
            z = Z;
        }

        @Override
        public int compareTo(GridCell cell) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (this.z < cell.z) {
                return BEFORE;
            } else if (this.z > cell.z) {
                return AFTER;
            }

            if (this.row < cell.row) {
                return BEFORE;
            } else if (this.row > cell.row) {
                return AFTER;
            }

            if (this.col < cell.col) {
                return BEFORE;
            } else if (this.col > cell.col) {
                return AFTER;
            }

            return EQUAL;
        }
    }
}
