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
package whitebox.ui.plugin_dialog;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ReclassTableModel extends AbstractTableModel {

    public static final int NEW_INDEX = 0;
    public static final int FROM_INDEX = 1;
    public static final int TO_INDEX = 2;
    public static final int HIDDEN_INDEX = 3;
    protected String[] columnNames;
    protected Vector dataVector;

    public ReclassTableModel(String[] columnNames) {
        this.columnNames = columnNames;
        dataVector = new Vector();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == HIDDEN_INDEX) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case NEW_INDEX:
            case FROM_INDEX:
            case TO_INDEX:
                return String.class;
            default:
                return Object.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        ReclassEntry record = (ReclassEntry) dataVector.get(row);
        switch (column) {
            case NEW_INDEX:
                return record.getNewValue();
            case FROM_INDEX:
                return record.getFromValue();
            case TO_INDEX:
                return record.getToValue();
            default:
                return new Object();
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        ReclassEntry record = (ReclassEntry) dataVector.get(row);
        switch (column) {
            case NEW_INDEX:
                record.setNewValue((String) value);
                break;
            case FROM_INDEX:
                record.setFromValue((String) value);
                break;
            case TO_INDEX:
                record.setToValue((String) value);
                break;
            default:
                System.out.println("invalid index");
        }
        fireTableCellUpdated(row, column);
    }

    @Override
    public int getRowCount() {
        return dataVector.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    public boolean hasEmptyRow() {
        if (dataVector.isEmpty()) {
            return false;
        }
        ReclassEntry record = (ReclassEntry) dataVector.get(dataVector.size() - 1);
        if (record.getNewValue().trim().equals("")
                && record.getFromValue().trim().equals("")
                && record.getToValue().trim().equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public void addEmptyRow() {
        dataVector.add(new ReclassEntry());
        fireTableRowsInserted(
                dataVector.size() - 1,
                dataVector.size() - 1);
    }
}
