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
//import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PaletteImage extends JPanel {
    public final static int VERTICAL_ORIENTATION = 0;
    public final static int HORIZONTAL_ORIENTATION = 1;
    
    private int[] reversePaletteData = null;
    private int[] paletteData = null;
    private int numPaletteEntries = 0;
    private boolean isReversed = false;
    private int orientation = VERTICAL_ORIENTATION;
    
    private String paletteFile;
    
    public PaletteImage() {
        
    }
    
    public PaletteImage(int width, int height, String paletteFile, boolean isReversed, int orientation) {
        this.setMaximumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));
        this.paletteFile = paletteFile;
        if (!paletteFile.contains("rgb.pal")) {
            readPalette();
        } else {
            numPaletteEntries = 256;
            paletteData = new int[numPaletteEntries];
        }
        reversePaletteData = new int[paletteData.length];
        for (int i = 0; i < paletteData.length; i++) {
            reversePaletteData[i] = paletteData[paletteData.length - 1 - i];
        }
        this.isReversed = isReversed;
        if (orientation > 1 || orientation < 0) { orientation = 0; }
        this.orientation = orientation;
    }
    
    public void initialize(int width, int height, String paletteFile, boolean isReversed, int orientation) {
        this.setMaximumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));
        this.paletteFile = paletteFile;
        if (!paletteFile.contains("rgb.pal")) {
            readPalette();
        } else {
            numPaletteEntries = 256;
            paletteData = new int[numPaletteEntries];
        }
        reversePaletteData = new int[paletteData.length];
        for (int i = 0; i < paletteData.length; i++) {
            reversePaletteData[i] = paletteData[paletteData.length - 1 - i];
        }
        this.isReversed = isReversed;
        if (orientation > 1 || orientation < 0) { orientation = 0; }
        this.orientation = orientation;
    }
    
    public String getPaletteFile() {
        return paletteFile;
    }
    
    public void setPaletteIsReversed(boolean value) {
        this.isReversed = value;
        this.repaint();
    }
    
    private double gamma = 1.0;
    public double getNonlinearity() {
        return gamma;
    }
    
    public void setNonlinearity(double value) {
        gamma = value;
        this.repaint();
    }
    
    private boolean categorical = false;
    private double minValue = 0;
    private double maxValue = 0;
    public void isCategorical(boolean value, double minValue, double maxValue) {
        categorical = value;
        if (categorical) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        } else {
            this.minValue = -1;
            this.maxValue = -1;
        }
        //(int) (value - minValue) % numPaletteEntries;
    }
    
    public boolean isSelected = false;
    
    @Override
    public void paint (Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D)g;
            if (numPaletteEntries > 50) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
            if (paletteFile != null && (paletteFile.toLowerCase().contains("qual") || paletteFile.toLowerCase().contains("categorical"))) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }            
            int width = getWidth();
            int height = getHeight();

            Image image = null;
            int numDisplayedPaletteEntries = 0;
            if (!categorical) {
                numDisplayedPaletteEntries = paletteData.length;
            } else {
                numDisplayedPaletteEntries = (int) (
                        Math.floor((maxValue - minValue + 1) / numPaletteEntries) + (maxValue - minValue + 1) % numPaletteEntries);
            }
            int[] imageData = new int[numDisplayedPaletteEntries];
            int i, j;
            int numPaletteEntriesLessOne = numPaletteEntries - 1;
            if (!isReversed && orientation == VERTICAL_ORIENTATION) {
                if (!categorical) {
                    for (i = 0; i < numPaletteEntries; i++) {
                        j = (int) ((Math.pow((1 - ((double) (i) / numPaletteEntriesLessOne)), gamma)) * numPaletteEntriesLessOne);
                        imageData[i] = paletteData[j];
                    }
                } else { // it is categorical
                    for (i = 0; i < numDisplayedPaletteEntries; i++) {
                        j = (int) (i % numPaletteEntries);
                        imageData[numDisplayedPaletteEntries - 1 - i] = paletteData[j];
                    }
                }
                image = createImage(new MemoryImageSource(1, numDisplayedPaletteEntries, imageData, 0, 1));
            } else if (!isReversed && orientation == HORIZONTAL_ORIENTATION) {
                for (i = 0; i < numPaletteEntries; i++) {
                    j = (int)((Math.pow(((double)(i) / numPaletteEntriesLessOne), gamma)) * numPaletteEntriesLessOne);
                    imageData[i] = paletteData[j];
                }
                image = createImage(new MemoryImageSource(numPaletteEntries, 1, imageData, 0, 1));
            } else if (isReversed && orientation == VERTICAL_ORIENTATION) {
                if (!categorical) {
                    for (i = 0; i < numPaletteEntries; i++) {
                        j = (int) ((Math.pow(((double) (i) / numPaletteEntriesLessOne), gamma)) * numPaletteEntriesLessOne);
                        imageData[i] = paletteData[j];
                    }
                } else { // it is categorical
                    for (i = 0; i < numDisplayedPaletteEntries; i++) {
                        j = (int) (numPaletteEntries - i % numPaletteEntries - 1);
                        imageData[numDisplayedPaletteEntries - 1 - i] = paletteData[j];
                    }
                }
                image = createImage(new MemoryImageSource(1, numDisplayedPaletteEntries, imageData, 0, 1));
            } else if (isReversed && orientation == HORIZONTAL_ORIENTATION) {
                for (i = 0; i < numPaletteEntries; i++) {
                    j = (int)((Math.pow((1 - ((double)(i) / numPaletteEntriesLessOne)), gamma)) * numPaletteEntriesLessOne);
                    imageData[i] = paletteData[j];
                }
                image = createImage(new MemoryImageSource(imageData.length, 1, imageData, 0, 1));
            }
            
            g.drawImage(image, 0, 0, width, height, this);

            if (!isSelected) {
                g.setColor(Color.black);
                g.drawRect(0, 0, width - 1, height - 1);
            } else {
                g.setColor(Color.white);
                g.drawRect(1, 1, width - 3, height - 3);
                g.setColor(Color.red);
                g.drawRect(0, 0, width - 1, height - 1);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
    
    private void readPalette() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        int i;
        try {
            if (paletteFile != null) {
                // see if the file exists, if not, set it to the default palette.
                File file = new File(paletteFile);
                
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

            }
            
        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try{ rIn.close(); } catch (Exception e){}
            }
        }
        
    }
    
}
