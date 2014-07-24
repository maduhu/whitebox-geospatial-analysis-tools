/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.stats;

import java.util.Arrays;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import whitebox.structures.ExtensionFileFilter;

/**
 *
 * @author johnlindsay
 */
public class Plot2DHistogram extends JPanel implements ActionListener, Printable, MouseMotionListener, MouseListener {
    
    private boolean plotCreated = false;
    private String xAxisTitle, yAxisTitle;
    private int bottomMargin = 75;
    private int topMargin = 45;
    private int leftMargin = 90;
    private int rightMargin = 20;
    private int[][] featureSpace;
    private int[] imageData;
    private int width;
    private int height;
    private int numPix;
    private double xMin, yMin, xMax, yMax;
    private boolean isHillshaded = false;
    private JPopupMenu myPopup = null;
    private JCheckBoxMenuItem cmi = null;
    private JCheckBoxMenuItem plotPeaksMi = null;
    private JMenuItem backgroundMi = null;
    private JMenuItem gridMi = null;
    private JMenuItem paletteMi = null;
    private int backgroundColor = 255;
    private boolean gridOn = true;
    private boolean spectrumPaletteMode = true;
    private Font myFont;
    private double[] xData;
    private double[] yData;
    private int xBinNumber;
    private int yBinNumber;

    // Constructors
    public Plot2DHistogram(double[] xData, double[] yData, int xBinNumber, int yBinNumber, String xAxisTitle, String yAxisTitle, Font myFont) throws Exception {
        this.xData = Arrays.copyOf(xData, xData.length);
        this.yData = Arrays.copyOf(yData, yData.length);
        this.xBinNumber = xBinNumber;
        this.yBinNumber = yBinNumber;
        this.xAxisTitle = xAxisTitle;
        this.yAxisTitle = yAxisTitle;
        this.myFont = myFont;
        
        calculateDensities();
                
        setMouseMotionListener();
        setMouseListener();
        setUp();
    }

    // Methods
    private void calculateDensities() throws Exception {
        double x, y;
        int xBin, yBin;
        if (xData.length != yData.length) {
            throw new Exception("Data are not paired. The x and y data arrays must have the same length.");
        }
        // find the min and max values
        xMin = Double.POSITIVE_INFINITY;
        yMin = Double.POSITIVE_INFINITY;
        xMax = Double.NEGATIVE_INFINITY;
        yMax = Double.NEGATIVE_INFINITY;
        
        for (int row = 0; row < xData.length; row++) {
            x = xData[row];
            y = yData[row];
            if (x < xMin) { xMin = x; }
            if (y < yMin) { yMin = y; }
            if (x > xMax) { xMax = x; }
            if (y > yMax) { yMax = y; }
        }
        
        double xBinSize = (xMax - xMin) / xBinNumber;
        double yBinSize = (yMax - yMin) / yBinNumber;
        
        featureSpace = new int[xBinNumber][yBinNumber];
        
        for (int row = 0; row < xData.length; row++) {
            x = xData[row];
            y = yData[row];
            xBin = (int)((x - xMin) / xBinSize);
            yBin = (int)((y - yMin) / yBinSize);
            if (xBin < xBinNumber && yBin < yBinNumber) {
                featureSpace[xBin][yBin]++;
            }
        }
        
    }
    
    private void setUp() {
        try {
            height = featureSpace[0].length;
            width = featureSpace.length;
            numPix = width * height;
            createPopupMenus();
        } catch (Exception e) {

        }
    }

    //java.util.ArrayList<Dimension> peaks = new java.util.ArrayList<Dimension>();
    boolean plotPeaks = false;
    boolean peaksFound = false;
    int[][] peaks = null;

    private void findLocalPeaks() {
        // local peaks are higher than all their neighbours, and are relatively high in the landscape
        double z;
        boolean flag = false;
        int rN, cN;
        double peakSize = 0.00001;
        int[] Dy = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[] Dx = {1, 1, 1, 0, -1, -1, -1, 0};
        double[] N = new double[8];
        int numPeaks = 0;

        // apply a 3x3 mean filter to remove insignificant local peaks
        double[][] featureSpace2 = new double[width][height];
        double cellTotal = 0;
        int numNeighbours = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (featureSpace[c][r] > 0) {
                    cellTotal = featureSpace2[c][r];
                    numNeighbours = 1;
                    for (int j = 0; j < 8; j++) {
                        rN = r + Dy[j];
                        cN = c + Dx[j];
                        if (rN >= 0 && rN < height && cN >= 0 && cN < width) {
                            if (featureSpace[cN][rN] != 0) {
                                cellTotal += featureSpace[cN][rN];
                                numNeighbours++;
                            }
                        }
                    }
                    featureSpace2[c][r] = cellTotal / numNeighbours;
                }
            }
        }

        long total = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                total += featureSpace[c][r];
            }
        }

        // first scan to find how many peaks there are
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                z = featureSpace2[c][r];
                if (z / total > peakSize) {
                    flag = true;
                    for (int j = 0; j < 8; j++) {
                        rN = r + Dy[j];
                        cN = c + Dx[j];
                        if (rN >= 0 && rN < height && cN >= 0 && cN < width) {
                            if (featureSpace2[cN][rN] > z) {
                                flag = false;
                                break;
                            }
                        }
                    }
                    if (flag) {
                        // it's a peak
                        numPeaks++;
                    }
                }
            }
        }

        peaks = new int[numPeaks][3];

        int k = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                z = featureSpace2[c][r];
                if (z / total > peakSize) {
                    flag = true;
                    for (int j = 0; j < 8; j++) {
                        rN = r + Dy[j];
                        cN = c + Dx[j];
                        if (rN >= 0 && rN < height && cN >= 0 && cN < width) {
                            if (featureSpace2[cN][rN] > z) {
                                flag = false;
                                break;
                            }
                        }
                    }
                    if (flag) {
                        // it's a peak
                        peaks[k][0] = c;
                        peaks[k][1] = r;
                        peaks[k][2] = featureSpace[c][r];
                        k++;
                        //peaks.add(new Dimension(r + image1Min, c + image2Min));
                    }
                }
            }
        }
    }

    private void createPlotImageData() {
        // find the maximum point density in featureSpace
        int maxDensityVal = -1;
        long total = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (featureSpace[c][r] > maxDensityVal) {
                    maxDensityVal = featureSpace[c][r];
                }
                total += featureSpace[c][r];
            }
        }
        double maxDensityValLog = Math.log(maxDensityVal);
        imageData = new int[numPix];
        int index = 0;
        int red, green, blue;
        double loc;
        double paletteZoneSize = 1d / 6;
        if (isHillshaded) {
            double[][] hillshade = new double[height][width];
            int[] Dy = {-1, 0, 1, 1, 1, 0, -1, -1};
            int[] Dx = {1, 1, 1, 0, -1, -1, -1, 0};
            double[] N = new double[8];
            double term1, term2, term3;
            final double radToDeg = 180 / Math.PI;
            final double degToRad = Math.PI / 180;
            double azimuth = 135 * degToRad;
            double altitude = 45 * degToRad;
            double z;
            double fx, fy, aspect;
            double Rad180 = 180 * degToRad;
            double Rad90 = 90 * degToRad;
            int rN, cN;
            double sinTheta;
            double cosTheta;
            double tanSlope;
            sinTheta = Math.sin(altitude);
            cosTheta = Math.cos(altitude);

            double minHill = 99999;
            double maxHill = -99999;
            for (int r = height - 1; r >= 0; r--) {
                for (int c = 0; c < width; c++) {
                    z = (double) (featureSpace[c][r] / (double) total * 1000);
                    for (int j = 0; j < 8; j++) {
                        rN = r + Dy[j];
                        cN = c + Dx[j];
                        if (rN >= 0 && cN >= 0 && rN < height && cN < width) {
                            N[j] = (featureSpace[cN][rN] / (double) total) * 1000;
                        } else {
                            N[j] = z;
                        }
                    }
                    // calculate slope and aspect
                    fy = (N[6] - N[4] + 2 * (N[7] - N[3]) + N[0] - N[2]) / 8;
                    fx = (N[2] - N[4] + 2 * (N[1] - N[5]) + N[0] - N[6]) / 8;
                    if (fx != 0) {
                        tanSlope = Math.sqrt(fx * fx + fy * fy);
                        aspect = (180 - Math.atan(fy / fx) * radToDeg + 90 * (fx / Math.abs(fx))) * degToRad;
                        term1 = tanSlope / Math.sqrt(1 + tanSlope * tanSlope);
                        term2 = sinTheta / tanSlope;
                        term3 = cosTheta * Math.sin(azimuth - aspect);
                        z = term1 * (term2 - term3);
                    } else {
                        z = 0.5;
                    }
                    //if (z > 1) { z = 1; }
                    //if (z < 0.3) { z = 0.3; }
                    if (z < minHill) {
                        minHill = z;
                    }
                    if (z > maxHill) {
                        maxHill = z;
                    }
                    hillshade[c][r] = z;
                }
            }

            double range = maxHill - minHill;
            for (int r = height - 1; r >= 0; r--) {
                for (int c = 0; c < width; c++) {
                    z = (hillshade[c][r] - minHill) / (range);
                    if (z < 0.3) {
                        z = 0.3;
                    }
                    hillshade[c][r] = z;
                }
            }

            for (int r = height - 1; r >= 0; r--) {
                for (int c = 0; c < width; c++) {
                    //loc = (double) featureSpace[c][r] / (double) maxDensityVal;
                    loc = Math.log(featureSpace[c][r]) / maxDensityValLog;
                    if (spectrumPaletteMode) {
                        if (featureSpace[c][r] == 0) {
                            red = backgroundColor;
                            green = backgroundColor;
                            blue = backgroundColor;
                        } else if (loc < paletteZoneSize) {
                            red = (int) (128 - (loc / paletteZoneSize * 128));
                            green = 0;
                            blue = (int) (128 + loc / paletteZoneSize * 128);
                        } else if (loc < 2 * paletteZoneSize) {
                            red = 0;
                            green = (int) ((loc - paletteZoneSize) / paletteZoneSize * 255);
                            blue = 255;
                        } else if (loc < 3 * paletteZoneSize) {
                            red = 0;
                            green = 255;
                            blue = (int) (255 - ((loc - 2 * paletteZoneSize) / paletteZoneSize * 255));
                        } else if (loc < 4 * paletteZoneSize) {
                            red = (int) ((loc - 3 * paletteZoneSize) / paletteZoneSize * 255);
                            green = 255;
                            blue = 0;
                        } else if (loc < 5 * paletteZoneSize) {
                            red = 255;
                            green = (int) (255 - ((loc - 4 * paletteZoneSize) / paletteZoneSize * 255));
                            blue = 0;
                        } else {
                            red = (int) (255 - ((loc - 5 * paletteZoneSize) / paletteZoneSize * 180));
                            green = 0;
                            blue = 0;
                        }
                        if (featureSpace[c][r] != 0) {
                            red = (int) (red * hillshade[c][r]);
                            green = (int) (green * hillshade[c][r]);
                            blue = (int) (blue * hillshade[c][r]);
                        }
                    } else {
                        if (featureSpace[c][r] == 0) {
                            red = 0;
                            green = 0;
                            blue = 0;
                        } else {
                            red = (int) (255 * loc);
                            green = (int) (255 * loc);
                            blue = (int) (255 * loc);
                        }
                    }
                    imageData[index] = ((255 << 24) | (red << 16) | (green << 8) | blue);
                    index++;
                }
            }
        } else {
            for (int r = height - 1; r >= 0; r--) {
                for (int c = 0; c < width; c++) {
                    loc = Math.log(featureSpace[c][r]) / maxDensityValLog;
                    if (spectrumPaletteMode) {
                        if (featureSpace[c][r] == 0) {
                            red = backgroundColor;
                            green = backgroundColor;
                            blue = backgroundColor;
                        } else if (loc < paletteZoneSize) {
                            red = (int) (128 - (loc / paletteZoneSize * 128));
                            green = 0;
                            blue = (int) (128 + loc / paletteZoneSize * 128);
                        } else if (loc < 2 * paletteZoneSize) {
                            red = 0;
                            green = (int) ((loc - paletteZoneSize) / paletteZoneSize * 255);
                            blue = 255;
                        } else if (loc < 3 * paletteZoneSize) {
                            red = 0;
                            green = 255;
                            blue = (int) (255 - ((loc - 2 * paletteZoneSize) / paletteZoneSize * 255));
                        } else if (loc < 4 * paletteZoneSize) {
                            red = (int) ((loc - 3 * paletteZoneSize) / paletteZoneSize * 255);
                            green = 255;
                            blue = 0;
                        } else if (loc < 5 * paletteZoneSize) {
                            red = 255;
                            green = (int) (255 - ((loc - 4 * paletteZoneSize) / paletteZoneSize * 255));
                            blue = 0;
                        } else {
                            red = (int) (255 - ((loc - 5 * paletteZoneSize) / paletteZoneSize * 128));
                            green = 0;
                            blue = 0;
                        }
                    } else {
                        if (featureSpace[c][r] == 0) {
                            red = 0;
                            green = 0;
                            blue = 0;
                        } else {
                            red = (int) (255 * loc);
                            green = (int) (255 * loc);
                            blue = (int) (255 * loc);
                        }
                    }
                    imageData[index] = ((255 << 24) | (red << 16) | (green << 8) | blue);
                    index++;
                }
            }
        }
        plotCreated = true;
    }

    private void setMouseMotionListener() {
        this.addMouseMotionListener(this);
    }

    private void setMouseListener() {
        this.addMouseListener(this);
    }

    public void refresh() {
        plotCreated = false;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        drawPlot(g);
    }

    private void drawPlot(Graphics g) {
        if (!plotCreated) {
            createPlotImageData();
        }

        leftMargin = 60;
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(myFont);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        double activeWidth = getWidth() - leftMargin - rightMargin;
        double activeHeight = getHeight() - topMargin - bottomMargin;
        int bottomY = getHeight() - bottomMargin;
        int rightX = getWidth() - rightMargin;
        int rectLeft, rectWidth, rectTop, rectHeight;

        Image image = createImage(new MemoryImageSource(width, height, imageData, 0, width));
        g2d.drawImage(image, leftMargin, topMargin, (int) activeWidth, (int) activeHeight, this);

        // draw axes
        g2d.setColor(Color.black);
        g2d.drawLine(leftMargin, bottomY, rightX, bottomY);
        g2d.drawLine(leftMargin, bottomY, leftMargin, topMargin);
        g2d.drawLine(leftMargin, topMargin, rightX, topMargin);
        g2d.drawLine(rightX, bottomY, rightX, topMargin);

        // draw ticks
        int tickSize = 4;
        g2d.drawLine(leftMargin, bottomY, leftMargin, bottomY + tickSize);
        g2d.drawLine(rightX, bottomY, rightX, bottomY + tickSize);
        g2d.drawLine(leftMargin, bottomY, leftMargin - tickSize, bottomY);
        g2d.drawLine(leftMargin, topMargin, leftMargin - tickSize, topMargin);

        // histo labels
        DecimalFormat df = new DecimalFormat("#,###,###.###");
        Font font = new Font("SanSerif", Font.PLAIN, 11);
        FontMetrics metrics = g.getFontMetrics(font);
        int hgt, adv;
        hgt = metrics.getHeight();

        // x-axis labels
        String label;
        label = df.format(xMin);
        g2d.drawString(label, leftMargin, bottomY + hgt + 4);
        label = df.format(xMax);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, rightX - adv, bottomY + hgt + 4);
        adv = metrics.stringWidth(label);
        int xAxisMidPoint = (int) (leftMargin + activeWidth / 2);
        g2d.drawString(xAxisTitle, xAxisMidPoint - adv / 2, bottomY + 2 * hgt + 4);

        // y-axis labels
        Font oldFont = g.getFont();
       
        int offset = metrics.stringWidth("0") + 12 + hgt;
        double xr = leftMargin - offset;
        double yr = (int) (topMargin + activeHeight / 2);
        g2d.translate(xr, yr);
        g2d.rotate(-Math.PI / 2.0, 0, 0);
        g2d.drawString(yAxisTitle, 0, 0);
        g2d.rotate(Math.PI / 2);
        g2d.translate(-xr, -yr);


        df = new DecimalFormat("0");
        label = df.format(yMin);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, leftMargin - adv - 12, bottomY + hgt / 2);
        label = df.format(yMax);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, leftMargin - adv - 12, topMargin + hgt / 2);

        // title
        // bold font
        oldFont = g.getFont();
        font = font = new Font("SanSerif", Font.BOLD, 12);
        g2d.setFont(font);

        label = "Feature Space Plot: " + xAxisTitle + " vs. " + yAxisTitle;
        adv = metrics.stringWidth(label);
        g2d.drawString(label, getWidth() / 2 - adv / 2, topMargin - hgt - 5);

        g2d.setFont(oldFont);

        if (gridOn) {
            // vertical grid lines
            int gridSpacing = 50;
            if (yMax > 255) {
                gridSpacing = 100;
            }
            int loc = 0;
            if (backgroundColor == 0 || !spectrumPaletteMode) {
                g2d.setColor(Color.white);
            } else {
                g2d.setColor(Color.black);
            }
//            for (int i = yMin; i <= yMax; i++) {
//                if (i % gridSpacing == 0) {
//                    loc = (int) (leftMargin + (i - yMin) / ((double) yMax - yMin) * activeWidth);
//
//                    g2d.drawLine(loc, bottomY, loc, topMargin - 1);
//                }
//            }

            // horizontal grid lines
            if (xMax > 255) {
                gridSpacing = 100;
            }
//            for (int i = xMin; i <= xMax; i++) {
//                if (i % gridSpacing == 0) {
//                    loc = (int) (bottomY - (i - xMin) / ((double) xMax - xMin) * activeHeight);
//
//                    g2d.drawLine(leftMargin + 1, loc, rightX, loc);
//                }
//            }
        }

        if (plotPeaks) {
            if (!peaksFound) {
                findLocalPeaks();
            }
            g2d.setColor(Color.black);
            int crossHairSize = 4;
            int row, col;
            for (int j = 0; j < peaks.length; j++) {
                col = (int) (leftMargin + (double)peaks[j][0] / width * activeWidth);
                row = (int) (bottomY - (double)peaks[j][1] / height * activeHeight);
                g2d.drawLine(col - crossHairSize, row, col + crossHairSize, row);
                g2d.drawLine(col, row - crossHairSize, col, row + crossHairSize);
            }
        }

        if (drawPositionLine) {
            // draw a red vertical line at this position.
            g2d.setColor(Color.red);
            g2d.drawLine(posX, bottomY, posX, topMargin);
            g2d.setColor(Color.white);
            g2d.drawLine(posX + 1, bottomY, posX + 1, topMargin);

            // draw a red horizontal line at this position.
            g2d.setColor(Color.red);
            g2d.drawLine(leftMargin, posY, rightX, posY);
            g2d.setColor(Color.white);
            g2d.drawLine(leftMargin, posY + 1, rightX, posY + 1);

            // which bin is it and how many are in the bin?
            int x = (int) (((double) posX - leftMargin) / activeWidth * (width - 1));
            int y = (int) (((1 - ((double) posY - topMargin) / activeHeight) * (height - 1)));
            df = new DecimalFormat("0");
            int val1 = featureSpace[x][y];
            String val = df.format(val1);
            label = "x: " + (x + xMin) + " y: " + (y + yMin) + " Value: " + val;
            adv = metrics.stringWidth(label);
            g2d.setColor(Color.black);
            g2d.drawString(label, 10, getHeight() - hgt - 10);

        }
    }

    public boolean saveToImage(String fileName) {
        try {
            int width = (int) this.getWidth();
            int height = (int) this.getHeight();
            // TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
            // into integer pixels
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics ig = bi.createGraphics();
            drawPlot(ig);
            int i = fileName.lastIndexOf(".");
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
            if (!ImageIO.write(bi, extension, new File(fileName))) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page)
            throws PrinterException {
        if (page > 0) {
            return NO_SUCH_PAGE;
        }

        int i = pf.getOrientation();

        // get the size of the page
        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();
        double myWidth = this.getWidth();// - borderWidth * 2;
        double myHeight = this.getHeight();// - borderWidth * 2;
        double scaleX = pageWidth / myWidth;
        double scaleY = pageHeight / myHeight;
        double minScale = Math.min(scaleX, scaleY);

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        g2d.scale(minScale, minScale);

        drawPlot(g);

        return PAGE_EXISTS;
    }

    private boolean drawPositionLine = false;
    private int posX = 0;
    private int posY = 0;

    @Override
    public void mouseMoved(MouseEvent e) {
        // is the mouse over the chart area?
        int x = e.getX();
        int y = e.getY();
        int width = getWidth();
        int height = getHeight();
        if (x >= leftMargin && x <= (width - rightMargin)
                && y >= topMargin && y <= (height - bottomMargin)) {
            drawPositionLine = true;
            posX = x;
            posY = y;
        } else {
            drawPositionLine = false;
        }
        repaint();
    }

    private void createPopupMenus() {
        // menu
        myPopup = new JPopupMenu();

        JMenuItem mi = new JMenuItem("Save");
        mi.addActionListener(this);
        mi.setActionCommand("save");
        myPopup.add(mi);

        mi = new JMenuItem("Print");
        mi.addActionListener(this);
        mi.setActionCommand("print");
        myPopup.add(mi);

        myPopup.addSeparator();

        cmi = new JCheckBoxMenuItem("Hillshade Plot");
        cmi.addActionListener(this);
        cmi.setState(isHillshaded);
        cmi.setActionCommand("hillshade plot");
        myPopup.add(cmi);

        backgroundMi = new JMenuItem("Black Background");
        backgroundMi.addActionListener(this);
        backgroundMi.setActionCommand("changeBackground");
        myPopup.add(backgroundMi);

        paletteMi = new JMenuItem("Grey-scale Palette");
        paletteMi.addActionListener(this);
        paletteMi.setActionCommand("changePalette");
        myPopup.add(paletteMi);

        gridMi = new JMenuItem("Turn Off Grid");
        gridMi.addActionListener(this);
        gridMi.setActionCommand("grid");
        myPopup.add(gridMi);

        plotPeaksMi = new JCheckBoxMenuItem("Plot Peaks");
        plotPeaksMi.addActionListener(this);
        plotPeaksMi.setState(plotPeaks);
        plotPeaksMi.setActionCommand("plot peaks");
        myPopup.add(plotPeaksMi);

        myPopup.setOpaque(true);
        myPopup.setLightWeightPopupEnabled(true);

    }

    private void printPlot() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        //PageFormat pf = job.pageDialog(aset);
        job.setPrintable(this);
        boolean ok = job.printDialog(aset);
        if (ok) {
            try {
                job.print(aset);
            } catch (PrinterException ex) {
                //showFeedback("An error was encountered while printing." + ex);
                /* The job did not successfully complete */
            }
        }
    }

    private void savePlotAsImage() {
        // get the possible image name.
        String imageName = xAxisTitle + "_vs_" + yAxisTitle;

        // Ask the user to specify a file name for saving the histo.
        String pathSep = File.separator;
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //fc.setCurrentDirectory(new File(workingDirectory + pathSep + imageName + ".png"));
        fc.setAcceptAllFileFilterUsed(false);

        //File f = new File(workingDirectory + pathSep + imageName + ".png");
        //fc.setSelectedFile(f);
        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String[] extensions = ImageIO.getReaderFormatNames(); //{"PNG", "JPEG", "JPG"};
        String filterDescription = "Image Files (" + extensions[0];
        for (int i = 1; i < extensions.length; i++) {
            filterDescription += ", " + extensions[i];
        }
        filterDescription += ")";
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        fc.setFileFilter(eff);

        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            // see if file has an extension.
            if (file.toString().lastIndexOf(".") <= 0) {
                String fileName = file.toString() + ".png";
                file = new File(fileName);
            }

            String fileDirectory = file.getParentFile() + pathSep;
//            if (!fileDirectory.equals(workingDirectory)) {
//                workingDirectory = fileDirectory;
//            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "The file already exists.\n"
                        + "Would you like to overwrite it?",
                        "Whitebox GAT Message",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, //do not use a custom Icon
                        options, //the titles of buttons
                        options[0]); //default button title

                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            if (!saveToImage(file.toString())) {
//                showFeedback("An error occurred while saving the map to the image file.");
            }
        }

    }

    @Override
    public void mouseDragged(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    // ActionListener for events
    @Override
    public void actionPerformed(ActionEvent ae) {
        String actionCommand = ae.getActionCommand().toLowerCase();
        if (actionCommand.equals("save")) {
            savePlotAsImage();
        } else if (actionCommand.equals("print")) {
            printPlot();
        } else if (actionCommand.equals("hillshade plot")) {
            isHillshaded = !isHillshaded;
            refresh();
        } else if (actionCommand.equals("plot peaks")) {
            plotPeaks = !plotPeaks;
            refresh();
        } else if (actionCommand.equals("changebackground")) {
            if (backgroundMi.getText().equals("White Background")) {
                backgroundMi.setText("Black Background");
                backgroundColor = 255;
            } else {
                backgroundMi.setText("White Background");
                backgroundColor = 0;
            }
            refresh();
        } else if (actionCommand.equals("grid")) {
            if (gridOn) {
                gridOn = false;
                gridMi.setText("Turn On Grid");
            } else if (!gridOn) {
                gridOn = true;
                gridMi.setText("Turn Off Grid");
            }
            refresh();
        } else if (actionCommand.equals("changepalette")) {
            spectrumPaletteMode = !spectrumPaletteMode;
            if (spectrumPaletteMode) {
                paletteMi.setText("Grey-scale Palette");
                backgroundMi.setText("Black Background");
                backgroundColor = 255;
                backgroundMi.setEnabled(true);
                cmi.setEnabled(true);
            } else {
                paletteMi.setText("Spectrum Palette");
                isHillshaded = false;
                cmi.setState(false);
                cmi.setEnabled(false);
                backgroundMi.setText("White Background");
                backgroundColor = 0;
                backgroundMi.setEnabled(false);
            }
            refresh();
        }
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        if (me.getButton() == 3 || me.isPopupTrigger()) {
            myPopup.show((Component) me.getSource(), me.getX(), me.getY());
        }
    }

    @Override
    public void mousePressed(MouseEvent me) {

    }

    @Override
    public void mouseReleased(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseEntered(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseExited(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

}
