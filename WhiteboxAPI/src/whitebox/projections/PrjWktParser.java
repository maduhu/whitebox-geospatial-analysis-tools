/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.projections;

import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;
import java.io.File;

/**
 *
 * @author Dr. John Lindsay
 */
public class PrjWktParser {

    String prjFileName = "";

    public PrjWktParser(String fileName) {
        this.prjFileName = fileName;
        setupParser();
    }

    private void setupParser() {
        if (prjFileName == null || prjFileName.isEmpty()) {
            return;
        }

        String token = "";

        try {

            List<String> temps;
            try ( // create Scanner inFile1
                    Scanner inFile1 = new Scanner(new File(this.getFileName()))) {
                temps = new LinkedList<>();
                // while loop
                while (inFile1.hasNext()) {
                    // find next line
                    token = inFile1.next();
                    temps.add(token);
                }
            }

            String[] tempsArray = temps.toArray(new String[0]);

            for (String s : tempsArray) {
                //System.out.println(s);
                this.parseWKT(s, 0);
            }
            
        } catch (Exception e) {

        }
    }

    public String getFileName() {
        return prjFileName;
    }

    private String getTabString(int i) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < i; j++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    StringBuilder sb = new StringBuilder();

    public String getParsedText() {
        return sb.toString();
    }

    private void parseWKT(String str, int order) {
        int c = str.indexOf(",");
        int s = str.indexOf("[");
        String orderStr = getTabString(order);
        if (c >= 0 && c < s) {
            if (c > 0) {
                String word = str.substring(0, c).trim();
                String content = str.substring(c + 1).trim();
                sb.append(orderStr).append(word).append("\n");
                parseWKT(content, order);
            } else {
                String content = str.substring(1);
                parseWKT(content, order);
            }
        } else if (s < c && s >= 0) {
            String word = str.substring(0, s).trim();
            // find the closing bracket
            char[] ch = str.toCharArray();
            int k = 0;
            int e = -1;
            for (int m = 0; m < ch.length; m++) {
                if (ch[m] == '[') {
                    k++;
                } else if (ch[m] == ']') {
                    k--;
                    if (k == 0) {
                        e = m;
                        break;
                    }
                }
            }
            if (e >= 0) {
                String content = str.substring(s + 1, e).trim();
                sb.append(orderStr).append(word).append("\n");
                if (!content.contains("[")) {
                    orderStr = getTabString(order + 1);
                    sb.append(orderStr).append(content).append("\n");
                }
                parseWKT(content, order + 1);
                if (e < str.length() - 1) {
                    String remainder = str.substring(e + 1);
                    parseWKT(remainder, order);
                }
            }
        }
    }

    // This method is used for testing purposes only.
    public static void main(String[] args) {
        PrjWktParser pp = new PrjWktParser("/Users/johnlindsay/Documents/Data/Beau's Data/ParisGaltGuelph Moraine Shape/ParisGalt.prj");
        System.out.println(pp.getParsedText());
//        String token = "";
//
//        try {
//
//            // create Scanner inFile1
//            Scanner inFile1 = new Scanner(new File(pp.getFileName())); //.useDelimiter(",\\s*");
//
//            List<String> temps = new LinkedList<>();
//
//            // while loop
//            while (inFile1.hasNext()) {
//                // find next line
//                token = inFile1.next();
//                temps.add(token);
//            }
//            inFile1.close();
//
//            String[] tempsArray = temps.toArray(new String[0]);
//
//            for (String s : tempsArray) {
//                //System.out.println(s);
//                pp.parseWKT(s, 0);
//            }
//            System.out.println(pp.getParsedText());
//
//        } catch (Exception e) {
//
//        }
    }
}
