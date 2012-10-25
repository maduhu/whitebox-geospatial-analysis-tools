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
package rastercalculator;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import whitebox.interfaces.ThreadListener;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.ExtensionFileFilter;


/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class RasterCalculator extends JDialog implements ActionListener, 
        ThreadListener {

    String workingDirectory;
    private JList list;
    private JProgressBar progress = new JProgressBar(0, 100);
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JScrollPane jScrollPane2 = new JScrollPane();;
    private JTextArea textArea = new JTextArea();
    private String pathSep = File.separator;
    private WhiteboxPluginHost host = null;
    private Map<String, String> images = new HashMap<String, String>();
    private Thread thread = null;

    public RasterCalculator(Frame owner, boolean modal, String workingDirectory) {
        super(owner, modal);
        this.workingDirectory = workingDirectory;
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost)owner;
        }
        createGui();
    }
    
    public void reportProgress(int value) {
        progress.setValue(value);
    }
    
    private JButton createButton(String buttonLabel, String toolTip) {
        JButton btn = new JButton(buttonLabel);
        btn.addActionListener(this);
        btn.setActionCommand(buttonLabel);
        btn.setToolTipText(toolTip);
        //btn.setPreferredSize(new Dimension(width, 22));
        return btn;
    }

    private JButton createButton(String buttonLabel) {
        JButton btn = new JButton(buttonLabel);
        btn.addActionListener(this);
        btn.setActionCommand(buttonLabel);
        //btn.setPreferredSize(new Dimension(width, 22));
        return btn;
    }
    
    Map<String,StringIntPair> map = new HashMap<String,StringIntPair>();
    private Vector getOtherFunctionsModel() {
        
        Vector data = new Vector();
        data.add("arccos()");
        data.add("arcsin()");
        data.add("arctan()");
        data.add("cosh()");
        data.add("delete([image],...)");
        data.add("if(Cond,Then,Else)");
        data.add("isNoData([image])");
        data.add("MAX(,)");
        data.add("MIN(,)");
        data.add("negate()");
        data.add("sinh()");
        data.add("tanh()");
        
        
        map.put("arccos()", new StringIntPair("arccos()", 1));
        map.put("arcsin()", new StringIntPair("arcsin()", 1));
        map.put("arctan()", new StringIntPair("arctan()", 1));
        map.put("cosh()", new StringIntPair("cosh()", 1));
        map.put("delete([image],...)", new StringIntPair("delete()", 1));
        map.put("MAX(,)", new StringIntPair("MAX(,)", 2));
        map.put("MIN(,)", new StringIntPair("MIN(,)", 2));
        map.put("negate()", new StringIntPair("negate()", 1));
        map.put("sinh()", new StringIntPair("sinh()", 1));
        map.put("tanh()", new StringIntPair("tanh()", 1));
        map.put("if(Cond,Then,Else)", new StringIntPair("if((Condition),Then,Else)", 21));
        map.put("isNoData([image])", new StringIntPair("isNoData()", 1));
        
        return data;
    }
    
    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        
        setTitle("Raster Calculator");
        
        // buttons
        JButton btnExit = createButton("Exit");
        JButton btnEvaluate = createButton("Evaluate");
        JButton btnStop = createButton("Stop");
        JButton btnHelp = createButton("Help");
        JButton btnAddImage = createButton("Add Image");
        JButton btnEqual = createButton("=", "Assignment");
        JButton btnClear = createButton("Clear");
        JButton btnBackspace = createButton("DEL", "Backspace");
        JButton btnSqrBrackets = createButton("[ ]", "Square brackets denote images.");
        JButton btn1 = createButton("1");
        JButton btn2 = createButton("2");
        JButton btn3 = createButton("3");
        JButton btn4 = createButton("4");
        JButton btn5 = createButton("5");
        JButton btn6 = createButton("6");
        JButton btn7 = createButton("7");
        JButton btn8 = createButton("8");
        JButton btn9 = createButton("9");
        JButton btn0 = createButton("0");
        JButton btnDecimal = createButton(".");
        JButton btnNegate = createButton("(-)", "Negate");
        JButton btnPi = createButton("pi", "pi");
        JButton btnBrackets = createButton("( )");
        JButton btnAbs = createButton("abs", "Absolute Value");
        JButton btnDivision = createButton("\u00F7", "Floating-Point Division");
        JButton btnMultiplication = createButton("\u00D7", "Multiplication");
        JButton btnAddition = createButton("+", "Addition");
        JButton btnSubtraction = createButton("\u2212", "Subtraction");
        JButton btnMod = createButton("%", "Modulo (Remainder)");
        JButton btnIntDiv = createButton("\\", "Integer Division");
        JButton btnSqrt = createButton("\u221A", "Square Root");
        JButton btnSqr = createButton("sqr", "Square");
        JButton btnPow = createButton("pow", "Power");
        JButton btnSin = createButton("sin", "Sine");
        JButton btnCos = createButton("cos", "Cosine");
        JButton btnTan = createButton("tan", "Tangent");
        JButton btnLog = createButton("log", "Base-10 Logarithm");
        JButton btnLn = createButton("Ln", "Natural Logarithm (base-e)");
        JButton btnExp = createButton("Exp", "Exponentiation");
        JButton btnEqualTo = createButton("==", "Equal To");
        JButton btnNotEqualTo = createButton("!=", "Not Equal To");
        JButton btnGreaterThan = createButton(">", "Greater Than");
        JButton btnGreaterThanEqualTo = createButton(">=", "Greater Than Equal To");
        JButton btnLessThan = createButton("<", "Less Than");
        JButton btnLessThanEqualTo = createButton("<=", "Less Than Equal To");
        JButton btnAnd = createButton("AND", "Boolean AND");
        JButton btnNot = createButton("NOT", "Boolean NOT");
        JButton btnOr = createButton("OR", "Boolean OR");
        JButton btnXor = createButton("XOR", "Boolean XOR");
    
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));
        topPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        Box topBox = Box.createHorizontalBox();
        Box expressionBox = Box.createVerticalBox();
        Box labelBox1 = Box.createHorizontalBox();
        JLabel label1 = new JLabel("Expression:");
        labelBox1.add(label1);
        labelBox1.add(Box.createHorizontalGlue());
        expressionBox.add(labelBox1);
        expressionBox.add(Box.createVerticalStrut(4));
        jScrollPane1 = new JScrollPane(textArea);
        expressionBox.add(jScrollPane1);
        expressionBox.setPreferredSize(new Dimension(10, 80));
        topBox.add(expressionBox);
        
        Box functionBox = Box.createVerticalBox();
        Box labelBox2 = Box.createHorizontalBox();
        JLabel label2 = new JLabel("Other Functions:");
        labelBox2.add(label2);
        labelBox2.add(Box.createHorizontalGlue());
        functionBox.add(labelBox2);
        functionBox.add(Box.createVerticalStrut(4));
        Vector data = getOtherFunctionsModel();
        list = new JList(data);
        list.addMouseListener(new ActionJList(list, textArea, map));
        
        jScrollPane2 = new JScrollPane(list);
        functionBox.setPreferredSize(new Dimension(160, 10));
        functionBox.setMaximumSize(new Dimension(160, 800));
        functionBox.add(jScrollPane2);
        functionBox.add(Box.createVerticalGlue());
        topBox.add(Box.createHorizontalStrut(5));
        topBox.add(functionBox);
        
        topPane.add(topBox);
        
        topPane.add(Box.createVerticalStrut(2));
        Box addImageBox = Box.createHorizontalBox();
        addImageBox.add(btnAddImage);
        addImageBox.add(btnSqrBrackets);
        addImageBox.add(btnEqual);
        addImageBox.add(btnClear);
        addImageBox.add(btnBackspace);
        addImageBox.add(Box.createHorizontalGlue());
        topPane.add(addImageBox);
        
        mainPane.add(topPane);
        
        JPanel buttonPane2 = new JPanel();
        buttonPane2.setLayout(new BoxLayout(buttonPane2, BoxLayout.Y_AXIS));
        buttonPane2.setBorder(BorderFactory.createLoweredBevelBorder());
        
        JPanel buttonPane = new JPanel();
        GridLayout layout = new GridLayout(0, 8);
        buttonPane.setLayout(layout);
        //buttonPane.setLayout(new GridLayout(8, 5));
        //buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
        //buttonPane.setBorder(BorderFactory.createLoweredBevelBorder());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        buttonPane.add(btn7);
        buttonPane.add(btn8);
        buttonPane.add(btn9);
        buttonPane.add(btnDivision);
        buttonPane.add(btnIntDiv);
        buttonPane.add(btnSin);
        buttonPane.add(btnEqualTo);
        buttonPane.add(btnNotEqualTo);
        
        buttonPane.add(btn4);
        buttonPane.add(btn5);
        buttonPane.add(btn6);
        buttonPane.add(btnMultiplication);
        buttonPane.add(btnSqrt);
        buttonPane.add(btnCos);
        buttonPane.add(btnGreaterThan);
        buttonPane.add(btnGreaterThanEqualTo);
        
        buttonPane.add(btn1);
        buttonPane.add(btn2);
        buttonPane.add(btn3);
        buttonPane.add(btnSubtraction);
        buttonPane.add(btnSqr);
        buttonPane.add(btnTan);
        buttonPane.add(btnLessThan);
        buttonPane.add(btnLessThanEqualTo);
        
        buttonPane.add(btn0);
        buttonPane.add(btnDecimal);
        buttonPane.add(btnNegate);
        buttonPane.add(btnAddition);
        buttonPane.add(btnPow);
        buttonPane.add(btnLog);
        buttonPane.add(btnAnd);
        buttonPane.add(btnNot);
        
        buttonPane.add(btnPi);
        buttonPane.add(btnBrackets);
        buttonPane.add(btnAbs);
        buttonPane.add(btnMod);
        buttonPane.add(btnExp);
        buttonPane.add(btnLn);
        buttonPane.add(btnOr);
        buttonPane.add(btnXor);
        
        buttonPane2.add(buttonPane);
        
        Box progressBox = Box.createHorizontalBox();
        progressBox.add(Box.createHorizontalStrut(10));
        JLabel label3 = new JLabel("Progress:");
        progressBox.add(label3);
        progressBox.add(Box.createHorizontalStrut(5));
        progressBox.add(progress);
        buttonPane2.add(progressBox);
        progressBox.add(Box.createHorizontalStrut(10));
        
        buttonPane2.add(Box.createVerticalStrut(10));
        
        mainPane.add(buttonPane2);
       
        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.X_AXIS));
        bottomPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        bottomPane.add(btnEvaluate);
        bottomPane.add(btnStop);
        bottomPane.add(Box.createHorizontalGlue());
        bottomPane.add(btnExit);
        bottomPane.add(btnHelp);

        Container contentPane = getContentPane();
        contentPane.add(mainPane, BorderLayout.CENTER);
        
        contentPane.add(bottomPane, BorderLayout.SOUTH);
        //contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();

        this.setVisible(true);
    }
    
    boolean firstImageSelected = false;

    private void addImage() {
        String str;
                
        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String filterDescription = "Raster Files (*.dep)";
        String[] extensions = {"DEP"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);

        filters.add(eff);

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(workingDirectory));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        int result = fc.showOpenDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            String fileDirectory = file.getParentFile() + pathSep;
            if (!firstImageSelected) {
                workingDirectory = fileDirectory;
            }
            if (fileDirectory.equals(workingDirectory)) {
                str = "[" + file.getName().replace(".dep", "") + "]";
            } else {
                str = "[" + file.toString() + "]";
            }
            
            textArea.insert(str, textArea.getCaretPosition());
            textArea.requestFocus();
        }
        
        firstImageSelected = true;
    }
    
    private void cancelOperation() {
        if (thread.isAlive()) {
            //ProcessExpression pe = (ProcessExpression)thread;
            
            thread.interrupt();
            textArea.insert("\nOperation Cancelled!", textArea.getText().length());
            textArea.requestFocus();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //Object source = e.getSource();
        String ac = e.getActionCommand();
        if (ac.equals("Evaluate")) {
            String[] expressions = textArea.getText().split("\n");
            processEquations(expressions);
        } else if (ac.equals("Exit")) {
            this.dispose();
        } else if (ac.equals("Stop")) {
            cancelOperation();
        } else if (ac.equals("[ ]")) {
            textArea.insert("[]", textArea.getCaretPosition());
            textArea.setCaretPosition(textArea.getCaretPosition() - 1);
            textArea.requestFocus();
        } else if (ac.equals("( )")) {
            textArea.insert("()", textArea.getCaretPosition());
            textArea.setCaretPosition(textArea.getCaretPosition() - 1);
            textArea.requestFocus();
        } else if (ac.equals("Add Image")) {
            addImage();
        } else if (ac.equals("Clear")) {
            textArea.setText("");
            textArea.requestFocus();
        } else if (ac.equals("DEL")) {
            String str = textArea.getText();
            int pos = textArea.getCaretPosition();
            if (pos > 0) {
                str = str.substring(0, pos - 1) + str.substring(pos, str.length());
                textArea.setText(str);
                textArea.setCaretPosition(pos - 1);
            }
            textArea.requestFocus();
        } else if (ac.toLowerCase().equals("pow")) {
            textArea.insert("^", textArea.getCaretPosition());
            textArea.requestFocus();
            
        } else if (ac.toLowerCase().equals("sin") || 
                ac.toLowerCase().equals("cos") ||
                ac.toLowerCase().equals("tan") || 
                ac.toLowerCase().equals("sqr") ||
                ac.toLowerCase().equals("sqrt") ||
                ac.toLowerCase().equals("log") ||
                ac.toLowerCase().equals("ln") ||
                ac.toLowerCase().equals("abs") ||
                ac.toLowerCase().equals("exp") ||
                ac.toLowerCase().equals("\u221A")){
            textArea.insert(ac + "()", textArea.getCaretPosition());
            textArea.setCaretPosition(textArea.getCaretPosition() - 1);
            textArea.requestFocus();
        } else if (ac.toLowerCase().equals("and") ||
                ac.toLowerCase().equals("not")||
                ac.toLowerCase().equals("or")||
                ac.toLowerCase().equals("xor")) {
            textArea.insert(ac + "(,)", textArea.getCaretPosition());
            textArea.setCaretPosition(textArea.getCaretPosition() - 2);
            textArea.requestFocus();
        } else {
            textArea.insert(ac, textArea.getCaretPosition());
            textArea.requestFocus();
        }
    }
    
    public void processEquations(String[] equations) {
        try {
            String expression;
            boolean needToInsertOutputImage = false;
            boolean expressionContainsImage = false;
            boolean isDeleteExpression = false;
            int a, b, c, i;
            int imageNumber = 1;
            boolean flag = false;
            String imageName, imageAlias;
            for (a = 0; a < equations.length; a++) {
                // first trim the white-spaces from the expression
                expression = equations[a].trim(); //.replace(" ", "");
                needToInsertOutputImage = false;
                expressionContainsImage = expression.contains("]");
                if (expression.toLowerCase().contains("delete(") || expression.toLowerCase().contains("del(")) {
                    isDeleteExpression = true;
                }
                if (!expression.contains("]=") && expressionContainsImage 
                        && !isDeleteExpression) {
                    needToInsertOutputImage = true;
                } else {
                    b = expression.indexOf("]=");
                    c = expression.indexOf("]==");
                    if (b == c && expressionContainsImage && !isDeleteExpression) {//it's an equality operation and not an assignment
                        needToInsertOutputImage = true;
                    }
                }
                if (needToInsertOutputImage) {
                    expression = "[" + getASuitableOutputFileName() + "]=" + expression;
                }
                
                //place all of the image names into the Images dictionary
                b = -1;
                flag = false;
                do {
                    b = expression.indexOf("[", b + 1);
                    if (b == -1) {
                        flag = true;
                    } else {
                        //modExpressions.NumberOfImages += 1
                        c = expression.indexOf("]", b + 1);
                        imageName = expression.substring(b + 1, c);
                        imageAlias = "IMAGE" + imageNumber;
                        expression = expression.replace("[" + imageName + "]", imageAlias);

                        if (imageName.indexOf(pathSep) == -1) { //no directory has been specified
                            imageName = workingDirectory + imageName;
                        }
                        if (imageName.indexOf(".dep") == -1) {
                            imageName = imageName + ".dep";
                        }
                        images.put(imageAlias, imageName);
                        //modExpressions.ImageNames.Add(ImageAlias, ImageName)
                        imageNumber++;
                    }
                } while (!flag);
                
                // remove all spaces, which is safe to do because the image names have been removed.
                //expression = equations[a].replace(" ", "");
                
                ProcessExpression pe = new ProcessExpression(workingDirectory, expression, this);
                pe.setImage(images);
                thread = new Thread(pe);
                thread.start();
                
            }
            //return true;
        } catch (Exception e) {
            System.out.print(e);
        }
    }
    
    private String getASuitableOutputFileName() {
        boolean flag = false;
        String fileName = "";
        int a = 0;
        do {
            a++;
            fileName = workingDirectory + "Calc_Output_" + a + ".dep";
            if (!(new File(fileName)).exists()) {
                flag = true;
            }
            if (a == 1000) {
                flag = true; //this is just in place to make sure that 
                //it doesn't encounter an endless loop
            }
        } while (!flag);
        return fileName;
    }
    
    
        /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        RasterCalculator rc = new RasterCalculator(frame, true, "");
    }

    @Override
    public void notifyOfThreadComplete(Runnable thread) {
        progress.setValue(0);
//        ProcessExpression pe = (ProcessExpression)thread;
//        String returnValue = pe.getReturnValue();
//        if (returnValue.contains("IMAGE")) {
//            returnValue = images.get(returnValue);
//        }
//        if (!returnValue.contains(".dep")) {
//            textArea.setText(textArea.getText() + "\n" + returnValue);
//        } else {
//            if (host != null) {
//                host.returnData(returnValue);
//            } else {
//                textArea.setText(textArea.getText() + "\n" + returnValue);
//            }
//        }
    }

    @Override
    public void notifyOfProgress(int progressVal) {
        progress.setValue(progressVal);
    }

    @Override
    public void passOnThreadException(Exception e) {
        if (e.getMessage() != null) {
            textArea.setText(textArea.getText() + "\n" + e.getMessage());
        } else {
            textArea.setText(textArea.getText() + "\n" + e.toString());
        }
    }

    @Override
    public void notifyOfReturn(String returnValue) {
        if (returnValue != null) {
            if (returnValue.contains("IMAGE")) {
                returnValue = images.get(returnValue);
            }
            if (!returnValue.contains(".dep")) {
                textArea.setText(textArea.getText() + "\n" + returnValue);
            } else {
                if (host != null) {
                    host.returnData(returnValue);
                } else {
                    textArea.setText(textArea.getText() + "\n" + returnValue);
                }
            }
        }
    }

    @Override
    public int showFeedback(String feedback) {
        textArea.setText(textArea.getText() + "\n" + feedback);
        return 0;
    }
}

class ActionJList extends MouseAdapter {
    protected JTextArea textArea;
    protected JList list;
    protected Map<String,StringIntPair> map;

    public ActionJList(JList l, JTextArea text, Map<String,StringIntPair> map) {
        list = l;
        textArea = text;
        this.map = map;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            int index = list.locationToIndex(e.getPoint());
            ListModel dlm = list.getModel();
            Object item = dlm.getElementAt(index);
            list.ensureIndexIsVisible(index);
            StringIntPair sip = (StringIntPair)map.get(item);
            String str = sip.getString(); // + "()";
            textArea.insert(str, textArea.getCaretPosition());
            textArea.setCaretPosition(textArea.getCaretPosition() - sip.getVal());
            textArea.requestFocus();
        }
    }
}

class StringIntPair {
    private String string = "";
    private int val = 0;

    
    public StringIntPair(String s, int i) {
        string = s;
        val = i;
    }
    
    public String getString() {
        return string;
    }

    public int getVal() {
        return val;
    }
}

