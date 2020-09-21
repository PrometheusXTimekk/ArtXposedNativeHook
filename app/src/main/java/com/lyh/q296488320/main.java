package com.lyh.q296488320;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class main implements IXposedHookLoadPackage {
    public static final String TAG = "Q296488320";
    public static final String PackageName = "com.avalon.caveonline.cn.leiting";
    //libcocos2dlua
    public static final String SoName = "libcocos2dlua.so";
    public static final String DUMP_LUA_SO = "/data/data/com.lyh.q296488320/lib/libdumpLua.so";
    public static Context mContext;
    public static ClassLoader mLoader;


    XC_LoadPackage.LoadPackageParam mParam;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {

        Log.e(TAG, param.packageName);
        if (!param.packageName.equals(PackageName))
            return;

        Log.e(TAG, "找到了 要Hook的 包名 " + param.packageName);




    }


    private void HookAttach(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        CLogUtils.e("走了 attachBaseContext方法 ");
                        mContext = (Context) param.args[0];
                        mLoader = mContext.getClassLoader();
                        //注入自己的代码

                    }
                });

    }

    private void HookSoLoad() {
//        System.load();
//        System.loadLibrary();

        //ClassUtils.getClassMethodInfo(Runtime.class);
        int version = android.os.Build.VERSION.SDK_INT;
        CLogUtils.e("当前系统 版本号 " + version);
        //android 9.0没有 doLoad 方法
        if (version >= 28) {
//            HiddenAPIEnforcementPolicyUtils.passApiCheck();
            try {
                XposedHelpers.findAndHookMethod(Runtime.class,
                        "nativeLoad",
                        String.class,
                        ClassLoader.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String name = (String) param.args[0];
                                //Hook 要加载的 so
                                //当 加载 的 so 是 要 加载的 就加载自己的  libcocos2dlua
                                if (param.hasThrowable() || name == null || !name.endsWith(SoName)) {
                                    return;
                                }
                                try {

                                    //如果 包含 加载的 so 就 加载自己的 在 自己so里面 再打开 目标 so
                                    if (((String) param.args[0]).contains(SoName)) {
                                        //注入 自己的 so 吧 classloader进行 传入
                                        initMySo(param.args[1]);
                                    }
                                } catch (Throwable e) {
                                    CLogUtils.e("initMySo 异常" + e.getMessage());
                                }
                                CLogUtils.e("java层执行完毕");
                            }
                        });
            } catch (Throwable e) {
                CLogUtils.e("Hook 28以上出现异常 " + e.toString());
                e.printStackTrace();
            }
            CLogUtils.e("Hook 9.0以上 load成功");
        } else {
            //小于9.0
            try {
                XposedHelpers.findAndHookMethod(Runtime.class,
                        "doLoad",
                        String.class,
                        ClassLoader.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String name = (String) param.args[0];

                                //Hook 要加载的 so
                                //当 加载 的 so 是 要 加载的 就加载自己的  libcocos2dlua
                                if (param.hasThrowable() || name == null || !name.endsWith(SoName)) {
                                    return;
                                }
                                try {
                                    if (android.os.Build.VERSION.SDK_INT <= 19) {
                                        CLogUtils.e("走了 4.4的  load方法 ");
                                        System.load(getMySoPath());
                                    } else {
                                        //如果 包含 加载的 so 就 加载自己的 在 自己so里面 再打开 目标 so
                                        if (((String) param.args[0]).contains(SoName)) {
                                            //注入 自己的 so 吧 classloader进行 传入
                                            initMySo(param.args[1]);
                                        }
                                    }
                                } catch (Exception e) {
                                    CLogUtils.e("异常" + e.getMessage());
                                }
                                CLogUtils.e("java层执行完毕");
                            }
                        });
            } catch (Exception e) {
                CLogUtils.e("Hook doLoad出现异常 " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 在这里 把自己的 so进行 注入
     *
     * @param arg
     */
    private void initMySo(Object arg) {
        String path = getMySoPath();
        int version = android.os.Build.VERSION.SDK_INT;
        //CLogUtils.e("当前系统 版本号 " + version);
        //android 9.0没有 doLoad 方法
        if (version >= 28) {
            XposedHelpers.callMethod(Runtime.getRuntime(), "nativeLoad", path, arg);
        }else {
            XposedHelpers.callMethod(Runtime.getRuntime(), "doLoad", path, arg);
        }
        //CLogUtils.e("intoMySo 注入成功");

    }

    /**
     * 获取模块的So文件路径
     */
    private String getMySoPath() {


        //CLogUtils.e("开始注入自己的 So getMySoPath   ");


        PackageManager pm = mContext.getPackageManager();
        List<PackageInfo> pkgList = pm.getInstalledPackages(0);
        if (pkgList.size() > 0) {
            for (PackageInfo pi : pkgList) {
                if (pi.applicationInfo.publicSourceDir.startsWith("/data/app/" + BuildConfig.APPLICATION_ID)) {
                    //data/app/com.lyh.nkddemo-YuNFiNvInJyE3ahHYBXAQw==/base.apk
                    String path = pi.applicationInfo.publicSourceDir.
                            replace("base.apk", "lib/arm/libLVmp.so");
                    CLogUtils.e("getMySoPath 对应的路径是" + path);
                    return path;
                }
            }
        }

        //CLogUtils.e("没找到 MySo注入的 路径 ");
        return null;
    }

}
