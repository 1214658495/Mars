package com.byd.vtdr2.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author byd_tw
 */
public class SysProp {
    private static Method sysPropGet;

    private SysProp() {
    }

    static {
        try {
            Class<?> S = Class.forName("android.os.SystemProperties");
            Method M[] = S.getMethods();
            for (Method m : M) {
                String n = m.getName();
                if (n.equals("get")) {
                    sysPropGet = m;
                } else if (n.equals("set")) {
//                    sysPropSet = m;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param name
     * @param default_value
     * @return
     */
    public static String get(String name, String default_value) {
        try {
            return (String) sysPropGet.invoke(null, name, default_value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return default_value;
    }

    /**
     * @param newString
     * @return
     */
    public static boolean versionCompare(String newString) {
        int index = newString.lastIndexOf('.');
        int newValue = Integer.parseInt(newString.substring(index + 1));
        return newValue > 1807170 || newValue == 1807170;
    }

}
