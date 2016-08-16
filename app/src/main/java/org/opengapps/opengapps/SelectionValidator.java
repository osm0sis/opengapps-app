package org.opengapps.opengapps;

@SuppressWarnings({"SimplifiableIfStatement", "WeakerAccess"})
public class SelectionValidator {

    private SelectionValidator(){}

    public static boolean isValid(String arch, String android, String variant) {
        if (arch.equals("x86") && android.equals("4.4") && (variant.equals("stock") || variant.equals("full")))
            return false;
        else
            return isValidArchAnd(arch, android) && isValidArchVar(arch, variant) && isValidAndVar(android, variant);
    }

    public static boolean isValidArchAnd(String arch, String android) {
        return !(arch.contains("64") && android.equals("4.4"));
    }

    public static boolean isValidArchVar(String arch, String variant) {
        return !(arch.contains("86") && variant.equals("aroma"));
    }

    public static boolean isValidAndVar(String android, String variant) {
        return !((android.equals("5.0") || android.equals("4.4")) && variant.equals("super"));
    }
}
