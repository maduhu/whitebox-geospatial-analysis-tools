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
import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.ResourceBundle;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.interfaces.WhiteboxPluginHost;

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
    private ResourceBundle bundle;
    private JList listOfSelectedFeatures;
    private WhiteboxPluginHost host;
    
    public FeatureSelectionPanel(ResourceBundle bundle, WhiteboxPluginHost host) {
        this.bundle = bundle;
        this.host = host;
        createGui();
    }

    public VectorLayerInfo getVectorLayerInfo() {
        return vli;
    }

    public void setVectorLayerInfo(VectorLayerInfo vli) {
        this.vli = vli;
        this.setShapeFileName(vli.getFileName());
        this.vli.pcs.addPropertyChangeListener("selectedFeatureNumber", this);
        createGui();
    }

    public String getShapeFileName() {
        if (this.shape != null) {
            return this.shape.getFileName();
        } else {
            return bundle.getString("NotSpecified");
        }
    }

    public void setShapeFileName(String shapeFileName) {
        try {
            this.shape = new ShapeFile(shapeFileName);
        } catch (IOException ioe) {
            
        }
        createGui();
    }

    public int getSelectedFeatureNumber() {
        return selectedFeature;
    }

    public void setSelectedFeatureNumber(int selectedFeature) {
        this.selectedFeature = selectedFeature;
        updateTable();
    }

    private void createGui() {
        try {
            this.removeAll();
            table = new JTable();
            selectedFeature = -1;
            Box mainBox = Box.createVerticalBox();
            mainBox.add(Box.createVerticalStrut(10));
            
            Box listBox = Box.createVerticalBox();
            Box headerBox2 = Box.createHorizontalBox();
            JLabel label2 = new JLabel("<html><b>Selected Features:</b></html>");
            headerBox2.add(label2);
            headerBox2.add(Box.createHorizontalGlue());
            listBox.add(headerBox2);
            listOfSelectedFeatures = new JList();
            listOfSelectedFeatures.removeAll();
            JScrollPane scroller1 = new JScrollPane(listOfSelectedFeatures);
            listBox.add(scroller1);
            mainBox.add(listBox);
            
            mainBox.add(Box.createVerticalStrut(2));
            
            JButton deselectAll = new JButton("Deselect All");
            deselectAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (host != null) {
                        host.delectedAllFeaturesInActiveLayer();
                        updateTable();
                    }
                }
            });
            Box buttonBox = Box.createHorizontalBox();
            buttonBox.add(deselectAll);
            buttonBox.add(Box.createHorizontalGlue());
            mainBox.add(buttonBox);
            
            mainBox.add(Box.createVerticalStrut(5));
            
            Box headerBox = Box.createHorizontalBox();
            //headerBox.add(Box.createHorizontalStrut(10));
            JLabel label = new JLabel(bundle.getString("SelectedFeatureAttributes") + ":");
            Font newLabelFont=new Font(label.getFont().getName(),Font.BOLD,label.getFont().getSize());
            label.setFont(newLabelFont);
            headerBox.add(label);
            headerBox.add(Box.createHorizontalGlue());
            mainBox.add(headerBox);
            mainBox.add(Box.createVerticalStrut(2));
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
            
            
            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JList theList = (JList) e.getSource();
                    String label = null;
                    int index = theList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = theList.getModel().getElementAt(index);
                        label = o.toString();
                    }
                    if (label != null && !label.isEmpty()) {
                        setSelectedFeatureNumber(Integer.parseInt(label));
                    }
                }
            };
            listOfSelectedFeatures.addMouseListener(ml);
            scroller1.setPreferredSize(new Dimension(tableWidth + 5, 80));
            
            this.add(mainBox, BorderLayout.WEST);
            this.validate();
            this.repaint();
        } catch (Exception e) {
            if (host != null) {
                host.logException("Error in FeatureSelectionPanel", e);
            }
            System.out.println(e.getMessage());
        }
    }

    private boolean noDatabaseAvailable = false;
    private JTable getDataTable() {
        try {
            if (shape == null) {
                return null;
            }

            int numColumns = 2;
            //File dbfFile = new File(shape.getDatabaseFile());
            if (!shape.databaseFileExists) {
                noDatabaseAvailable = true;
                return null;
            }
            shape.refreshAttributeTable();
            AttributeTable attributeTable = shape.getAttributeTable();
            int numRows = shape.getAttributeTable().getFieldCount() + 1;
            //String[] ch = shape.attributeTable.getAttributeTableFieldNames();
            fields = attributeTable.getAllFields();
            String[] columnHeaders = {bundle.getString("Attribute"), 
                bundle.getString("Value")};
            Object[][] data = new Object[numRows][numColumns];
            data[0][0] = "REC #";
            if (selectedFeature >= 0) {
                Object[] rowData = attributeTable.getRecord(selectedFeature);
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
            for (int i = 0; i < numColumns; i++) {
                column = table.getColumnModel().getColumn(i);
                if (i == 0) {
                    column.setPreferredWidth(80);
                } else {
                    column.setPreferredWidth(100);
                }

            }
            return table;
        } catch (Exception e) {
            if (host != null) {
                host.logException("Error in FeatureSelectionPanel", e);
            }
            return null;
        }
    }

    public void updateTable() {
        try {
            if (table == null || noDatabaseAvailable) {
                return;
            }
            
            
            listOfSelectedFeatures.removeAll();
            DefaultListModel model = new DefaultListModel();
            if (vli != null) {
                ArrayList<Integer> selectedRecords = vli.getSelectedFeatureNumbers();
                int a = 0;
                for (Integer i : selectedRecords) {
                    model.add(a, i);
                    a++;
                }
                listOfSelectedFeatures.setModel(model);
            }
            
            
            shape.refreshAttributeTable();
            if (fields.length != shape.getAttributeTable().getFieldCount()) {
                int oldSelectedFeature = selectedFeature;
                createGui();
                selectedFeature = oldSelectedFeature;
            }
            DefaultTableModel tm = (DefaultTableModel)table.getModel();
            if (selectedFeature >= 0) {
                tm.setValueAt(selectedFeature, 0, 1);
                AttributeTable attributeTable = shape.getAttributeTable();
                Object[] rowData = attributeTable.getRecord(selectedFeature - 1);
                if (rowData == null) {
                    return;
                }
                for (int a = 0; a < rowData.length; a++) {
                    //tm.setValueAt(rowData[a], a, 1);
                    if (fields[a].getDataType() == DBFField.DBFDataType.NUMERIC 
                            && !(rowData[a] == null)) {
                        tm.setValueAt((double)(rowData[a]), a + 1, 1);
                    } else if (fields[a].getDataType() == DBFField.DBFDataType.FLOAT
                            && !(rowData[a] == null)) {
                        tm.setValueAt((float)(rowData[a]), a + 1, 1);
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
            if (host != null) {
                host.logException("Error in FeatureSelectionPanel", e);
            }
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
