//package whiteboxgis;
//
//import whitebox.geospatialfiles.WhiteboxRaster;
////import javax.swing.*;
//import java.io.*;
///**
// *
// * @author johnlindsay
// */
//public class ImageFunctions {
//    int numPaletteEntries = 255;
//    public int[] getImagePixels(String headerFile, String paletteFile, int alpha) {
//        try {
//            //Image image = null;
//
//            File file = new File(headerFile);
//            if (!file.exists()) {
//                System.out.println("File not found.");
//            }
//
//            WhiteboxRaster source = new WhiteboxRaster(headerFile, "r");
//
//            //Image art = createImage(new MemoryImageSource(w, h, pixels, 0, w));
//
//            // read the palette file in and convert it to RGBa colours.
//
//            double displayMin = source.getDisplayMinimum();
//            double displayMax = source.getDisplayMaximum();
//            double range = displayMax - displayMin;
//            double value = 0;
//            int entryNum = 0;
//            int rows = source.getNumberRows();
//            int cols = source.getNumberColumns();
//            int numCells = rows * cols;
//            int[] pixels = new int[numCells];
//            int i = 0;
//            //int r, g, b, a;
//            int[] palette = readPalette(paletteFile, alpha);
//            for (int y = 0; y < rows; y++) {
//                for (int x = 0; x < cols; x++) {
//                    value = source.getValue(y, x);
//                    entryNum = (int)(((value - displayMin) / range) * numPaletteEntries);
//                    if (entryNum < 0) { entryNum = 0; }
//                    if (entryNum > numPaletteEntries) { entryNum = numPaletteEntries; }
//                    
//                    pixels[i] = palette[entryNum];
//                    i++;
//                    /*
//                    r = (x ^ y) & 0xff;
//                    g = (x * 2 ^ y * 2) & 0xff;
//                    b = (x * 4 ^ y * 4) & 0xff;
//                    pixels[i++] = (a << 24) | (r << 16) | (g << 8) | b;
//                     * 
//                     */
//                }
//            }
//
//            //image = createImage(new MemoryImageSource (cols, rows, pixels, 0, cols));
//            //gg.drawImage(art, 0, 0, this)
//            return pixels;
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            return null;
//        }
//    }
//    
//    public int getPixelARGB(int a, int r, int g, int b) {
//        return (a << 24) | (r << 16) | (g << 8) | b;
//    }
//    
//    public void printPixelARGB(int pixel) {
//        int alpha = (pixel >> 24) & 0xff;
//        int red = (pixel >> 16) & 0xff;
//        int green = (pixel >> 8) & 0xff;
//        int blue = (pixel) & 0xff;
//        System.out.println("argb: " + alpha + ", " + red + ", " + green + ", " + blue);
//    }
//    
//    public int[] readPalette(String paletteFile, int alpha) {
//        int[] palette = null;
//        RandomAccessFile raf = null;
//        String deliminator = "\t";
//        String line;
//                
//        int numPaletteLines = 0;
//        try {
//            
//            if (paletteFile != null) {
//                File file = new File(paletteFile);
//                raf = new RandomAccessFile(file, "r");
//                while ((line = raf.readLine()) != null) {
//                    numPaletteLines++;
//                }
//                numPaletteEntries = numPaletteLines - 1;
//                palette = new int[numPaletteLines];
//                raf.seek(0);
//                
//                String[] values;
//                int i = 0;
//                int r, g, b;
//                //Read File Line By Line
//                while ((line = raf.readLine()) != null) {
//                    values = line.split(deliminator);
//                    // make sure that the default deliminator is correct.
//                    if (!line.trim().equals("") && values.length < 3) {
//                        deliminator = " ";
//                        values = line.split(deliminator);
//                        if (!line.trim().equals("") && values.length == 1) {
//                            deliminator = ",";
//                            values = line.split(deliminator);
//                        }
//                    }
//                    if (values.length > 2) {
//                        r = Integer.parseInt(values[0]);
//                        g = Integer.parseInt(values[1]);
//                        b = Integer.parseInt(values[2]);
//                        palette[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
//                        i++;
//                    }
//                }
//                raf.close();
//            }
//
//        } catch (java.io.IOException e) {
//            System.err.println("Error: " + e.getMessage());
//        } catch (Exception e) { //Catch exception if any
//            System.err.println("Error: " + e.getMessage());
//        } finally {
//            try {
//                if (raf != null) {
//                    raf.close();
//                }
//            } catch (java.io.IOException ex) {
//            }
//
//        }
//
//        
//        return palette;
//    }
//}
