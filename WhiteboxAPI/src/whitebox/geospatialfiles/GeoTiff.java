/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package whitebox.geospatialfiles;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Low level read/write geotiff files.
 *
 * @author John Caron
 * @author Yuan Ho
 * @author John Lindsay
 * @see GeotiffWriter
 */
public class GeoTiff {

    private String filename;
    private RandomAccessFile file;
    private FileChannel channel;
    private List<IFDEntry> tags = new ArrayList<>();
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private boolean readonly;
    private boolean showBytes = false, debugRead = false, debugReadGeoKey = false;
    private boolean showHeaderBytes = false;
    
    /**
     * Constructor. Does not open or create the file.
     * @param filename pathname of file
     */
    public GeoTiff(String filename) {
        this.filename = filename;
    }

    /**
     * Close the Geotiff file.
     * @throws java.io.IOException on io error
     */
    public void close() throws IOException {
        if (channel != null) {
            if (!readonly) {
                channel.force(true);
                channel.truncate(nextOverflowData);
            }
            channel.close();
        }
        if (file != null) {
            file.close();
        }
    }
    /////////////////////////////////////////////////////////////////////////////
    // writing
    private int headerSize = 8;
    private int firstIFD = 0;
    private int lastIFD = 0;
    private int startOverflowData = 0;
    private int nextOverflowData = 0;

    void addTag(IFDEntry ifd) {
        tags.add(ifd);
    }

    void deleteTag(IFDEntry ifd) {
        tags.remove(ifd);
    }

    void setTransform(double xStart, double yStart, double xInc, double yInc) {
        // tie the raster 0, 0 to xStart, yStart
        addTag(new IFDEntry(Tag.ModelTiepointTag, FieldType.DOUBLE).setValue(
                new double[]{0.0, 0.0, 0.0, xStart, yStart, 0.0}));

        // define the "affine transformation" : requires grid to be regular (!)
        addTag(new IFDEntry(Tag.ModelPixelScaleTag, FieldType.DOUBLE).setValue(
                new double[]{xInc, yInc, 0.0}));
    }
    private List<GeoKey> geokeys = new ArrayList<>();

    void addGeoKey(GeoKey geokey) {
        geokeys.add(geokey);
    }

    private void writeGeoKeys() {
        if (geokeys.isEmpty()) {
            return;
        }

        // count extras
        int extra_chars = 0;
        int extra_ints = 0;
        int extra_doubles = 0;
        for (GeoKey geokey : geokeys) {
            if (geokey.isDouble) {
                extra_doubles += geokey.count();
            } else if (geokey.isString) {
                extra_chars += geokey.valueString().length() + 1;
            } else if (geokey.count() > 1) {
                extra_ints += geokey.count();
            }
        }

        int n = (geokeys.size() + 1) * 4;
        int[] values = new int[n + extra_ints];
        double[] dvalues = new double[extra_doubles];
        char[] cvalues = new char[extra_chars];
        int icounter = n;
        int dcounter = 0;
        int ccounter = 0;

        values[0] = 1;
        values[1] = 1;
        values[2] = 0;
        values[3] = geokeys.size();
        int count = 4;
        for (GeoKey geokey : geokeys) {
            values[count++] = geokey.tagCode();

            if (geokey.isDouble) {
                values[count++] = Tag.GeoDoubleParamsTag.getCode(); // extra double values here
                values[count++] = geokey.count();
                values[count++] = dcounter;
                for (int k = 0; k < geokey.count(); k++) {
                    dvalues[dcounter++] = geokey.valueD(k);
                }

            } else if (geokey.isString) {
                String s = geokey.valueString();
                values[count++] = Tag.GeoAsciiParamsTag.getCode(); // extra double values here
                values[count++] = s.length(); // dont include trailing 0 in the count
                values[count++] = ccounter;
                for (int k = 0; k < s.length(); k++) {
                    cvalues[ccounter++] = s.charAt(k);
                }
                cvalues[ccounter++] = (char) 0;

            } else if (geokey.count() > 1) { // more than one int value
                values[count++] = Tag.GeoKeyDirectoryTag.getCode(); // extra int values here
                values[count++] = geokey.count();
                values[count++] = icounter;
                for (int k = 0; k < geokey.count(); k++) {
                    values[icounter++] = geokey.value(k);
                }

            } else { // normal case of one int value
                values[count++] = 0;
                values[count++] = 1;
                values[count++] = geokey.value();
            }
        } // loop over geokeys

        addTag(new IFDEntry(Tag.GeoKeyDirectoryTag, FieldType.SHORT).setValue(values));
        if (extra_doubles > 0) {
            addTag(new IFDEntry(Tag.GeoDoubleParamsTag, FieldType.DOUBLE).setValue(dvalues));
        }
        if (extra_chars > 0) {
            addTag(new IFDEntry(Tag.GeoAsciiParamsTag, FieldType.ASCII).setValue(new String(cvalues)));
        }
    }

    int writeData(byte[] data, int imageNumber) throws IOException {
        if (file == null) {
            init();
        }

        if (imageNumber == 1) {
            channel.position(headerSize);
        } else {
            channel.position(nextOverflowData);
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        channel.write(buffer);

        if (imageNumber == 1) {
            firstIFD = headerSize + data.length;
        } else {
            firstIFD = data.length + nextOverflowData;
        }

        return nextOverflowData;
    }

    int writeData(float[] data, int imageNumber) throws IOException {
        if (file == null) {
            init();
        }

        if (imageNumber == 1) {
            channel.position(headerSize);
        } else {
            channel.position(nextOverflowData);
        }

        // no way around making a copy
        ByteBuffer direct = ByteBuffer.allocateDirect(4 * data.length);
        FloatBuffer buffer = direct.asFloatBuffer();
        buffer.put(data);
        //buffer.flip();
        channel.write(direct);

        if (imageNumber == 1) {
            firstIFD = headerSize + 4 * data.length;
        } else {
            firstIFD = 4 * data.length + nextOverflowData;
        }

        return nextOverflowData;
    }

    void writeMetadata(int imageNumber) throws IOException {
        if (file == null) {
            init();
        }

        // geokeys all get added at once
        writeGeoKeys();

        // tags gotta be in order
        Collections.sort(tags);
        int start = 0;
        if (imageNumber == 1) {
            start = writeHeader(channel);
        } else {
            //now this is not the first image we need to fill the Offset of nextIFD
            channel.position(lastIFD);
            ByteBuffer buffer = ByteBuffer.allocate(4);
            if (debugRead) {
                System.out.println("position before writing nextIFD= " + channel.position() + " IFD is " + firstIFD);
            }
            buffer.putInt(firstIFD);
            buffer.flip();
            channel.write(buffer);
        }
        writeIFD(channel, firstIFD);
    }

    private int writeHeader(FileChannel channel) throws IOException {
        channel.position(0);

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put((byte) 'M');
        buffer.put((byte) 'M');
        buffer.putShort((short) 42);
        buffer.putInt(firstIFD);

        buffer.flip();
        channel.write(buffer);

        return firstIFD;
    }

    public void initTags() throws IOException {
        tags = new ArrayList<>();
        geokeys = new ArrayList<>();
    }

    private void init() throws IOException {
        file = new RandomAccessFile(filename, "rw");
        channel = file.getChannel();
        if (debugRead) {
            System.out.println("Opened file to write: '" + filename + "', size=" + channel.size());
        }
        readonly = false;
    }

    private void writeIFD(FileChannel channel, int start) throws IOException {
        channel.position(start);

        ByteBuffer buffer = ByteBuffer.allocate(2);
        int n = tags.size();
        buffer.putShort((short) n);
        buffer.flip();
        channel.write(buffer);

        start += 2;
        startOverflowData = start + 12 * tags.size() + 4;
        nextOverflowData = startOverflowData;

        for (IFDEntry elem : tags) {
            writeIFDEntry(channel, elem, start);
            start += 12;
        }
        // firstIFD = startOverflowData;
        // position to where the "next IFD" goes
        channel.position(startOverflowData - 4);
        lastIFD = startOverflowData - 4;
        if (debugRead) {
            System.out.println("pos before writing nextIFD= " + channel.position());
        }
        buffer = ByteBuffer.allocate(4);
        buffer.putInt(0);
        buffer.flip();
        channel.write(buffer);
    }

    private void writeIFDEntry(FileChannel channel, IFDEntry ifd, int start) throws IOException {
        channel.position(start);
        ByteBuffer buffer = ByteBuffer.allocate(12);

        buffer.putShort((short) ifd.tag.getCode());
        buffer.putShort((short) ifd.type.code);
        buffer.putInt(ifd.count);

        int size = ifd.count * ifd.type.size;
        if (size <= 4) {
            int done = writeValues(buffer, ifd);
            for (int k = 0; k < 4 - done; k++) // fill out to 4 bytes
            {
                buffer.put((byte) 0);
            }
            buffer.flip();
            channel.write(buffer);

        } else { // write offset
            buffer.putInt(nextOverflowData);
            buffer.flip();
            channel.write(buffer);
            // write data
            channel.position(nextOverflowData);
            //System.out.println(" write offset = "+ifd.tag.getName());
            ByteBuffer vbuffer = ByteBuffer.allocate(size);
            writeValues(vbuffer, ifd);
            vbuffer.flip();
            channel.write(vbuffer);
            nextOverflowData += size;
        }
    }

    private int writeValues(ByteBuffer buffer, IFDEntry ifd) {
        int done = 0;

        if (ifd.type == FieldType.ASCII) {
            return writeSValue(buffer, ifd);

        } else if (ifd.type == FieldType.RATIONAL) {
            for (int i = 0; i < ifd.count * 2; i++) {
                done += writeIntValue(buffer, ifd, ifd.value[i]);
            }

        } else if (ifd.type == FieldType.FLOAT) {
            for (int i = 0; i < ifd.count; i++) {
                buffer.putFloat((float) ifd.valueD[i]);
            }
            done += ifd.count * 4;

        } else if (ifd.type == FieldType.DOUBLE) {
            for (int i = 0; i < ifd.count; i++) {
                buffer.putDouble(ifd.valueD[i]);
            }
            done += ifd.count * 8;

        } else {
            for (int i = 0; i < ifd.count; i++) {
                done += writeIntValue(buffer, ifd, ifd.value[i]);
            }
        }

        return done;
    }

    private int writeIntValue(ByteBuffer buffer, IFDEntry ifd, int v) {
        switch (ifd.type.code) {
            case 1:
                buffer.put((byte) v);
                return 1;
            case 3:
                buffer.putShort((short) v);
                return 2;
            case 4:
                buffer.putInt(v);
                return 4;
            case 5:
                buffer.putInt(v);
                return 4;
        }
        return 0;
    }

    private int writeSValue(ByteBuffer buffer, IFDEntry ifd) {
        buffer.put(ifd.valueS.getBytes());
        int size = ifd.valueS.length();
        if ((size % 2) == 1) {
            size++;
        }
        return size;
    }

    /////////////////////////////////////////////////////////////////////////////
    // reading
    /**
     * Read the geotiff file, using the filename passed in the constructor.
     *
     * @throws IOException on read error
     */
    public void read() throws IOException {
        file = new RandomAccessFile(filename, "r");
        channel = file.getChannel();
        if (debugRead) {
            System.out.println("Opened file to read:'" + filename + "', size=" + channel.size());
        }
        readonly = true;

        int nextOffset = readHeader(channel);
        while (nextOffset > 0) {
            nextOffset = readIFD(channel, nextOffset);
            parseGeoInfo();
        }

        //parseGeoInfo();
    }

    private IFDEntry findTag(Tag tag) {
        if (tag == null) {
            return null;
        }
        for (IFDEntry ifd : tags) {
            if (ifd.tag == tag) {
                return ifd;
            }
        }
        return null;
    }

    public int getNumberRows() {
        return findTag(Tag.ImageLength).value[0];
    }

    public int getNumberColumns() {
        return findTag(Tag.ImageWidth).value[0];
    }

    public int getNumberBitsPerSample() {
        return findTag(Tag.BitsPerSample).value[0];
    }
    
    public int getSampleFormat() {
        return findTag(Tag.SampleFormat).value[0];
    }
    
//    public String getProjectionInfo() {
//        return findTag(Tag.GeoKeyDirectoryTag).value[0];
//    }
    
    public double getNorth() {
        IFDEntry ModelTiepointTag = findTag(Tag.ModelTiepointTag);
        IFDEntry ModelPixelScaleTag = findTag(Tag.ModelPixelScaleTag);
        if (ModelTiepointTag != null && ModelPixelScaleTag != null) {
            double[] modelTiepoint = ModelTiepointTag.valueD.clone();
            double[] modelPixelScale = ModelPixelScaleTag.valueD.clone();
            return modelTiepoint[4] + modelTiepoint[1] * modelPixelScale[1];
        } else {
            return (double)getNumberRows();
        }
    }
    
    public double getSouth() {
        IFDEntry ModelTiepointTag = findTag(Tag.ModelTiepointTag);
        IFDEntry ModelPixelScaleTag = findTag(Tag.ModelPixelScaleTag);
        if (ModelTiepointTag != null && ModelPixelScaleTag != null) {
            double[] modelTiepoint = ModelTiepointTag.valueD.clone();
            double[] modelPixelScale = ModelPixelScaleTag.valueD.clone();
            return modelTiepoint[4] - (getNumberRows() - modelTiepoint[1]) * modelPixelScale[1];
        } else {
            return 0;
        }
    }
    
    public double getWest() {
        IFDEntry ModelTiepointTag = findTag(Tag.ModelTiepointTag);
        IFDEntry ModelPixelScaleTag = findTag(Tag.ModelPixelScaleTag);
        if (ModelTiepointTag != null && ModelPixelScaleTag != null) {
            double[] modelTiepoint = ModelTiepointTag.valueD.clone();
            double[] modelPixelScale = ModelPixelScaleTag.valueD.clone();
            return modelTiepoint[3] - modelTiepoint[0] * modelPixelScale[0];
        } else {
            return 0;
        }
    }

    public double getEast() {
        IFDEntry ModelTiepointTag = findTag(Tag.ModelTiepointTag);
        IFDEntry ModelPixelScaleTag = findTag(Tag.ModelPixelScaleTag);
        if (ModelTiepointTag != null && ModelPixelScaleTag != null) {
            double[] modelTiepoint = ModelTiepointTag.valueD.clone();
            double[] modelPixelScale = ModelPixelScaleTag.valueD.clone();
            return modelTiepoint[3] + (getNumberColumns() - modelTiepoint[0]) * modelPixelScale[0];
        } else {
            return getNumberColumns();
        }
    }

    public double getNoData() {
        if (findTag(Tag.GDALNoData) != null) {
            if (findTag(Tag.GDALNoData).valueS != null) {
                return Double.parseDouble(findTag(Tag.GDALNoData).valueS);
            } else {
                return -32768;
            }
        } else {
            return -32768;
        }
    }
    
    public int getPhotometricInterpretation() {
        if (findTag(Tag.PhotometricInterpretation) != null) {
            return findTag(Tag.PhotometricInterpretation).value[0];
        } else {
            return -9999;
        }
    }

    public double[] getRowData(int row) {
        int nCols = getNumberColumns();
        double[] data = new double[nCols];
        int offset = 0;
        int size = 0;
        int compressionType = findTag(Tag.Compression).value[0];
        int[] bitsPerSample = findTag(Tag.BitsPerSample).value;
        int sampleFormat = 1; //findTag(Tag.SampleFormat).value[0];

        if (findTag(Tag.SampleFormat) != null) {
            sampleFormat = findTag(Tag.SampleFormat).value[0];
        }
        if (findTag(Tag.TileOffsets) != null) {
            return null; // can't handle this type of tiff.
        }
        
        if (getPhotometricInterpretation() != 2) {
            // how many rows per strip?
            int rowsPerStrip = findTag(Tag.RowsPerStrip).value[0];
            // which strip will contain row?
            int stripNum = (int) row / rowsPerStrip;

            int stripOffset = findTag(Tag.StripOffsets).value[stripNum];
            //int stripSize = findTag(Tag.StripByteCounts).value[0];
            offset = stripOffset + (row % rowsPerStrip * nCols * (bitsPerSample[0] / 8));
            size = nCols * (bitsPerSample[0] / 8); //stripSize;


            try {
                channel.position(offset);
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.order(byteOrder);

                if (compressionType == 1) {

                    channel.read(buffer);
                    buffer.flip();

                    if (sampleFormat == 1 && bitsPerSample[0] == 8) { // unsigned byte
                        byte b;
                        for (int i = 0; i < size; i++) {
                            b = buffer.get();
                            data[i] = (short) (0x000000FF & ((int) b));
                        }
                    } else if (sampleFormat == 2 && bitsPerSample[0] == 8) { // signed byte
                        byte b;
                        for (int i = 0; i < size; i++) {
                            b = buffer.get();
                            data[i] = b;
                        }
                    } else if (sampleFormat == 1 && bitsPerSample[0] == 16) { // unsigned 16-bit short
                        int b1;
                        int b2;
                        for (int i = 0; i < nCols; i++) {
                            b1 = (0x000000FF & ((int)buffer.get()));
                            b2 = (0x000000FF & ((int)buffer.get()));
                            data[i] = (b2 << 8 | b1);
                        }
                    } else if (sampleFormat == 2 && bitsPerSample[0] == 16) { // signed 16-bit short
                        ShortBuffer sb = buffer.asShortBuffer();
                        short[] sa = new short[nCols];
                        sb.get(sa);
                        sb = null;
                        for (int j = 0; j < nCols; j++) {
                            data[j] = sa[j];
                        }
                        sa = null;
                    } else if (sampleFormat == 1 && bitsPerSample[0] == 32) { // unsigned 32-bit int
                        int b1, b2, b3, b4;
                        for (int i = 0; i < nCols; i++) {
                            b1 = (0x000000FF & ((int)buffer.get()));
                            b2 = (0x000000FF & ((int)buffer.get()));
                            b3 = (0x000000FF & ((int)buffer.get()));
                            b4 = (0x000000FF & ((int)buffer.get()));
                            data[i]  = ((long) (b1 << 24 | b2 << 16 | b3 << 8 | b4)) & 0xFFFFFFFFL;
                        }
                        
                    } else if (sampleFormat == 2 && bitsPerSample[0] == 32) { // signed 32-bit int
                        IntBuffer ib = buffer.asIntBuffer();
                        int[] ia = new int[nCols];
                        ib.get(ia);
                        ib = null;
                        for (int j = 0; j < nCols; j++) {
                            data[j] = ia[j];
                        }
                        ia = null;
                    } else if (sampleFormat == 1 && bitsPerSample[0] == 64) { // unsigned 64-bit long
                        // I don't know what data type your could cast an unsigned long into. Perhaps a BigInteger.
                        return null;
                    } else if (sampleFormat == 2 && bitsPerSample[0] == 64) { // signed 64-bit long
                        LongBuffer lb = buffer.asLongBuffer();
                        long[] la = new long[nCols];
                        lb.get(la);
                        lb = null;
                        for (int j = 0; j < nCols; j++) {
                            data[j] = la[j];
                        }
                        la = null;
                    } else if (sampleFormat == 3 && bitsPerSample[0] == 32) { // 32-bit single-precision float
                        FloatBuffer fb = buffer.asFloatBuffer();
                        float[] fa = new float[nCols];
                        fb.get(fa);
                        fb = null;
                        for (int j = 0; j < nCols; j++) {
                            data[j] = fa[j];
                        }
                        fa = null;
                    } else if (sampleFormat == 3 && bitsPerSample[0] == 64) { // 64-bit double-precision float
                        DoubleBuffer db = buffer.asDoubleBuffer();
                        double[] da = new double[nCols];
                        db.get(da);
                        db = null;
                        data = da.clone();
                        da = null;
                    } else {
                        return null;
                    }
                } else if (compressionType == 32773) {
                    return null;
                } else {
                    return null;
                }

            } catch (IOException e) {
                // do nothing
            }
            return data;
        } else {
            // how many rows per strip?
            int rowsPerStrip = findTag(Tag.RowsPerStrip).value[0];
            // which strip will contain row?
            int stripNum = (int) row / rowsPerStrip;

            int stripOffset = findTag(Tag.StripOffsets).value[stripNum];
            //int stripSize = findTag(Tag.StripByteCounts).value[0];
            
            int totalBitsPerSample = 0;
            for (int a = 0; a < bitsPerSample.length; a++) {
                totalBitsPerSample += bitsPerSample[a];
            }
            offset = stripOffset + (row % rowsPerStrip * nCols * (totalBitsPerSample / 8));
            size = nCols * (totalBitsPerSample / 8); //stripSize;


            try {
                channel.position(offset);
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.order(byteOrder);

                if (compressionType == 1) {

                    channel.read(buffer);
                    buffer.flip();
                    
                    if (totalBitsPerSample == 24 && bitsPerSample.length == 3) {
                        int r, g, b;
                        for (int i = 0; i < nCols; i++) {
                            r = (0x000000FF & ((int)buffer.get()));
                            g = (0x000000FF & ((int)buffer.get()));
                            b = (0x000000FF & ((int)buffer.get()));
                            data[i] = (double) ((255 << 24) | (b << 16) | (g << 8) | r);
                        }
                    } else if (totalBitsPerSample == 32 && bitsPerSample.length == 4) {
                        int r, g, b, a;
                        for (int i = 0; i < nCols; i++) {
                            r = (0x000000FF & ((int)buffer.get()));
                            g = (0x000000FF & ((int)buffer.get()));
                            b = (0x000000FF & ((int)buffer.get()));
                            a = (0x000000FF & ((int)buffer.get()));
                            data[i] = (double) ((a << 24) | (b << 16) | (g << 8) | r);
                        }
                    } else {
                        return null;
                    }

                } else if (compressionType == 32773) {
                    return null;
                } else {
                    return null;
                }

            } catch (IOException e) {
                // do nothing
            }
            return data;
//            return null;
        }
    }

    private int readHeader(FileChannel channel) throws IOException {
        channel.position(0);

        ByteBuffer buffer = ByteBuffer.allocate(8);
        channel.read(buffer);
        buffer.flip();
        if (showHeaderBytes) {
            printBytes(System.out, "header", buffer, 4);
            buffer.rewind();
        }

        byte b = buffer.get();
        if (b == 73) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        }
        buffer.order(byteOrder);
        buffer.position(4);
        int firstIFD = buffer.getInt();
        if (debugRead) {
            System.out.println(" firstIFD == " + firstIFD);
        }

        return firstIFD;
    }
    
    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    private int readIFD(FileChannel channel, int start) throws IOException {
        channel.position(start);

        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(byteOrder);

        int n = channel.read(buffer);
        buffer.flip();
        if (showBytes) {
            printBytes(System.out, "IFD", buffer, 2);
            buffer.rewind();
        }
        short nentries = buffer.getShort();
        if (debugRead) {
            System.out.println(" nentries = " + nentries);
        }

        start += 2;
        for (int i = 0; i < nentries; i++) {
            IFDEntry ifd = readIFDEntry(channel, start);
            if (debugRead) {
                System.out.println(i + " == " + ifd);
            }

            tags.add(ifd);
            start += 12;
        }

        if (debugRead) {
            System.out.println(" looking for nextIFD at pos == " + channel.position() + " start = " + start);
        }
        channel.position(start);
        buffer = ByteBuffer.allocate(4);
        buffer.order(byteOrder);
        n = channel.read(buffer);
        buffer.flip();
        int nextIFD = buffer.getInt();
        if (debugRead) {
            System.out.println(" nextIFD == " + nextIFD);
        }
        return nextIFD;
    }

    private IFDEntry readIFDEntry(FileChannel channel, int start) throws IOException {
        if (debugRead) {
            System.out.println("readIFDEntry starting position to " + start);
        }

        channel.position(start);
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.order(byteOrder);
        channel.read(buffer);
        buffer.flip();
        if (showBytes) {
            printBytes(System.out, "IFDEntry bytes", buffer, 12);
        }

        IFDEntry ifd;
        buffer.position(0);
        int code = readUShortValue(buffer);
        Tag tag = Tag.get(code);
        if (tag == null) {
            tag = new Tag(code);
        }
        FieldType type = FieldType.get(readUShortValue(buffer));
        int count = buffer.getInt();

        ifd = new IFDEntry(tag, type, count);

        if (ifd.count * ifd.type.size <= 4) {
            readValues(buffer, ifd);
        } else {
            int offset = buffer.getInt();
            if (debugRead) {
                System.out.println("position to " + offset);
            }
            channel.position(offset);
            ByteBuffer vbuffer = ByteBuffer.allocate(ifd.count * ifd.type.size);
            vbuffer.order(byteOrder);
            channel.read(vbuffer);
            vbuffer.flip();
            readValues(vbuffer, ifd);
        }

        return ifd;
    }

    /*
     * Construct a GeoKey from an IFDEntry.
     * @param id GeoKey.Tag number
     * @param v value
     *
    GeoKey(int id, IFDEntry data, int vcount, int offset) {
    this.id = id;
    this.geoTag = GeoKey.Tag.get(id);
    this.count = vcount;
    
    if (data.type == FieldType.SHORT) {
    
    if (vcount == 1)
    geoValue = TagValue.get(geoTag, offset);
    else {
    value = new int[vcount];
    for (int i=0; i<vcount; i++)
    value[i] = data.value[offset + i];
    }
    
    if (geoValue == null) {
    if (data.type == FieldType.ASCII)
    valueS = data.valueS.substring( offset, offset+vcount);
    else {
    value = new int[vcount];
    for (int i=0; i<vcount; i++)
    value[i] = data.value[offset + i];
    }
    }
    }
    
     */
    private void readValues(ByteBuffer buffer, IFDEntry ifd) {

        if (ifd.type == FieldType.ASCII) {
            ifd.valueS = readSValue(buffer, ifd);

        } else if (ifd.type == FieldType.RATIONAL) {
            ifd.value = new int[ifd.count * 2];
            for (int i = 0; i < ifd.count * 2; i++) {
                ifd.value[i] = readIntValue(buffer, ifd);
            }

        } else if (ifd.type == FieldType.FLOAT) {
            ifd.valueD = new double[ifd.count];
            for (int i = 0; i < ifd.count; i++) {
                ifd.valueD[i] = (double) buffer.getFloat();
            }

        } else if (ifd.type == FieldType.DOUBLE) {
            ifd.valueD = new double[ifd.count];
            for (int i = 0; i < ifd.count; i++) {
                ifd.valueD[i] = buffer.getDouble();
            }

        } else {
            ifd.value = new int[ifd.count];
            for (int i = 0; i < ifd.count; i++) {
                ifd.value[i] = readIntValue(buffer, ifd);
            }
        }

    }

    private int readIntValue(ByteBuffer buffer, IFDEntry ifd) {
        switch (ifd.type.code) {
            case 1:
                return (int) buffer.get();
            case 2:
                return (int) buffer.get();
            case 3:
                return readUShortValue(buffer);
            case 4:
                return buffer.getInt();
            case 5:
                return buffer.getInt();
        }
        return 0;
    }

    private int readUShortValue(ByteBuffer buffer) {
        return buffer.getShort() & 0xffff;
    }

    private String readSValue(ByteBuffer buffer, IFDEntry ifd) {
        byte[] dst = new byte[ifd.count];
        buffer.get(dst);
        return new String(dst);
    }

    private void printBytes(PrintStream ps, String head, ByteBuffer buffer, int n) {
        ps.print(head + " == ");
        for (int i = 0; i < n; i++) {
            byte b = buffer.get();
            int ub = (b < 0) ? b + 256 : b;
            ps.print(ub + "(");
            ps.write(b);
            ps.print(") ");
        }
        ps.println();
    }

    /////////////////////////////////////////////////////////////////////////////
    // geotiff stuff
    private void parseGeoInfo() {
        IFDEntry keyDir = findTag(Tag.GeoKeyDirectoryTag);
        IFDEntry dparms = findTag(Tag.GeoDoubleParamsTag);
        IFDEntry aparams = findTag(Tag.GeoAsciiParamsTag);

        if (null == keyDir) {
            return;
        }

        int nkeys = keyDir.value[3];
        if (debugReadGeoKey) {
            System.out.println("parseGeoInfo nkeys = " + nkeys + " keyDir= " + keyDir);
        }
        int pos = 4;

        for (int i = 0; i < nkeys; i++) {
            int id = keyDir.value[pos++];
            int location = keyDir.value[pos++];
            int vcount = keyDir.value[pos++];
            int offset = keyDir.value[pos++];

            GeoKey.Tag tag = GeoKey.Tag.getOrMake(id);

            GeoKey key = null;
            if (location == 0) { // simple case
                key = new GeoKey(id, offset);

            } else { // more than one, or non short value
                IFDEntry data = findTag(Tag.get(location));
                if (data == null) {
                    System.out.println("********ERROR parseGeoInfo: cant find Tag code = " + location);
                } else if (data.tag == Tag.GeoDoubleParamsTag) { // double params
                    double[] dvalue = new double[vcount];
                    for (int k = 0; k < vcount; k++) {
                        dvalue[k] = data.valueD[offset + k];
                    }
                    key = new GeoKey(tag, dvalue);

                } else if (data.tag == Tag.GeoKeyDirectoryTag) { // int params
                    int[] value = new int[vcount];
                    for (int k = 0; k < vcount; k++) {
                        value[k] = data.value[offset + k];
                    }
                    key = new GeoKey(tag, value);

                } else if (data.tag == Tag.GeoAsciiParamsTag) { // ascii params
                    String value;
                    if ((offset + vcount) < data.valueS.length()) {
                        value = data.valueS.substring(offset, offset + vcount);
                    } else {
                        value = data.valueS.substring(offset, data.valueS.length() - 1);
                    }
                    key = new GeoKey(tag, value);
                }

            }

            if (key != null) {
                keyDir.addGeoKey(key);
                if (debugReadGeoKey) {
                    System.out.println(" yyy  add geokey=" + key);
                }
            }
        }

    }

    /**
     * Write the geotiff Tag information to out.
     */
    public void showInfo(PrintStream out) {
        out.println("Geotiff file= " + filename);
        for (int i = 0; i < tags.size(); i++) {
            IFDEntry ifd = tags.get(i);
            out.println(i + " IFDEntry == " + ifd);
        }
    }

    /**
     * Write the geotiff Tag information to a String.
     */
    public String[] showInfo() {
        String[] ret = new String[tags.size() + 1];
        ret[0] = "Geotiff file= " + filename;
        for (int i = 0; i < tags.size(); i++) {
            IFDEntry ifd = tags.get(i);
            ret[i + 1] = ifd.toString();
        }
        return ret;
    }
}