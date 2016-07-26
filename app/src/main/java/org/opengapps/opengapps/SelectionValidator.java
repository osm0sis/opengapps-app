package org.opengapps.opengapps;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class SelectionValidator {
    private Context context;

    public SelectionValidator(Context context) {
        this.context = context;
    }

    public static boolean isValid(String arch, String android, String variant) {
        if (arch.equals("x86") && android.equals("4.4") && (variant.equals("stock") || variant.equals("full")))
            return false;
        else
            return isValidArchAnd(arch, android) && isValidArchVar(arch, variant) && isValidAndVar(android, variant);
    }

    public static boolean isValidArchAnd(String arch, String android) {
        if (arch.contains("64") && android.equals("4.4"))
            return false;
        else
            return true;
    }

    public static boolean isValidArchVar(String arch, String variant) {
        if (arch.contains("86") && variant.equals("aroma"))
            return false;
        else
            return true;
    }

    public static boolean isValidAndVar(String android, String variant) {
        if ((android.equals("5.0") || android.equals("4.4")) && variant.equals("super"))
            return false;
        else
            return true;
    }
}
