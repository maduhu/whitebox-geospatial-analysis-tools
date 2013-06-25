/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.internationalization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author johnlindsay
 */
public final class WhiteboxInternationalizationTools {

    private static Locale currentLocale = new Locale("en", "CA");

    public static Locale getLocale() {
        return currentLocale;
    }

    public static void setLocale(String languageCode, String countryCode) {
        currentLocale = new Locale(languageCode, countryCode);
    }

    public static ResourceBundle getGuiLabelsBundle() {
        return ResourceBundle.getBundle("whitebox.internationalization.GuiLabelsBundle", currentLocale);

    }

    public static ResourceBundle getMessagesBundle() {
        return ResourceBundle.getBundle("whitebox.internationalization.messages", currentLocale);

    }

    public static void main(String[] args) {
        WhiteboxInternationalizationTools wit = new WhiteboxInternationalizationTools();
        wit.updateBundle();

    }

    private void updateBundle() {
        // this is used to automatically replace the default text in bundles with the
        // translated text provided within spreadsheets.
        
        String inputFile = "/Users/johnlindsay/Documents/italian translation.txt";
        //String bundleFile = "/Users/johnlindsay/Documents/GuiLabelsBundle_it_IT.properties";
        //String outputFile = "/Users/johnlindsay/Documents/GuiLabelsBundle_it_IT2.properties";
        String bundleFile = "/Users/johnlindsay/Documents/messages_it_IT.properties";
        String outputFile = "/Users/johnlindsay/Documents/messages_it_IT2.properties";
        
        int numLinesInOutput = 0;
        DataInputStream in = null;
        BufferedReader br = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        String delimiter = "=";
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(bundleFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            String line;
            String[] str;
            //Read File Line By Line
            while ((line = br.readLine()) != null) {
                numLinesInOutput++;
            }

            String[][] outputText = new String[numLinesInOutput][2];

            fstream = new FileInputStream(bundleFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            //Read File Line By Line
            int i = 0;
            while ((line = br.readLine()) != null) {
                str = line.split(delimiter);
                if (str.length < 3) {
                    System.arraycopy(str, 0, outputText[i], 0, str.length);
                } else {
                    System.out.println("Something's not right on line " + i);
                }
                i++;
            }


            fstream = new FileInputStream(inputFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            //Read File Line By Line
            delimiter = "\t";
            String key, english, translated;

            while ((line = br.readLine()) != null) {
                str = line.split(delimiter);
                if (str.length == 3) {
                    key = str[0];
                    english = str[1];
                    translated = str[2];
                    //traditional = str[3];

                    for (int a = 0; a < numLinesInOutput; a++) {
                        if (outputText[a][0] != null) {
                            if (outputText[a][0].trim().equals(key.trim())) {
                                outputText[a][1] = translated;
                            }
                        }
                    }

                }
            }

            //Close the input stream
            in.close();
            br.close();


            File file = new File(outputFile);
            if (file.exists()) { file.delete(); }

            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);
            
            for (int a = 0; a < numLinesInOutput; a++) {
                if (outputText[a][1] != null && 
                        !outputText[a][1].trim().isEmpty()) {
                    line = outputText[a][0].trim() + " = " + outputText[a][1].trim();
                } else {
                    line = outputText[a][0];
                }
                out.println(line);
            }


            System.out.println("Complete!");

        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                if (in != null || br != null) {
                    in.close();
                    br.close();
                }
                if (out != null || bw != null) {
                    out.flush();
                    out.close();
                }

            } catch (java.io.IOException ex) {
            }

        }

    }
}
