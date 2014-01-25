/*
 *  Copyright (C) 2014 Ehsan Roshani
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.stats;

import Jama.*;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterBase;
import whitebox.geospatialfiles.shapefile.MultiPoint;
import whitebox.geospatialfiles.shapefile.MultiPointM;
import whitebox.geospatialfiles.shapefile.MultiPointZ;
import whitebox.geospatialfiles.shapefile.PointM;
import whitebox.geospatialfiles.shapefile.PointZ;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTZ;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTZ;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.structures.KdTree;
import java.util.Random;
import jmetal.util.JMException;
import whitebox.geospatialfiles.shapefile.attributes.DBFWriter;

/**
 *
 * @author Ehsan Roshani, Ph.D. Department of Geography University of Guelph
 * Guelph, Ont. N1G 2W1 CANADA Phone: (519) 824-4120 x53527 Email:
 * eroshani@uoguelph.ca
 *
 * modified by John Lindsay
 */
public class Kriging {

    public boolean Anisotropic;
    public double BandWidth;
    public double Angle;
    public double Tolerance;

    public double resolution;
    //public double 

    public double bMinX;    //Bounding box Minimum X
    public double bMinY;
    public double bMaxX;
    public double bMaxY;

    private PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public void SetBoundary(double MinimumX, double MaximumX, double MinimumY, double MaximumY) {
        bMinX = MinimumX;
        bMinY = MinimumY;
        bMaxX = MaximumX;
        bMaxY = MaximumY;
    }
    public double MinX;     //Minimum X Coordinat in the Points 
    public double MinY;     //Minimum Y Coordinat in the Points
    public double MaxX;     //Maximum X Coordinat in the Points
    public double MaxY;     //Maximum Y Coordinat in the Points
    public int NumberOfLags;
    public double LagSize;
    //public double 
    public KdTree<Double> pointsTree;      //This is the point tree which will be filled in the calcPair method
    public Matrix DistanceMatrix;   //The matrix that contains the distance of each known point to all other known points
    public int nKown;               //Number of known points
    //public double[][] Points;       //Array of points location x=0, y = 1, z = 2
    public double MaximumDistance;
    public bin[][] BinSurface;       //n*3 matrix to store all the bins
//    public class point
//    {
//        public double x;
//        public double y;
//        public double z;
//    }
//    public point point(double x , double y, double z){
//        point p = new point();
//        p.x = x;
//        p.y = y;
//        p.z = z;
//        return p;
//    }
    public List<KrigingPoint> Points = new ArrayList();

    public class pair {

        int FirstP;
        int SecondP;
        double Distance;
        double Direction;
        double MomentI;             //Moment of Inertia
        double VerDistance;         //Vertical Distance (Y Axes)
        double HorDistance;         //Horizontal Distance (X Axes)
    }

    //List<bin> bins = new ArrayList();
    public bin[][] bins; // = new bin[]      

    public class bin {

        double GridHorDistance;
        double GridVerDistance;
        double HorDistance;
        double VerDistance;
        double Distance;
        double Value;
        double Weight;
        int Size;
    }
    public List<pair> Pairs = new ArrayList();

    private KdTree<Double> PairsTree;

    public SemivariogramType SemiVariogramModel;

    public enum SemivariogramType {

        Spherical, Exponential, Gaussian
    }
    public double Range;        //SemiVariogram a value
    public double Sill;        //SemiVariogram h value
    public double Nugget;       //Semivariogram Nugget value
    public boolean ConsiderNugget;  //If nugget should be considered or not
    public SemivariogramType SVType;

    private int nthSVariogram;    //this is the nth SV for Anisotropic

    private double[] x;// This is the x value for fitting the theoritical SV

    public class Variogram {

        public double Range;
        public double Sill;
        public double Nugget;
        public SemivariogramType Type;
        public double mse;
    }

    public void DrawShapeFile(String outputFile, List<KrigingPoint> pnts) throws DBFException, IOException {

        File file = new File(outputFile);
        if (file.exists()) {
            file.delete();
        }

        // set up the output files of the shapefile and the dbf
        ShapeFile output = new ShapeFile(outputFile, ShapeType.POINT);

        DBFField fields[] = new DBFField[2];

        fields[0] = new DBFField();
        fields[0].setName("FID");
        fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
        fields[0].setFieldLength(10);
        fields[0].setDecimalCount(0);

        fields[1] = new DBFField();
        fields[1].setName("Z");
        fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
        fields[1].setFieldLength(10);
        fields[1].setDecimalCount(3);

        String DBFName = output.getDatabaseFile();

        DBFWriter writer = new DBFWriter(new File(DBFName));
        writer.setFields(fields);
        int numPointsInFile = pnts.size();
        double x, y, z;
        for (int a = 0; a < numPointsInFile; a++) {
            x = pnts.get(a).x;
            y = pnts.get(a).y;
            z = pnts.get(a).z;

            whitebox.geospatialfiles.shapefile.Point wbGeometry = new whitebox.geospatialfiles.shapefile.Point(x, y);
            output.addRecord(wbGeometry);

            Object[] rowData = new Object[2];
            rowData[0] = new Double(a + 1);
            rowData[1] = new Double(z);
            writer.addRecord(rowData);
        }
        output.write();
        writer.write();
    }

    /**
     * Calculates the bin values based on sector classification and for
     * isotropic model
     *
     * @param Range
     */
    void CalcBins4Sec(double Range) {
        int ad = 0;
        if (Range % this.LagSize == 0) {
            ad = 0;
        }

        if (!this.Anisotropic) {
            bins = new bin[(int) Math.ceil(Range / this.LagSize) + ad][1];
            int r = 0;
            for (int i = 0; i < Pairs.size(); i++) {
                if (Pairs.get(i).Distance < Range && Pairs.get(i).HorDistance >= 0) {
                    r = (int) Math.floor(Pairs.get(i).Distance / LagSize);
                    if (bins[r][0] == null) {
                        bin bb = new bin();
                        bins[r][0] = bb;
                    }

                    bins[r][0].Distance += Pairs.get(i).Distance;
                    bins[r][0].Value += Pairs.get(i).MomentI;
                    bins[r][0].Size++;
                }
            }
            for (int i = 0; i < bins.length; i++) {
                if (bins[i][0] == null) {
                    bin bb = new bin();
                    bins[i][0] = bb;
                }
                bins[i][0].Distance = bins[i][0].Distance / bins[i][0].Size;
                bins[i][0].Value = bins[i][0].Value / bins[i][0].Size;
            }
        }
        //==========================

    }

    /**
     * Calculates the bin values based on sector classification and for
     * AnIsotropic model
     *
     * @param Range
     * @param Angle
     * @param Tolerance
     * @param BandWidth
     */
    void CalcBins4Sec(double Range, double Angle, double Tolerance, double BandWidth) {
        int ad = 0;
        if (Range % this.LagSize == 0) {
            ad = 0;
        }
        double width = 0;

        if (this.Anisotropic) {

            bins = new bin[(int) Math.ceil(Range / this.LagSize) + ad][1];
            int r = 0;
            for (int i = 0; i < Pairs.size(); i++) {
                boolean tt = Between(Angle, Tolerance, Pairs.get(i).Direction);
                width = Pairs.get(i).Distance * Math.cos((Math.PI / 2) - Angle + Pairs.get(i).Direction);
                if (tt && Pairs.get(i).Distance < Range && Math.abs(width) <= BandWidth) {

                    r = (int) Math.floor(Pairs.get(i).Distance / LagSize);
                    if (bins[r][0] == null) {
                        bin bb = new bin();
                        bins[r][0] = bb;
                    }

                    bins[r][0].Distance += Pairs.get(i).Distance;
                    bins[r][0].Value += Pairs.get(i).MomentI;
                    bins[r][0].Size++;
                }
            }
            for (int i = 0; i < bins.length; i++) {
                if (bins[i][0] == null) {
                    bin bb = new bin();
                    bins[i][0] = bb;
                }
                bins[i][0].Distance = bins[i][0].Distance / bins[i][0].Size;
                bins[i][0].Value = bins[i][0].Value / bins[i][0].Size;
            }
        }
        //==========================

    }

    /**
     * Checks to see if a pair is located in a sector of interest or not
     *
     * @param Angle
     * @param Tolerance
     * @param Direction
     * @return
     */
    private boolean Between(double Angle, double Tolerance, double Direction) {

        boolean flag = false;
        double la = (Angle - Tolerance);
        if (la < 0) {
            la = 2 * Math.PI + la;
            flag = true;
        }

        double ha = (Angle + Tolerance);
        if (ha >= 2 * Math.PI) {
            ha = ha - 2 * Math.PI;
            flag = true;
        }

        if (flag) {
            if (Direction >= ha && Direction <= la) {
                return false;
            } else {
                return true;
            }
        } else {
            if (Direction >= la && Direction <= ha) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Calculates the Bin list for SV Map
     *
     * @param Range
     */
    void CalcBins4Map(double Range) {

        //bins = new bin[this.NumberOfLags][this.NumberOfLags];
        //bins Category on the axies 
        //2 . 1
        //3   4         //Only 1 and 4 are calculated the rest are mirror
        int ad = 0;
        if (Range % this.LagSize == 0) {
            ad = 0;
        }
        bin[][] bins1 = new bin[(int) Math.ceil(Range / this.LagSize) + ad][(int) Math.ceil(Range / this.LagSize + ad)];
        bin[][] bins4 = new bin[(int) Math.ceil(Range / this.LagSize) + ad][(int) Math.ceil(Range / this.LagSize + ad)];

        bin[][] bins1c = new bin[(int) Math.ceil(Range / this.LagSize) + ad][(int) Math.ceil(Range / this.LagSize + ad)];
        bin[][] bins4c = new bin[(int) Math.ceil(Range / this.LagSize) + ad][(int) Math.ceil(Range / this.LagSize + ad)];

        BinSurface = new bin[2 * ((int) Math.ceil(Range / this.LagSize) + ad)][2 * ((int) Math.ceil(Range / this.LagSize + ad))];
        //double radious =Math.sqrt(2*this.LagSize*this.LagSize);
        double radious = this.LagSize * 2 / Math.sqrt(2);
        double halfLagSize = this.LagSize;
        List<pair> prs = new ArrayList();
        double w = 0;
        for (int r = 0; r < bins1.length; r++) {
            for (int c = 0; c < bins1[r].length; c++) {

                if (bins1[r][c] == null) {
                    bin bb = new bin();
                    bin bbc = new bin();

                    bins1[r][c] = bb;
                    bins1c[r][c] = bbc;
                }

                bins1[r][c].GridHorDistance = 0.5 * this.LagSize + c * this.LagSize;
                bins1[r][c].GridVerDistance = 0.5 * this.LagSize + r * this.LagSize;

                bins1c[r][c].GridHorDistance = -0.5 * this.LagSize - c * this.LagSize;
                bins1c[r][c].GridVerDistance = -0.5 * this.LagSize - r * this.LagSize;

                double[] center = new double[]{bins1[r][c].GridVerDistance, bins1[r][c].GridHorDistance};
                prs = getBinNNPairs4Map(PairsTree, center, halfLagSize, radious);

                for (int n = 0; n < prs.size(); n++) {
                    bins1[r][c].HorDistance += prs.get(n).HorDistance;
                    bins1[r][c].VerDistance += prs.get(n).VerDistance;
                    w = (1 - (Math.abs(bins1[r][c].GridHorDistance - prs.get(n).HorDistance) / this.LagSize))
                            * (1 - (Math.abs(bins1[r][c].GridVerDistance - prs.get(n).VerDistance) / this.LagSize));

                    bins1[r][c].Weight += w;
                    bins1[r][c].Value += prs.get(n).MomentI * w;
                    bins1[r][c].Size += 1;

                    bins1c[r][c].HorDistance += prs.get(n).HorDistance;
                    bins1c[r][c].VerDistance += prs.get(n).VerDistance;
                    bins1c[r][c].Weight += w;

                    bins1c[r][c].Value += prs.get(n).MomentI * w;
                    bins1c[r][c].Size += 1;

                }
            }
        }

        for (int i = 0; i < bins1.length; i++) {
            for (int j = 0; j < bins1[i].length; j++) {
                if (bins1[i][j] == null) {
                    bin bb = new bin();
                    bins1[i][j] = bb;
                    bins1[i][j].HorDistance = i * this.LagSize;
                    bins1[i][j].VerDistance = j * this.LagSize;
                    bins1[i][j].Value = -1;

                    bin bbc = new bin();
                    bins1c[i][j] = bbc;
                    bins1c[i][j].HorDistance = -i * this.LagSize;
                    bins1c[i][j].VerDistance = -j * this.LagSize;
                    bins1c[i][j].Value = -1;
                } else {
                    bins1[i][j].HorDistance = bins1[i][j].HorDistance / bins1[i][j].Size;
                    bins1[i][j].VerDistance = bins1[i][j].VerDistance / bins1[i][j].Size;
                    bins1[i][j].Value = bins1[i][j].Value / bins1[i][j].Weight;

                    bins1c[i][j].HorDistance = bins1c[i][j].HorDistance / bins1c[i][j].Size;
                    bins1c[i][j].VerDistance = bins1c[i][j].VerDistance / bins1c[i][j].Size;
                    bins1c[i][j].Value = bins1c[i][j].Value / bins1c[i][j].Weight;
                }

                //System.out.println( (0.5*this.LagSize + j*this.LagSize) + " , " + (0.5*this.LagSize+i*this.LagSize) + " , " +
                //        Bins1[i][j].HorDistance + " , " + Bins1[i][j].VerDistance + " , " + Bins1[i][j].Value);
            }
        }
        //==========================

        for (int r = 0; r < bins4.length; r++) {
            for (int c = 0; c < bins4[r].length; c++) {

                if (bins4[r][c] == null) {
                    bin bb = new bin();
                    bin bbc = new bin();

                    bins4[r][c] = bb;
                    bins4c[r][c] = bbc;
                }

                bins4[r][c].GridHorDistance = 0.5 * this.LagSize + c * this.LagSize;
                bins4[r][c].GridVerDistance = -0.5 * this.LagSize - r * this.LagSize;

                bins4c[r][c].GridHorDistance = -0.5 * this.LagSize - c * this.LagSize;
                bins4c[r][c].GridVerDistance = 0.5 * this.LagSize + r * this.LagSize;

                double[] center = new double[]{bins4[r][c].GridVerDistance, bins4[r][c].GridHorDistance};
                prs = getBinNNPairs4Map(PairsTree, center, halfLagSize, radious);

                for (int n = 0; n < prs.size(); n++) {
                    bins4[r][c].HorDistance += prs.get(n).HorDistance;
                    bins4[r][c].VerDistance += prs.get(n).VerDistance;
                    w = (1 - (Math.abs(bins4[r][c].GridHorDistance - prs.get(n).HorDistance) / this.LagSize))
                            * (1 - (Math.abs(bins4[r][c].GridVerDistance - prs.get(n).VerDistance) / this.LagSize));

                    bins4[r][c].Weight += w;
                    bins4[r][c].Value += prs.get(n).MomentI * w;
                    bins4[r][c].Size += 1;

                    bins4c[r][c].HorDistance += prs.get(n).HorDistance;
                    bins4c[r][c].VerDistance += prs.get(n).VerDistance;
                    bins4c[r][c].Weight += w;

                    bins4c[r][c].Value += prs.get(n).MomentI * w;
                    bins4c[r][c].Size += 1;

                }
            }
        }

        for (int i = 0; i < bins4.length; i++) {
            for (int j = 0; j < bins4[i].length; j++) {
                if (bins4[i][j] == null) {
                    bin bb = new bin();
                    bins4[i][j] = bb;
                    bins4[i][j].HorDistance = i * this.LagSize;
                    bins4[i][j].VerDistance = j * this.LagSize;
                    bins4[i][j].Value = -1;

                    bin bbc = new bin();
                    bins4c[i][j] = bbc;
                    bins4c[i][j].HorDistance = -i * this.LagSize;
                    bins4c[i][j].VerDistance = -j * this.LagSize;
                    bins4c[i][j].Value = -1;
                } else {
                    bins4[i][j].HorDistance = bins4[i][j].HorDistance / bins4[i][j].Size;
                    bins4[i][j].VerDistance = bins4[i][j].VerDistance / bins4[i][j].Size;
                    bins4[i][j].Value = bins4[i][j].Value / bins4[i][j].Weight;

                    bins4c[i][j].HorDistance = bins4c[i][j].HorDistance / bins4c[i][j].Size;
                    bins4c[i][j].VerDistance = bins4c[i][j].VerDistance / bins4c[i][j].Size;
                    bins4c[i][j].Value = bins4c[i][j].Value / bins4c[i][j].Weight;
                }

                //System.out.println( (0.5*this.LagSize + j*this.LagSize) + " , " + (0.5*this.LagSize+i*this.LagSize) + " , " +
                //        bins1[i][j].HorDistance + " , " + bins1[i][j].VerDistance + " , " + bins1[i][j].Value);
            }
        }

        int stI = BinSurface.length / 2;
        int stJ = BinSurface[0].length / 2;

        //bins1c = bins1.clone();
        for (int i = 0; i < bins1.length; i++) {
            for (int j = 0; j < bins1[i].length; j++) {
                BinSurface[stI + i][stJ + j] = bins1[i][j];
                BinSurface[stI - 1 - i][stJ - 1 - j] = bins1c[i][j];
            }
        }

        stI = BinSurface.length / 2;
        stJ = BinSurface[0].length / 2;
        for (int i = 0; i < bins4.length; i++) {
            for (int j = 0; j < bins4[i].length; j++) {
                BinSurface[stI - 1 - i][stJ + j] = bins4[i][j];
                BinSurface[stI + i][stJ - 1 - j] = bins4c[i][j];
            }
        }

//        for (int i = 0; i < BinSurface.length; i++) {
//            for (int j = 0; j < BinSurface[i].length; j++) {
//                System.out.println(BinSurface[i][j].GridHorDistance + " , " + BinSurface[i][j].GridVerDistance
//                        + " , " + BinSurface[i][j].HorDistance+ " , " + BinSurface[i][j].VerDistance
//                        + " , " + BinSurface[i][j].Value);
//            }
//        }
        int resd = 0;
    }

    /**
     * Draw Semivariogram surface map and also draw the search are if
     * Anisotropic
     *
     * @param Radius
     * @param AnIsotropic
     */
    public void DrawSemivariogramSurface(double Radius, boolean AnIsotropic) {
        double[][] data = new double[3][BinSurface.length * BinSurface[0].length];
        int n = 0;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < BinSurface.length; i++) {
            for (int j = 0; j < BinSurface[i].length; j++) {
                data[0][n] = BinSurface[i][j].GridHorDistance;
                data[1][n] = BinSurface[i][j].GridVerDistance;
                if ((Math.pow(data[0][n], 2) + Math.pow(data[1][n], 2)) <= Radius * Radius
                        && !Double.isNaN(BinSurface[i][j].Value)) {
                    data[2][n] = BinSurface[i][j].Value;
                    if (max < data[2][n]) {
                        max = data[2][n];
                    }
                } else {
                    data[2][n] = -1;
                }
                n++;
            }
        }
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries("Value", data);
        NumberAxis xAxis = new NumberAxis();

        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockWidth(LagSize);
        renderer.setBlockHeight(LagSize);
        renderer.setBlockAnchor(RectangleAnchor.CENTER);

        LookupPaintScale paintScale = new LookupPaintScale(0, max, Color.white);
        double colorRange = max / 6;
        //double colorRange = 23013;
        paintScale.add(0.0, Color.blue);
        paintScale.add(1 * colorRange, Color.green);
        paintScale.add(2 * colorRange, Color.cyan);
        paintScale.add(3 * colorRange, Color.yellow);
        paintScale.add(4 * colorRange, Color.ORANGE);
        paintScale.add(5 * colorRange, Color.red);

        renderer.setPaintScale(paintScale);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);

        if (AnIsotropic) {
            CombinedRangeXYPlot combinedrangexyplot = new CombinedRangeXYPlot();
            XYSeries seriesT1 = new XYSeries("1");
            XYSeriesCollection AngleCollct = new XYSeriesCollection();

            double bw = BandWidth;
            double r = bw / Math.sin(Tolerance);
            if (r > Radius) {
                bw = Radius * Math.sin(Tolerance);
                r = Radius;
            }
            seriesT1.add(r * Math.cos(Angle + Tolerance), r * Math.sin(Angle + Tolerance));

            if ((double) Math.round(Math.sin(Angle) * 10000) / 10000 != 0) {
                if ((double) Math.round(Math.cos(Angle) * 10000) / 10000 != 0) {
                    double a = (1 + Math.pow(Math.tan(Angle), 2));
                    double b = 2 * bw / Math.sin(Angle) * Math.pow(Math.tan(Angle), 2);
                    double c = Math.pow(Math.tan(Angle), 2) * Math.pow(bw / Math.sin(Angle), 2) - Math.pow(Radius, 2);
                    double x1 = (-b + Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
                    double y1 = Math.tan(Angle) * (x1 + bw / Math.sin(Angle));
                    double x2 = (-b - Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
                    double y2 = Math.tan(Angle) * (x2 + bw / Math.sin(Angle));
                    double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                    double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x2), 2)) + (Math.pow((Radius * Math.sin(Angle) - y2), 2)));
                    if (d1 < d2) {
                        seriesT1.add(x1, y1);
                    } else {
                        seriesT1.add(x2, y2);
                    }
                } else {
                    double x1 = -bw * Math.sin(Angle);
                    double y1 = Math.sqrt(Math.pow(Radius, 2) - Math.pow(x1, 2));
                    double y2 = -Math.sqrt(Math.pow(Radius, 2) - Math.pow(x1, 2));
                    double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                    double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y2), 2)));

                    if (d1 < d2) {
                        seriesT1.add(x1, y1);
                    } else {
                        seriesT1.add(x1, y2);
                    }
                }
            } else {
                double y1 = bw * Math.cos(Angle);
                double x1 = Math.sqrt(Math.pow(Radius, 2) - Math.pow(y1, 2));
                double x2 = -Math.sqrt(Math.pow(Radius, 2) - Math.pow(y1, 2));
                double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x2), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));

                if (d1 < d2) {
                    seriesT1.add(x1, y1);
                } else {
                    seriesT1.add(x2, y1);
                }
            }

            AngleCollct.addSeries(seriesT1);

            XYSeries seriesT2 = new XYSeries("2");
            seriesT2.add(r * Math.cos(Angle + Tolerance), r * Math.sin(Angle + Tolerance));
            seriesT2.add(0.0, 0.0);
            AngleCollct.addSeries(seriesT2);

            XYSeries seriesT3 = new XYSeries("3");
            seriesT3.add(Radius * Math.cos(Angle), Radius * Math.sin(Angle));
            seriesT3.add(0, 0);
            AngleCollct.addSeries(seriesT3);

            XYSeries seriesT4 = new XYSeries("4");
            seriesT4.add(r * Math.cos(Angle - Tolerance), r * Math.sin(Angle - Tolerance));
            seriesT4.add(0, 0);
            AngleCollct.addSeries(seriesT4);

            XYSeries seriesT5 = new XYSeries("5");

            seriesT5.add(r * Math.cos(Angle - Tolerance), r * Math.sin(Angle - Tolerance));
            if ((double) Math.round(Math.sin(Angle) * 10000) / 10000 != 0) {
                if ((double) Math.round(Math.cos(Angle) * 10000) / 10000 != 0) {
                    double a = (1 + Math.pow(Math.tan(Angle), 2));
                    double b = -2 * bw / Math.sin(Angle) * Math.pow(Math.tan(Angle), 2);
                    double c = Math.pow(Math.tan(Angle), 2) * Math.pow(bw / Math.sin(Angle), 2) - Math.pow(Radius, 2);
                    double x1 = (-b + Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
                    double y1 = Math.tan(Angle) * (x1 - bw / Math.sin(Angle));
                    double x2 = (-b - Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
                    double y2 = Math.tan(Angle) * (x2 - bw / Math.sin(Angle));
                    double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                    double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x2), 2)) + (Math.pow((Radius * Math.sin(Angle) - y2), 2)));
                    if (d1 < d2) {
                        seriesT5.add(x1, y1);
                    } else {
                        seriesT5.add(x2, y2);
                    }
                } else {
                    double x1 = bw * Math.sin(Angle);
                    double y1 = Math.sqrt(Math.pow(Radius, 2) - Math.pow(x1, 2));
                    double y2 = -Math.sqrt(Math.pow(Radius, 2) - Math.pow(x1, 2));
                    double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                    double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y2), 2)));

                    if (d1 < d2) {
                        seriesT5.add(x1, y1);
                    } else {
                        seriesT5.add(x1, y2);
                    }
                }
            } else {
                double y1 = -bw * Math.cos(Angle);
                double x1 = Math.sqrt(Math.pow(Radius, 2) - Math.pow(y1, 2));
                double x2 = -Math.sqrt(Math.pow(Radius, 2) - Math.pow(y1, 2));
                double d1 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x1), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));
                double d2 = Math.sqrt((Math.pow((Radius * Math.cos(Angle) - x2), 2)) + (Math.pow((Radius * Math.sin(Angle) - y1), 2)));

                if (d1 < d2) {
                    seriesT5.add(x1, y1);
                } else {
                    seriesT5.add(x2, y1);
                }
            }
            AngleCollct.addSeries(seriesT5);
            plot.setDataset(1, AngleCollct);
            XYLineAndShapeRenderer lineshapRend = new XYLineAndShapeRenderer(true, false);
            for (int i = 0; i < AngleCollct.getSeriesCount(); i++) {
                //plot.getRenderer().setSeriesPaint(i , Color.BLUE);
                lineshapRend.setSeriesPaint(i, Color.BLACK);
            }
            plot.setRenderer(1, lineshapRend);
            combinedrangexyplot.add(plot);
        }
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        JFreeChart chart = new JFreeChart("Semivariogram Surface", plot);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);

        // create and display a frame...
        ChartFrame frame = new ChartFrame("", chart);
        frame.pack();
        //frame.setSize(100, 50);
        frame.setVisible(true);
    }

    /**
     * This method uses NSGA algorithm to fit the Semi Variogram
     *
     * @param semiType
     * @param n
     * @return
     */
    Variogram TheoryVariogramNSGA(SemivariogramType semiType, int n) {
        // Set solver parameters

        double[] y = new double[bins.length];

        for (int i = 0; i < y.length; i++) {
            y[i] = bins[i][n].Value;

        }
        int nNan = 0;
        for (int i = 0; i < y.length; i++) {
            if (!Double.isNaN(y[i])) {
                nNan++;
            }
        }
        x = new double[nNan];

        double[] y2 = new double[nNan];
        int ntmp = 0;
        for (int i = 0; i < y.length; i++) {
            if (!Double.isNaN(y[i])) {
                y2[ntmp] = y[i];
                x[ntmp] = bins[i][nthSVariogram].Distance;
                ntmp++;
            }

        }
        y = y2;

        double[][] pnts = new double[y.length][2];
        for (int i = 0; i < y.length; i++) {
            pnts[i][1] = y[i];
            pnts[i][0] = x[i];
        }
        Variogram var = new Variogram();
        var.Type = semiType;
        SemivariogramCurveFitter svcf = new SemivariogramCurveFitter();
        try {
            var = svcf.Run(pnts, semiType, ConsiderNugget);
        } catch (JMException ex) {
            //Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            //Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        return var;
    }

    /**
     *
     * @param semiType
     * @param n is the nth sector for anisotropic
     * @return
     */
    Variogram TheoryVariogram(SemivariogramType semiType, int n) {
        SVType = semiType;
        nthSVariogram = n;
        //x = xValue ;
        //TheoryVariogramNSGA(SVType,n);

        LevenbergMarquardt optimizer = new LevenbergMarquardt() {
            // Override your objective function here

            public void setValues(double[] parameters, double[] values) {
                //parameters[0] = sill, parameters[1] Range, parameters[2] nugget    
//                double [] x = new double[values.length];
//                    for (int i = 0; i < values.length; i++) {
//                        x[i]=bins[i][ nthSVariogram].Distance;
//                    }
                switch (SVType) {
                    case Exponential:
                        for (int i = 0; i < x.length; i++) {
                            if (x[i] != 0) {
                                values[i] = (ConsiderNugget ? parameters[2] : 0) + parameters[0] * (1 - Math.exp(-x[i] / parameters[1]));
                            } else {
                                values[i] = 0;
                            }
                        }
                        break;
                    case Gaussian:
                        for (int i = 0; i < x.length; i++) {
                            if (x[i] != 0) {
                                values[i] = (ConsiderNugget ? parameters[2] : 0) + parameters[0] * (1 - Math.exp(-(Math.pow(x[i], 2)) / (Math.pow(parameters[1], 2))));
                            } else {
                                values[i] = 0;
                            }
                        }
                        break;
                    case Spherical:
                        for (int i = 0; i < x.length; i++) {
                            if (x[0] > parameters[1]) {
                                values[i] = (ConsiderNugget ? parameters[2] : 0) + parameters[0];

                            } else if (0 < x[0] && x[0] <= parameters[1]) {
                                values[i] = (ConsiderNugget ? parameters[2] : 0) + parameters[0] * (1.5 * x[i] / parameters[1] - 0.5 * Math.pow((x[i] / parameters[1]), 3));
                            } else {
                                values[i] = 0;
                            }
                        }
                        break;
                }
            }
        };

        // Set solver parameters
        double[] y = new double[bins.length];

        for (int i = 0; i < y.length; i++) {
            y[i] = bins[i][n].Value;

        }
        int nNan = 0;
        for (int i = 0; i < y.length; i++) {
            if (!Double.isNaN(y[i])) {
                nNan++;
            }
        }
        x = new double[nNan];

        double[] y2 = new double[nNan];
        int ntmp = 0;
        for (int i = 0; i < y.length; i++) {
            if (!Double.isNaN(y[i])) {
                y2[ntmp] = y[i];
                x[ntmp] = bins[i][nthSVariogram].Distance;
                ntmp++;
            }

        }
        y = y2;

        double[] iniPar = new double[y.length];
        double[] w = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            iniPar[i] = 1;
            w[i] = 1;
        }
        double tmp = 0;
        int tmpN = 0;
        for (int i = 0; i < y.length; i++) {
            if (!Double.isNaN(y[i])) {
                tmp += y[i];
                tmpN++;
            } else {
                w[i] = 0;
            }
        }
        iniPar[1] = this.LagSize;
        iniPar[0] = tmp / tmpN;
        optimizer.setInitialParameters(iniPar);
        optimizer.setWeights(w);
        optimizer.setMaxIteration(100);
        optimizer.setErrorTolerance(0.1);
        optimizer.setTargetValues(y);
        try {
            optimizer.run();
        } catch (SolverException ex) {
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }

        double[] bestParameters = optimizer.getBestFitParameters();
//        this.Sill = bestParameters[0];
//        this.Range=bestParameters[1];
//        this.Nugget = (ConsiderNugget ? bestParameters[2] : 0 );

        Variogram var = new Variogram();
        var.Sill = bestParameters[0];
        var.Range = bestParameters[1];

        if (var.Sill < 0) {
            var.Sill = 0;
        }
        var.Nugget = (ConsiderNugget ? bestParameters[2] : 0);
        var.Type = semiType;
        return var;
    }

    /**
     * Calculates the Theoretical SV value to be drawn on the SV
     *
     * @param Distance
     * @param vario
     * @return
     */
    public double getTheoreticalSVValue(double Distance, Variogram vario) {

        double res = 0.0;
        switch (vario.Type) {
            case Exponential:
                if (Distance != 0) {
                    res = vario.Nugget + vario.Sill * (1 - Math.exp(-Distance / vario.Range));
                } else {
                    res = 0;
                }
                break;
            case Gaussian:
                if (Distance != 0) {
                    res = vario.Nugget + vario.Sill * (1 - Math.exp(-3 * (Math.pow(Distance, 2)) / (Math.pow(vario.Range, 2))));
                } else {
                    res = 0;
                }
                break;
            case Spherical:

                if (Distance > vario.Range) {
                    res = vario.Nugget + vario.Sill;
                } else if (0 < Distance && Distance <= vario.Range) {
                    res = vario.Nugget + vario.Sill * (1.5 * Distance / vario.Range - 0.5 * Math.pow((Distance / vario.Range), 3));
                } else {
                    res = 0;
                }
                break;
        }
        return res;
    }

    /**
     * Calculates the points for drawing the theoretical variogram
     *
     * @param SVType
     * @return
     */
    double[][] CalcTheoreticalSVValues(Variogram vario, double MaximumDisplyDistanst) {
        double[][] res = new double[2 * NumberOfLags + 1][2];       //0=X,  1= Y
        for (int i = 0; i < res.length; i++) {
            res[i][0] = i * MaximumDisplyDistanst / (2 * NumberOfLags);
            switch (vario.Type) {
                case Exponential:
                    if (res[i][0] != 0) {
                        res[i][1] = vario.Nugget + vario.Sill * (1 - Math.exp(-res[i][0] / vario.Range));
                    } else {
                        res[i][1] = vario.Nugget;
                    }

                    break;
                case Gaussian:
                    if (res[i][0] != 0) {
                        res[i][1] = vario.Nugget + vario.Sill * (1 - Math.exp(-3 * (Math.pow(res[i][0], 2)) / (Math.pow(vario.Range, 2))));
                    } else {
                        res[i][1] = vario.Nugget;
                    }
                    break;
                case Spherical:
                    if (res[i][0] > vario.Range) {
                        res[i][1] = vario.Nugget + vario.Sill;
                    } else if (res[i][0] > 0 && res[i][0] <= vario.Range) {
                        res[i][1] = vario.Nugget + vario.Sill * (1.5 * res[i][0] / vario.Range - 0.5 * Math.pow((res[i][0] / vario.Range), 3));
                    } else {
                        res[i][1] = vario.Nugget;
                    }
                    break;
            }
        }
        return res;
    }
    //Reads the points coordinates in a shapefile

    /**
     * Reads the point in a shapfile based on its field name
     *
     * @param inputFile
     * @param fieldName
     * @return
     */
    public List<KrigingPoint> ReadPointFile(String inputFile, String fieldName) {
        int fieldNum = 0;
        WhiteboxRasterBase.DataType dataType = WhiteboxRasterBase.DataType.INTEGER;
        boolean useRecID = false;
        // initialize the shapefile input
        ShapeFile input = null;
        try {
            input = new ShapeFile(inputFile);
        } catch (IOException ex) {
            System.out.println(ex.getMessage().toString());
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (input.getShapeType() != ShapeType.POINT
                && input.getShapeType() != ShapeType.POINTZ
                && input.getShapeType() != ShapeType.POINTM
                && input.getShapeType() != ShapeType.MULTIPOINT
                && input.getShapeType() != ShapeType.MULTIPOINTZ
                && input.getShapeType() != ShapeType.MULTIPOINTM) {
            //showFeedback("The input shapefile must be of a 'point' data type.");
            JOptionPane.showMessageDialog(null, "The input shapefile must be of a 'point' data type.");
            return null;
        }
        ///////////////
        // what type of data is contained in fieldName?
        AttributeTable reader = input.getAttributeTable(); //new DBFReader(input.getDatabaseFile());
        int numberOfFields = reader.getFieldCount();

        for (int i = 0; i < numberOfFields; i++) {
            DBFField field = reader.getField(i);

            if (field.getName().equals(fieldName)) {
                fieldNum = i;
                if (field.getDataType() == DBFField.DBFDataType.NUMERIC
                        || field.getDataType() == DBFField.DBFDataType.FLOAT) {
                    if (field.getDecimalCount() == 0) {
                        dataType = WhiteboxRasterBase.DataType.INTEGER;
                    } else {
                        dataType = WhiteboxRasterBase.DataType.FLOAT;
                    }
                } else {
                    useRecID = true;
                }
            }
        }
        if (fieldNum < 0) {
            useRecID = true;
        }
        //////////////////////
        Object[] data = null;
        double[][] geometry;
        List<KrigingPoint> Points = new ArrayList<KrigingPoint>();

        for (ShapeFileRecord record : input.records) {
            try {
                data = reader.nextRecord();
            } catch (DBFException ex) {
                Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
            }
            geometry = getXYFromShapefileRecord(record);
            for (int i = 0; i < geometry.length; i++) {
                KrigingPoint p = new KrigingPoint(geometry[i][0], geometry[i][1], Double.valueOf(data[fieldNum].toString()));
                Points.add(p);
            }
        }

        return Points;
    }

    private double[][] getXYFromShapefileRecord(ShapeFileRecord record) {
        double[][] ret;
        ShapeType shapeType = record.getShapeType();
        switch (shapeType) {
            case POINT:
                whitebox.geospatialfiles.shapefile.Point recPoint
                        = (whitebox.geospatialfiles.shapefile.Point) (record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPoint.getX();
                ret[0][1] = recPoint.getY();
                break;
            case POINTZ:
                PointZ recPointZ = (PointZ) (record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPointZ.getX();
                ret[0][1] = recPointZ.getY();
                break;
            case POINTM:
                PointM recPointM = (PointM) (record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPointM.getX();
                ret[0][1] = recPointM.getY();
                break;
            case MULTIPOINT:
                MultiPoint recMultiPoint = (MultiPoint) (record.getGeometry());
                return recMultiPoint.getPoints();
            case MULTIPOINTZ:
                MultiPointZ recMultiPointZ = (MultiPointZ) (record.getGeometry());
                return recMultiPointZ.getPoints();
            case MULTIPOINTM:
                MultiPointM recMultiPointM = (MultiPointM) (record.getGeometry());
                return recMultiPointM.getPoints();
            default:
                ret = new double[1][2];
                ret[1][0] = -1;
                ret[1][1] = -1;
                break;
        }

        return ret;
    }

    /**
     * It calculates the location of each grid cell. the resolution should be
     * set before calling this method
     *
     * @return a point list
     */
    public List<KrigingPoint> calcInterpolationPoints() {
        double north, south, east, west;
        int nrows, ncols;
        double northing, easting;
        west = bMinX - 0.5 * resolution;
        north = bMaxY + 0.5 * resolution;
        nrows = (int) (Math.ceil((north - bMinY) / resolution));
        ncols = (int) (Math.ceil((bMaxX - west) / resolution));
        south = north - nrows * resolution;
        east = west + ncols * resolution;
        int row, col;
        List<KrigingPoint> pnts = new ArrayList();
        // Create the whitebox raster object.
        double halfResolution = resolution / 2;
        for (row = 0; row < nrows; row++) {
            for (col = 0; col < ncols; col++) {
                easting = (col * resolution) + (west + halfResolution);
                northing = (north - halfResolution) - (row * resolution);
                pnts.add(new KrigingPoint(easting, northing, 0.0));
            }
        }
        return pnts;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    /**
     * Produces the output raster
     *
     * @param outputRaster
     * @param pnts
     * @param drawKrigingVariance
     */
    public void buildRaster(String outputRaster, List<KrigingPoint> pnts, boolean drawKrigingVariance) {
        double north, south, east, west;
        int nrows, ncols;
        double northing, easting;
        west = bMinX - 0.5 * resolution;
        north = bMaxY + 0.5 * resolution;
        nrows = (int) (Math.ceil((north - bMinY) / resolution));
        ncols = (int) (Math.ceil((bMaxX - west) / resolution));
        south = north - nrows * resolution;
        east = west + ncols * resolution;
        String outputHeader = outputRaster;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        String str1;
        double noData = -32768;
        // see if the output files already exist, and if so, delete them.
        if ((new File(outputHeader)).exists()) {
            (new File(outputHeader)).delete();
            (new File(outputHeader.replace(".dep", ".tas"))).delete();
        }
        try {
            // create the whitebox header file.
            fw = new FileWriter(outputHeader, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
            out.println(str1);
            str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
            out.println(str1);
            str1 = "North:\t" + Double.toString(north);
            out.println(str1);
            str1 = "South:\t" + Double.toString(south);
            out.println(str1);
            str1 = "East:\t" + Double.toString(east);
            out.println(str1);
            str1 = "West:\t" + Double.toString(west);
            out.println(str1);
            str1 = "Cols:\t" + Integer.toString(ncols);
            out.println(str1);
            str1 = "Rows:\t" + Integer.toString(nrows);
            out.println(str1);
            str1 = "Data Type:\t" + "float";
            out.println(str1);
            str1 = "Z Units:\t" + "not specified";
            out.println(str1);
            str1 = "XY Units:\t" + "not specified";
            out.println(str1);
            str1 = "Projection:\t" + "not specified";
            out.println(str1);
            str1 = "Data Scale:\tcontinuous";
            out.println(str1);
            str1 = "Preferred Palette:\t" + "rgb.pal";
            out.println(str1);
            str1 = "NoData:\t" + noData;
            out.println(str1);
            if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
            } else {
                str1 = "Byte Order:\t" + "BIG_ENDIAN";
            }
            out.println(str1);

            out.close();

        } catch (Exception e) {

            return;
        }
        int row, col;
        // Create the whitebox raster object.
        WhiteboxRaster image = new WhiteboxRaster(outputHeader, "rw");

        double halfResolution = resolution / 2;

        int nn = 0;
        int progress;
        int oldProgress = -1;
        for (row = 0; row < nrows; row++) {
            for (col = 0; col < ncols; col++) {
                easting = (col * resolution) + (west + halfResolution);
                northing = (north - halfResolution) - (row * resolution);
                if (!drawKrigingVariance) {
                    image.setValue(row, col, pnts.get(nn).z);
                } else {
                    image.setValue(row, col, pnts.get(nn).v);
                }
                nn++;
            }

            progress = (int) (100f * row / (nrows - 1));
            if (progress > oldProgress) {
                changes.firePropertyChange("progress", oldProgress, progress);
                //host.updateProgress("Interpolating Data:", progress);
                oldProgress = progress;
            }
        }
        image.addMetadataEntry("Created by the Kriging Interpolation Tool.");
        image.addMetadataEntry("Created on " + new Date());

        image.close();
    }

    /**
     * Gets the variogram and unknown point list and returns the interpolated
     * values for the known points This is to calculate the predicted value for
     * each known point, the result would be used for cross validation.
     *
     * @param variogram
     * @param pnts
     * @return
     */
    public List<KrigingPoint> CrossValidationPoints(Variogram variogram, List<KrigingPoint> pnts, int NumberOfNearestPoints) {

        double[] res = new double[NumberOfNearestPoints];
        double[][] D = new double[NumberOfNearestPoints + 1][1];

        List<KrigingPoint> NNPoitns = new ArrayList();
        List<KrigingPoint> outPnts = new ArrayList();
        for (int n = 0; n < pnts.size(); n++) {
            NNPoitns = getNNpoints(this.pointsTree, pnts.get(n), NumberOfNearestPoints + 1);
            for (int ni = 0; ni < NumberOfNearestPoints + 1; ni++) {
                if (pnts.get(n).x == NNPoitns.get(ni).x
                        && pnts.get(n).y == NNPoitns.get(ni).y
                        && pnts.get(n).z == NNPoitns.get(ni).z) {
                    NNPoitns.remove(ni);
                    break;
                }
            }

            double[][] C = CalcConstantCoef(variogram, NNPoitns);
            double[] tm = CalcVariableCoef(variogram, pnts.get(n), NNPoitns); ///------------
            for (int i = 0; i < tm.length; i++) {
                D[i][0] = tm[i];
            }
            //double[][] d = {{1,2,3},{4,5,6,},{7,8,10}};
            Matrix tmp = Matrix.constructWithCopy(C);
            Matrix VariableCoef = Matrix.constructWithCopy(D);
            Matrix w = null;
            boolean flag = false;
            try {
                w = tmp.solve(VariableCoef);
                double[][] Wi = w.getArray();
                double s = 0;
                for (int i = 0; i < Wi.length - 1; i++) {
                    s = s + Wi[i][0] * NNPoitns.get(i).z;
                }
                KrigingPoint pnt = new KrigingPoint(pnts.get(n).x, pnts.get(n).y, s);
                outPnts.add(pnt);
                //pnts.get(n).z = s;
                //res[n]=s;
                s = 0;

            } catch (Exception ex) {
                SingularValueDecomposition svd = tmp.svd();
                Matrix u = svd.getU();
                Matrix s = svd.getS();
                Matrix v = svd.getV();
                //u.print(u.getRowDimension(), u.getColumnDimension());
                //s.print(s.getRowDimension(), s.getColumnDimension());
                //v.print(v.getRowDimension(), v.getColumnDimension());

                int rrr = svd.rank();
                double[][] stemp = s.getArray();
                for (int nn = 0; nn < stemp.length; nn++) {
                    if (stemp[nn][nn] > 0.03) {
                        stemp[nn][nn] = 1 / stemp[nn][nn];
                    } else {
                        stemp[nn][nn] = 0;
                    }
                }
                Matrix sp = new Matrix(stemp);
                w = v.times(sp).times(u.transpose()).times(VariableCoef);
                //Matrix test = tmp.times(w).minus(VariableCoef);
                double[][] Wi = w.getArray();
                double ss = 0;
                for (int i = 0; i < Wi.length - 1; i++) {
                    ss = ss + Wi[i][0] * NNPoitns.get(i).z;
                }
                KrigingPoint pnt = new KrigingPoint(pnts.get(n).x + 1, pnts.get(n).y, ss);
                outPnts.add(pnt);

                //pnts.get(n).z = ss;
                ss = 0;
            }
        }

        return outPnts;
    }

    /**
     * Gets the variogram and unknown point list and returns the interpolated
     * values for the unknown points It also calculates the Kriging Variance and
     * sets the KrigingPoint.V
     *
     * @param variogram
     * @param pnts
     * @return
     */
    public List<KrigingPoint> InterpolatePoints(Variogram variogram, List<KrigingPoint> pnts, int NumberOfNearestPoints) {

        double[] res = new double[NumberOfNearestPoints];
        double[][] D = new double[NumberOfNearestPoints + 1][1];

        List<KrigingPoint> nnPoints = new ArrayList();
        List<KrigingPoint> outPnts = new ArrayList();
        for (int n = 0; n < pnts.size(); n++) {
            nnPoints = getNNpoints(this.pointsTree, pnts.get(n), NumberOfNearestPoints);

            double[][] C = CalcConstantCoef(variogram, nnPoints);
            double[] tm = CalcVariableCoef(variogram, pnts.get(n), nnPoints); ///------------
            for (int i = 0; i < tm.length; i++) {
                D[i][0] = tm[i];
            }
            //double[][] d = {{1,2,3},{4,5,6,},{7,8,10}};
            Matrix tmp = Matrix.constructWithCopy(C);
            Matrix VariableCoef = Matrix.constructWithCopy(D);
            Matrix w = null;
            boolean flag = false;
            try {
                double vs = 0;
                w = tmp.solve(VariableCoef);
                double[][] Wi = w.getArray();
                double s = 0;
                for (int i = 0; i < Wi.length - 1; i++) {
                    s = s + Wi[i][0] * nnPoints.get(i).z;
                    vs = vs + Wi[i][0] * D[i][0];
                }
                KrigingPoint pnt = new KrigingPoint(pnts.get(n).x, pnts.get(n).y, s);
                pnt.v = vs + Wi[Wi.length - 1][0];
                if (pnt.v <= 0) {
                    pnt.v = pnt.v;
                }
                outPnts.add(pnt);
                //pnts.get(n).z = s;
                //res[n]=s;
                s = 0;

            } catch (Exception ex) {
                SingularValueDecomposition svd = tmp.svd();
                Matrix u = svd.getU();
                Matrix s = svd.getS();
                Matrix v = svd.getV();
                //u.print(u.getRowDimension(), u.getColumnDimension());
                //s.print(s.getRowDimension(), s.getColumnDimension());
                //v.print(v.getRowDimension(), v.getColumnDimension());

                int rrr = svd.rank();
                double[][] stemp = s.getArray();
                for (int nn = 0; nn < stemp.length; nn++) {
                    if (stemp[nn][nn] > 0.003) {
                        stemp[nn][nn] = 1 / stemp[nn][nn];
                    } else {
                        stemp[nn][nn] = 0;
                    }
                }
                Matrix sp = new Matrix(stemp);
                w = v.times(sp).times(u.transpose()).times(VariableCoef);
                //Matrix test = tmp.times(w).minus(VariableCoef);
                double[][] Wi = w.getArray();
                double ss = 0;
                double vs = 0;
                for (int i = 0; i < Wi.length - 1; i++) {
                    ss = ss + Wi[i][0] * nnPoints.get(i).z;
                    vs = vs + Wi[i][0] * D[i][0];
                }
                KrigingPoint pnt = new KrigingPoint(pnts.get(n).x, pnts.get(n).y, ss);
                pnt.v = vs + Wi[Wi.length - 1][0];
                if (pnt.v <= 0) {
                    pnt.v = pnt.v;
                    for (int i = 0; i < nnPoints.size(); i++) {
                        System.out.println(nnPoints.get(i).x + " "
                                + nnPoints.get(i).y + " "
                                + nnPoints.get(i).z);

                    }

                }

                outPnts.add(pnt);

                //pnts.get(n).z = ss;
                ss = 0;
            }
        }

        return outPnts;
    }

    /**
     * Returns the list of Pairs which are in the Nearest Neighborhood of the
     * bin center point
     *
     * @param Tree
     * @param entry (y,x)
     * @param HalfBinSize
     * @param Range is the search radius
     * @return
     */
    private List<pair> getBinNNPairs4Map(KdTree<Double> Tree, double[] entry, double BinSize, double Range) {

        List<KdTree.Entry<Double>> results;
        results = Tree.neighborsWithinRange(entry, Range);
        List<pair> res = new ArrayList();
        double xd = 0;
        double yd = 0;
        for (int i = 0; i < results.size(); i++) {
            xd = Math.sqrt(Math.pow((Pairs.get(results.get(i).value.intValue()).HorDistance - entry[1]), 2));
            yd = Math.sqrt(Math.pow((Pairs.get(results.get(i).value.intValue()).VerDistance - entry[0]), 2));
            if (xd <= BinSize && yd <= BinSize) {
                res.add(Pairs.get(results.get(i).value.intValue()));
            }
        }
        return res;
    }

    /**
     * Returns the list of nearest neighbor points
     *
     * @param Tree
     * @param pnt
     * @param numPointsToUse
     * @return
     */
    private List<KrigingPoint> getNNpoints(KdTree<Double> Tree, KrigingPoint pnt, int numPointsToUse) {
        double[] entry;
        //double[] outentry;
        entry = new double[]{pnt.y, pnt.x};
        List<KdTree.Entry<Double>> results;
        results = Tree.nearestNeighbor(entry, numPointsToUse, false);
        List<KrigingPoint> pnts = new ArrayList();
        List<KrigingPoint> res = new ArrayList();
        for (int i = 0; i < results.size(); i++) {
            //KrigingPoint tmp = new KrigingPoint();
            //int id = results.get(i).value.intValue();
            res.add(Points.get(results.get(i).value.intValue()));
        }
        return res;
    }

    /**
     * calculates the D matrix for Kriging system
     *
     * @param variogram
     * @param p is the unknown point
     * @param NNPoints is the list of nearest neighbor points
     * @return
     */
    private double[] CalcVariableCoef(Variogram variogram, KrigingPoint p, List<KrigingPoint> NNPoints) {
        int n = NNPoints.size();
        double[] mat = new double[n + 1];
        double dist = 0.0;
        for (int i = 0; i < n; i++) {
            dist = Math.sqrt(Math.abs(Math.pow(NNPoints.get(i).x - p.x, 2))
                    + Math.abs(Math.pow(NNPoints.get(i).y - p.y, 2)));
            mat[i] = getTheoreticalSVValue(dist, variogram);
        }
        mat[n] = 1;
        return mat;
    }

    /**
     * This prepares the known points matrix for ordinary Kriging
     *
     * @param variogarm
     * @return
     */
    private double[][] CalcConstantCoef(Variogram variogarm, List<KrigingPoint> NNPoints) {
        int n = NNPoints.size();
        double[][] mat = new double[n + 1][n + 1];
        double dist = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                dist = Math.sqrt(Math.abs(Math.pow(NNPoints.get(i).x - NNPoints.get(j).x, 2))
                        + Math.abs(Math.pow(NNPoints.get(i).y - NNPoints.get(j).y, 2)));
                mat[i][j] = getTheoreticalSVValue(dist, variogarm);
                mat[j][i] = mat[i][j];
            }
        }
        for (int i = 0; i < n; i++) {
            mat[i][n] = 1;
            mat[n][i] = 1;
        }

//        
//        String s= new String();
//        try {
//            PrintWriter pr = new PrintWriter("G:\\test.txt");
//            for (int i = 0; i < mat.length; i++) {
//                for (int j = 0; j < mat.length; j++) {
//                    s = s + "," + mat[i][j];
//                }
//                pr.println(s);
//                s = "";
//            }
//            pr.close();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
        return mat;

    }

    /**
     * This just to use when the semivariogram model is provided by the user.
     * (Kriging Optimizer)
     */
    void BuildPointTree() {
        pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()));
        double[] entry;
        for (int i = 0; i < this.Points.size(); i++) {
            entry = new double[]{this.Points.get(i).y, this.Points.get(i).x};
            pointsTree.addPoint(entry, (double) i);
        }

    }

    /**
     * Creates the pairs list based on sector classification. calcs the distance
     * and moment of inertia for each pair It also calculates the min and max
     * points and boundary It also build the KDTree object to be used with the
     * Kriging
     */
    void CalPairs4Sec() throws FileNotFoundException {
        MaximumDistance = 0;
        MinX = Double.POSITIVE_INFINITY;
        MinY = Double.POSITIVE_INFINITY;
        MaxX = Double.NEGATIVE_INFINITY;
        MaxY = Double.NEGATIVE_INFINITY;
        pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()));
        //PairsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()*(this.Points.size()-1)/2));
        PairsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size() * (this.Points.size())));
        double[] entry;
        double[] pairentry;

        String s = new String();
//        PrintWriter pw ;
//        pw = new PrintWriter("G:\\test.txt");

        double dx = 0;
        double dy = 0;
        for (int i = 0; i < this.Points.size(); i++) {

            if (this.Points.get(i).x < MinX) {
                MinX = this.Points.get(i).x;
            }
            if (this.Points.get(i).y < MinY) {
                MinY = this.Points.get(i).y;
            }
            if (this.Points.get(i).x > MaxX) {
                MaxX = this.Points.get(i).x;
            }
            if (this.Points.get(i).y > MaxY) {
                MaxY = this.Points.get(i).y;
            }

            entry = new double[]{this.Points.get(i).y, this.Points.get(i).x};
            pointsTree.addPoint(entry, (double) i);

            for (int j = 0; j < this.Points.size(); j++) {
                pair pr = new pair();

                if (i != j) {

                    pr.FirstP = i;
                    pr.SecondP = j;
                    pr.Distance = Math.sqrt(Math.pow((Points.get(i).x - Points.get(j).x), 2)
                            + Math.pow((Points.get(i).y - Points.get(j).y), 2));

                    pr.HorDistance = (Points.get(j).x - Points.get(i).x);
                    pr.VerDistance = (Points.get(j).y - Points.get(i).y);

                    if (MaximumDistance < pr.Distance) {
                        MaximumDistance = pr.Distance;
                    }

                    dx = Points.get(j).x - Points.get(i).x;
                    dy = Points.get(j).y - Points.get(i).y;

                    if (dx != 0) {
                        if ((dx > 0 && dy >= 0)) {
                            pr.Direction = Math.atan(dy / dx);
                        }
                        if (dx < 0 && dy >= 0) {
                            pr.Direction = Math.atan(dy / dx) + Math.PI;
                        }
                        if (dx > 0 && dy < 0) {
                            pr.Direction = Math.atan(dy / dx) + 2 * Math.PI;
                        }
                        if (dx < 0 && dy < 0) {
                            pr.Direction = Math.atan(dy / dx) + Math.PI;;
                        }
                    } else {
                        if (dy >= 0) {
                            pr.Direction = Math.PI / 2;
                        } else {
                            pr.Direction = 3 * Math.PI / 2;
                        }
                    }
                    pr.MomentI = Math.pow((Points.get(i).z - Points.get(j).z), 2) / 2;
                    Pairs.add(pr);

                    pairentry = new double[]{pr.VerDistance, pr.HorDistance};
                    PairsTree.addPoint(pairentry, (double) Pairs.size() - 1.0);

//                    s =  Double.toString(pr.Distance) + "," + Double.toString(pr.Direction)+
//                            "," + Double.toString(pr.MomentI)+
//                            "," + Double.toString(pr.HorDistance)+
//                            ","+Double.toString(pr.VerDistance)+
//                            "," + Integer.toString(pr.FirstP)+
//                            "," + Integer.toString(pr.SecondP);
                    //System.out.println(s);
                }

            }
        }

//        pw.close();
        //LagSize  = MaximumDistance/NumberOfLags;
        bMaxX = MaxX;
        bMaxY = MaxY;
        bMinX = MinX;
        bMinY = MinY;

    }

    /**
     * Creates the pairs list based for Map classification
     *
     * @throws FileNotFoundException
     */
    void CalPairs4Map() throws FileNotFoundException {
        MaximumDistance = 0;
        MinX = Double.POSITIVE_INFINITY;
        MinY = Double.POSITIVE_INFINITY;
        MaxX = Double.NEGATIVE_INFINITY;
        MaxY = Double.NEGATIVE_INFINITY;
        pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()));
        PairsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size() * (this.Points.size() - 1) / 2));
        double[] entry;
        double[] pairentry;

//        String s= new String();
//        PrintWriter pw ;
//        pw = new PrintWriter("G:\\test.txt");
        double dx = 0;
        double dy = 0;
        for (int i = 0; i < this.Points.size(); i++) {

            if (this.Points.get(i).x < MinX) {
                MinX = this.Points.get(i).x;
            }
            if (this.Points.get(i).y < MinY) {
                MinY = this.Points.get(i).y;
            }
            if (this.Points.get(i).x > MaxX) {
                MaxX = this.Points.get(i).x;
            }
            if (this.Points.get(i).y > MaxY) {
                MaxY = this.Points.get(i).y;
            }

            entry = new double[]{this.Points.get(i).y, this.Points.get(i).x};
            pointsTree.addPoint(entry, (double) i);

            for (int j = 0; j < this.Points.size(); j++) {
                pair pr = new pair();

                if (Points.get(i).x <= Points.get(j).x && i != j) {

                    pr.FirstP = i;
                    pr.SecondP = j;
                    pr.Distance = Math.sqrt(Math.pow((Points.get(i).x - Points.get(j).x), 2)
                            + Math.pow((Points.get(i).y - Points.get(j).y), 2));

                    pr.HorDistance = (Points.get(j).x - Points.get(i).x);
                    pr.VerDistance = (Points.get(j).y - Points.get(i).y);

                    if (MaximumDistance < pr.Distance) {
                        MaximumDistance = pr.Distance;
                    }
                    dx = Points.get(j).x - Points.get(i).x;
                    dy = Points.get(j).y - Points.get(i).y;

                    if (dx != 0) {
                        if ((dx > 0 && dy >= 0)) {
                            pr.Direction = Math.atan(dy / dx);
                        }
                        if (dx < 0 && dy >= 0) {
                            pr.Direction = Math.atan(dy / dx) + Math.PI;
                        }
                        if (dx > 0 && dy < 0) {
                            pr.Direction = Math.atan(dy / dx) + 2 * Math.PI;
                        }
                        if (dx < 0 && dy < 0) {
                            pr.Direction = Math.atan(dy / dx) + Math.PI;;
                        }
                    } else {
                        if (dy >= 0) {
                            pr.Direction = Math.PI / 2;
                        } else {
                            pr.Direction = 3 * Math.PI / 2;
                        }
                    }

                    pr.MomentI = Math.pow((Points.get(i).z - Points.get(j).z), 2) / 2;
                    Pairs.add(pr);

                    pairentry = new double[]{pr.VerDistance, pr.HorDistance};
                    PairsTree.addPoint(pairentry, (double) Pairs.size() - 1.0);

//                    s =  Double.toString(pr.Distance) + "," + Double.toString(pr.Direction)+
//                            "," + Double.toString(pr.MomentI)+
//                            "," + Double.toString(pr.HorDistance)+
//                            ","+Double.toString(pr.VerDistance)+
//                            "," + Integer.toString(pr.FirstP)+
//                            "," + Integer.toString(pr.SecondP);
//
//                    pw.println(s);
                }

            }
        }

//        pw.close();
        //LagSize  = MaximumDistance/NumberOfLags;
        bMaxX = MaxX;
        bMaxY = MaxY;
        bMinX = MinX;
        bMinY = MinY;

    }

    /**
     * It gets the semivariogram type and bins list and draw a graph for them
     * TheoryVariogram should be called first
     *
     * @param Bins
     * @param variogram
     * @param Type
     */
    public void DrawSemivariogram(bin[][] Bins, Variogram variogram) {
        XYSeriesCollection sampleCollct = new XYSeriesCollection();
        XYSeries series = new XYSeries("Sample Variogram");
//        for (Iterator<bin> i = bins.iterator(); i.hasNext(); )
//        {
//            series.add(bins.get(j).Distance,bins.get(j).Value);
//            i.next();
//            j++;
//        }
        XYLineAndShapeRenderer xylineshapRend = new XYLineAndShapeRenderer(false, true);
        CombinedRangeXYPlot combinedrangexyplot = new CombinedRangeXYPlot();
        for (int i = 0; i < Bins[0].length; i++) {
            for (int k = 0; k < Bins.length; k++) {
                if (!Double.isNaN(Bins[k][i].Value)) {
                    series.add(Bins[k][i].Distance, Bins[k][i].Value);
                }
            }
            sampleCollct.addSeries(series);
            double[][] res = CalcTheoreticalSVValues(variogram, series.getMaxX());
            XYSeries seriesTSV = new XYSeries("Theoretical Variogram");
            for (int l = 0; l < res.length; l++) {
                seriesTSV.add(res[l][0], res[l][1]);
            }
            XYSeriesCollection theorCollct = new XYSeriesCollection();
            theorCollct.addSeries(seriesTSV);

            XYDataset xydataset = sampleCollct;

            XYPlot xyplot1 = new XYPlot(xydataset, new NumberAxis(), null, xylineshapRend);

            xyplot1.setDataset(1, theorCollct);
            XYLineAndShapeRenderer lineshapRend = new XYLineAndShapeRenderer(true, false);
            xyplot1.setRenderer(1, lineshapRend);
            xyplot1.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
            combinedrangexyplot.add(xyplot1);

        }

        DecimalFormat df = new DecimalFormat("###,##0.000");
        String title = "Semivariogram (RMSE = " + df.format(Math.sqrt(variogram.mse)) + ")";
        JFreeChart chart = new JFreeChart(title,
                JFreeChart.DEFAULT_TITLE_FONT, combinedrangexyplot, true);

//        JFreeChart chart = ChartFactory.createScatterPlot(
//            "Semivariogram", // chart title
//            "Distance", // x axis label
//            "Moment of Inertia", // y axis label
//            result, // data  
//            PlotOrientation.VERTICAL,
//            true, // include legend
//            true, // tooltips
//            false // urls
//            );
        // create and display a frame...
        ChartFrame frame = new ChartFrame("Semivariogram", chart);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * This is the main method to classify the pairs for the map and to calc the
     * bin average on the map
     *
     * @param Type
     * @param DistanseRatio
     * @param NumberOfLags
     * @param Anisotropic
     */
    public void calcBinSurface(SemivariogramType Type, double DistanseRatio, int NumberOfLags,
            boolean Anisotropic) {
        this.NumberOfLags = NumberOfLags;
        try {
            CalPairs4Map();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (this.LagSize == 0) {
            this.LagSize = (this.MaximumDistance * DistanseRatio) / this.NumberOfLags;
        }

        CalcBins4Map(this.LagSize * this.NumberOfLags);

    }

    /**
     * This Calculates the Sill and Range Value for the Theoretical Semi
     * Variogram Points list should be filled first This function fills the Sill
     * and Range in the Kriging object
     *
     * @param Type
     * @param DistanseRatio is the ratio of the maximum distance in point to the
     * maximum distance of the variogram
     * @param NumberOfLags
     * @param Anisotropic
     * @param UseNSGA
     * @return
     */
    public Variogram Semivariogram(SemivariogramType Type, double DistanseRatio, int NumberOfLags,
            boolean Anisotropic, boolean UseNSGA) {
        this.NumberOfLags = NumberOfLags;
        try {
            CalPairs4Sec();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (this.LagSize == 0) {
            this.LagSize = (this.MaximumDistance * DistanseRatio) / this.NumberOfLags;
        }

        int n = 0;
        if (!Anisotropic) {
            n = 0;
            CalcBins4Sec(this.LagSize * this.NumberOfLags);
        } else {
            n = 0;
            CalcBins4Sec(this.LagSize * this.NumberOfLags, this.Angle, this.Tolerance, this.BandWidth);
        }
        return (UseNSGA) ? TheoryVariogramNSGA(Type, n) : TheoryVariogram(Type, n);
    }

    public Variogram Semivariogram(SemivariogramType Type, double Range, double Sill, double Nugget,
            boolean Anisotropic) {
        Variogram var = new Variogram();
        var.Type = Type;
        var.Range = Range;
        var.Sill = Sill;
        var.Nugget = Nugget;
        return var;
    }

    /**
     * Randomly selects the n points from the entered point list This is not a
     * necessary method to use but with large point list (More than 1000 points)
     * It is better to apply it
     *
     * @param pnts
     * @param n
     * @return
     */
    public List<KrigingPoint> RandomizePoints(List<KrigingPoint> pnts, int n) {
        Random rnd = new Random();
        List<KrigingPoint> res = new ArrayList();
        double drnd = 0.0;
        for (int i = 0; i < n; i++) {
            res.add(pnts.get(rnd.nextInt(pnts.size())));
        }
        return res;
    }

    public static void main(String[] args) {
        try {
            //ChartPanel(createChart(createDataset()));
            Kriging k = new Kriging();
            k.Points = k.ReadPointFile("/Users/johnlindsay/Documents/Data/Krigging Test Data/test.shp", "Z");
            k.ConsiderNugget = false;
            k.LagSize = 50;
            k.NumberOfLags = 100;
            k.Anisotropic = true;
            k.Tolerance = Math.PI / 4;
            k.BandWidth = 5000;

            PrintWriter pw = new PrintWriter("/Users/johnlindsay/Documents/Data/Krigging Test Data/test.txt");

            for (int i = 0; i < 13; i++) {
                k.Angle = Math.PI / 12.0 * i;
                Variogram var = k.Semivariogram(SemivariogramType.Spherical, 1, k.NumberOfLags, true, true);
                pw.println(k.Angle + "," + var.Range + "," + var.Sill + "," + var.Nugget);
                pw.flush();
                System.out.println((i + 1) + " of 12");
            }
            System.out.println("Done!");
        } catch (Exception e) {

        }

//        k.ConsiderNugget = false;
//        k.Points  =  k.ReadPointFile(
//                "G:\\Optimized Sensory Network\\PALS\\20120607\\test.shp","Z");
//        k.LagSize = 2000;
//        k.Anisotropic = false;
//        Variogram var = k.SemiVariogram(SemiVariogramType.Spherical, 0.27, 50,false, true);
//        
//        //var.Range = 4160.672768;
//        //var.Sill = 1835.571948;
//        
//        k.resolution = 914;
//        k.BMinX = 588450 + k.resolution/2;
//        k.BMaxX = 601246 - k.resolution/2;
//        k.BMinY = 5474650 + k.resolution/2;
//        k.BMaxY = 5545942 - k.resolution/2;
//
//        List<KrigingPoint> outPnts = k.calcInterpolationPoints() ;
//        outPnts = k.InterpolatePoints(var, outPnts, 5);
//        k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\Pnts60.dep", outPnts,false);
//        k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\PntsVar60.dep", outPnts,true);
//
//        k.DrawSemiVariogram(k.bins, var);
        //k.calcBinSurface(SemiVariogramType.Spherical,  1, 99,false);
        //k.DrawSemiVariogramSurface(k.LagSize*(k.NumberOfLags), false);
//        k.LagSize = 2500;
//        k.Anisotropic = false;
////        k.Angle = Math.PI*3/4;
////        k.Tolerance = Math.PI/4;
////        k.BandWidth = 3*k.LagSize;
//        for (int i = 0; i < 50; i++) {
//            k.Points  =  k.ReadPointFile("G:\\Optimized Sensory Network\\PALS\\Pals Shapefiles\\PALS_TA_20120607_HiAlt_v100.shp","h");
//            k.Points = k.RandomizePoints(k.Points, 200);
//            Variogram var = k.SemiVariogram(SemiVariogramType.Spherical, 1, 25,false);
//            k.resolution = 900;
//            List<KrigingPoint> pnts = k.calcInterpolationPoints();
//            pnts = k.InterpolatePoints(var, pnts, 5);
//            k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\Pals Shapefiles\\PALS_TA_20120607_HiAlt_v100"+ i +".dep", pnts);
//        }
        //k.DrawSemiVariogram(k.bins, var);
        //k.calcBinSurface(SemiVariogramType.Spherical,  0.27, 25,false);
        //k.DrawSemiVariogramSurface(k.LagSize*(k.NumberOfLags),false);
        //k.Points  =  k.ReadPointFile("G:\\Papers\\AGU 2013\\Sample\\Sample.shp","V");
        //k.Points  =  k.ReadPointFile("G:\\Papers\\AGU 2013\\WakerLake\\WakerLake.shp","V");
//        String s= new String();
//        PrintWriter pw = null ;
//        try {
//            pw = new PrintWriter("G:\\test.txt");
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        for (int i = 0; i < 500; i++) {
//            Kriging k = new Kriging();
//            k.ConsiderNugget = false;
//            k.LagSize = 50;
//            k.Anisotropic = true;
//            k.Angle = Math.PI*3/4;
//            k.Tolerance = Math.PI/4;
//            k.BandWidth = 3*k.LagSize;
//            k.Points  =  k.ReadPointFile("G:\\Optimized Sensory Network\\PALS\\AGU\\SV_Test.shp","v");
//            k.Points = k.RandomizePoints(k.Points, 500);
//            Variogram var = k.SemiVariogram(SemiVariogramType.Exponential, 0.27, 99,true);
//            s =  var.Range + " , " + var.Sill;
//
//
//            pw.println(s);
//            pw.flush();
//            k = null;
//        }
//             pw.close();;
        //k.resolution = 2.5;
        //k.DrawSemiVariogram(k.bins, var);
        //List<point> pnts = k.calcInterpolationPoints();
//        var.Range = 50;
//        var.Sill = 104843.2;
//        var.Type = SemiVariogramType.Exponential;
//
//        
        //pnts = k.InterpolatePoints(var, pnts,10);
        //k.BuildRaster("G:\\Papers\\AGU 2013\\WakerLake\\WakerLakeOut15.dep", pnts);
        //k.calcBinSurface(SemiVariogramType.Exponential,  0.27, 99,false);
        //k.DrawSemiVariogramSurface(k.LagSize*(k.NumberOfLags),true);
        //Kriging.point p = k.point(65, 137, 0);
//        List<Kriging.point> pnts = new ArrayList();
//        pnts.add(p);
    }
}
