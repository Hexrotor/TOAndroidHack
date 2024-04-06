package com.hexrotor.dddlocktarget;

import android.util.Log;
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
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.tankionline.china")) return;

        XSharedPreferences xsp = new XSharedPreferences("com.hexrotor.dddlocktarget", "function_config");
        Log.d("DDD", "Loaded preferences"+xsp.getAll());
        XposedHelpers.findAndHookMethod("projects.tanks.android.sdk.impl.ChinaSDKService", loadPackageParam.classLoader, "logData", android.content.Context.class, java.lang.String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if(param.args[1].toString().contains("initEnterPoint")) {
                    param.args[1] = "initEnterPoint "+ xsp.getAll();
                    XposedBridge.log(xsp.getAll().toString());
                }
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        setTargetMark(loadPackageParam, xsp.getBoolean("scorpio_remember_target", false) || xsp.getBoolean("striker_remember_target", false));
        final Class<?> LockResult = XposedHelpers.findClass("alternativa.tanks.battle.weapons.aiming.LockResult", loadPackageParam.classLoader);
        //striker
        XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.components.StrikerWeapon", loadPackageParam.classLoader, "lockTarget", LockResult, java.lang.Long.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                //执行原方法
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
                //执行原方法
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
        final Class<?> TargetSelectionEvent = XposedHelpers.findClass("alternativa.tanks.battle.weapons.aiming.fsm.TargetSelectionEvent", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.fsm.TargetSelectionState", loadPackageParam.classLoader, "handleChildStateMachineEvent",TargetSelectionEvent , new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if(xsp.getBoolean("scorpio_lock", false) && methodHookParam.args[0].toString().contains("TargetSelectionEvent$Interrupted")) {
                    //Log.d("DDD", methodHookParam.args[0].toString());
                    return null;
                }
                //调用原函数
                return XposedBridge.invokeOriginalMethod(methodHookParam.method, methodHookParam.thisObject, methodHookParam.args);
            }
        });
    }

    private void setTargetMark(XC_LoadPackage.LoadPackageParam loadPackageParam, boolean show) {
        if (show) {
            XposedHelpers.findAndHookMethod("alternativa.tanks.battle.weapons.types.striker.components.StrikerHUD", loadPackageParam.classLoader, "setShowTargetMark", boolean.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    //获取StrikerHUD.position.value.x.value
                    //float xValue = (float) XposedHelpers.getObjectField(XposedHelpers.getObjectField(methodHookParam.thisObject, "position"), "x");
                    //Log.d("DDD", "StrikerHUD xValue: "+xValue);
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
}
