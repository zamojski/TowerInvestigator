/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package info.zamojski.soft.towerinvestigator.utils;

import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

public class ReflectionUtils {
    private static final String TAG = ReflectionUtils.class.getSimpleName();

    public static String dumpClasses(Class<?> clazz, Object... instances) {
        if (clazz == null || instances == null || instances.length == 0)
            return "";

        StringBuilder sb = new StringBuilder();

        dumpHeader(sb, clazz);
        int i = 0;
        for (Object instance : instances) {
            sb.append("INDEX = " + i++ + "\n");
            dumpFields(sb, clazz, instance);
            dumpMethods(sb, clazz, instance);
        }
        sb.append("\n\n");

        return sb.toString();
    }

    public static String dumpClass(Class<?> clazz, Object instance) {
        if (clazz == null || instance == null)
            return "";

        StringBuilder sb = new StringBuilder();

        dumpHeader(sb, clazz);
        dumpFields(sb, clazz, instance);
        dumpMethods(sb, clazz, instance);
        sb.append("\n\n");

        return sb.toString();
    }

    private static void dumpHeader(StringBuilder sb, Class<?> clazz) {
        sb.append(clazz.getSimpleName());
        sb.append("\n");
    }

    private static void dumpFields(StringBuilder sb, Class<?> clazz, Object instance) {
        sb.append("FIELDS:");
        sb.append("\n");
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            sb.append("\t");
            sb.append(field.getName());
            sb.append(" : ");
            sb.append(field.getType().getSimpleName());
            sb.append(" = ");
            try {
                sb.append(getResult(field.get(instance)));
            } catch (Exception ex) {
                String message = (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                sb.append("{exception = " + message + "}");
                Log.w(TAG, "dumpFields(): Could not get field value '" + field.getName() + "'.", ex);
            }
            sb.append("\n");
        }
    }

    private static void dumpMethods(StringBuilder sb, Class<?> clazz, Object instance) {
        sb.append("METHODS:");
        sb.append("\n");
        for (Method method : clazz.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("set")
                    || methodName.equals("wait")
                    || methodName.equals("notify")
                    || methodName.equals("notifyAll")
                    || methodName.equals("hashCode")
                    || methodName.equals("equals"))
                continue;
            method.setAccessible(true);
            // skip setters
            if (method.getReturnType().equals(void.class)) {
                dumpMethodHeader(sb, method, null);
                sb.append("{not called}");
                sb.append("\n");
                continue;
            }
            // try to call getters
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 0) {
                try {
                    dumpMethodHeader(sb, method, null);
                    sb.append(getResult(method.invoke(instance, new Object[0])));
                } catch (Exception ex) {
                    String message = (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                    sb.append("{exception = " + message + "}");
                    Log.w(TAG, "dumpMethods(): Could not get method value '" + method.getName() + "'.", ex);
                }
                sb.append("\n");
            } else if (parameters.length == 1 && parameters[0].equals(int.class)) {
                for (int i = 0; i <= 1; i++) {
                    try {
                        dumpMethodHeader(sb, method, i);
                        sb.append(getResult(method.invoke(instance, new Object[]{i})));
                    } catch (Exception ex) {
                        String message = (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                        sb.append("{exception = " + message + "}");
                        Log.w(TAG, "dumpMethods(): Could not get method value '" + method.getName() + "' for parameter '" + i + "'.", ex);
                    }
                    sb.append("\n");
                }
            } else {
                dumpMethodHeader(sb, method, null);
                sb.append("{not called}");
                sb.append("\n");
            }
        }
    }

    private static Object getResult(Object result) {
        if (result == null)
            return "{null}";
        if (Iterable.class.isAssignableFrom(result.getClass())) {
            return dumpIterable((Iterable) result);
        }
        if (result.getClass().isArray()) {
            return dumpArray(result);
        }
        return result;
    }

    private static String dumpIterable(Iterable iterable) {
        Iterator iterator = iterable.iterator();
        StringBuilder sb = new StringBuilder("[");
        boolean addSeparator = false;
        while (iterator.hasNext()) {
            if (addSeparator) {
                sb.append(", ");
            }
            Object value = iterator.next();
            sb.append(value);
            addSeparator = true;
        }
        sb.append("]");
        return sb.toString();
    }

    private static String dumpArray(Object array) {
        int arrayLength = Array.getLength(array);
        StringBuilder sb = new StringBuilder("[");
        boolean addSeparator = false;
        for (int i = 0; i < arrayLength; i++) {
            if (addSeparator) {
                sb.append(", ");
            }
            Object value = Array.get(array, i);
            sb.append(value);
            addSeparator = true;
        }
        sb.append("]");
        return sb.toString();
    }

    private static void dumpMethodHeader(StringBuilder sb, Method method, Integer index) {
        sb.append("\t");
        sb.append(method.getName());
        sb.append("(");
        boolean addSeparator = false;
        for (Class<?> paramClass : method.getParameterTypes()) {
            if (addSeparator) {
                sb.append(", ");
            }
            if (index != null) {
                sb.append(index);
                sb.append(" as ");
            }
            sb.append(paramClass.getSimpleName());
            addSeparator = true;
        }
        sb.append(") : ");
        sb.append(method.getReturnType().getSimpleName());
        sb.append(" = ");
    }
}
