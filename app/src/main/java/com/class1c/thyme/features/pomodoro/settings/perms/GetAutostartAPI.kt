package com.class1c.thyme.features.pomodoro.settings.perms

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

fun getAutostartAPI(context: Context) : ComponentName? {
    val intent = Intent()
    val manufacturer = Build.MANUFACTURER

    when {
        "xiaomi".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }

        "oppo".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        }

        "vivo".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        }

        "letv".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"
            )
        }

        "honor".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        }

        "asus".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.powersaver.PowerSaverSettings"
            )
        }

        "nokia".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.evenwell.powersaving.g3",
                "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
            )
        }

        "huawei".equals(manufacturer, ignoreCase = true) -> {
            intent.component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        }
    }

    return intent.component
}