package me.ebk21.meprop;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class hokes implements IXposedHookLoadPackage {
    private String originalprop(Member m, Object o, int length, String prop) {
        String[] s;
        if (length == 2) {
            s = new String[]{prop, null};
        } else {
            s = new String[]{prop};
        }
        try {
            final String res = (String) XposedBridge.invokeOriginalMethod(m, o, s);
            if (!res.isBlank())
                return res;
        } catch (Exception ignored) {
        }
        return "none";
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.contains("webview") || lpparam.processName.toLowerCase().contains("webview"))
            return;
        final ClassLoader SC = ClassLoader.getSystemClassLoader();
        final Class<?> c = findClass("android.os.SystemProperties", SC);
        if (c == null) return;
        final ArrayList<String> ar = new ArrayList<>(Arrays.asList("ro.miui.internal.storage", "ro.miui.ui.version.code", "ro.miui.version.name",
                "ro.miui.version.code_time", "ro.product.brand", "ro.product.manufacturer", "ro.product.name", "ro.product.vendor.brand"));
        final ArrayList<String> ar2 = new ArrayList<>(Arrays.asList("/sdcard/", "10", "V12",
                "1592409600", "Xiaomi", "Xiaomi", "Xiaomi", "Xiaomi"));
        final ArrayList<String> ar3 = new ArrayList<>(Arrays.asList("mp.rmis", "mp.rmuvc", "mp.rmvn",
                "mp.rmvc", "mp.rpb", "mp.rpm", "mp.rpn", "mp.rpvb"));
        XC_MethodHook m = new XC_MethodHook(PRIORITY_HIGHEST) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                boolean isBD = false;
                String BD = originalprop(param.method, param.thisObject, param.args.length, "mp.bd");
                if (BD.equals("true"))
                    isBD = true;

                int index = 0;
                for (String c : ar
                ) {
                    if (param.args[0].equals(c)) {
                        if (index <= 3) {
                            final String s;
                            final String or = originalprop(param.method, param.thisObject, param.args.length, ar3.get(index));
                            if (!or.equals("none")) {
                                s = or;
                            } else {
                                s = ar2.get(index);
                            }
                            param.setResult(s);
                            XposedBridge.log("MPH: "+lpparam.packageName+" => faking "+c+" with "+s);
                            break;
                        } else {
                            if (isBD) {
                                final String s;
                                final String or = originalprop(param.method, param.thisObject, param.args.length, ar3.get(index));
                                if (!or.equals("none")) {
                                    s = or;
                                } else {
                                    s = ar2.get(index);
                                }
                                param.setResult(s);
                            }
                        }
                    }
                    ++index;
                }

            }
        };
        XposedBridge.hookAllMethods(c, "get", m);
    }
}
