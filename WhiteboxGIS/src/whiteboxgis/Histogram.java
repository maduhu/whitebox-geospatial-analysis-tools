/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whiteboxgis;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.utilities.Parallel;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Histogram extends JPanel implements ActionListener, Printable, MouseMotionListener {

    private String headerFile = "";
    private String paletteFile = "";
    private WhiteboxRasterInfo wbInfo = null;
    private int[] paletteData = new int[2048];
    private int numPaletteEntries = 2048;
    private int numHistoEntries = 1;
    private double[] histo = new double[2048];
    private double fullestBinVal = 0;
    private int fullestBinVal2 = 0;
    private boolean histoCreated = false;
    private boolean cumulative = false;
    private double noData;
    private double minVal;
    private double maxVal;
    private double range;
    private String shortName;
    private long total;
    private int bottomMargin = 50;
    private int topMargin = 45;
    private int leftMargin = 90;
    private int rightMargin = 20;
    private boolean isIntegerData = false;

    // Constructors
    public Histogram() {
        setMouseMotionListener();
    }

    public Histogram(String headerFile) {
        setHeaderFile(headerFile);
        setMouseMotionListener();
    }

    // Setters and getters
    public final void setHeaderFile(String headerFile) {
        this.headerFile = headerFile;
        wbInfo = new WhiteboxRasterInfo(headerFile);
        shortName = wbInfo.getShortHeaderFile().replace(".dep", "");
        getPaletteFileName();
        readPalette();
    }

    public String getShortName() {
        return shortName;
    }

    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
        this.histoCreated = false;
        repaint();
    }

    // Methods
    private void setMouseMotionListener() {
        this.addMouseMotionListener(this);
    }

    public void refresh() {
        histoCreated = false;
        wbInfo = new WhiteboxRasterInfo(headerFile);
        getPaletteFileName();
        readPalette();
        repaint();
    }

    private void getPaletteFileName() {
        try {
            if (wbInfo != null) {
                String pathSep = File.separator;

                String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
                //getClass().getProtectionDomain().
                if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                    applicationDirectory = new File(applicationDirectory).getParent();
                } else {
                    // Add the path to the class files
                    applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                    // Step one level up as we are only interested in the
                    // directory containing the class files
                    applicationDirectory = new File(applicationDirectory).getParent();
                }
                String paletteDirectory = applicationDirectory + pathSep + "resources" + pathSep + "palettes" + pathSep;

                paletteFile = paletteDirectory + wbInfo.getPreferredPalette();
            } else {
                throw new Exception("File has not been set.");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void readPalette() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(paletteFile);
            if (!file.exists()) {
                return;
            }

            numPaletteEntries = (int) (file.length() / 4);

            buf = ByteBuffer.allocate(numPaletteEntries * 4);

            rIn = new RandomAccessFile(paletteFile, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(ByteOrder.LITTLE_ENDIAN);

            buf.rewind();
            IntBuffer ib = buf.asIntBuffer();
            paletteData = new int[numPaletteEntries];
            ib.get(paletteData);
            ib = null;

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }

    }

    int nCols;
    int numHistoEntriesLessOne;

    private void createHistogram() {
        try {
            if (wbInfo != null) {
                whitebox.geospatialfiles.WhiteboxRasterBase.DataType dataType;
                dataType = wbInfo.getDataType();
                if (((dataType == DataType.DOUBLE) || (dataType == DataType.FLOAT))
                        && wbInfo.doesDataContainFractionalParts()) {
                    numHistoEntries = numPaletteEntries;
                } else {
                    numHistoEntries = (int) (wbInfo.getDisplayMaximum() - wbInfo.getDisplayMinimum());
                    isIntegerData = true;
                }
                histo = new double[numHistoEntries];
                numHistoEntriesLessOne = numHistoEntries - 1;
                int nRows = wbInfo.getNumberRows();
                nCols = wbInfo.getNumberColumns();
                double[] data = null;
                noData = wbInfo.getNoDataValue();
                minVal = wbInfo.getDisplayMinimum();
                maxVal = wbInfo.getDisplayMaximum();
                range = maxVal - minVal;
                boolean tailed = false;
                if (wbInfo.getDisplayMaximum() < wbInfo.getMaximumValue() || wbInfo.getDisplayMinimum() > wbInfo.getMinimumValue()) {
                    tailed = true;
                }
                //double binSize = range / numPaletteEntries;
                double z = 0;
                //int bin = 0;

                Parallel.For(0, nRows, 1, new Parallel.LoopBody<Integer>() {

                    double[] data = null;
                    double z;
                    int bin = 0;

                    @Override
                    public void run(Integer row) {
                        //for (int row = 0; row < nRows; row++) {
                        data = wbInfo.getRowValues(row);
                        for (int col = 0; col < nCols; col++) {
                            if (data[col] != noData) {
                                z = data[col];
                                if (z < minVal) {
                                    z = minVal;
                                }
                                if (z > maxVal) {
                                    z = maxVal;
                                }

                                bin = (int) ((z - minVal) / range * numHistoEntriesLessOne);
                                histo[bin]++;
                            }
                        }
                    }
                });

                total = 0;
                fullestBinVal2 = 0;
                for (int a = 0; a < numHistoEntries; a++) {
                    total += histo[a];
                    if (histo[a] > fullestBinVal2) {
                        fullestBinVal2 = (int) histo[a];
                    }
                }

                if (cumulative) {
                    histo[0] = histo[0] / total;
                    fullestBinVal = 0;
                    for (int a = 1; a < numHistoEntries; a++) {
                        histo[a] = histo[a] / total + histo[a - 1];
                        if (histo[a] > fullestBinVal) {
                            fullestBinVal = histo[a];
                        }
                    }
                } else {
                    fullestBinVal = 0;
                    if (!tailed) {
                        for (int a = 0; a < numHistoEntries; a++) {
                            histo[a] = histo[a] / total;
                            if (histo[a] > fullestBinVal) {
                                fullestBinVal = histo[a];
                            }
                        }
                    } else {
                        for (int a = 0; a < numHistoEntries; a++) {
                            histo[a] = histo[a] / total;
                            if (histo[a] > fullestBinVal && a > 0 && a < numHistoEntries - 1) {
                                fullestBinVal = histo[a];
                            }
                        }
                    }
                }

                //wbInfo.close();
                histoCreated = true;
            } else {
                throw new Exception("File has not been set.");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void paint(Graphics g) {
        drawHistogram(g);
    }

    private void drawHistogram(Graphics g) {
        if (!histoCreated) {
            createHistogram();
        }

        if (!cumulative) {
            leftMargin = 90;
        } else {
            leftMargin = 70;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        double activeWidth = getWidth() - leftMargin - rightMargin;
        double activeHeight = getHeight() - topMargin - bottomMargin;
        int bottomY = getHeight() - bottomMargin;
        int rightX = getWidth() - rightMargin;
        int rectLeft, rectWidth, rectTop, rectHeight;

        double binWidth = activeWidth / (numHistoEntries + 1);
        rectWidth = (int) (Math.round(binWidth));
        if (rectWidth < 1) {
            rectWidth = 1;
        }
        int paletteEntry = 0;
        double paletteScale = numPaletteEntries / numHistoEntries;
        for (int a = 0; a < numHistoEntries; a++) {
            paletteEntry = (int) (a * paletteScale);
            Color newColour = new Color(paletteData[paletteEntry]);
            g2d.setColor(newColour);
            rectLeft = (int) (leftMargin + a * binWidth);
            if (histo[a] > fullestBinVal) { histo[a] = fullestBinVal; } 
            rectTop = (int) (topMargin + (double) (fullestBinVal - histo[a]) / fullestBinVal * activeHeight); //(int)(bottomMargin);
            rectHeight = bottomY - rectTop;
            g2d.fillRect(rectLeft, rectTop, rectWidth, rectHeight);
        }

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
        label = df.format(minVal);
        g2d.drawString(label, leftMargin, bottomY + hgt + 4);
        label = df.format(maxVal);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, rightX - adv, bottomY + hgt + 4);
        label = "Value";
        adv = metrics.stringWidth(label);
        int xAxisMidPoint = (int) (leftMargin + activeWidth / 2);
        g2d.drawString(label, xAxisMidPoint - adv / 2, bottomY + 2 * hgt + 4);

        // y-axis labels
        // rotate the font
        Font oldFont = g.getFont();
        //Font f = oldFont.deriveFont(AffineTransform.getRotateInstance(-Math.PI / 2.0));
        //g2d.setFont(f);

        int yAxisMidPoint = (int) (topMargin + activeHeight / 2);
        int offset;
        if (!cumulative) {
            label = "Frequency Prob.";
            offset = metrics.stringWidth("0.0000") + 12 + hgt;
        } else {
            label = "Cumulative Prob.";
            offset = metrics.stringWidth("0.0") + 12 + hgt;
        }
        adv = metrics.stringWidth(label);
        //g2d.drawString(label, leftMargin - offset, yAxisMidPoint + adv / 2);

        double xr = leftMargin - offset;
        double yr = yAxisMidPoint + adv / 2;
        g2d.translate(xr, yr);
        g2d.rotate(-Math.PI / 2.0, 0, 0);
        g2d.drawString(label, 0, 0);
        g2d.rotate(Math.PI / 2);
        g2d.translate(-xr, -yr);

        // replace the rotated font.
        //g2d.setFont(oldFont);
        if (!cumulative) {
            df = new DecimalFormat("0.0000");
        } else {
            df = new DecimalFormat("0.0");
        }
        label = df.format(0.000);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, leftMargin - adv - 12, bottomY + hgt / 2);
        label = df.format(fullestBinVal);
        adv = metrics.stringWidth(label);
        g2d.drawString(label, leftMargin - adv - 12, topMargin + hgt / 2);

        // title
        // bold font
        oldFont = g.getFont();
        font = font = new Font("SanSerif", Font.BOLD, 12);
        g2d.setFont(font);

        label = "Histogram: " + shortName;
        adv = metrics.stringWidth(label);
        g2d.drawString(label, xAxisMidPoint - adv / 2, topMargin - hgt - 5);

        g2d.setFont(oldFont);

        if (drawPositionLine) {
            // draw a red vertical line at this position.
            g2d.setColor(Color.red);
            g2d.drawLine(posX, bottomY, posX, topMargin);
            g2d.setColor(Color.white);
            g2d.drawLine(posX + 1, bottomY, posX + 1, topMargin);

            // which histo bin is it and how many are in the bin?
            int binNum = (int) (((double) posX - leftMargin) / activeWidth * (numHistoEntries - 1));
            df = new DecimalFormat("0.000");
            double val1 = minVal + ((double) posX - leftMargin) / activeWidth * (maxVal - minVal);
            if (isIntegerData) {
                df = new DecimalFormat("0");
                val1 = (int) val1;
            }
            String val = df.format(val1);
            //df = new DecimalFormat("#,###,###");
            df = new DecimalFormat("0.0000");
//            if (!cumulative) {
//                label = "Value: " + val + " Freq: " + df.format((histo[binNum] * fullestBinVal2));
//            } else {
            label = "Value: " + val + " Freq: " + df.format((histo[binNum]));
//            }
            adv = metrics.stringWidth(label);
            g2d.setColor(Color.black);
            g2d.drawString(label, 5, hgt + 5);

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
            drawHistogram(ig);
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

        drawHistogram(g);

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

    @Override
    public void mouseDragged(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // ActionListener for events
    @Override
    public void actionPerformed(ActionEvent ae) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//    // this is for debugging purposes.
//    public static void main(String[] args) {
//        JFrame frame = new JFrame();
//        String file = "/Users/johnlindsay/Documents/Data/DEM filled.dep";
//        //String file = "/Users/johnlindsay/Documents/Data/Picton Data/picton intensity_HistoEqual.dep";
//        //String file = "/Users/johnlindsay/Documents/Data/Picton Data/picton intensity.dep";
//        Histogram histo = new Histogram(file);
//        Container contentPane = frame.getContentPane();
//        contentPane.add(histo, BorderLayout.CENTER);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setPreferredSize(new Dimension(600, 400));
//        frame.pack();
//        frame.setVisible(true);
//        
//    }
}
