/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.ui;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Adrian BER (beradrian@yahoo.com)
 */
public class JFontChooserDemo extends JPanel {

    private static final Insets INSETS = new Insets(5, 5, 5, 5);

    private JFontChooser fontChooser;
    private JCheckBox defaultPreviewCheckBox;
    private JTextField previewTextField;
    private JLabel previewLabel;
    private JTextArea codeTextArea;

    public JFontChooserDemo() {
        init();
    }

    private void init() {
        setLayout(new GridBagLayout());

        defaultPreviewCheckBox = new JCheckBox("Use font name as the preview text");
        defaultPreviewCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean selected = defaultPreviewCheckBox.isSelected();
                fontChooser.setPreviewText(selected ? null : previewTextField.getText());
                previewLabel.setEnabled(!selected);
                previewTextField.setEnabled(!selected);
                updateCode();
            }
        });
        add(defaultPreviewCheckBox, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, INSETS, 0, 0));

        previewLabel = new JLabel("Preview text:");
        add(previewLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, INSETS, 0, 0));

        previewTextField = new JTextField();
        previewTextField.getDocument().addDocumentListener(new DocumentListener() {
            private void changePreviewText() {
                fontChooser.setPreviewText(previewTextField.getText());
                updateCode();
            }

            public void insertUpdate(DocumentEvent e) {
                changePreviewText();
            }

            public void removeUpdate(DocumentEvent e) {
                changePreviewText();
            }

            public void changedUpdate(DocumentEvent e) {
                changePreviewText();
            }
        });
        add(previewTextField, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, INSETS, 0, 0));

        JButton testButton = new JButton("Test");
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Font font = fontChooser.showDialog(JFontChooserDemo.this, "Choose a font");
                JOptionPane.showMessageDialog(JFontChooserDemo.this, font == null ? "You canceled the dialog."
                        : "You have selected " + font.getName() + ", " + font.getSize()
                        + (font.isBold() ? ", Bold" : "") + (font.isItalic() ? ", Italic" : ""));
            }
        });
        add(testButton, new GridBagConstraints(0, 2, 2, 1, 1, 0, GridBagConstraints.NORTHEAST,
                GridBagConstraints.NONE, INSETS, 0, 0));

        codeTextArea = new JTextArea(5, 30);
        codeTextArea.setOpaque(false);
        codeTextArea.setEditable(false);
        codeTextArea.setBorder(BorderFactory.createTitledBorder("Code"));
        add(codeTextArea, new GridBagConstraints(0, 3, 2, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, INSETS, 0, 0));

        setFontChooser(new JFontChooser());
    }

    private void setFontChooser(JFontChooser fontChooser) {
        this.fontChooser = fontChooser;
        String previewText = fontChooser.getPreviewText();
        defaultPreviewCheckBox.setSelected(previewText == null);
        previewTextField.setText(previewText);
        updateCode();
    }

    private void updateCode() {
        codeTextArea.setText("JFontChooser fontChooser = new JFontChooser();\n"
                + (defaultPreviewCheckBox.isSelected() ? "" : "fontChooser.setPreviewText(\""
                    + previewTextField.getText() + "\");\n")
                + "Font font = fontChooser.showDialog(invokerComponent, \"Choose a font\");\n"
                + "System.out.println(font == null ? \"You have canceled the dialog.\" : \"You have selected \" + font);");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (fontChooser != null)
            SwingUtilities.updateComponentTreeUI(fontChooser);
    }
    
    public static void main(String args[])  {
        JFrame frame = new JFrame();
        
        JFontChooserDemo fontChooserDemo = new JFontChooserDemo();
        frame.add(fontChooserDemo);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setVisible(true);
    }
    
}