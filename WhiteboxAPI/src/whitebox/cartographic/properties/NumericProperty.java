
package whitebox.cartographic.properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.text.NumberFormat;

/**
 *
 * @author johnlindsay
 */
public class NumericProperty extends JComponent implements MouseListener, 
        PropertyChangeListener {
    private String labelText;
    private String value;
    private Color backColour = Color.WHITE;
    private int leftMargin = 10;
    private int rightMargin  = 10;
    private int preferredWidth = 200;
    private int preferredHeight = 24;
    private int textboxWidth = 15;
    private Boolean parseIntegersOnly = false;
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    
    private JFormattedTextField formattedTextField = new JFormattedTextField();
    private NumberFormat numberFormat;
    
    public NumericProperty() {
        setOpaque(true);
        revalidate();
    }
    
    public NumericProperty(String labelText, String value) {
        setOpaque(true);
        this.labelText = labelText;
        this.value = value;
        revalidate();
    }
    
    public NumericProperty(String labelText, String value, double minValue, double maxValue) {
        setOpaque(true);
        this.labelText = labelText;
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
        revalidate();
    }
    
    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public int getTextboxWidth() {
        return textboxWidth;
    }

    public void setTextboxWidth(int textboxWidth) {
        this.textboxWidth = textboxWidth;
    }

    public Boolean getParseIntegersOnly() {
        return parseIntegersOnly;
    }

    public void setParseIntegersOnly(Boolean integerNumbersOnly) {
        this.parseIntegersOnly = integerNumbersOnly;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }
    
    @Override
    public final void revalidate() {
        this.removeAll();
        this.
        setupFormat();
                
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBackground(backColour);
        this.add(Box.createHorizontalStrut(leftMargin));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        this.add(label);
        this.add(Box.createHorizontalGlue());
        formattedTextField = new JFormattedTextField(numberFormat);
        formattedTextField.setMaximumSize(new Dimension(5000, 24));
        formattedTextField.setColumns(textboxWidth);
        formattedTextField.setHorizontalAlignment(JTextField.RIGHT);
        formattedTextField.setValue(new Integer(value));
        formattedTextField.addPropertyChangeListener("value", this);
        this.add(formattedTextField);
        //formattedTextField.revalidate();
        this.add(Box.createHorizontalStrut(rightMargin));
        super.revalidate();
    }
    
    private void setupFormat() {
        numberFormat = NumberFormat.getNumberInstance();
        if (parseIntegersOnly) {
            numberFormat.setParseIntegerOnly(true);
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        Object source = e.getSource();
        if (source == formattedTextField) {
            
            if (parseIntegersOnly) {
                int val = ((Number)formattedTextField.getValue()).intValue();
                if (val < minValue) { 
                    val = (int)minValue; 
                    formattedTextField.setValue(val);
                }
                if (val > maxValue) { 
                    val = (int)maxValue; 
                    formattedTextField.setValue(val);
                }
                setValue(String.valueOf(val));
            } else {
                double val = ((Number)formattedTextField.getValue()).doubleValue();
                if (val < minValue) { 
                    val = minValue; 
                    formattedTextField.setValue(val);
                }
                if (val > maxValue) { 
                    val = maxValue; 
                    formattedTextField.setValue(val);
                }
                setValue(String.valueOf(val));
            }
            //}
        }
    }
 
    
    @Override
    public void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(getForeground());
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        
    }

    @Override
    public void mousePressed(MouseEvent e) {
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        
    }
    
}
