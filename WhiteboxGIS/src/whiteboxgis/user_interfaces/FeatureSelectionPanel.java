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

import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;

/**
 * This class is used to report the attributes of a selected feature from a
 * displayed vector file.
 *
 * @author johnlindsay
 */
public class FeatureSelectionPanel extends JPanel implements PropertyChangeListener {

    private ShapeFile shape = null;
    private JTable table = new JTable();
    private int selectedFeature = -1;
    private VectorLayerInfo vli = null;
    private DBFField[] fields;
    
    public FeatureSelectionPanel() {
        createGui();
    }

    public VectorLayerInfo getVectorLayerInfo() {
        return vli;
    }

    public void setVectorLayerInfo(VectorLayerInfo vli) {
        this.vli = vli;
        this.setShapeFileName(vli.getFileName());
        this.vli.pcs.addPropertyChangeListener("selectedFeatureNumber", this);
    }

    public String getShapeFileName() {
        if (this.shape != null) {
            return this.shape.getFileName();
        } else {
            return "not specified";
        }
    }

    public void setShapeFileName(String shapeFileName) {
        this.shape = new ShapeFile(shapeFileName);
        createGui();
    }

    public int getSelectedFeatureNumber() {
        return selectedFeature;
    }

    public void setSelectedFeatureNumber(int selectedFeature) {
        this.selectedFeature = selectedFeature;
        updateTable();
        //createGui();
    }

    private void createGui() {
        try {
            this.removeAll();

            Box mainBox = Box.createVerticalBox();
            Box headerBox = Box.createHorizontalBox();
            //headerBox.add(Box.createHorizontalStrut(10));
            headerBox.add(new JLabel("Selected Feature Attributes:"));
            headerBox.add(Box.createHorizontalGlue());
            mainBox.add(headerBox);
            Box scrollBox = Box.createHorizontalBox();
            //scrollBox.add(Box.createHorizontalStrut(10));
            setBackground(Color.WHITE);
            getDataTable();
            JScrollPane scroll = new JScrollPane(table);
            int tableWidth = 170;
            int numRows = 20;
            if (table != null) {
                tableWidth = table.getPreferredSize().width;
                numRows = Math.min(20, table.getRowCount());
            }
            scroll.setPreferredSize(
                    new Dimension(tableWidth + 5, table.getRowHeight() * (numRows + 1) + 5));

            scrollBox.add(scroll);
            scrollBox.add(Box.createHorizontalGlue());
            mainBox.add(scrollBox);
            this.add(mainBox, BorderLayout.WEST);
            this.validate();
            this.repaint();
            //this.add(mainBox);
//            this.getContentPane().add(mainBox, BorderLayout.CENTER);
//            pack();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private JTable getDataTable() {
        try {
            if (shape == null) {
                return null;
            }

            int numColumns = 2;
            int numRows = shape.attributeTable.getFieldCount() + 1;
            //String[] ch = shape.attributeTable.getAttributeTableFieldNames();
            fields = shape.attributeTable.getAllFields();
            String[] columnHeaders = {"Attribute", "Value"};
            Object[][] data = new Object[numRows][numColumns];
            data[0][0] = "FID";
            if (selectedFeature >= 0) {
                Object[] rowData = shape.attributeTable.getRecord(selectedFeature);
                for (int a = 0; a < numRows - 1; a++) {
                    data[a + 1][0] = fields[a].getName(); //ch[a];
                    data[a + 1][1] = String.valueOf(rowData[a]);
                }
            } else {
                for (int a = 0; a < numRows - 1; a++) {
                    data[a + 1][0] = fields[a].getName(); //ch[a];
                    data[a + 1][1] = "";
                }
            }
            DefaultTableModel tm = new DefaultTableModel(data, columnHeaders);
            
            table = new JTable(tm) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                    Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                    //even index, selected or not selected
                    if (Index_row % 2 == 0) {// && !isCellSelected(Index_row, Index_col)) {
                        comp.setBackground(Color.WHITE);
                        comp.setForeground(Color.BLACK);
                    } else {
                        comp.setBackground(new Color(225, 245, 255)); //new Color(210, 230, 255));
                        comp.setForeground(Color.BLACK);
                    }
                    if (isCellSelected(Index_row, Index_col)) {
                        comp.setForeground(Color.RED);
                    }
                    return comp;
                }
            };

            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            TableColumn column = null;
            for (int i = 0; i <= numColumns; i++) {
                column = table.getColumnModel().getColumn(i);
                if (i == 0) {
                    column.setPreferredWidth(70);
                } else {
                    column.setPreferredWidth(100);
                }

            }
            return table;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateTable() {
        try {
            if (table == null) {
                return;
            }
            DefaultTableModel tm = (DefaultTableModel)table.getModel();
            if (selectedFeature >= 0) {
                tm.setValueAt(selectedFeature, 0, 1);
                Object[] rowData = shape.attributeTable.getRecord(selectedFeature);
                for (int a = 0; a < rowData.length; a++) {
                    //tm.setValueAt(rowData[a], a, 1);
                    if (fields[a].getDataType() == DBFField.FIELD_TYPE_N ||
                            fields[a].getDataType() == DBFField.FIELD_TYPE_F) {
                        tm.setValueAt((double)(rowData[a]), a + 1, 1);
                    } else {
                        tm.setValueAt(String.valueOf(rowData[a]), a + 1, 1);
                    }
                    //tm.fireTableCellUpdated(a, 2);
                }
            } else {
                tm.setValueAt("", 0, 1);
                for (int a = 0; a < fields.length; a++) {
                    tm.setValueAt("", a + 1, 1);
                }
            }
            //tm.fireTableDataChanged();
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("selectedFeatureNumber")) {
            setSelectedFeatureNumber((int) (evt.getNewValue()));
        }
    }
}
