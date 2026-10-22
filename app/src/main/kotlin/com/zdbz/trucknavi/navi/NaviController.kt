package com.zdbz.trucknavi.navi

import android.content.Context
import com.amap.api.navi.AMapNavi
import com.amap.api.navi.AMapNaviListener
import com.amap.api.navi.AMapNaviView
import com.amap.api.navi.enums.NaviType
import com.amap.api.navi.model.AMapCalcRouteResult
import com.amap.api.navi.model.AMapCarInfo
import com.amap.api.navi.model.AMapLaneInfo
import com.amap.api.navi.model.AMapModelCross
import com.amap.api.navi.model.AMapNaviCameraInfo
import com.amap.api.navi.model.AMapNaviCross
import com.amap.api.navi.model.AMapNaviLocation
import com.amap.api.navi.model.AMapNaviRouteNotifyData
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo
import com.amap.api.navi.model.AMapServiceAreaInfo
import com.amap.api.navi.model.AimLessModeCongestionInfo
import com.amap.api.navi.model.AimLessModeStat
import com.amap.api.navi.model.NaviInfo
import com.amap.api.navi.model.NaviLatLng
import com.zdbz.trucknavi.model.TruckInfo

/**
 * 导航控制器：封装高德 [AMapNavi]（货车算路 + 引导）与 [AMapNaviView]（渲染）。
 * 实现完整 [AMapNaviListener]，通过 [Callback] 只把关心的事件抛给 Activity。
 */
class NaviController(
    context: Context,
    private val naviView: AMapNaviView
) : AMapNaviListener {

    interface Callback {
        fun onRouteSucceeded()
        fun onRouteFailed(errorCode: Int)
        fun onNaviInfo(naviInfo: NaviInfo)
        fun onArrived()
        fun onStarted()
    }

    var callback: Callback? = null

    val navi: AMapNavi = AMapNavi.getInstance(context)

    init {
        navi.addAMapNaviListener(this)
    }

    /** 货车算路。simulate=true 时使用模拟速度，便于无真实行驶时演示。 */
    fun calculateTruckRoute(truck: TruckInfo, start: NaviLatLng, end: NaviLatLng, simulate: Boolean) {
        val car = AMapCarInfo().apply {
            setCarType("1")                       // 1 = 货车（0=小车）
            setCarNumber(truck.plate)
            setVehicleSize(truck.carSize.toString()) // 1微型 2轻/小 3中 4重
            setVehicleLength(truck.length)
            setVehicleWidth(truck.width)
            setVehicleHeight(truck.height)
            setVehicleWeight(truck.weight)         // 核定载重(吨)
            setVehicleLoad(truck.load)             // 总重(吨)
            setVehicleAxis(truck.axis)
            setVehicleLoadSwitch(truck.loadSwitch)
            setRestriction(truck.avoidRestrict)    // 躲避限行
        }
        navi.setCarInfo(car)
        if (simulate) navi.setEmulatorNaviSpeed(360)
        // 躲避拥堵
        val strategy = navi.strategyConvert(true, false, false, false, false)
        navi.calculateDriveRoute(listOf(start), listOf(end), emptyList(), strategy)
    }

    /** 开始导航（在算路成功回调里调用）。 */
    fun startNavigation(simulate: Boolean) {
        navi.startNavi(if (simulate) NaviType.EMULATOR else NaviType.GPS)
    }

    fun stopNavigation() {
        navi.stopNavi()
    }

    fun isNavigating(): Boolean = AMapNavi.isNaviStarted()

    fun release() {
        navi.removeAMapNaviListener(this)
        AMapNavi.destroy() // 静态方法
    }

    // ================= AMapNaviListener 实现 =================

    override fun onInitNaviFailure() {}
    override fun onInitNaviSuccess() {}
    override fun onStartNavi(type: Int) { callback?.onStarted() }
    override fun onTrafficStatusUpdate() {}
    override fun onLocationChange(location: AMapNaviLocation?) {}
    override fun onGetNavigationText(type: Int, text: String?) {}
    override fun onGetNavigationText(text: String?) {}
    override fun onEndEmulatorNavi() {}
    override fun onArriveDestination() { callback?.onArrived() }
    override fun onCalculateRouteFailure(errorInfo: Int) { callback?.onRouteFailed(errorInfo) }
    override fun onReCalculateRouteForYaw() {}
    override fun onReCalculateRouteForTrafficJam() {}
    override fun onArrivedWayPoint(wayID: Int) {}
    override fun onGpsOpenStatus(enabled: Boolean) {}
    override fun onNaviInfoUpdate(naviInfo: NaviInfo?) { naviInfo?.let { callback?.onNaviInfo(it) } }
    override fun updateCameraInfo(infoArray: Array<AMapNaviCameraInfo>) {}
    override fun updateIntervalCameraInfo(start: AMapNaviCameraInfo, end: AMapNaviCameraInfo, status: Int) {}
    override fun onServiceAreaUpdate(infoArray: Array<AMapServiceAreaInfo>) {}
    override fun showCross(cross: AMapNaviCross) {}
    override fun hideCross() {}
    override fun showModeCross(cross: AMapModelCross) {}
    override fun hideModeCross() {}
    override fun showLaneInfo(laneInfos: Array<AMapLaneInfo>, laneBackgroundInfo: ByteArray, laneRecommendedInfo: ByteArray) {}
    override fun showLaneInfo(laneInfo: AMapLaneInfo) {}
    override fun hideLaneInfo() {}
    override fun onCalculateRouteSuccess(routeIds: IntArray) { callback?.onRouteSucceeded() }
    override fun notifyParallelRoad(parallelRoadType: Int) {}
    override fun OnUpdateTrafficFacility(infoArray: Array<AMapNaviTrafficFacilityInfo>) {}
    override fun OnUpdateTrafficFacility(info: AMapNaviTrafficFacilityInfo) {}
    override fun updateAimlessModeStatistics(stat: AimLessModeStat) {}
    override fun updateAimlessModeCongestionInfo(info: AimLessModeCongestionInfo) {}
    override fun onPlayRing(type: Int) {}
    override fun onCalculateRouteSuccess(routeResult: AMapCalcRouteResult) { callback?.onRouteSucceeded() }
    override fun onCalculateRouteFailure(routeResult: AMapCalcRouteResult) { callback?.onRouteFailed(routeResult.errorCode) }
    override fun onNaviRouteNotify(notifyData: AMapNaviRouteNotifyData) {}
    override fun onGpsSignalWeak(isWeak: Boolean) {}
}
