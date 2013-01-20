/*
 * Copyright (C) 2013 johnlindsay
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
package whitebox.geospatialfiles.shapefile.attributes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import whitebox.utilities.FileUtilities;

/**
 *
 * @author johnlindsay
 */
public class AttributeTable {
    private String fileName;
    private boolean isDirty = false;
    private boolean initialized = false;
    protected String characterSetName = "8859_1";
    protected final int END_OF_DATA = 0x1A;

    /** 
     * Used to create an AttributeTable object when the DBF file already exists.
     * @param fileName  String containing the full file name and directory.
     */
    public AttributeTable(String fileName) {
        this.signature = SIG_DBASE_III;
        this.terminator1 = 0x0D;
        
        this.fileName = fileName;
        try {
            initialize();
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Used to create an AttributeTable object when the DBF file does not already exist.
     * A new DBF file will be created and initialized with the specified fields but it will
     * not contain any records.
     * @param fileName  String containing the full file name and directory.
     * @param fields Array of DBFField type.
     */
    public AttributeTable(String fileName, DBFField[] fields) {
        this.signature = SIG_DBASE_III;
        this.terminator1 = 0x0D;
        
        this.fileName = fileName;
        try {
            DBFWriter writer = new DBFWriter(new File(fileName));
            writer.setFields(fields);
            writer.write();
            //setFields(fields);
            initialize();
        } catch (Exception e) {
            
        }
    }
    
    // properties
    public String getFileName() {
        return fileName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getCurrentRecord() {
        return currentRecord;
    }

    public void setCurrentRecord(int currentRecord) {
        this.currentRecord = currentRecord;
    }
    
    /* 
    If the library is used in a non-latin environment use this method to set 
    corresponding character set. More information: 
    http://www.iana.org/assignments/character-sets
    Also see the documentation of the class java.nio.charset.Charset
     */
    public String getCharactersetName() {

        return this.characterSetName;
    }

    public void setCharactersetName(String characterSetName) {

        this.characterSetName = characterSetName;
    }
    
    public int getNumberOfRecords() {
        return numberOfRecords;
    }
    
    
    
    // public methods

    /**
    Returns the asked Field. In case of an invalid index,
    it returns a ArrayIndexOutofboundsException.
    
    @param index. Index of the field. Index of the first field is zero.
     */
    public DBFField getField(int index)
            throws DBFException {

        if (!initialized) {

            throw new DBFException("Source is not open");
        }

        return this.fieldArray[index];
    }
    
    /**
     * Retrieves all fields in this database.
     * @return DBFField array
     * @throws DBFException 
     */
    public DBFField[] getAllFields() throws DBFException {
        if (!initialized) {

            throw new DBFException("Source is not open");
        }

        return this.fieldArray;
    }

    int fieldCount;
    /**
    Returns the number of field in the DBF.
     */
    public int getFieldCount() throws DBFException {

        if (!initialized) {
            throw new DBFException("Source is not open");
        }

        if (this.fieldArray != null) {
            fieldCount = this.fieldArray.length;
            return fieldCount;
        }

        return -1;
    }
    
    /**
     * Returns a String array of fields.
     * @return String array
     */
    public String[] getAttributeTableFieldNames() {
        try {
            int numberOfFields = this.getFieldCount();
            String[] ret = new String[numberOfFields];
            for (int i = 0; i < numberOfFields; i++) {

                DBFField field = this.getField(i);

                ret[i] = field.getName();
            }

            return ret;
        } catch (DBFException dbfe) {
            System.out.println(dbfe);
            return null;
        } 
    }
    
    /**
     * Sets fields.
     */
    public final void setFields(DBFField[] fields)
            throws DBFException {

        if (this.fieldArray != null) {

            throw new DBFException("Fields has already been set");
        }

        if (fields == null || fields.length == 0) {

            throw new DBFException("Should have at least one field");
        }

        for (int i = 0; i < fields.length; i++) {

            if (fields[i] == null) {

                throw new DBFException("Field " + (i + 1) + " is null");
            }
        }

        this.fieldArray = fields;

        try {
            RandomAccessFile raf = new RandomAccessFile(this.fileName, "rw");
            writeHeader(raf);
            raf.close();
        } catch (IOException e) {

            throw new DBFException("Error accesing file");
        }
    }
    
    /**
     * Appends a new field at the end of the attribute table. Notice that 
     * the dbf file must already exist.
     * @param field the DBFField to append.
     * @throws DBFException 
     */
    public void addField(DBFField field) throws DBFException {
        try {
            if (this.fileName.isEmpty()) {
                throw new DBFException("DBF file name not specified");
            }
            if (!(new File(this.fileName).exists())) {
                throw new DBFException("DBF file does not exist");
            }
            // create a temporary file to house the new dbf
            String fileNameCopy = this.fileName.replace(".dbf", "_copy.dbf");
            if (new File(fileNameCopy).exists()) {
                new File(fileNameCopy).delete();
            }
            DBFField[] outFields = new DBFField[this.fieldCount + 1];
            DBFField[] inFields = getAllFields();
            System.arraycopy(inFields, 0, outFields, 0, this.fieldCount);
            
            outFields[this.fieldCount] = field;
            AttributeTable newTable = new AttributeTable(fileNameCopy, outFields); // used to set up the dbf copy

            //DBFReader reader = new DBFReader(fileName);
            //DBFWriter writer = new DBFWriter(fileNameCopy);
            for (int a = 0; a < this.numberOfRecords; a++) {
                Object[] inRec = getRecord(a);
                Object[] outRec = new Object[this.fieldCount + 1];
                System.arraycopy(inRec, 0, outRec, 0, this.fieldCount);
                newTable.addRecord(outRec);
            }
            
            newTable.write();
            
            new File(this.fileName).delete();
            FileUtilities.copyFile(new File(fileNameCopy), new File(this.fileName));
            new File(fileNameCopy).delete();
            
            initialize();
                    
        } catch (Exception e) {
            throw new DBFException(e.getMessage());
        }
    }
    
    public void addField(DBFField field, int insertAfter) {
        
    }
    
    public void deleteField(int fieldNum) {
        
    }
    
    public void deleteField(String fieldName) {
        
    }
    
    private int currentRecord = -1;
    /**
     * Reads the returns the <i>n</i>th row in the DBF stream.
     * @param n Record number.
     * @return The <i>n</i>th record as an Object array. Types of the elements 
      these arrays follow the convention mentioned in the class description.
     * @throws DBFException 
     */
    public Object[] getRecord(int n) throws DBFException  {
        currentRecord = n;

        if (currentRecord < 0) {
            throw new DBFException("Record number is out of bounds.");
        }
        if (currentRecord >= this.numberOfRecords) {
            return null;
        }

        Object recordObjects[] = new Object[this.fieldArray.length];

        RandomAccessFile rIn = null;
        ByteBuffer buf;
        FileChannel inChannel = null;

        try {
            buf = ByteBuffer.allocate(this.recordLength);

            rIn = new RandomAccessFile(this.fileName, "r");

            inChannel = rIn.getChannel();

            int pos = (32 + (32 * this.fieldArray.length)) + 1 + n * this.recordLength;
            inChannel.position(pos);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            if (buf.get() == END_OF_DATA) {
                return null;
            } // record has been deleted

            for (int i = 0; i < this.fieldArray.length; i++) {

                switch (this.fieldArray[i].getDataType()) {

                    case 'C':

                        byte b_array[] = new byte[this.fieldArray[i].getFieldLength()];
                        //dataInputStream.read(b_array);
                        buf.get(b_array);
                        recordObjects[i] = new String(b_array, characterSetName);
                        break;

                    case 'D':

                        byte t_byte_year[] = new byte[4];
                        //dataInputStream.read(t_byte_year);
                        buf.get(t_byte_year);

                        byte t_byte_month[] = new byte[2];
                        //dataInputStream.read(t_byte_month);
                        buf.get(t_byte_month);

                        byte t_byte_day[] = new byte[2];
                        //dataInputStream.read(t_byte_day);
                        buf.get(t_byte_day);

                        try {

                            GregorianCalendar calendar = new GregorianCalendar(
                                    Integer.parseInt(new String(t_byte_year)),
                                    Integer.parseInt(new String(t_byte_month)) - 1,
                                    Integer.parseInt(new String(t_byte_day)));

                            recordObjects[i] = calendar.getTime();
                        } catch (NumberFormatException e) {
                            /*
                             * this field may be empty or may have improper
                             * value set
                             */
                            recordObjects[i] = null;
                        }

                        break;

                    case 'F':

                        try {

                            byte t_float[] = new byte[this.fieldArray[i].getFieldLength()];
                            //dataInputStream.read(t_float);
                            buf.get(t_float);

                            t_float = Utils.trimLeftSpaces(t_float);
                            if (t_float.length > 0 && !Utils.contains(t_float, (byte) '?')) {

                                recordObjects[i] = new Float(new String(t_float));
                            } else {

                                recordObjects[i] = null;
                            }
                        } catch (NumberFormatException e) {

                            throw new DBFException("Failed to parse Float: " + e.getMessage());
                        }

                        break;

                    case 'N':

                        try {

                            byte t_numeric[] = new byte[this.fieldArray[i].getFieldLength()];
                            //dataInputStream.read(t_numeric);
                            buf.get(t_numeric);

                            t_numeric = Utils.trimLeftSpaces(t_numeric);

                            if (t_numeric.length > 0 && !Utils.contains(t_numeric, (byte) '?')) {

                                recordObjects[i] = new Double(new String(t_numeric));
                            } else {

                                recordObjects[i] = null;
                            }
                        } catch (NumberFormatException e) {

                            throw new DBFException("Failed to parse Number: " + e.getMessage());
                        }

                        break;

                    case 'L':

                        byte t_logical = buf.get();
                        if (t_logical == 'Y' || t_logical == 't' || t_logical == 'T' || t_logical == 't') {

                            recordObjects[i] = Boolean.TRUE;
                        } else {

                            recordObjects[i] = Boolean.FALSE;
                        }
                        break;

                    case 'M':
                        // TODO Later
                        recordObjects[i] = new String("null");
                        break;

                    default:
                        recordObjects[i] = new String("null");
                }
            }
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (Exception e) {
                }
            }
        }

        return recordObjects;
    }
    
    /**
     * Reads the records from <i>startingRecord</i> to <i>endingRecord</i> from the table.
     * @param startingRecord the first record read (zero-based).
     * @param endingRecord the last record read (note the range is inclusive of endingRecord).
     * @return an Object[] array where each element is another Object[] array of record values.
     * @throws DBFException 
     */
    public Object[] getRecords(int startingRecord, int endingRecord) throws DBFException {
        if (startingRecord < 0) {
            throw new DBFException("Record number is out of bounds.");
        }
        if (endingRecord >= this.numberOfRecords) {
            endingRecord = this.numberOfRecords - 1;
        }

        currentRecord = endingRecord;

        int numRecsRead = endingRecord - startingRecord + 1;

        Object returnRecords[] = new Object[numRecsRead];

        RandomAccessFile rIn = null;
        ByteBuffer buf;
        FileChannel inChannel = null;

        try {
            int numBytesToRead = this.recordLength * numRecsRead;
            
            buf = ByteBuffer.allocate(numBytesToRead);

            rIn = new RandomAccessFile(this.fileName, "r");

            inChannel = rIn.getChannel();
            
            int pos = (32 + (32 * this.fieldArray.length)) + 1 + startingRecord * this.recordLength;
            inChannel.position(pos);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            
            for (int n = startingRecord; n <= endingRecord; n++) {
                

                if (buf.get() == END_OF_DATA) {
                    return null;
                } // record has been deleted

                Object recordObjects[] = new Object[this.fieldArray.length];

                for (int i = 0; i < this.fieldArray.length; i++) {

                    switch (this.fieldArray[i].getDataType()) {

                        case 'C':

                            byte b_array[] = new byte[this.fieldArray[i].getFieldLength()];
                            //dataInputStream.read(b_array);
                            buf.get(b_array);
                            recordObjects[i] = new String(b_array, characterSetName);
                            break;

                        case 'D':

                            byte t_byte_year[] = new byte[4];
                            //dataInputStream.read(t_byte_year);
                            buf.get(t_byte_year);

                            byte t_byte_month[] = new byte[2];
                            //dataInputStream.read(t_byte_month);
                            buf.get(t_byte_month);

                            byte t_byte_day[] = new byte[2];
                            //dataInputStream.read(t_byte_day);
                            buf.get(t_byte_day);

                            try {

                                GregorianCalendar calendar = new GregorianCalendar(
                                        Integer.parseInt(new String(t_byte_year)),
                                        Integer.parseInt(new String(t_byte_month)) - 1,
                                        Integer.parseInt(new String(t_byte_day)));

                                recordObjects[i] = calendar.getTime();
                            } catch (NumberFormatException e) {
                                /*
                                 * this field may be empty or may have improper
                                 * value set
                                 */
                                recordObjects[i] = null;
                            }

                            break;

                        case 'F':

                            try {

                                byte t_float[] = new byte[this.fieldArray[i].getFieldLength()];
                                //dataInputStream.read(t_float);
                                buf.get(t_float);

                                t_float = Utils.trimLeftSpaces(t_float);
                                if (t_float.length > 0 && !Utils.contains(t_float, (byte) '?')) {

                                    recordObjects[i] = new Float(new String(t_float));
                                } else {

                                    recordObjects[i] = null;
                                }
                            } catch (NumberFormatException e) {

                                throw new DBFException("Failed to parse Float: " + e.getMessage());
                            }

                            break;

                        case 'N':

                            try {

                                byte t_numeric[] = new byte[this.fieldArray[i].getFieldLength()];
                                //dataInputStream.read(t_numeric);
                                buf.get(t_numeric);

                                t_numeric = Utils.trimLeftSpaces(t_numeric);

                                if (t_numeric.length > 0 && !Utils.contains(t_numeric, (byte) '?')) {

                                    recordObjects[i] = new Double(new String(t_numeric));
                                } else {

                                    recordObjects[i] = null;
                                }
                            } catch (NumberFormatException e) {

                                throw new DBFException("Failed to parse Number: " + e.getMessage());
                            }

                            break;

                        case 'L':

                            byte t_logical = buf.get();
                            if (t_logical == 'Y' || t_logical == 't' || t_logical == 'T' || t_logical == 't') {

                                recordObjects[i] = Boolean.TRUE;
                            } else {

                                recordObjects[i] = Boolean.FALSE;
                            }
                            break;

                        case 'M':
                            // TODO Later
                            recordObjects[i] = new String("null");
                            break;

                        default:
                            recordObjects[i] = new String("null");
                    }
                }
                returnRecords[n - startingRecord] = recordObjects;
            }
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (Exception e) {
                }
            }
        }

        return returnRecords;
    }
    
    /**
     * Reads the returns the next row in the DBF stream. @returns The next row
     * as an Object array. Types of the elements these arrays follow the
     * convention mentioned in the class description.
     */
    public Object[] nextRecord() throws DBFException {
        currentRecord++;
        if (currentRecord < 0) {
            throw new DBFException("Record number is out of bounds.");
        }
        if (currentRecord >= this.numberOfRecords) {
            return null;
        }
        return getRecord(currentRecord);
    }
    
    
    ArrayList recordData = new ArrayList();
    /**
     * Add a record to the dbf. Note that this method actually writes the record
     * to a temporary ArrayList and that the file will not be updated until the
     * write() method is called.
     * @param values an Object array containing the record values for each field.
     * @throws DBFException 
     */
    public void addRecord(Object[] values) throws DBFException {

        if (this.fieldArray == null) {

            throw new DBFException("Fields should be set before adding records");
        }

        if (values == null) {

            throw new DBFException("Null cannot be added as row");
        }

        if (values.length != this.fieldArray.length) {

            throw new DBFException("Invalid record. Invalid number of fields in row");
        }

        for (int i = 0; i < this.fieldArray.length; i++) {

            if (values[i] == null) {

                continue;
            }

            switch (this.fieldArray[i].getDataType()) {

                case 'C':
                    if (!(values[i] instanceof String)) {
                        throw new DBFException("Invalid value for field " + i);
                    }
                    break;

                case 'L':
                    if (!(values[i] instanceof Boolean)) {
                        throw new DBFException("Invalid value for field " + i);
                    }
                    break;

                case 'N':
                    if (!(values[i] instanceof Double)) {
                        throw new DBFException("Invalid value for field " + i);
                    }
                    break;

                case 'D':
                    if (!(values[i] instanceof Date)) {
                        throw new DBFException("Invalid value for field " + i);
                    }
                    break;

                case 'F':
                    if (!(values[i] instanceof Double)) {

                        throw new DBFException("Invalid value for field " + i);
                    }
                    break;
            }
        }
        
        recordData.add(values);

    }
    
    public void updateRecord(int recordNumber, Object[] rowData) throws DBFException {
        if (recordNumber < 0) {
            throw new DBFException("Record number is out of bounds.");
        }

        if (recordNumber > this.numberOfRecords) {
            throw new DBFException("Record number is out of bounds.");
        }

        if (recordNumber == this.numberOfRecords) { // append it to the end of the file
            addRecord(rowData);
        }

        RandomAccessFile raf = null;
        ByteBuffer buf;

        try {
            buf = ByteBuffer.allocate(this.recordLength);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            buf.put((byte) ' ');
            
            for (int j = 0; j < this.fieldArray.length; j++) { /*
                 * iterate throught fields
                 */

                switch (this.fieldArray[j].getDataType()) {

                    case 'C':
                        if (rowData[j] != null) {

                            String str_value = rowData[j].toString();
                            buf.put(Utils.textPadding(str_value, characterSetName, this.fieldArray[j].getFieldLength()));
                        } else {
                            buf.put(Utils.textPadding("", this.characterSetName, this.fieldArray[j].getFieldLength()));
                        }

                        break;

                    case 'D':
                        if (rowData[j] != null) {

                            GregorianCalendar calendar = new GregorianCalendar();
                            calendar.setTime((Date) rowData[j]);
                            StringBuffer t_sb = new StringBuffer();
                            buf.put(String.valueOf(calendar.get(Calendar.YEAR)).getBytes());
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.MONTH) + 1), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                        } else {

                            buf.put("        ".getBytes());
                        }

                        break;

                    case 'F':

                        if (rowData[j] != null) {

                            buf.put(Utils.doubleFormating((Double) rowData[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;

                    case 'N':

                        if (rowData[j] != null) {

                            buf.put(
                                    Utils.doubleFormating((Double) rowData[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(
                                    Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;
                    case 'L':

                        if (rowData[j] != null) {

                            if ((Boolean) rowData[j] == Boolean.TRUE) {

                                buf.put((byte) 'T');
                            } else {

                                buf.put((byte) 'F');
                            }
                        } else {

                            buf.put((byte) '?');
                        }

                        break;

                    case 'M':

                        break;

                    default:
                        throw new DBFException("Unknown field type " + this.fieldArray[j].getDataType());
                }
                
            }
            raf = new RandomAccessFile(this.fileName, "rw");
            int pos = (32 + (32 * this.fieldArray.length)) + 1 + recordNumber * this.recordLength;
            raf.seek(pos);
            raf.write(buf.array());
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        } finally {
            isDirty = true;
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void deleteRecord(int recordNumber) {
        
    }
    
    // private methods
    private void initialize() throws DBFException {
        try {
            readHeader();
            fieldCount = this.fieldArray.length;

            initialized = true;
        } catch (IOException e) {

            throw new DBFException(e.getMessage());
        }
    }
    
    private void writeRecord(RandomAccessFile raf, Object[] values) throws DBFException {
        //RandomAccessFile raf = null;
        ByteBuffer buf;

        try {
            
            numberOfRecords++;
            
            buf = ByteBuffer.allocate(this.recordLength);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            buf.put((byte) ' ');
            
            for (int j = 0; j < this.fieldArray.length; j++) { 
                /*
                 * iterate throught fields
                 */

                switch (this.fieldArray[j].getDataType()) {

                    case 'C':
                        if (values[j] != null) {

                            String str_value = values[j].toString();
                            buf.put(Utils.textPadding(str_value, characterSetName, this.fieldArray[j].getFieldLength()));
                        } else {
                            buf.put(Utils.textPadding("", this.characterSetName, this.fieldArray[j].getFieldLength()));
                        }

                        break;

                    case 'D':
                        if (values[j] != null) {

                            GregorianCalendar calendar = new GregorianCalendar();
                            calendar.setTime((Date) values[j]);
                            StringBuffer t_sb = new StringBuffer();
                            buf.put(String.valueOf(calendar.get(Calendar.YEAR)).getBytes());
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.MONTH) + 1), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                        } else {

                            buf.put("        ".getBytes());
                        }

                        break;

                    case 'F':

                        if (values[j] != null) {

                            buf.put(Utils.doubleFormating((Double) values[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;

                    case 'N':

                        if (values[j] != null) {

                            buf.put(
                                    Utils.doubleFormating((Double) values[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(
                                    Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;
                    case 'L':

                        if (values[j] != null) {

                            if ((Boolean) values[j] == Boolean.TRUE) {

                                buf.put((byte) 'T');
                            } else {

                                buf.put((byte) 'F');
                            }
                        } else {

                            buf.put((byte) '?');
                        }

                        break;

                    case 'M':

                        break;

                    default:
                        throw new DBFException("Unknown field type " + this.fieldArray[j].getDataType());
                }
                
            }
            //raf = new RandomAccessFile(this.fileName, "rw");
            raf.seek(raf.length() - 1);
            raf.write(buf.array());
            
            raf.seek(raf.length());
            raf.writeByte(END_OF_DATA);
            
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        }// finally {
//            isDirty = true;
//            if (raf != null) {
//                try {
//                    raf.close();
//                } catch (Exception e) {
//                }
//            }
//        }
    }
    
    public void write() throws DBFException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(this.fileName, "rw");
            
            if (!recordData.isEmpty()) {
                for (int i = 0; i < recordData.size(); i++) {
                    Object[] t_values = (Object[]) recordData.get(i);
                    writeRecord(raf, t_values);
                }
                
                recordData.clear();
                
            }
            
            // update the file header
            writeHeader(raf);
            
            isDirty = false;
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        } finally {
            isDirty = true;
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    // HEADER FILE
    static final byte SIG_DBASE_III = (byte) 0x03;
    /* DBF structure start here */
    byte signature;              /* 0 */
    byte year;                   /* 1 */
    byte month;                  /* 2 */
    byte day;                    /* 3 */
    int numberOfRecords;         /* 4-7 */
    short headerLength;          /* 8-9 */
    short recordLength;          /* 10-11 */
    short reserv1;               /* 12-13 */
    byte incompleteTransaction;  /* 14 */
    byte encryptionFlag;         /* 15 */
    int freeRecordThread;        /* 16-19 */
    int reserv2;                 /* 20-23 */
    int reserv3;                 /* 24-27 */
    byte mdxFlag;                /* 28 */
    byte languageDriver;         /* 29 */
    short reserv4;               /* 30-31 */
    DBFField[] fieldArray;       /* each 32 bytes */
    byte terminator1;            /* n+1 */
    /* DBF structure ends here */

    void readHeader() throws IOException {
        DataInputStream in = new DataInputStream(new
            BufferedInputStream(new FileInputStream(fileName)));
            DataInputStream dataInput = new DataInputStream(in);
        
        signature = dataInput.readByte(); /* 0 */
        year = dataInput.readByte();      /* 1 */
        month = dataInput.readByte();     /* 2 */
        day = dataInput.readByte();       /* 3 */
        numberOfRecords = Utils.readLittleEndianInt(dataInput); /* 4-7 */

        headerLength = Utils.readLittleEndianShort(dataInput); /* 8-9 */
        recordLength = Utils.readLittleEndianShort(dataInput); /* 10-11 */

        reserv1 = Utils.readLittleEndianShort(dataInput);      /* 12-13 */
        incompleteTransaction = dataInput.readByte();           /* 14 */
        encryptionFlag = dataInput.readByte();                  /* 15 */
        freeRecordThread = Utils.readLittleEndianInt(dataInput); /* 16-19 */
        reserv2 = dataInput.readInt();                            /* 20-23 */
        reserv3 = dataInput.readInt();                            /* 24-27 */
        mdxFlag = dataInput.readByte();                           /* 28 */
        languageDriver = dataInput.readByte();                    /* 29 */
        reserv4 = Utils.readLittleEndianShort(dataInput);        /* 30-31 */

        ArrayList al_fields = new ArrayList();

        DBFField field = DBFField.createField(dataInput); /* 32 each */
        while (field != null) {

            al_fields.add(field);
            field = DBFField.createField(dataInput);
        }

        fieldArray = new DBFField[al_fields.size()];

        for (int i = 0; i < fieldArray.length; i++) {

            fieldArray[ i] = (DBFField) al_fields.get(i);
        }

        dataInput.close();
        in.close();
    }

    void writeHeader(RandomAccessFile raf) throws IOException {
        //DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(this.fileName));
        raf.seek(0);
        
        raf.writeByte(signature);                       /* 0 */

        GregorianCalendar calendar = new GregorianCalendar();
        year = (byte) (calendar.get(Calendar.YEAR) - 1900);
        month = (byte) (calendar.get(Calendar.MONTH) + 1);
        day = (byte) (calendar.get(Calendar.DAY_OF_MONTH));

        raf.writeByte(year);  /* 1 */
        raf.writeByte(month); /* 2 */
        raf.writeByte(day);   /* 3 */

        numberOfRecords = Utils.littleEndian(numberOfRecords);
        raf.writeInt(numberOfRecords); /* 4-7 */

        headerLength = findHeaderLength();
        raf.writeShort(Utils.littleEndian(headerLength)); /* 8-9 */

        recordLength = findRecordLength();
        raf.writeShort(Utils.littleEndian(recordLength)); /* 10-11 */

        raf.writeShort(Utils.littleEndian(reserv1)); /* 12-13 */
        raf.writeByte(incompleteTransaction); /* 14 */
        raf.writeByte(encryptionFlag); /* 15 */
        raf.writeInt(Utils.littleEndian(freeRecordThread));/* 16-19 */
        raf.writeInt(Utils.littleEndian(reserv2)); /* 20-23 */
        raf.writeInt(Utils.littleEndian(reserv3)); /* 24-27 */

        raf.writeByte(mdxFlag); /* 28 */
        raf.writeByte(languageDriver); /* 29 */
        raf.writeShort(Utils.littleEndian(reserv4)); /* 30-31 */

        for (int i = 0; i < fieldArray.length; i++) {

            fieldArray[i].write(raf);
        }

        raf.writeByte(terminator1); /* n+1 */
        
        //raf.flush();
        //raf.close();
    }

    private short findHeaderLength() {

        return (short) (1
                + 3
                + 4
                + 2
                + 2
                + 2
                + 1
                + 1
                + 4
                + 4
                + 4
                + 1
                + 1
                + 2
                + (32 * fieldArray.length)
                + 1);
    }

    private short findRecordLength() {

        int recordLength = 0;
        for (int i = 0; i < fieldArray.length; i++) {

            recordLength += fieldArray[i].getFieldLength();
        }

        return (short) (recordLength + 1);
    }
}
