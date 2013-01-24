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
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.DBFReader;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AttributesFileViewer extends JDialog implements ActionListener {
    
    private String dbfFileName = "";
    private String shapeFileName = "";
    private DBFReader dbfReader;
    private AttributeTable attTable;
    //private JButton edit = new JButton("Edit");
    private JButton close = new JButton("Close");
    private JTable table = new JTable();
    private JTable fieldTable = new JTable();
    private JTabbedPane tabs;
    private WhiteboxPluginHost host = null;
    private ShapeFile shapeFile = null;
    
    public AttributesFileViewer(Frame owner, boolean modal, String shapeFileName) {
        super(owner, modal);
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost)owner;
        }
        if (owner != null) {
            Dimension parentSize = owner.getSize(); 
            Point p = owner.getLocation(); 
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        this.shapeFileName = shapeFileName;
        shapeFile = new ShapeFile(shapeFileName);
        
        if (shapeFileName.toLowerCase().contains(".shp")) {
            dbfFileName = shapeFileName.replace(".shp", ".dbf");
        } else if (shapeFileName.toLowerCase().contains(".dbf")) {
            dbfFileName = shapeFileName;
        }
        attTable = new AttributeTable(dbfFileName);
        try {
            dbfReader = new DBFReader(dbfFileName);
        } catch (DBFException dbfe) {
            if (owner instanceof WhiteboxPluginHost) {
                WhiteboxPluginHost wph = (WhiteboxPluginHost)owner;
                wph.showFeedback("DBF file not read properly. It is possible that there is no database file.");
            } else {
                JLabel warning = new JLabel("DBF file not read properly. It is possible that there is no database file.");
                this.add(warning);
            }
        }
        createGui();
    }
    
    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }
            
            JPanel contentPanel = new JPanel();
            
            File file = new File(dbfFileName);
            String shortFileName = file.getName();
            shortFileName = shortFileName.replace(".dbf", "");
        
            setTitle("Layer Attribute Table: " + shortFileName);

            // okay and close buttons.
            Box box1 = Box.createHorizontalBox();
            box1.add(Box.createHorizontalStrut(10));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));
            box1.add(close);
            close.setActionCommand("close");
            close.addActionListener(this);
            close.setToolTipText("Exit without saving changes");
            box1.add(Box.createHorizontalStrut(100));
            box1.add(Box.createHorizontalGlue());

            add(box1, BorderLayout.SOUTH);

            Box mainBox = Box.createVerticalBox();
            
            table = getDataTable();
          
            JScrollPane scroll = new JScrollPane(table);
            tabs = new JTabbedPane();

            JPanel panel1 = new JPanel();
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
            panel1.add(scroll);
            tabs.addTab("Attributes Table", panel1);
            //tabs.setMnemonicAt(0, KeyEvent.VK_1);

            
            // field table
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
            
            DBFField[] fields = dbfReader.getAllFields();
            int numColumns = 4;
            int numRows = fields.length;
            String[] columnHeaders = new String[]{"Name", "Type", "Length", "Precision"};
            Object[][] data = new Object[numRows][numColumns];
            byte dataType = 0;
            String outputDataType;
            for (int a = 0; a < numRows; a++) {
                data[a][0] = fields[a].getName();
                dataType = fields[a].getDataType();
                switch (dataType) {
                    case 'D':
                        outputDataType = "Date";
                        break;
                    case 'C':
                        outputDataType = "String";
                        break;
                    case 'L':
                        outputDataType = "Boolean";
                        break;
                    case 'N':
                        outputDataType = "Numeric";
                        break;
                    case 'F':
                        outputDataType = "Float";
                        break;
                    case 'M':
                        outputDataType = "Memo";
                        break;
                    default:
                        outputDataType = "Unrecognized";
                        break;
                }
                data[a][1] = outputDataType;
                data[a][2] = fields[a].getFieldLength();
                data[a][3] = fields[a].getDecimalCount();
            }
            
            fieldTable = new JTable(data, columnHeaders) {

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
            
            JScrollPane scroll2 = new JScrollPane(fieldTable);
            panel2.add(scroll2);
            tabs.addTab("Field Summary", panel2);
            
            
            mainBox.add(tabs);
            this.getContentPane().add(mainBox, BorderLayout.CENTER);
            
            JMenuBar menubar = createMenu();
            this.setJMenuBar(menubar);

            pack();

            // Centre the dialog on the screen.
            // Get the size of the screen
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            //setSize(screenWidth / 2, screenHeight / 2);
            this.setLocation(screenWidth / 4, screenHeight / 4);
            
            dbfReader = null;
            
        } catch (DBFException dbfe) {
            System.out.println("DBF file not read properly.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private JTable getDataTable() {
        try {
            int numColumns = dbfReader.getFieldCount();
            int numRows = dbfReader.getRecordCount();
            String[] ch = dbfReader.getAttributeTableFieldNames();
            String[] columnHeaders = new String[numColumns + 1];
            columnHeaders[0] = "ID";
            System.arraycopy(ch, 0, columnHeaders, 1, numColumns);
            Object[] row;
            Object[][] data = new Object[numRows][numColumns + 1];
            int a = 0;
            while ((row = dbfReader.nextRecord()) != null) {
                data[a][0] = String.valueOf(a);
                System.arraycopy(row, 0, data[a], 1, numColumns);
                a++;
            }
            
            table = new JTable(data, columnHeaders) {

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
                    column.setPreferredWidth(40);
                } else {
                    column.setPreferredWidth(70);
                }
                
            }
            return table;
        } catch (Exception e) {
            return null;
        }
    }

    private JMenuBar createMenu() {
        JMenuBar menubar = new JMenuBar();

        // Add Field menu
        JMenu addFieldMenu = new JMenu("Add Field");
        
        JMenuItem addFID = new JMenuItem("Add Feature ID");
        addFID.setActionCommand("addFID");
        addFID.addActionListener(this);
        //addFID.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        addFieldMenu.add(addFID);
        
        if (shapeFile.getShapeType().getBaseType() == ShapeType.POLYGON) {
            JMenuItem addAreaField = new JMenuItem("Add Area Field");
            addAreaField.setActionCommand("addAreaField");
            addAreaField.addActionListener(this);
            addFieldMenu.add(addAreaField);
            
            JMenuItem addPerimeterField = new JMenuItem("Add Perimeter Field");
            addPerimeterField.setActionCommand("addPerimeterField");
            addPerimeterField.addActionListener(this);
            addFieldMenu.add(addPerimeterField);
        }
        
        menubar.add(addFieldMenu);
        
        return menubar;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            this.dispose();
        } else if (actionCommand.equals("addFID")) {
            addFID();
        } else if (actionCommand.equals("addAreaField")) {
            addAreaField();
        } else if (actionCommand.equals("addPerimeterField")) {
            addPerimeterField();
        }
    }
    
    private void addFID() {
        
        try {
            
            DBFField field = new DBFField();
            field = new DBFField();
            field.setName("FID");
            field.setDataType(DBFField.FIELD_TYPE_N);
            field.setFieldLength(10);
            field.setDecimalCount(0);
            attTable.addField(field);
            
            for (int a = 0; a < attTable.getNumberOfRecords(); a++) {
                Object[] recData = attTable.getRecord(a);
                recData[recData.length - 1] = new Double(a);
                attTable.updateRecord(a, recData);
            }
            //dbfReader = new DBFReader(dbfFileName);
            //attTable = new AttributeTable(dbfFileName);
            //table = getDataTable();
            //createGui();
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void addAreaField() {
        
        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback("This function can only be applied to polygon type shapefiles.");
                    return;
                }
            }
            double area;
            int recNum;
            //double numRecordsDone = 0;
            //int progress = 0;
            //double numRecords = shapeFile.getNumberOfRecords();
            DBFField field = new DBFField();
            field = new DBFField();
            field.setName("Area");
            field.setDataType(DBFField.FIELD_TYPE_N);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.shapeFile.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                    if (inputType == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon =
                                (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        area = recPolygon.getArea();
                    }
                    
                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.shapeFile.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(area);
                    this.shapeFile.attributeTable.updateRecord(recNum, recData);

                }
//                if (host != null) {
//                    numRecordsDone++;
//                    progress = (int)(100 * numRecordsDone / numRecords);
//                    host.updateProgress(progress);
//                }
                        
            }
            
            host.showFeedback("Calculation complete!");
            
        } catch (Exception e) {
            if (host != null) {
                host.showFeedback(e.getMessage());
            }
        }
        
    }
    
    
    private void addPerimeterField() {
        
        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback("This function can only be applied to polygon type shapefiles.");
                    return;
                }
            }
            double perimeter;
            int recNum;
            //double numRecordsDone = 0;
            //int progress = 0;
            //double numRecords = shapeFile.getNumberOfRecords();
            DBFField field = new DBFField();
            field = new DBFField();
            field.setName("Perimeter");
            field.setDataType(DBFField.FIELD_TYPE_N);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.shapeFile.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (inputType != ShapeType.NULLSHAPE) {
                    if (shapeFile.getShapeType() == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon =
                                (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    }
                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.shapeFile.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(perimeter);
                    this.shapeFile.attributeTable.updateRecord(recNum, recData);

                }

            }
            
            host.showFeedback("Calculation complete!");
            
        } catch (Exception e) {
            if (host != null) {
                host.showFeedback(e.getMessage());
            }
        }
        
    }
    
}
