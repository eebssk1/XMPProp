package me.ebk21.meprop;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.callbacks.XCallback.PRIORITY_HIGHEST;

import java.lang.reflect.Member;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class hokes implements IXposedHookLoadPackage {
    static HashMap<String, String> hm1;
    static HashMap<String, String> hm2;

    static {
        hm1 = new HashMap<>();
        hm2 = new HashMap<>();
        hm1.put("ro.miui.internal.storage", "/sdcard/");
        hm1.put("ro.miui.ui.version.code", "10");
        hm1.put("ro.miui.version.name", "V12");
        hm1.put("ro.miui.version.code_time", "1592409600");
        hm2.put("ro.product.brand", "Xiaomi");
        hm2.put("ro.product.manufacturer", "Xiaomi");
        hm2.put("ro.product.name", "Xiaomi");
        hm2.put("ro.product.vendor.brand", "Xiaomi");
    }

    static private String originalprop(Member m, Object o, int length, String prop) {
        final String[] s;
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

        final Class<?> cls;
        final Class<?> AP = findClassIfExists("android.os.SystemProperties", lpparam.classLoader);
        final Class<?> SP = findClassIfExists("android.os.SystemProperties", XposedBridge.BOOTCLASSLOADER);
        if (AP != null) {
            cls = AP;
        } else if (SP != null) {
            cls = SP;
            XposedBridge.log("MPH: " + lpparam.processName + " => Can not find system properties class in application ! Hooking framework instead...");
        } else {
            XposedBridge.log("MPH: " + lpparam.processName + " => Can not find system properties class in both application and framework !");
            return;
        }
        final XC_MethodHook m = new XC_MethodHook(PRIORITY_HIGHEST) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                final String key = (String) param.args[0];
                boolean isBD = false;
                final String BD = originalprop(param.method, param.thisObject, param.args.length, "mp.bd");
                if (BD.equals("true"))
                    isBD = true;

                if (hm1.containsKey(key)) {
                    final String s = hm1.get(key);
                    param.setResult(s);
                    XposedBridge.log("MPH: "+lpparam.processName+" > faking miui prop "+key+" as "+s);
                } else if (hm2.containsKey(key) && isBD) {
                    final String s = hm2.get(key);
                    param.setResult(s);
                    XposedBridge.log("MPH: "+lpparam.processName+" > faking device prop "+key+" as "+s);
                }

            }
        };
        XposedBridge.hookAllMethods(cls, "get", m);
    }
}
