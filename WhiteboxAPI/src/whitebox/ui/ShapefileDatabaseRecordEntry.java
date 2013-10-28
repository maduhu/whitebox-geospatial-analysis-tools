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
package whitebox.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
import whitebox.geospatialfiles.shapefile.attributes.*;
import static whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType.NUMERIC;
import whitebox.ui.carto_properties.*;

/**
 *
 * @author johnlindsay
 */
public class ShapefileDatabaseRecordEntry extends JDialog implements PropertyChangeListener, ActionListener {
    // global variables

    private ShapeFile shapefile;
    private String[] attributeFieldNames;
    private DBFField[] fields;
    private Object[] recordData;
    private double mValue;
    private double zValue;

    // constructors
    public ShapefileDatabaseRecordEntry() {
        // no-args constructor
    }

    public ShapefileDatabaseRecordEntry(Frame owner, boolean modal, ShapeFile shapefile) {
        super(owner, modal);
        this.shapefile = shapefile;
        init();
    }

    // properties
    public ShapeFile getShapefile() {
        return shapefile;
    }

    public void setShapefile(ShapeFile shapefile) {
        this.shapefile = shapefile;
        init();
    }
    
    public double getMValue() {
        return mValue;
    }
    
    public double getZValue() {
        return zValue;
    }

    // methods
    private void init() {
        if (shapefile == null) {
            return;
        }

        NumericProperty numProp;
        StringProperty stringProp;
        BooleanProperty boolProp;

        attributeFieldNames = shapefile.getAttributeTableFields();
        fields = shapefile.getAttributeTable().getAllFields();

        recordData = new Object[fields.length];

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        setTitle("Record Entry");

        Box box = Box.createVerticalBox();

        ShapeTypeDimension shapeTypeDimension = shapefile.getShapeType().getDimension();
        if (shapeTypeDimension == ShapeTypeDimension.Z) {
            numProp = new NumericProperty("Z value", "");
            numProp.setName("Z value");
            numProp.setBackColour(Color.WHITE);
            numProp.setTextboxWidth(10);
            numProp.setPrecision(NumericProperty.Precision.DOUBLE);
            numProp.addPropertyChangeListener("value", this);
            numProp.setPreferredWidth(250);
            numProp.revalidate();
            box.add(numProp);
            
            numProp = new NumericProperty("M value", "");
            numProp.setName("M value");
            numProp.setBackColour(Color.WHITE);
            numProp.setTextboxWidth(10);
            numProp.setPrecision(NumericProperty.Precision.DOUBLE);
            numProp.addPropertyChangeListener("value", this);
            numProp.setPreferredWidth(250);
            numProp.revalidate();
            box.add(numProp);
        } else if (shapeTypeDimension == ShapeTypeDimension.M) {
            numProp = new NumericProperty("M value:", "");
            numProp.setName("M value");
            numProp.setBackColour(Color.WHITE);
            numProp.setTextboxWidth(10);
            numProp.setPrecision(NumericProperty.Precision.DOUBLE);
            numProp.addPropertyChangeListener("value", this);
            numProp.setPreferredWidth(250);
            numProp.revalidate();
            box.add(numProp);
        }

        int i = 0;
        for (DBFField field : fields) {
            switch (field.getDataType()) {
                case NUMERIC:
                    numProp = new NumericProperty(attributeFieldNames[i], "");
                    numProp.setName(attributeFieldNames[i]);
                    numProp.setBackColour(Color.WHITE);
                    numProp.setTextboxWidth(10);
                    if (field.getDecimalCount() == 0) {
                        numProp.setParseIntegersOnly(true);
                    } else {
                        numProp.setParseIntegersOnly(false);
                    }
                    numProp.addPropertyChangeListener("value", this);
                    numProp.setPreferredWidth(250);
                    numProp.revalidate();
                    box.add(numProp);
                    break;
                case FLOAT:
                    numProp = new NumericProperty(attributeFieldNames[i], "");
                    numProp.setName(attributeFieldNames[i]);
                    numProp.setBackColour(Color.WHITE);
                    numProp.setPrecision(NumericProperty.Precision.FLOAT);
                    numProp.setTextboxWidth(10);
                    if (field.getDecimalCount() == 0) {
                        numProp.setParseIntegersOnly(true);
                    } else {
                        numProp.setParseIntegersOnly(false);
                    }
                    numProp.addPropertyChangeListener("value", this);
                    numProp.setPreferredWidth(250);
                    numProp.revalidate();
                    box.add(numProp);
                    break;
                case STRING:
                    stringProp = new StringProperty(attributeFieldNames[i], "");
                    stringProp.setName(attributeFieldNames[i]);
                    stringProp.setBackColour(Color.WHITE);
                    stringProp.setTextboxWidth(10);
                    stringProp.addPropertyChangeListener("value", this);
                    stringProp.setPreferredWidth(250);
                    stringProp.revalidate();
                    box.add(stringProp);
                    break;
                case BOOLEAN:
                    boolProp = new BooleanProperty(attributeFieldNames[i], false);
                    boolProp.setName(attributeFieldNames[i]);
                    boolProp.setBackColour(Color.WHITE);
                    boolProp.addPropertyChangeListener("value", this);
                    boolProp.setPreferredWidth(250);
                    boolProp.revalidate();
                    box.add(boolProp);
                    break;
            }
            i++;

        }
        box.add(Box.createVerticalGlue());
        JScrollPane scroll = new JScrollPane(box);

        this.add(scroll);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(300, 44));
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalStrut(2));
        box1.add(Box.createHorizontalGlue());
        Box box2 = Box.createHorizontalBox();
        ok.addActionListener(this);
        ok.setActionCommand("ok");
        box2.add(ok);
        box2.add(Box.createHorizontalStrut(5));
        cancel.addActionListener(this);
        cancel.setActionCommand("cancel");
        box2.add(Box.createHorizontalStrut(5));
        box2.add(cancel);
        panel.add(box1);
        panel.add(box2);
        this.getContentPane().add(panel, BorderLayout.SOUTH);
        //this.addWindowListener(this);
    }

    public Object[] getValue() {
        return recordData;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "ok":
                this.setVisible(false);
                break;
            case "cancel":
                recordData = null;
                this.setVisible(false);
                break;

        }
    }
    

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        if (!evt.getPropertyName().equals("value")) {
            return;
        }
        if (fields == null || fields.length == 0) {
            return;
        }
        if (source instanceof NumericProperty) {
            NumericProperty np = (NumericProperty) source;
            if (np.getLabelText().equals("Z value")) {
                
            } else if (np.getLabelText().equals("M value")) {
                
            } else if (np.getPrecision() == NumericProperty.Precision.DOUBLE) {
                for (int i = 0; i < fields.length; i++) {
                    if (np.getName().equals(attributeFieldNames[i])) {
                        Double value = Double.parseDouble((String) evt.getNewValue());
                        recordData[i] = value;
                    }
                }
            } else if (np.getPrecision() == NumericProperty.Precision.FLOAT) {
                for (int i = 0; i < fields.length; i++) {
                    if (np.getName().equals(attributeFieldNames[i])) {
                        Double value = (double)(Float.parseFloat((String) evt.getNewValue()));
                        recordData[i] = value;
                    }
                }
            } else {
                for (int i = 0; i < fields.length; i++) {
                    if (np.getName().equals(attributeFieldNames[i])) {
                        Double value = (double)(Integer.parseInt((String) evt.getNewValue()));
                        recordData[i] = value;
                    }
                }
            }
        } else if (source instanceof StringProperty) {
            StringProperty sp = (StringProperty) source;

            for (int i = 0; i < fields.length; i++) {
                if (sp.getName().equals(attributeFieldNames[i])) {
                    String value = (String) evt.getNewValue();
                    recordData[i] = value;
                }
            }
        } else if (source instanceof BooleanProperty) {
            BooleanProperty bp = (BooleanProperty) source;

            for (int i = 0; i < fields.length; i++) {
                if (bp.getName().equals(attributeFieldNames[i])) {
                    Boolean value = (Boolean) evt.getNewValue();
                    recordData[i] = value;
                }
            }
        }
    }
}
