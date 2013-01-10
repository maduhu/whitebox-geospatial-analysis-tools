package whitebox.utilities;

import java.text.DecimalFormatSymbols;

/**
 *
 * @author johnlindsay
 */
public class StringUtilities {

    public static boolean isStringNumeric(String str) {
        try {
            if (str.isEmpty()) { return false; }
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
}
