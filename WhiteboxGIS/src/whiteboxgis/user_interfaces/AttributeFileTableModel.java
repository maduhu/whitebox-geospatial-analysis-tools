/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

package whiteboxgis.user_interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;

/**
 * Model for displaying an AttributeTable in AttributeFilesViewer.
 * @author Kevin Green
 */
public class AttributeFileTableModel extends AbstractTableModel {
    
    private AttributeTable table;
    
    // The number of columns shown that are not part of the AttributeTable's fields
    private static final int GENERATED_COLUMN_COUNT = 2;
    
    // A lookup table for changed rows. This allows changes to exist only in memory
    private HashMap<Integer, Object[]> changedRows = new HashMap<>();
    
    // 
    private List<Integer> hiddenColumns = new ArrayList<>();
    
    public AttributeFileTableModel(AttributeTable table) {
        this.table = table;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0 || columnIndex == 1) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public int getRowCount() {
        return table.getNumberOfRecords();
    }

    @Override
    public int getColumnCount() {
        // Add 2 for the modified column and ID column
        return table.getFieldCount() + GENERATED_COLUMN_COUNT - hiddenColumns.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        
        if (columnIndex == 0) {
            return String.class;
        } else if (columnIndex == 1) {
            return Integer.class;
        }
        
        int fieldIndex = getActualColumn(columnIndex);
        
        Class<?> klass = Object.class;
        DBFField[] fields = table.getAllFields();
        if (fieldIndex < fields.length) {
            klass = fields[fieldIndex].getDataType().getEquivalentClass();
        }
        
        return klass;
    }
    
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "";
        } else if (column == 1) {
            return "REC #";
        }
        int fieldIndex = getActualColumn(column);
        String[] names = table.getAttributeTableFieldNames();
        if (names != null && names.length > fieldIndex) {
            return names[fieldIndex];
        }
        
        return super.getColumnName(column);
    }
    
    /**
     * Used to support hidden columns. Skips over hidden columns to find the 
     * attribute table index for this column.
     * @param col. JTable column index
     * @return AttributeTable field index for correct data
     */
    private int getActualColumn(int col) {
        
        Collections.sort(hiddenColumns);
        
        col = col - GENERATED_COLUMN_COUNT;

        int actualIndex = col;
        for (int deletedRow : hiddenColumns) {
            if (deletedRow <= actualIndex) {
                actualIndex++;
            }
        }
        
        return actualIndex;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        if (columnIndex == 0) {
            // Mark with * if the row is modified and unsaved
            return (changedRows.get(rowIndex) != null ? "*" : "");
        } else if (columnIndex == 1) {
            return rowIndex;
        }
        int fieldIndex = getActualColumn(columnIndex);
        try {
            
            // First check if the row is in the changed rows
            Object[] row = changedRows.get(rowIndex);
            
            // Not changed, get it from disk
            if (row == null) {
                row = table.getRecord(rowIndex);
            }

            if (row != null && row.length > fieldIndex) {
                // First column shown is the ID
                Object ret = row[fieldIndex];
                if (ret == null) { return null; }
                if (ret.getClass().equals(String.class)) {
                    ret = ((String)(ret)).trim();
                }
                return ret; //row[fieldIndex];
            }
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        }
        
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Check if the type of aValue fits to table at rowIndex, columnIndex
        
        try {
            int fieldIndex = getActualColumn(columnIndex);
            Object[] row = table.getRecord(rowIndex);
            
            if (row != null && row.length > fieldIndex) {
                row[fieldIndex] = aValue;
                changedRows.put(rowIndex, row);
            }
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Entered value not compatible with field type");
        }
        
        this.fireTableRowsUpdated(rowIndex, rowIndex);
    }
    
    /**
     * Hides the column with model index column from view. This takes the column
     * index for the visible column in the table, not the AttributeTable field
     * for that column.
     * @param column. Visible column index from the table
     */
    public void hideColumn(int column) {
        hiddenColumns.add(column);
        this.fireTableStructureChanged();
    }
    
    public void unhideColumns() {
        hiddenColumns.clear();
        this.fireTableStructureChanged();
    }
    
    /**
     * Reverts all changes to the given row.
     * @param row 
     */
    public void revertRow(int row) {
        
        // Remove row from changedRows    
        changedRows.remove(row);
        
        fireTableRowsUpdated(row, row);
    }
    
    /**
     * Method to signal from the view to the model that changes to row values should
     * be saved to disk.
     * @return True if all changes were saved and false if some changes couldn't
     * be saved.
     */
    public boolean saveChanges() {
        
        Set<Map.Entry<Integer, Object[]>> entries = changedRows.entrySet();
        
        for (Iterator<Map.Entry<Integer, Object[]>> iter = entries.iterator(); iter.hasNext();) {
            Map.Entry<Integer, Object[]> entry = iter.next();
            try {
                table.updateRecord(entry.getKey(), entry.getValue());
                iter.remove();
            } catch (DBFException e) {
                System.out.println(e.getMessage());
            }
            
        }
        
        if (!changedRows.isEmpty()) {
            // Some changes weren't saved
            return false;
        }
        
        unhideColumns();
        
        return true;

    }
    
    /**
     * Returns false if there are unsaved changes.
     * @return True if all changes are saved
     */
    public boolean isSaved() {
        if (changedRows.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns true if the row for the given rowIndex is modified.
     * @param row
     * @return True if the row is modified.
     */
    public boolean isModified(int rowIndex) {
        if (changedRows.containsKey(rowIndex)) {
            return true;
        }
        
        return false;
    }
}
