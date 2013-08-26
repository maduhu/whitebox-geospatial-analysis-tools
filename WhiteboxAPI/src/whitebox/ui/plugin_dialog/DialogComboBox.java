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

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;

public class DialogComboBox extends JPanel implements ActionListener, DialogComponent {
   
    private int numArgs = 5;
    private String name;
    private String description;
    private String value;
    private String label;
    private JLabel lbl = new JLabel();
    private JComboBox comboBox = new JComboBox();
    
    private void createUI() {
        try {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            
            comboBox.addActionListener(this);
            lbl = new JLabel(label);
            panel.add(lbl);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(comboBox);
            panel.setToolTipText(description);
            this.setToolTipText(description);
            comboBox.setToolTipText(description);
            lbl.setToolTipText(description);
            this.add(panel);
            this.add(Box.createHorizontalGlue());
            
            this.setMaximumSize(new Dimension(2500, 40));
            this.setPreferredSize(new Dimension(350, 40));
        
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    @Override
    public String getValue() {
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
            String[] listItems = args[3].split(",");
            for (int i = 0; i < listItems.length; i++) {
                listItems[i] = listItems[i].trim();
            }
            comboBox = new JComboBox(listItems);
            comboBox.setSelectedIndex(Integer.parseInt(args[4]));
            value = (String)comboBox.getSelectedItem();
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
        argsDescriptors[3] = "String[] listItems";
        argsDescriptors[4] = "Int defaultItem (zero-based)";
        return argsDescriptors;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox)e.getSource();
        value = (String)cb.getSelectedItem();
    }
}
