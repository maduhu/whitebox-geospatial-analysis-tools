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
package whitebox.ui.plugin_dialog;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.DialogComponent;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DialogFieldSelector extends JPanel implements ActionListener, DialogComponent,
        PropertyChangeListener {

    private String name;
    private String description;
    private String value;
    private String label;
    private JLabel lbl = new JLabel();
    private JList list = new JList();
    private DialogFile df;
    private Communicator hostDialog = null;
    private String shapefile = null;
    private boolean multiSelect = true;

    private void createUI() {
        try {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            Box box = Box.createVerticalBox();

            String[] args = new String[7];
            args[0] = "fileSelector"; //Name
            args[1] = "file selector"; //Description
            args[2] = "Input Shapefile:"; //getTextValue(el, "LabelText");
            args[3] = Integer.toString(DialogFile.MODE_OPEN);
            args[4] = "true"; //getTextValue(el, "ShowButton").toLowerCase();
            args[5] = "ShapeFile (*.shp), SHP"; //getTextValue(el, "Filter");
            args[6] = "false"; //getTextValue(el, "MakeOptional").toLowerCase();

            df = new DialogFile(hostDialog);
            df.setArgs(args);
            df.addPropertyChangeListener("value", this);
            df.setTextFieldActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    okPressed();
                }
            });
            box.add(df);

            //list.addListSelectionListener(this);
            Box hbox = Box.createHorizontalBox();
            lbl = new JLabel(label);
            hbox.add(lbl);
            hbox.add(Box.createHorizontalGlue());
            box.add(hbox);
            box.add(Box.createHorizontalStrut(5));

            list = new JList();

            if (!multiSelect) {
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            } else { //true
                list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }

            ListSelectionListener listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent listSelectionEvent) {
                    raisePropertyChangedEvent("");
                }
            };
            list.addListSelectionListener(listSelectionListener);

            updateList();

            JScrollPane scroller1 = new JScrollPane(list);

            box.add(scroller1);
            box.setToolTipText(description);
            this.setToolTipText(description);
            list.setToolTipText(description);
            lbl.setToolTipText(description);
            panel.add(box);
            this.add(panel);
            this.add(Box.createHorizontalGlue());

            this.setMaximumSize(new Dimension(2500, 140));
            this.setPreferredSize(new Dimension(350, 140));

        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    public void setHostDialog(Communicator hostDialog) {
        this.hostDialog = hostDialog;
    }

    private void raisePropertyChangedEvent(String oldValue) {
        value = getValue();
        firePropertyChange("value", oldValue, value);
    }

    private void updateList() {
        list.removeAll();
        DefaultListModel model = new DefaultListModel();
        if (shapefile == null) {
            return;
        }
        if (new File(shapefile.replace(".shp", ".dbf")).exists()) {

            try {
                AttributeTable table = new AttributeTable(shapefile.replace(".shp", ".dbf"));
                DBFField[] fields = table.getAllFields();
                for (DBFField field : fields) {
                    model.add(model.size(), field.getName());
                }
            } catch (Exception e) {

            }
        } else {
            model.add(0, "");
        }
        list.setModel(model);
    }

    @Override
    public String getValue() {
        Object[] selectedValues = list.getSelectedValuesList().toArray();
        value = shapefile + ";";
        for (int i = 0; i < selectedValues.length; i++) {
            if (i < selectedValues.length - 1) {
                value = value + selectedValues[i].toString() + ";";
            } else {
                value = value + selectedValues[i].toString();
            }
        }
        return value.trim();
    }
    
    @Override
    public String getComponentName() {
        return name;
    }

    @Override
    public boolean getOptionalStatus() {
        return false;
    }

    @Override
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            label = args[2];
            if (args[3].toLowerCase().contains("false")) {
                multiSelect = false;
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            } else { //true
                multiSelect = true;
                list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
            createUI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int numArgs = 4;

    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "String label";
        argsDescriptors[3] = "boolean allowMultipleSelection";
        return argsDescriptors;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();
        value = (String) cb.getSelectedItem();
    }

    private void okPressed() {
        hostDialog.showFeedback("Please press the OK button");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        shapefile = df.getValue();
        raisePropertyChangedEvent("");
        updateList();
    }
}
