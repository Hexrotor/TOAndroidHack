package com.hexrotor.dddlocktarget;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class callXposed implements IXposedHookLoadPackage {
    boolean targetValid = true;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.packageName.equals("com.tankionline.china") && !loadPackageParam.packageName.equals("com.tankionline.mobile.production")) {
            return;
        }

        XSharedPreferences xsp = new XSharedPreferences("com.hexrotor.dddlocktarget", "function_config");
        Log.d("DDD", "Loaded preferences"+xsp.getAll());
        createConfigToast(loadPackageParam, xsp);
        setTargetMark(loadPackageParam, xsp.getBoolean("scorpio_remember_target", false) || xsp.getBoolean("striker_remember_target", false));
        final Class<?> LockResult = XposedHelpers.findClass("alternativa.tanks.battle.weapons.aiming.LockResult", loadPackageParam.classLoader);
        //striker
        XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.components.StrikerWeapon", loadPackageParam.classLoader, "lockTarget", LockResult, java.lang.Long.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                // Check if the target is valid, required by setTargetMark
                boolean result = (boolean) XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
                long targetId = (Long) XposedHelpers.callMethod(methodHookParam.args[0], "getTargetId");
                targetValid = (boolean) XposedHelpers.callMethod(methodHookParam.thisObject, "isValidTarget", targetId);
                if(xsp.getBoolean("striker_lock", false)) {
                    if (xsp.getBoolean("striker_remember_target", false)) {
                        return result || targetValid;
                    }
                    return result || (methodHookParam.args[1] != null);
                }
                return result;

            }

        });
        //scorpio
        XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.scorpio.components.ScorpioWeapon", loadPackageParam.classLoader, "lockTarget", LockResult, java.lang.Long.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                long targetId = (Long) XposedHelpers.callMethod(methodHookParam.args[0], "getTargetId");
                targetValid = (boolean) XposedHelpers.callMethod(methodHookParam.thisObject, "isValidTarget", targetId);
                boolean result = (boolean) XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
                if (xsp.getBoolean("scorpio_lock", false)) {
                    if (xsp.getBoolean("scorpio_remember_target", false)) {
                        return result || targetValid;
                    }
                    return result || (methodHookParam.args[1] != null);
                }
                return result;

            }
        });
        // Modified this function is meaningless because if your turret is not aiming at the target, the server will detect it as hacking.
        // This will also break gauss's aiming function.
//        final Class<?> TargetSelectionEvent = XposedHelpers.findClass("alternativa.tanks.battle.weapons.aiming.fsm.TargetSelectionEvent", loadPackageParam.classLoader);
//        XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.fsm.TargetSelectionState", loadPackageParam.classLoader, "handleChildStateMachineEvent",TargetSelectionEvent , new XC_MethodReplacement() {
//            @Override
//            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                if(xsp.getBoolean("scorpio_lock", false) && methodHookParam.args[0].toString().contains("TargetSelectionEvent$Interrupted")) {
//                    // Avoid aiming charging been interrupted
//                    return null;
//                }
//                return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
//            }
//        });
    }

    private void setTargetMark(XC_LoadPackage.LoadPackageParam loadPackageParam, boolean show) {
        if (show) {
            XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.components.StrikerHUD", loadPackageParam.classLoader, "setShowTargetMark", boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    // If target is valid, prohibit to hide the target mark
                    if(targetValid) methodHookParam.args[0] = true;
                    return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
                }
            });

            XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.components.StrikerHUD", loadPackageParam.classLoader, "setRender", boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if(targetValid) methodHookParam.args[0] = true;
                    return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
                }
            });
        }
    }
    private void createConfigToast(XC_LoadPackage.LoadPackageParam loadPackageParam, XSharedPreferences xsp) {
        final Class<?> clazz = XposedHelpers.findClass("android.app.Instrumentation", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(clazz, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context context = (Context) XposedHelpers.callMethod(param.args[0], "getApplicationContext");
                Toast.makeText(context, loadPackageParam.packageName+xsp.getAll(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
