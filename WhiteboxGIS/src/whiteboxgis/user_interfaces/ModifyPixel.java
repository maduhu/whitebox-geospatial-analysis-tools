package whiteboxgis.user_interfaces;

import java.awt.Frame;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Container;
import javax.swing.*;
import java.awt.BorderLayout;
import whitebox.structures.GridCell;

/**
 *
 * @author johnlindsay
 */
public class ModifyPixel extends JDialog implements ActionListener {

    GridCell point = null;
    JTextField tf = null;
    JTextField tfR = null;
    JTextField tfG = null;
    JTextField tfB = null;
    JTextField tfA = null;

    public ModifyPixel(Frame owner, boolean modal, GridCell point, String fileName) {
        super(owner, modal);
        this.setTitle(fileName);
        this.point = point;

        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }


        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));

        JPanel rowAndColPane = new JPanel();
        rowAndColPane.setLayout(new BoxLayout(rowAndColPane, BoxLayout.X_AXIS));
        rowAndColPane.add(new JLabel("Row: " + point.row));
        rowAndColPane.add(Box.createHorizontalStrut(15));
        rowAndColPane.add(new JLabel("Column: " + point.col));
        rowAndColPane.add(Box.createHorizontalGlue());
        mainPane.add(rowAndColPane);
        mainPane.add(Box.createVerticalStrut(5));

        tf = new JTextField(15);
        tf.setHorizontalAlignment(JTextField.RIGHT);
        if (!point.isValueNoData()) {
            tf.setText(String.valueOf(point.z));
        } else {
            tf.setText("NoData");
        }
        tf.setMaximumSize(new Dimension(35, 22));

        if (!point.isRGB) {
            JPanel valPane = new JPanel();
            valPane.setLayout(new BoxLayout(valPane, BoxLayout.X_AXIS));
            valPane.add(new JLabel("Value: "));
            valPane.add(tf);
            valPane.add(Box.createHorizontalGlue());
            mainPane.add(valPane);
            mainPane.add(Box.createVerticalStrut(5));

        } else {
            JPanel valPane = new JPanel();
            valPane.setLayout(new BoxLayout(valPane, BoxLayout.X_AXIS));
            valPane.add(new JLabel("Value: "));
            valPane.add(tf);
            valPane.add(Box.createHorizontalGlue());
            mainPane.add(valPane);
            mainPane.add(Box.createVerticalStrut(5));

            String r = "";
            String g = "";
            String b = "";
            String a = "";

            if (!point.isValueNoData()) {
                r = String.valueOf((int) point.z & 0xFF);
                g = String.valueOf(((int) point.z >> 8) & 0xFF);
                b = String.valueOf(((int) point.z >> 16) & 0xFF);
                a = String.valueOf(((int) point.z >> 24) & 0xFF);
            }

            tfR = new JTextField(5);
            tfG = new JTextField(5);
            tfB = new JTextField(5);
            tfA = new JTextField(5);

            tfR.setHorizontalAlignment(JTextField.RIGHT);
            tfG.setHorizontalAlignment(JTextField.RIGHT);
            tfB.setHorizontalAlignment(JTextField.RIGHT);
            tfA.setHorizontalAlignment(JTextField.RIGHT);

            tfR.setText(r);
            tfG.setText(g);
            tfB.setText(b);
            tfA.setText(a);

            JPanel rgbPane = new JPanel();

            rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
            rgbPane.add(new JLabel("R: "));
            rgbPane.add(tfR);
            rgbPane.add(Box.createHorizontalGlue());

            rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
            rgbPane.add(new JLabel(" G: "));
            rgbPane.add(tfG);
            rgbPane.add(Box.createHorizontalGlue());

            rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
            rgbPane.add(new JLabel(" B: "));
            rgbPane.add(tfB);
            rgbPane.add(Box.createHorizontalGlue());

            rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
            rgbPane.add(new JLabel(" a: "));
            rgbPane.add(tfA);
            rgbPane.add(Box.createHorizontalGlue());

            mainPane.add(rgbPane);
            mainPane.add(Box.createVerticalStrut(5));

        }

        // buttons
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setActionCommand("ok");
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        cancel.setActionCommand("cancel");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(ok);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(cancel);
        buttonPane.add(Box.createHorizontalGlue());

        Container contentPane = getContentPane();
        contentPane.add(mainPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();

        this.setVisible(true);
    }

    public void confirmValue() {
        try {
            if (!point.isRGB) {
                if (tf.getText().toLowerCase().contains("nodata")) {
                    point.z = point.noDataValue;
                } else {
                    double z = Double.parseDouble(tf.getText());
                    point.z = z;
                }
                successful = true;
            } else {
                if (tf.getText().toLowerCase().contains("nodata")) {
                    point.z = point.noDataValue;
                } else {
                    int r = Integer.parseInt(tfR.getText());
                    int g = Integer.parseInt(tfG.getText());
                    int b = Integer.parseInt(tfB.getText());
                    int a = Integer.parseInt(tfA.getText());
                    double z = (double) ((a << 24) | (b << 16) | (g << 8) | r);
                    point.z = z;
                }
                successful = true;
            }
        } catch (Exception e) {
            System.out.println(e);
            successful = false;
        }
    }
    boolean successful = false;

    public boolean wasSuccessful() {
        return successful;
    }

    public GridCell getValue() {
        return point;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("ok")) {
            confirmValue();
            this.dispose();
        } else if (actionCommand.equals("cancel")) {
            this.dispose();
        }
    }
}