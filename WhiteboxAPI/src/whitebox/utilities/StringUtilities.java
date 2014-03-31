package whitebox.utilities;

import java.text.DecimalFormatSymbols;

/**
 *
 * @author johnlindsay
 */
public abstract class StringUtilities {

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static boolean isInteger(String str) {
        return str.matches("([0-9]*)\\.[0]");
    }

    public static boolean isBoolean(String str) {
        return str.toLowerCase().trim().equals("true")
                || str.toLowerCase().trim().equals("false");
    }

    public static boolean isStringNumeric(String str) {
        try {
            if (str.isEmpty()) {
                return false;
            }
            DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
            char localeMinusSign = currentLocaleSymbols.getMinusSign();

            if (!Character.isDigit(str.charAt(0)) && str.charAt(0) != localeMinusSign) {
                return false;
            }

            boolean isDecimalSeparatorFound = false;
            char localeDecimalSeparator = currentLocaleSymbols.getDecimalSeparator();

            for (char c : str.substring(1).toCharArray()) {
                if (!Character.isDigit(c)) {
                    if (c == localeDecimalSeparator && !isDecimalSeparatorFound) {
                        isDecimalSeparatorFound = true;
                        continue;
                    }
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }

    public static String toTitleCase(String input) {
        StringBuilder ret = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
                nextTitleCase = false;
            }

            ret.append(c);
        }

        return ret.toString();
    }
}
