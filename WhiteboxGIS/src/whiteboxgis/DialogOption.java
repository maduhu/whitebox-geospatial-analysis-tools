package whiteboxgis;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;

/**
 *
 * @author johnlindsay
 */
public class DialogOption extends JPanel implements ActionListener, DialogComponent {
   
    private int numArgs = 5;
    private String name;
    private String description;
    private String value;
    private String label;
    private String button1Label = "";
    private String button2Label = "";
    private JRadioButton button1 = new JRadioButton();
    private JRadioButton button2 = new JRadioButton();
    private JPanel panel = new JPanel();
    
    private void createUI() {
        try {
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY), label);
            title.setTitleJustification(TitledBorder.LEFT);
            panel.setBorder(title);
            
            button1 = new JRadioButton(button1Label);
            button1.setActionCommand(button1Label);
            button1.setSelected(true);
            button2 = new JRadioButton(button2Label);
            button2.setActionCommand(button2Label);
            
            ButtonGroup group = new ButtonGroup();
            group.add(button1);
            group.add(button2);
            
            button1.addActionListener(this);
            button2.addActionListener(this);
            
            Box box1 = Box.createHorizontalBox();
            box1.add(Box.createHorizontalStrut(5));
            box1.add(button1);
            box1.add(Box.createHorizontalGlue());
            
            Box box2 = Box.createHorizontalBox();
            box2.add(Box.createHorizontalStrut(5));
            box2.add(button2);
            box2.add(Box.createHorizontalGlue());
            
            panel.add(box1);
            panel.add(box2);
            
            this.add(panel);
            this.setToolTipText(description);
            button1.setToolTipText(description);
            button2.setToolTipText(description);
            
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    public String getValue() {
        return value.trim();
    }
    
    public String getComponentName() {
        return name;
    }
    
    public boolean getOptionalStatus() {
        return false;
    }
    
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            label = args[2];
            button1Label = args[3];
            button2Label = args[4];
            value = button1Label;
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
        argsDescriptors[3] = "String button1String";
        argsDescriptors[4] = "String button2String";
        return argsDescriptors;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        value = e.getActionCommand();
    }
}
