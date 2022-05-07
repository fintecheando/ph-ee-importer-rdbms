package org.apache.fineract.paymenthub.importer;

import java.text.SimpleDateFormat;

public class OperatorUtils {

    public static String strip(String str) {
        return str.replaceAll("^\"|\"$", "");
    }

    public static SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }
}
