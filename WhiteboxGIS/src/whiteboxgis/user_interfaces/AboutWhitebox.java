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
package whiteboxgis.user_interfaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import whitebox.interfaces.WhiteboxPluginHost;
import whiteboxgis.WhiteboxGui;


/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AboutWhitebox extends JDialog implements ActionListener {

    private String graphicsDirectory = "";
    private String versionNumber = "";
    private String versionName = "";
    private ResourceBundle bundle;
    
    public AboutWhitebox(Frame owner, boolean modal, String graphicsDirectory, 
            String versionName, String versionNumber) {
        super(owner, modal);
        if (bundle != null) {
            this.setTitle(bundle.getString("About") + " Whitebox GAT");
        } else {
            this.setTitle("About Whitebox GAT");
        }
        this.graphicsDirectory = graphicsDirectory;
        this.versionName = versionName;
        this.versionNumber = versionNumber;
        if (owner instanceof WhiteboxPluginHost) {
            WhiteboxPluginHost host = (WhiteboxPluginHost) owner;
            this.bundle = host.getGuiLabelsBundle();
        }
        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        
        File file = new File(graphicsDirectory + "WhiteboxLogo.png");
        if (!file.exists()) {
            return;
        }
        ImagePanel imagePane = new ImagePanel(file.toString());
        
        mainPane.add(imagePane);
        mainPane.add(Box.createVerticalStrut(10));
        
        JLabel label4 = new JLabel(versionName + " (" + versionNumber + ") released 2013");
        Box box4 = Box.createHorizontalBox();
        box4.add(Box.createHorizontalGlue());
        box4.add(label4);
        box4.add(Box.createHorizontalGlue());
        mainPane.add(box4);
        mainPane.add(Box.createVerticalStrut(10));
        
        
        JTextArea contributors = new JTextArea();
        JScrollPane scroll = new JScrollPane(contributors);
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Dr. John Lindsay (Lead Developer)\n");
        strBuilder.append("Centre for Hydrogeomatics\n");
        strBuilder.append("The University of Guelph, Canada\n");
        strBuilder.append("e-mail: jlindsay@uoguelph.ca\n\n");
        
        strBuilder.append(bundle.getString("i18nContributors")).append("\n\n");
        //strBuilder.append("Heikki Doeleman\n");
        strBuilder.append("Agustin Diez Castillo\n");
        strBuilder.append("Sergi Gumà\n");
        strBuilder.append("George Miliaresis\n");
        strBuilder.append("Hannes Reuter\n");
        strBuilder.append("Hu Xuemei\n");
        strBuilder.append("Raf Roset\n");
        strBuilder.append("Ehsan Roshani\n");
        strBuilder.append("Rafal Wawer\n");
        strBuilder.append("秦承志 (QIN Cheng-Zhi)\n");
        
        contributors.setText(strBuilder.toString());
        contributors.setLineWrap(true);
        contributors.setWrapStyleWord(true);
        contributors.setCaretPosition(0);
        contributors.setEditable(false);
        mainPane.add(scroll);
        
//        JPanel textPane = new JPanel();
//        
//        JLabel label4 = new JLabel(versionName + " (" + versionNumber + ") released 2013");
//        Box box4 = Box.createHorizontalBox();
//        box4.add(label4);
//        textPane.add(box4);
//        textPane.add(Box.createVerticalStrut(15));
//        
//        textPane.setLayout(new BoxLayout(textPane, BoxLayout.Y_AXIS));
//        JLabel label1 = new JLabel("Dr. John Lindsay (Lead Developer)");
//        Box box1 = Box.createHorizontalBox();
//        box1.add(label1);
//        box1.add(Box.createHorizontalGlue());
//        
//        JLabel label5 = new JLabel("Centre for Hydrogeomatics");
//        Box box5 = Box.createHorizontalBox();
//        box5.add(label5);
//        box5.add(Box.createHorizontalGlue());
//        
//        JLabel label2 = new JLabel("The University of Guelph, Canada");
//        Box box2 = Box.createHorizontalBox();
//        box2.add(label2);
//        box2.add(Box.createHorizontalGlue());
//        
//        JLabel label3 = new JLabel("e-mail: jlindsay@uoguelph.ca");
//        Box box3 = Box.createHorizontalBox();
//        box3.add(label3);
//        box3.add(Box.createHorizontalGlue());
//        
//        Box vbox = Box.createVerticalBox();
//        vbox.add(box1);
//        vbox.add(box2);
//        vbox.add(box3);
//        //vbox.add(box4);
//        
//        textPane.add(vbox);
//        mainPane.add(textPane);
        mainPane.add(Box.createVerticalStrut(10));
        

        // buttons
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setActionCommand("ok");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));

        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(ok);
        buttonPane.add(Box.createHorizontalGlue());

        Container contentPane = getContentPane();
        contentPane.add(mainPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();

        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("ok")) {
            this.dispose();
        }
    }
    
    
    class ImagePanel extends JPanel {

        private BufferedImage image;

        public ImagePanel(String fileName) {
            try {
                image = ImageIO.read(new File(fileName));
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                setMinimumSize(new Dimension(image.getWidth(), image.getHeight()));
                setMaximumSize(new Dimension(image.getWidth(), image.getHeight()));
                
            } catch (IOException ex) {
                // handle exception...
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);

        }
    }
}
