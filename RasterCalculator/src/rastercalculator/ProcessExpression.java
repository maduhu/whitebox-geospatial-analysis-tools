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

import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.ThreadListener;

/**
 * This is originally based on a math parser written in VB by Rod Stephens.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ProcessExpression implements WhiteboxPluginHost, Runnable {

    private Map<String, String> images = new HashMap<>();
    private String outputFileName;
    private int numberOfImages = 0;
    private int eqDepth = 0;
    private ArrayList<String> listOfTemporaryFiles = new ArrayList<>();
    private String imageKey;
    //private boolean cancelOp = false;
    private String workingDirectory = "";
    private ThreadListener myListener = null;
    private String expressionLine = null;
    
    public ProcessExpression(String workingDirectory, String expressionLine) {
        this.workingDirectory = workingDirectory;
        this.expressionLine = expressionLine;
    }

    public ProcessExpression(String workingDirectory, String expressionLine, ThreadListener tl) {
        this.workingDirectory = workingDirectory;
        this.expressionLine = expressionLine;
        this.myListener = tl;
    }

    private String getNextTempFile() {
        numberOfImages++;
        String fileName = workingDirectory + "TemporaryFile_" + numberOfImages + ".dep";
        if ((new File(fileName)).exists()) {
            (new File(fileName)).delete();
            (new File(fileName.replace(".dep", ".tas"))).delete();
            if ((new File(fileName.replace(".dep", ".wstat"))).exists()) {
                new File(fileName.replace(".dep", ".wstat")).delete();
            }
        }

        imageKey = "IMAGE" + numberOfImages;
        images.put(imageKey, fileName);
        listOfTemporaryFiles.add(fileName);
        return imageKey;
    }
    
    public void setImage(Map<String, String> images) {
        this.images = images;
        numberOfImages = images.size();
    }

    private void cleanUpTempFiles() {
        for (String str : listOfTemporaryFiles) {
            (new File(str)).delete();
            (new File(str.replace(".dep", ".tas"))).delete();
        }
    }

    @Override
    public void cancelOperation() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void launchDialog(String pluginName) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void returnData(Object ret) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void runPlugin(String pluginName, String[] args) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pluginComplete() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int showFeedback(String message) {
        if (myListener != null) {
            myListener.showFeedback(message);
        }
        return 0;
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        
        return 0;
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateProgress(String progressLabel, int progress) {
        if (myListener != null) {
            myListener.notifyOfProgress(progress);
        }
    }

    @Override
    public void updateProgress(int progress) {
        if (myListener != null) {
            myListener.notifyOfProgress(progress);
        }
    }

    @Override
    public void refreshMap(boolean updateLayersTab) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void run() {
        try {
            returnValue = evaluateExpression(expressionLine);
            if (myListener != null) {
                myListener.notifyOfReturn(returnValue);
                myListener.notifyOfThreadComplete(this);
            }
        } catch (NotSupportedException e) {
            
        }
    }
    
    public void handelException(Exception e) {
        if (myListener != null) {
            myListener.passOnThreadException(e);
        }
    }
    
    String returnValue;
    public String getReturnValue() {
        return returnValue;
    }

    @Override
    public String getWorkingDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getApplicationDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getResourcesDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List returnPluginList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void editVector() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteFeature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Font getDefaultFont() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLogDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceBundle getGuiLabelsBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceBundle getMessageBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLanguageCountryCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLanguageCountryCode(String code) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logException(String message, Exception e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logThrowable(String message, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logMessage(Level level, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum Precedence {

        NONE(15),
        UNARY(14),
        POWER(13),
        TIMES(12),
        DIV(11),
        INTDIV(10),
        MODULUS(9),
        PLUS(8),
        EQUALITY(7),
        INEQULAITY(6),
        GREATER_THAN(5),
        LESS_THAN(4),
        GREATER_THAN_EQUAL_TO(3),
        LESS_THAN_EQUAL_TO(2),
        ASSIGNMENT(1);
        private final int value;

        Precedence(int val) {
            this.value = val;
        }

        public int getValue() {
            return value;
        }
    }

    public String evaluateExpression(String expression) throws NotSupportedException {

        try {
            eqDepth++;
            
            if (Thread.currentThread().isInterrupted()) {
                return "Cancelled";
            }
            /* Remove all spaces. We can be assured that this won't affect the image names (which 
            // could possibly have spaces in them) because they will have been entered in the
            // ImageNames dictionary and replaced by 'IMAGEX// keys. */
            String expr = expression.replace(" ", "");

            // simple negate
            expr = expr.replace("(-)", "-1*");

            //Operators have to be a single character to be parsed properly
            expr = expr.replace("==", "@");
            expr = expr.replace("!=", "~");
            expr = expr.replace(">=", "#");
            expr = expr.replace("<=", "$");
            
            //replace pi with its double-equivalent value.
            expr = expr.replace("\u03C0", String.valueOf(Math.PI));

            int expr_len = expr.length();
            if (expr_len == 0) {
                eqDepth--;
                return null;
            }

            // If we find + or - now, it is a unary operator.
            boolean is_unary = true;

            // So far we have nothing.
            int best_prec = Precedence.NONE.getValue();
            int best_pos = 0;

            // Find the operator with the lowest precedence.
            // Look for places where there are no open
            // parentheses.
            int parens = 0;
            boolean next_unary = false;
            char[] exprChar = expr.toCharArray();
            for (int pos = 0; pos < expr_len; pos++) {
                // Examine the next character.
                char ch = exprChar[pos];

                // Assume we will not find an operator. In
                // that case, the next operator will not
                // be unary.
                next_unary = false;

                if (ch == ' ') {
                    // Just skip spaces. We keep them here
                    // to make the error messages easier to read.
                } else if (ch == '(') {
                    // Increase the open parentheses count.
                    parens++;

                    // A + or - after "(" is unary.
                    next_unary = true;
                } else if (ch == ')') {
                    // Decrease the open parentheses count.
                    parens--;

                    // An operator after ")" is not unary.
                    next_unary = false;

                    // If parens < 0, too many ')'s.
                    if (parens < 0) {
                        throw new NotSupportedException("Too many )s in expression '" + expression + "'");
                    }
                } else if (parens == 0) {
                    // See if this is an operator.
                    if (ch == '^' || ch == '*'
                            || ch == '/' || ch == '\\'
                            || ch == '%' || ch == '+'
                            || ch == '-' || ch == '='
                            || ch == '@' || ch == '~'
                            || ch == '>' || ch == '<'
                            || ch == '#' || ch == '$'
                            || ch == '\u00D7' || ch == '\u2212'
                            || ch == '\u00F7') {
                        // An operator after an operator
                        // is unary.
                        next_unary = true;

                        // See if this operator has higher
                        // precedence than the current one.
                        switch (ch) {
                            case '^':
                                if (best_prec >= Precedence.POWER.getValue()) {
                                    best_prec = Precedence.POWER.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '*': case '\u00D7':
                            case '/': case '\u00F7':
                                if (best_prec >= Precedence.TIMES.getValue()) {
                                    best_prec = Precedence.TIMES.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '\\':
                                if (best_prec >= Precedence.INTDIV.getValue()) {
                                    best_prec = Precedence.INTDIV.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '%':
                                if (best_prec >= Precedence.MODULUS.getValue()) {
                                    best_prec = Precedence.MODULUS.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '+':
                            case '-': case '\u2212':
                                // Ignore unary operators
                                // for now.
                                if ((!is_unary) & best_prec >= Precedence.PLUS.getValue()) {
                                    best_prec = Precedence.PLUS.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '=':
                                if (best_prec >= Precedence.ASSIGNMENT.getValue()) {
                                    best_prec = Precedence.ASSIGNMENT.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '@':
                                if (best_prec >= Precedence.EQUALITY.getValue()) {
                                    best_prec = Precedence.EQUALITY.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '~':
                                if (best_prec >= Precedence.INEQULAITY.getValue()) {
                                    best_prec = Precedence.INEQULAITY.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '>':
                                if (best_prec >= Precedence.GREATER_THAN.getValue()) {
                                    best_prec = Precedence.GREATER_THAN.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '<':
                                if (best_prec >= Precedence.LESS_THAN.getValue()) {
                                    best_prec = Precedence.LESS_THAN.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '#':
                                if (best_prec >= Precedence.GREATER_THAN_EQUAL_TO.getValue()) {
                                    best_prec = Precedence.GREATER_THAN_EQUAL_TO.getValue();
                                    best_pos = pos;
                                }
                                break;
                            case '$':
                                if (best_prec >= Precedence.LESS_THAN_EQUAL_TO.getValue()) {
                                    best_prec = Precedence.LESS_THAN_EQUAL_TO.getValue();
                                    best_pos = pos;
                                }
                                break;
                        }
                    }
                }
                is_unary = next_unary;
            }
            
            
            // If the parentheses count is not zero,
            // there's a ')// missing.
            if (parens != 0) {
                throw new NotSupportedException("Missing ) in expression '" + expression + "'");
            }
            
            if (best_prec < Precedence.NONE.getValue()) {
                String lexpr = expr.substring(0, best_pos);
                String rexpr = expr.substring(best_pos + 1);
                String obj1;
                String obj2;
                boolean isObj1AnImage = false;
                boolean isObj2AnImage = false;
                String[] args;
                obj1 = evaluateExpression(lexpr);
                obj2 = evaluateExpression(rexpr);
                if (obj1.contains("IMAGE")) {
                    isObj1AnImage = true;
                }
                if (obj2.contains("IMAGE")) {
                    isObj2AnImage = true;
                }
                
                char op = expr.charAt(best_pos);
                switch (op) {
                    case '^': // exponent
                        if (!isObj1AnImage && !isObj2AnImage) {
                            double val = Math.pow(Double.parseDouble(obj1), Double.parseDouble(obj2));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Power plug = new plugins.Power();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '*': case '\u00D7': // multiplication
                        if (!isObj1AnImage && !isObj2AnImage) {
                            double val = Double.parseDouble(obj1) * Double.parseDouble(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Multiply plug = new plugins.Multiply();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '/': case '\u00F7': // division
                        if (!isObj1AnImage && !isObj2AnImage) {
                            if (Double.parseDouble(obj2) == 0) {
                                throw new Exception("Division by zero.");
                            }
                            double val = Double.parseDouble(obj1) / Double.parseDouble(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Divide plug = new plugins.Divide();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '\\': // IntDiv
                        if (!isObj1AnImage && !isObj2AnImage) {
                            if (Integer.parseInt(obj2) == 0) {
                                throw new Exception("Division by zero.");
                            }
                            double val = Integer.parseInt(obj1) / Integer.parseInt(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.IntDiv plug = new plugins.IntDiv();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '%': // Modulus
                        if (!isObj1AnImage && !isObj2AnImage) {
                            double val = Double.parseDouble(obj1) % Double.parseDouble(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Modulo plug = new plugins.Modulo();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '+': // Addition
                        if (!isObj1AnImage && !isObj2AnImage) {
                            double val = Double.parseDouble(obj1) + Double.parseDouble(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Add plug = new plugins.Add();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '-': case '\u2212': // Subtraction
                        if (!isObj1AnImage && !isObj2AnImage) {
                            double val = Double.parseDouble(obj1) - Double.parseDouble(obj2);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Subtract plug = new plugins.Subtract();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '@': // Equality
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) == Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.EqualTo plug = new plugins.EqualTo();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '~': // Inequality
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) != Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.NotEqualTo plug = new plugins.NotEqualTo();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '>': // Greater Than
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) > Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.GreaterThan plug = new plugins.GreaterThan();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '<': // Less Than
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) < Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.LessThan plug = new plugins.LessThan();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '#': // Greater Than Equal To
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) >= Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.GreaterThanEqualTo plug = new plugins.GreaterThanEqualTo();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '$': // Less Than Equal To
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            if (Double.parseDouble(obj1) <= Double.parseDouble(obj2)) {
                                return "1";
                            } else {
                                return "0";
                            }
                        } else {
                            args = new String[3];
                            if (obj1.contains("IMAGE")) {
                                args[0] = images.get(obj1);
                            } else {
                                args[0] = obj1;
                            }
                            if (obj2.contains("IMAGE")) {
                                args[1] = images.get(obj2);
                            } else {
                                args[1] = obj2;
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.LessThanEqualTo plug = new plugins.LessThanEqualTo();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    case '=': // Assignment
                        if (!isObj1AnImage && !isObj2AnImage) {
                            eqDepth--;
                            return obj1 + "=" + obj2;
                        } else {
                            if (assignment(obj1, obj2).equals("operation complete")) {
                                eqDepth--;
                                if (eqDepth == 0) {
                                    cleanUpTempFiles();
                                }
                                return obj1;
                            } else {
                                throw new NotSupportedException("Assignment operation encountered an error.");
                            }
                        }
                    
                }
            }
            
            /*' If we do not yet have an operator, there
            ' are several possibilities:
            '
            ' 1. expr is (expr2) for some expr2.
            ' 2. expr is -expr2 or +expr2 for some expr2.
            ' 3. expr is Fun(expr2) for a function Fun.
            ' 4. expr is a primitive.
            ' 5. It's a literal like "3.14159". */

            // Look for (expr2).
            if (expr.startsWith("(") && expr.endsWith(")")) {
                // Remove the parentheses.
                eqDepth--;
                return evaluateExpression(expr.substring(1, expr_len - 1));
            }

            // Look for -expr2.
            if (expr.startsWith("-")) {
                if (!expr.contains("IMAGE")) {
                    double val = Double.parseDouble(evaluateExpression(expr.substring(1)));
                    eqDepth--;
                    return String.valueOf(-val);
                } else {
                    String[] args = new String[2];
                    String obj1 = evaluateExpression(expr.substring(1));
                    args[0] = obj1;
                    imageKey = getNextTempFile();
                    args[1] = images.get(imageKey);
                    plugins.Negate plug = new plugins.Negate();
                    plug.setArgs(args);
                    plug.setPluginHost(this);
                    plug.run();
                    eqDepth--;
                    return imageKey;
                }
            }

            // Look for +expr2. I'm not sure why this would happen.
            if (expr.startsWith("+")) {
                eqDepth--;
                return evaluateExpression(expr.substring(1));
            }

            // Look for Fun(expr2).
            if (expr.endsWith(")")) { //expr_len > 5
                // Find the first (.
                int pos = expr.indexOf("(");
                if (pos > 0) {
                    // See what the function is.
                    String lexpr = expr.substring(0, pos).toLowerCase();
                    String rexpr = expr.substring(pos + 1, expr_len - 1);
                    // this next bit of code is done in the event that there are 
                    // parentheses within the function that represent subexpressions
                    // that need to be done first.
                    if (rexpr.contains("(")) {
                        do {
                            int j = rexpr.indexOf("(");
                            int k = rexpr.indexOf(")");
                            String str = rexpr.substring(j + 1, k);
                            String str2 = evaluateExpression(str);
                            rexpr = rexpr.replace("(" + str + ")", str2);

                        } while (rexpr.contains("("));
                    }
                    String obj;
                    boolean isObjAnImage = false;
                    String[] args;
                    obj = evaluateExpression(rexpr);
                    if (obj.indexOf("IMAGE") >= 0) { isObjAnImage = true; }
                    
                    if (lexpr.equals("sin")) {
                        if (!isObjAnImage) {
                            double val = Math.sin(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Sin plug = new plugins.Sin();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("cos")) {
                        if (!isObjAnImage) {
                            double val = Math.cos(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Cos plug = new plugins.Cos();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("tan")) {
                        if (!isObjAnImage) {
                            double val = Math.tan(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Tan plug = new plugins.Tan();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("arccos")) {
                        if (!isObjAnImage) {
                            double val = Math.acos(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.ArcCos plug = new plugins.ArcCos();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("arcsin")) {
                        if (!isObjAnImage) {
                            double val = Math.asin(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.ArcSin plug = new plugins.ArcSin();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("arctan")) {
                        if (!isObjAnImage) {
                            double val = Math.atan(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.ArcTan plug = new plugins.ArcTan();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("cosh")) {
                        if (!isObjAnImage) {
                            double val = Math.cosh(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Cosh plug = new plugins.Cosh();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("sinh")) {
                        if (!isObjAnImage) {
                            double val = Math.sinh(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Sinh plug = new plugins.Sinh();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("tanh")) {
                        if (!isObjAnImage) {
                            double val = Math.tanh(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Tanh plug = new plugins.Tanh();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("log")) {
                        if (!isObjAnImage) {
                            double val = Math.log10(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Log10 plug = new plugins.Log10();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("ln")) {
                        if (!isObjAnImage) {
                            double val = Math.log(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Ln plug = new plugins.Ln();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("log2")) {
                        if (!isObjAnImage) {
                            double log2 = 0.301029995663981;
                            double val = Math.log(Double.parseDouble(obj)) / log2;
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Log2 plug = new plugins.Log2();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("exp")) {
                        if (!isObjAnImage) {
                            double val = Math.exp(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Exp plug = new plugins.Exp();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("abs")) {
                        if (!isObjAnImage) {
                            double val = Math.abs(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Abs plug = new plugins.Abs();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("sqr")) {
                        if (!isObjAnImage) {
                            double val = Double.parseDouble(obj) * Double.parseDouble(obj);
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Square plug = new plugins.Square();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("sqrt") || lexpr.equals("\u221A")) {
                        if (!isObjAnImage) {
                            double val = Math.sqrt(Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.SqrRt plug = new plugins.SqrRt();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("isnodata")) {
                        if (isObjAnImage) {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.IsNoData plug = new plugins.IsNoData();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        } else {
                            throw new Exception("This function requires an input image.");
                        }
                    } else if (lexpr.equals("negate")) {
                        if (!isObjAnImage) {
                            double val = -1 * (Double.parseDouble(obj));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[2];
                            args[0] = images.get(obj);
                            imageKey = getNextTempFile();
                            args[1] = images.get(imageKey);
                            plugins.Negate plug = new plugins.Negate();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("min")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            double val = Math.min(Double.parseDouble(objs[0]), Double.parseDouble(objs[1]));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Min plug = new plugins.Min();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("max")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            double val = Math.max(Double.parseDouble(objs[0]), Double.parseDouble(objs[1]));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Max plug = new plugins.Max();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("pow")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            double val = Math.pow(Double.parseDouble(objs[0]), Double.parseDouble(objs[1]));
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.Power plug = new plugins.Power();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("if")) {
                        String[] objs = obj.split(",");
                        if (isObjAnImage && objs.length == 3) {
                            if (!objs[0].contains("IMAGE")) {
                                throw new NotSupportedException("if-then-else operation must contain image.");
                            }
                            args = new String[4];
                            args[0] = images.get(objs[0]);
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            if (objs[2].contains("IMAGE")) {
                                args[2] = images.get(objs[2]);
                            } else {
                                args[2] = objs[2];
                            }
                            imageKey = getNextTempFile();
                            args[3] = images.get(imageKey);
                            ifThenElse(args);
                            eqDepth--;
                            return imageKey;
                        } else {
                            throw new NotSupportedException("if-then-else operation must contain image.");
                        }
                    } else if (lexpr.equals("delete") || lexpr.equals("del")) {
                        if (isObjAnImage) {
                            String[] objs = obj.split(",");
                            for (int j = 0; j < objs.length; j++) {
                                String str = images.get(objs[j]);
                                (new File(str)).delete();
                                (new File(str.replace(".dep", ".tas"))).delete();
                            }
                            eqDepth--;
                            return "Files deleted!";
                        } else {
                            throw new NotSupportedException("delete operation must contain image.");
                        }
                    } else if (lexpr.equals("and")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            int val1, val2;
                            if (Integer.parseInt(objs[0]) != 0) {
                                val1 = 1;
                            } else {
                                val1 = 0;
                            }
                            if (Integer.parseInt(objs[1]) != 0) {
                                val2 = 1;
                            } else {
                                val2 = 0;
                            }
                            int val = val1 * val2;
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.AND plug = new plugins.AND();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("not")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            int val1, val2;
                            if (Integer.parseInt(objs[0]) != 0) {
                                val1 = 1;
                            } else {
                                val1 = 0;
                            }
                            if (Integer.parseInt(objs[1]) != 0) {
                                val2 = 1;
                            } else {
                                val2 = 0;
                            }
                            int val;
                            if (val1 != 0 && val2 == 0) {
                                val = 1;
                            } else {
                                val = 0;
                            }
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.NOT plug = new plugins.NOT();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("or")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            int val1, val2;
                            if (Integer.parseInt(objs[0]) != 0) {
                                val1 = 1;
                            } else {
                                val1 = 0;
                            }
                            if (Integer.parseInt(objs[1]) != 0) {
                                val2 = 1;
                            } else {
                                val2 = 0;
                            }
                            int val;
                            if (val1 + val2 > 0) {
                                val = 1;
                            } else {
                                val = 0;
                            }
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.OR plug = new plugins.OR();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    } else if (lexpr.equals("xor")) {
                        String[] objs = obj.split(",");
                        if (!isObjAnImage) {
                            int val1, val2;
                            if (Integer.parseInt(objs[0]) != 0) {
                                val1 = 1;
                            } else {
                                val1 = 0;
                            }
                            if (Integer.parseInt(objs[1]) != 0) {
                                val2 = 1;
                            } else {
                                val2 = 0;
                            }
                            int val;
                            if (val1 + val2 == 1) {
                                val = 1;
                            } else {
                                val = 0;
                            }
                            eqDepth--;
                            return String.valueOf(val);
                        } else {
                            args = new String[3];
                            if (objs[0].contains("IMAGE")) {
                                args[0] = images.get(objs[0]);
                            } else {
                                args[0] = objs[0];
                            }
                            if (objs[1].contains("IMAGE")) {
                                args[1] = images.get(objs[1]);
                            } else {
                                args[1] = objs[1];
                            }
                            imageKey = getNextTempFile();
                            args[2] = images.get(imageKey);
                            plugins.XOR plug = new plugins.XOR();
                            plug.setArgs(args);
                            plug.setPluginHost(this);
                            plug.run();
                            eqDepth--;
                            return imageKey;
                        }
                    }
                }
            }
            
            if (expr.toLowerCase().equals("pi") || expr.equals("\u03C0")) {
                eqDepth--;
                return String.valueOf(Math.PI);
            }

            if (expr.toLowerCase().equals("e")) {
                eqDepth--;
                return String.valueOf(Math.E);
            }

            // this needs fixing for the case of file-specific nodata values.
            if (expr.toLowerCase().equals("nodata")) {
                eqDepth--;
                return String.valueOf(-32768);
            }
            
            // it's a number or an image
            eqDepth--;
            return expr;
            
        } catch (Exception e) {
            eqDepth--;
            handelException(e);
            return "";
        }
    }
    
    private String assignment(String obj1, String obj2) {
        try {
            obj1 = images.get(obj1);
            obj2 = images.get(obj2);
            String file1Hdr = obj1;
            String file1Data = obj1.replace(".dep", ".tas");
            String file2Hdr = obj2;
            String file2Data = obj2.replace(".dep", ".tas");

            File file1HdrInfo = new File(file1Hdr);
            File file1DataInfo = new File(file1Data);

            if (file1HdrInfo.exists()) {
                file1HdrInfo.delete();
            }
            if (file1DataInfo.exists()) {
                file1DataInfo.delete();
            }

            File file2HdrInfo = new File(file2Hdr);
            File file2DataInfo = new File(file2Data);

            if (!file2HdrInfo.exists() || !file2DataInfo.exists()) {
                throw new NotSupportedException("File not found");
            }

            whitebox.utilities.FileUtilities.copyFile(file2HdrInfo, file1HdrInfo);
            whitebox.utilities.FileUtilities.copyFile(file2DataInfo, file1DataInfo);
            
            return "operation complete";
        } catch (Exception e) {
            handelException(e);
            return "";
        }
    }
    
    private String ifThenElse(String[] args) {
        try {
            
            boolean image1Bool = false;
            boolean image2Bool = false;
            double constant1 = -1;
            double constant2 = -1;
            
            String inputHeader1 = args[0]; // condition is true--must be an image
            String inputHeader2 = args[1]; // then--image or constant
            String inputHeader3 = args[2]; // else--image or constant
            String outputHeader = args[3];

            // is inputHeader1 a file?
            File file = new File(inputHeader2);
            image1Bool = file.exists();
            if (image1Bool) {
                constant1 = -1;
            } else {
                constant1 = Double.parseDouble(file.getName().replace(".dep", ""));
            }
            file = null;
            
            file = new File(inputHeader3);
            image2Bool = file.exists();
            if (image2Bool) {
                constant2 = -1;
            } else {
                constant2 = Double.parseDouble(file.getName().replace(".dep", ""));
            }
            file = null;
       
            int row, col;
            double z1, z2, z3;
            float progress = 0;
            double[] data1;
            double[] data2;
            double[] data3;

            WhiteboxRaster inputFile1 = new WhiteboxRaster(inputHeader1, "r");
            int rows = inputFile1.getNumberRows();
            int cols = inputFile1.getNumberColumns();
            double noData = inputFile1.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader1, 
                    WhiteboxRaster.DataType.FLOAT, noData);
            
            if (image1Bool && image2Bool) {
                WhiteboxRaster inputFile2 = new WhiteboxRaster(inputHeader2, "r");
                // make sure that the input images have the same dimensions.
                if ((inputFile2.getNumberRows() != rows) || (inputFile2.getNumberColumns() != cols)) {
                    showFeedback("The input images must have the same dimensions and coordinates. Operation cancelled.");
                    return "";
                }
                WhiteboxRaster inputFile3 = new WhiteboxRaster(inputHeader3, "r");
                if ((inputFile3.getNumberRows() != rows) || (inputFile3.getNumberColumns() != cols)) {
                    showFeedback("The input images must have the same dimensions and coordinates. Operation cancelled.");
                    return "";
                }
                
                double noData2 = inputFile2.getNoDataValue();
                double noData3 = inputFile3.getNoDataValue();
                
                for (row = 0; row < rows; row++) {
                    data1 = inputFile1.getRowValues(row);
                    data2 = inputFile2.getRowValues(row);
                    data3 = inputFile3.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if ((data1[col] != noData)) {
                            if (data1[col] != 0) {
                                if (data2[col] != noData2) {
                                    outputFile.setValue(row, col, data2[col]);
                                } else {
                                    outputFile.setValue(row, col, noData2);
                                }
                            } else {
                                if (data3[col] != noData3) {
                                    outputFile.setValue(row, col, data3[col]);
                                } else {
                                    outputFile.setValue(row, col, noData3);
                                }
                            }

                        }
                    }
//                    if (cancelOp) {
//                        return "Cancelled";
//                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                inputFile2.close();
                inputFile3.close();
                
            } else if (!image1Bool && image2Bool) {
                WhiteboxRaster inputFile3 = new WhiteboxRaster(inputHeader3, "r");
                if ((inputFile3.getNumberRows() != rows) || (inputFile3.getNumberColumns() != cols)) {
                    showFeedback("The input images must have the same dimensions and coordinates. Operation cancelled.");
                    return "";
                }
                
                double noData3 = inputFile3.getNoDataValue();
                
                for (row = 0; row < rows; row++) {
                    data1 = inputFile1.getRowValues(row);
                    data3 = inputFile3.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if ((data1[col] != noData)) {
                            if (data1[col] != 0) {
                                outputFile.setValue(row, col, constant1);
                            } else {
                                if (data3[col] != noData3) {
                                    outputFile.setValue(row, col, data3[col]);
                                } else {
                                    outputFile.setValue(row, col, noData3);
                                }
                            }

                        }
                    }
//                    if (cancelOp) {
//                        return "Cancelled";
//                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                inputFile3.close();
                
            } else if (image1Bool && !image2Bool) {
                WhiteboxRaster inputFile2 = new WhiteboxRaster(inputHeader2, "r");
                // make sure that the input images have the same dimensions.
                if ((inputFile2.getNumberRows() != rows) || (inputFile2.getNumberColumns() != cols)) {
                    showFeedback("The input images must have the same dimensions and coordinates. Operation cancelled.");
                    return "";
                }
                
                double noData2 = inputFile2.getNoDataValue();
                
                for (row = 0; row < rows; row++) {
                    data1 = inputFile1.getRowValues(row);
                    data2 = inputFile2.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if ((data1[col] != noData)) {
                            if (data1[col] != 0) {
                                if (data2[col] != noData2) {
                                    outputFile.setValue(row, col, data2[col]);
                                } else {
                                    outputFile.setValue(row, col, noData2);
                                }
                            } else {
                                outputFile.setValue(row, col, constant2);
                            }

                        }
                    }
//                    if (cancelOp) {
//                        return "Cancelled";
//                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                inputFile2.close();
                
            } else if (!image1Bool && !image2Bool) {
                for (row = 0; row < rows; row++) {
                    data1 = inputFile1.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if ((data1[col] != noData)) {
                            if (data1[col] != 0) {
                                outputFile.setValue(row, col, constant1);
                                
                            } else {
                                outputFile.setValue(row, col, constant2);
                            }

                        }
                    }
//                    if (cancelOp) {
//                        return "Cancelled";
//                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress((int) progress);
                }
                
            }
            
            
            

            outputFile.addMetadataEntry("Created by the "
                    + "IF-THEN-ELSE" + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());

            // close all of the open Whitebox rasters.
            inputFile1.close();
            outputFile.close();

            // returning a header file string displays the image.
            return outputHeader;
        } catch (Exception e) {
            handelException(e);
            return "";
        }
    }
}
class NotSupportedException extends Exception {

    public NotSupportedException(String msg) {
        super(msg);
    }
}
