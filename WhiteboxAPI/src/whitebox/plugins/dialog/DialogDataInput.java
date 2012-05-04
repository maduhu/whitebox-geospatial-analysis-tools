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
package whitebox.plugins.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;

/**
 *
 * @author johnlindsay
 */
public class DialogDataInput extends JPanel implements ActionListener, DialogComponent {
   
    private int numArgs = 6;
    private String name;
    private String description;
    private String value;
    private String label;
    private JLabel lbl = new JLabel();
    private boolean makeOptional = false;
    private boolean numericalInputOnly = false;
    private JTextField text = new JTextField(25);
    private String initialText = "";
    
    private void createUI() {
        try {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setMaximumSize(new Dimension(2500, 40));
            this.setPreferredSize(new Dimension(350, 40));
        
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            lbl = new JLabel(label);
            this.add(lbl);
            this.add(Box.createHorizontalStrut(5));
            text = new JTextField();
            if (numericalInputOnly) { text.setHorizontalAlignment(JTextField.RIGHT); }
            text.setText(initialText);
            this.add(text);
            this.setToolTipText(description);
            this.setToolTipText(description);
            text.setToolTipText(description);
            lbl.setToolTipText(description);
            
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    @Override
    public String getValue() {
        if (validateValue(text.getText())) {
            value = text.getText();
        } else {
            value = null;
        }
        if (value.equals("")) {
            if (makeOptional) {
                value = "not specified";
            } else {
                value = null;
            }
        }
        return value;
    }
    
    private boolean validateValue(String val) {
        String ret = "";
        if (!numericalInputOnly) {
            return true;
        } else {
            try {
                if (!val.equals("")) {
                    Double dbl = Double.parseDouble(val);
                }
                return true;
            } catch (Exception e) {
                text.setText("");
                return false;
            }
        }
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
            initialText = args[3];
            numericalInputOnly = Boolean.parseBoolean(args[4]);
            makeOptional = Boolean.parseBoolean(args[5]);
            
            if (validateValue(initialText)) {
                value = initialText;
                text.setText(value);
            }
            
            createUI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "String label";
        argsDescriptors[3] = "String initialText";
        argsDescriptors[4] = "boolean numericalInputOnly";
        argsDescriptors[5] = "boolean makeOptional";
        return argsDescriptors;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox)e.getSource();
        value = (String)cb.getSelectedItem();
    }
}
