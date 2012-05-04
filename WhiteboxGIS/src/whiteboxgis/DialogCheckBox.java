package whiteboxgis;

//import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;

/**
 *
 * @author johnlindsay
 */
public class DialogCheckBox extends JPanel implements ItemListener, DialogComponent {
   
    private int numArgs = 4;
    private String name;
    private String description;
    private String value;
    private String label;
    private JCheckBox check = new JCheckBox();
    private boolean initialState = true;
    
    private void createGui() {
        try {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(check);
            
            check.addItemListener(this);
            this.setToolTipText(description);
            panel.setToolTipText(description);
            check.setToolTipText(description);
            check.setSelected(initialState);
            this.add(panel);
            this.add(Box.createHorizontalGlue());
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    @Override
    public String getValue() {
        return value;
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
            check.setText(label);
            initialState = Boolean.parseBoolean(args[3]);
            value = Boolean.toString(initialState);
            createGui();
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
        argsDescriptors[3] = "boolean initialState";
        return argsDescriptors;
    }
    
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            value = "false";
        } else if (e.getStateChange() == ItemEvent.SELECTED) {
            value = "true";
        }
    }
}
