package com.zdbz.trucknavi

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.amap.api.navi.NaviSetting

/**
 * 高德 SDK 初始化 + 隐私合规声明。
 * 必须在任何高德组件使用前调用隐私合规接口（2022 年起强制）。
 * 高德 Key 通过 AndroidManifest 的 com.amap.api.v2.apikey 读取
 * （其值引用 res/values/amap_config.xml 中的 @string/amap_key，请手工填入）。
 */
class TruckNaviApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 定位 SDK 隐私合规
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        // 地图 SDK 隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        // 导航 SDK 隐私合规
        NaviSetting.updatePrivacyShow(this, true, true)
        NaviSetting.updatePrivacyAgree(this, true)
    }
}
