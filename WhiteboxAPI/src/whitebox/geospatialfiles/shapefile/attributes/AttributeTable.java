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
import static whitebox.geospatialfiles.shapefile.attributes.AttributeTable.SIG_DBASE_III;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.BOOLEAN;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.DATE;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.FLOAT;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.MEMO;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.NUMERIC;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.STRING;
/**
 *
 * @author johnlindsay
 */
public class AttributeTable {
    private String fileName;
    private boolean isDirty = false;
    protected String characterSetName = "8859_1";
    protected final int END_OF_DATA = 0x1A;

    /** 
     * Used to create an AttributeTable object when the DBF file already exists.
     * @param fileName  String containing the full file name and directory.
     */
    public AttributeTable(String fileName) throws IOException {
        this.signature = SIG_DBASE_III;
        this.terminator1 = 0x0D;
                
        this.fileName = fileName;
        initialize();
    }
    
    /**
     * Used to create an AttributeTable object when the DBF file does not already exist.
     * A new DBF file will be created and initialized with the specified fields but it will
     * not contain any records.
     * @param fileName  String containing the full file name and directory.
     * @param fields Array of DBFField type.
     * @param destroy Option to destroy an existing DBF file.
     */
    public AttributeTable(String fileName, DBFField[] fields, boolean destroy) throws DBFException, IOException {
        this.signature = SIG_DBASE_III;
        this.terminator1 = 0x0D;
        
        this.fileName = fileName;
        createDBFFile(new File(fileName), destroy);
        setFields(fields);
        write();
        initialize();
    }
    
    /**
     * Verifies the existence of or creates a valid DBF file on disk which can
     * then have fields and records added or removed.
     *
     * @param dbfFile. The file passed in should be a valid DBF file or non-existent.
     * @exception Throws DBFException if the passed in file does exist but not a
     * valid DBF file, or if an IO error occurs.
     */
    private void createDBFFile(File dbfFile, boolean destroy) throws DBFException, IOException {
        
        if (destroy == true) {
            if (dbfFile.exists()) {
                try {
                    dbfFile.delete();
                } catch (Exception e) {
                }
            }
        }
        
        try (RandomAccessFile raf = new RandomAccessFile(dbfFile, "rw")) {

            if (dbfFile.length() == 0) {
                writeHeader(raf);
            } else {
                readHeader();
            }
        } catch (FileNotFoundException e) {

            throw new DBFException("Specified file is not found. " + e.getMessage());
        }
    }
    
    // properties
    public String getFileName() {
        return fileName;
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
    public DBFField getField(int index) {

        return this.fieldArray[index];
    }
    
    /**
     * Retrieves all fields in this database.
     * @return DBFField array
     * @throws DBFException 
     */
    public DBFField[] getAllFields() {

        return this.fieldArray;
    }

    int fieldCount;
    /**
    Returns the number of field in the DBF.
     */
    public int getFieldCount() {
        
        if (this.fieldArray != null) {
            return this.fieldArray.length;
        }

        return -1;
    }
    
    /**
     * Used to determine whether the table has been modified since it was last saved.
     * @return boolean
     */
    public boolean isTableDirty() {
        return isDirty;
    }
    
    /**
     * Returns a String array of fields.
     * @return String array
     */
    public String[] getAttributeTableFieldNames() {
        int numberOfFields = this.getFieldCount();
        String[] ret = new String[numberOfFields];
        for (int i = 0; i < numberOfFields; i++) {

            DBFField field = this.getField(i);

            ret[i] = field.getName();
        }

        return ret;
    }
    
    /**
     * Sets fields for new files only.
     */
    private void setFields(DBFField[] fields)
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

        try (RandomAccessFile raf = new RandomAccessFile(this.fileName, "rw")) {
            writeHeader(raf);
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
        addField(field, this.fieldCount);
    }
    
    /**
     * Adds a DBFField after the specified index.
     * Index 0 specifies before the first field and index {@link fieldCount}
     * specifies after the last field.
     * @param field. An initialized DBFField object.
     * @param insertAt. The index to insert field.
     * @throws DBFException 
     */
    public void addField(DBFField field, int insertAt) throws DBFException {
        
        if (field == null) {
            throw new DBFException("New field name is empty");
        } else if (insertAt < 0 || insertAt > this.fieldCount) {
            throw new DBFException("Param insertAt is out of table range");
        } else if (this.fileName.isEmpty()) {
            throw new DBFException("DBF file name not specified");
        } else if (!(new File(this.fileName).exists())) {
            throw new DBFException("DBF file does not exist");
        }
        
        try {
            // create a temporary file to house the new dbf
            String fileNameCopy = this.fileName.replace(".dbf", "_copy.dbf");
            if (new File(fileNameCopy).exists()) {
                new File(fileNameCopy).delete();
            }
            DBFField[] outFields = new DBFField[this.fieldCount + 1];
            DBFField[] inFields = getAllFields();
            
            // Copy all fields before insertAt            
            System.arraycopy(inFields, 0, outFields, 0, insertAt);
                        
            // Copy new field
            outFields[insertAt] = field;           
            
            // If we're not at the end, copy the rest of the fields
            if (insertAt < this.fieldCount) {
                System.arraycopy(inFields, insertAt, outFields, insertAt + 1, this.fieldCount - insertAt);
            }
            
            AttributeTable newTable = new AttributeTable(fileNameCopy, outFields, true); // used to set up the dbf copy

            for (int a = 0; a < this.numberOfRecords; a++) {
                Object[] inRec = getRecord(a);
                Object[] outRec = new Object[this.fieldCount + 1];
                // Record data for new field is left null
                System.arraycopy(inRec, 0, outRec, 0, insertAt);
                System.arraycopy(inRec, insertAt, outRec, insertAt + 1, this.fieldCount - insertAt);
                
                newTable.addRecord(outRec);
            }
            
            newTable.write();
            
            File oldFile = new File(this.fileName);
            // Rename old file in case something horrible happens
            if (oldFile.renameTo(new File(this.fileName.concat(".bak")))) {
                
                File newFile = new File(fileNameCopy);
                // Rename new file to old file's name
                if (newFile.renameTo(new File(this.fileName))) {
                    // Delete the backup for oldFile
                    new File(this.fileName.concat(".bak")).delete();

                    initialize();
                }
            }

        } catch (Exception e) {
            throw new DBFException(e.getMessage());
        }
    }
    
    /**
     * Removes the specified index from the fields. Warning: this method creates
     * a new file with less fields and will delete the old one.
     * @param removeIndex The index to remove from the fieldArray
     * @throws DBFException 
     */
    public void deleteField(int removeIndex) throws DBFException {
        
        // Can't be below 0 and if fieldNum == 0, we can't remove anything
        if (removeIndex < 0 || removeIndex >= this.fieldCount) {
            throw new DBFException("Param fieldNum is out of table range");
        } else if (this.fileName.isEmpty()) {
            throw new DBFException("DBF file name not specified");
        } else if (!(new File(this.fileName).exists())) {
            throw new DBFException("DBF file does not exist");
        }
        
        try {
            // create a temporary file to house the new dbf
            String fileNameCopy = this.fileName.replace(".dbf", "_copy.dbf");
            if (new File(fileNameCopy).exists()) {
                new File(fileNameCopy).delete();
            }
            
            DBFField[] outFields = new DBFField[this.fieldCount - 1];
            DBFField[] inFields = getAllFields();
            
            // Copy all fields before fieldNum            
            System.arraycopy(inFields, 0, outFields, 0, removeIndex);

            // Copy all fields after fieldNum
            System.arraycopy(inFields, removeIndex + 1, outFields, removeIndex, (this.fieldCount - removeIndex) - 1);
            
            AttributeTable newTable = new AttributeTable(fileNameCopy, outFields, true); // used to set up the dbf copy

            for (int a = 0; a < this.numberOfRecords; a++) {
                Object[] inRec = getRecord(a);
                Object[] outRec = new Object[this.fieldCount - 1];
                // Discard the old field
                System.arraycopy(inRec, 0, outRec, 0, removeIndex);
                System.arraycopy(inRec, removeIndex + 1, outRec, removeIndex, (this.fieldCount - removeIndex) - 1);
                newTable.addRecord(outRec);
            }
            
            newTable.write();
            
            File oldFile = new File(this.fileName);
            // Rename old file in casenew File(oldFile.getPath().concat(".bak")) something horrible happens
            if (oldFile.renameTo(new File(this.fileName.concat(".bak")))) {
                
                File newFile = new File(fileNameCopy);
                // Rename new file to old file's name
                if (newFile.renameTo(new File(this.fileName))) {
                    // Delete the backup for oldFile
                    new File(this.fileName.concat(".bak")).delete();

                    initialize();
                }
            }

        } catch (Exception e) {
            throw new DBFException(e.getMessage());
        }
    }
    
    /**
     * Deletes the field with the name fieldName. If multiple fields exist with
     * the name, the first in index order will be removed.
     * @param fieldName String matching the field to be removed
     * @throws DBFException 
     */
    public void deleteField(String fieldName) throws DBFException {
        // Find the field with the given name
        
        if (fieldName == null) {
            throw new DBFException("fieldName can not be null");
        }

        DBFField[] fields = getAllFields();
        
        for (int i = 0; i < fields.length; i++) {
            if (fieldName.equals(fields[i].getName())) {
                deleteField(i);
                break;
            }
        }
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

                    case STRING:

                        byte b_array[] = new byte[this.fieldArray[i].getFieldLength()];
                        //dataInputStream.read(b_array);
                        buf.get(b_array);
                        recordObjects[i] = new String(b_array, characterSetName);
                        break;

                    case DATE:

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

                    case FLOAT:

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

                    case NUMERIC:

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

                    case BOOLEAN:

                        byte t_logical = buf.get();
                        if (t_logical == 'Y' || t_logical == 't' || t_logical == 'T' || t_logical == 't') {

                            recordObjects[i] = Boolean.TRUE;
                        } else {

                            recordObjects[i] = Boolean.FALSE;
                        }
                        break;

                    case MEMO:
                        // TODO Later
                        recordObjects[i] = "null";
                        break;

                    default:
                        recordObjects[i] = "null";
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

                        case STRING:

                            byte b_array[] = new byte[this.fieldArray[i].getFieldLength()];
                            //dataInputStream.read(b_array);
                            buf.get(b_array);
                            recordObjects[i] = new String(b_array, characterSetName);
                            break;

                        case DATE:

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

                        case FLOAT:

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

                        case NUMERIC:

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

                        case BOOLEAN:

                            byte t_logical = buf.get();
                            if (t_logical == 'Y' || t_logical == 't' || t_logical == 'T' || t_logical == 't') {

                                recordObjects[i] = Boolean.TRUE;
                            } else {

                                recordObjects[i] = Boolean.FALSE;
                            }
                            break;

                        case MEMO:
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
                // null values are not checked
                continue;
            }
            
            Class equivalentClass = this.fieldArray[i].getDataType().getEquivalentClass();
            
            // Check if values[i] is an instance or subclassed instance of the field's expected type
            if (!(values[i].getClass().isAssignableFrom(equivalentClass))) {
                throw new DBFException("Invalid value for field " + i);
            }

        }
        
        recordData.add(values);
        isDirty = true;
    }
    
    /**
     * Replace an existing record. Note that this method actually writes the record
     * to a temporary buffer and that the file will not be updated until the
     * write() method is called.
     * @param values an Object array containing the record values for each field.
     * @throws DBFException 
     */
    public void changeRecord(int index, Object[] values) throws DBFException {

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
                // null values are not checked
                continue;
            }
            
            Class equivalentClass = this.fieldArray[i].getDataType().getEquivalentClass();
            
            // Check if values[i] is an instance or subclassed instance of the field's expected type
            if (!(values[i].getClass().isAssignableFrom(equivalentClass))) {
                throw new DBFException("Invalid value for field " + i);
            }

        }
        
        recordData.add(index, values);

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

                    case STRING:
                        if (rowData[j] != null) {

                            String str_value = rowData[j].toString();
                            buf.put(Utils.textPadding(str_value, characterSetName, this.fieldArray[j].getFieldLength()));
                        } else {
                            buf.put(Utils.textPadding("", this.characterSetName, this.fieldArray[j].getFieldLength()));
                        }

                        break;

                    case DATE:
                        if (rowData[j] != null) {

                            GregorianCalendar calendar = new GregorianCalendar();
                            calendar.setTime((Date) rowData[j]);
                            buf.put(String.valueOf(calendar.get(Calendar.YEAR)).getBytes());
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.MONTH) + 1), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                            buf.put(Utils.textPadding(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), this.characterSetName, 2, Utils.ALIGN_RIGHT, (byte) '0'));
                        } else {

                            buf.put("        ".getBytes());
                        }

                        break;

                    case FLOAT:

                        if (rowData[j] != null) {

                            buf.put(Utils.doubleFormating((Double) rowData[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;

                    case NUMERIC:

                        if (rowData[j] != null) {

                            buf.put(
                                    Utils.doubleFormating((Double) rowData[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(
                                    Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;
                    case BOOLEAN:

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

                    case MEMO:

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
    
    /**
     * Removes the record of index recordNumber from the record data.
     * @param recordNumber. Index of record to remove
     * @throws DBFException
     */
    public void deleteRecord(int recordNumber) throws DBFException {
        if (recordNumber < 0 || recordNumber >= recordData.size()) {
            throw new DBFException("Record number outside of table range.");
        }
        
        recordData.remove(recordNumber);
        isDirty = true;
    }
    
    // private methods
    private void initialize() throws IOException {
        readHeader();
        fieldCount = this.fieldArray.length;
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

                    case STRING:
                        if (values[j] != null) {

                            String str_value = values[j].toString();
                            buf.put(Utils.textPadding(str_value, characterSetName, this.fieldArray[j].getFieldLength()));
                        } else {
                            buf.put(Utils.textPadding("", this.characterSetName, this.fieldArray[j].getFieldLength()));
                        }

                        break;

                    case DATE:
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

                    case FLOAT:

                        if (values[j] != null) {

                            buf.put(Utils.doubleFormating((Double) values[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;

                    case NUMERIC:

                        if (values[j] != null) {

                            buf.put(
                                    Utils.doubleFormating((Double) values[j], this.characterSetName, this.fieldArray[j].getFieldLength(), this.fieldArray[j].getDecimalCount()));
                        } else {

                            buf.put(
                                    Utils.textPadding("?", this.characterSetName, this.fieldArray[j].getFieldLength(), Utils.ALIGN_RIGHT));
                        }

                        break;
                    case BOOLEAN:

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

//                    case MEMO:
//
//                        break;

                    default:
                        throw new DBFException("Unknown field type " + this.fieldArray[j].getDataType());
                }
                
            }
            
            // see if there is an end-of-data byte at the end of the file.
            long length = raf.length();
            raf.seek(length - 1);
            byte lastByte = raf.readByte();
            if (lastByte == END_OF_DATA) {
                // over-write the END_OF_DATA byte to append new records.
                raf.seek(length - 1);
            } else {
                raf.seek(length);
            }
            //raf.seek(raf.length());
            raf.write(buf.array());
            
            
            //raf.seek(raf.length());
            
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
        }
    }
    
    public final void write() throws DBFException {

        try (RandomAccessFile raf = new RandomAccessFile(this.fileName, "rw")) {
            
            if (!recordData.isEmpty()) {
                for (int i = 0; i < recordData.size(); i++) {
                    Object[] t_values = (Object[]) recordData.get(i);
                    writeRecord(raf, t_values);
                }
                raf.writeByte(END_OF_DATA);
                recordData.clear();
            }
            
            // update the file header
            writeHeader(raf);
            
            raf.close();
            
            isDirty = false;
        } catch (IOException e) {
            throw new DBFException(e.getMessage());
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
        try (DataInputStream dataInput = new DataInputStream(new
                 BufferedInputStream(new FileInputStream(fileName)))) {
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
        } // try with resource auto closes
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

        raf.writeInt(Utils.littleEndian(numberOfRecords)); /* 4-7 */

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

        if (fieldArray != null) {
            for (int i = 0; i < fieldArray.length; i++) {

                fieldArray[i].write(raf);
            }
        }

        raf.writeByte(terminator1); /* n+1 */
        
        //raf.flush();
        //raf.close();
    }

    private short findHeaderLength() {

        int nfields = fieldArray == null ? 0 : fieldArray.length;
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
                + (32 * nfields)
                + 1);
    }

    private short findRecordLength() {
        
        if (fieldArray == null) {
            return 0;
        }

        int recordLength = 0;
        for (int i = 0; i < fieldArray.length; i++) {

            recordLength += fieldArray[i].getFieldLength();
        }

        return (short) (recordLength + 1);
    }
}
