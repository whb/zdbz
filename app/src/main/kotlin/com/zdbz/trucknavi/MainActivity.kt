package com.zdbz.trucknavi

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.navi.AMapNaviView
import com.amap.api.navi.model.NaviInfo
import com.amap.api.navi.model.NaviLatLng
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.zdbz.trucknavi.model.TruckInfo
import com.zdbz.trucknavi.navi.NaviController

class MainActivity : AppCompatActivity() {

    private lateinit var naviView: AMapNaviView
    private var amap: AMap? = null

    private lateinit var naviController: NaviController
    private var locationClient: AMapLocationClient? = null

    private var startLatLng: NaviLatLng? = null
    private var endLatLng: NaviLatLng? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private lateinit var truckInfo: TruckInfo

    private var isNavigating = false
    private var pendingSimulate = false

    private lateinit var etKeyword: EditText
    private lateinit var btnStart: Button
    private lateinit var tvStart: TextView
    private lateinit var tvDest: TextView
    private lateinit var rbSim: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        truckInfo = TruckInfo.load(this)

        naviView = findViewById(R.id.naviView)
        naviView.onCreate(savedInstanceState)
        naviController = NaviController(this, naviView)
        naviController.callback = object : NaviController.Callback {
            override fun onRouteSucceeded() {
                runOnUiThread {
                    naviView.displayOverview()
                    naviController.startNavigation(pendingSimulate)
                }
            }

            override fun onRouteFailed(errorCode: Int) {
                runOnUiThread { toast(getString(R.string.toast_route_fail, errorCode)) }
            }

            override fun onNaviInfo(naviInfo: NaviInfo) {
                // AMapNaviView 已展示实时引导信息，这里不重复处理
            }

            override fun onStarted() {
                runOnUiThread {
                    isNavigating = true
                    updateStartButton()
                }
            }

            override fun onArrived() {
                runOnUiThread {
                    isNavigating = false
                    updateStartButton()
                    toast("已到达目的地")
                }
            }
        }

        naviView.setOnMapLoadedListener {
            amap = naviView.getMap()
            amap?.uiSettings?.setMyLocationButtonEnabled(true)
            amap?.isMyLocationEnabled = true
            val default = LatLng(39.908823, 116.397470) // 默认北京
            amap?.moveCamera(CameraUpdateFactory.newLatLngZoom(default, 12f))
            if (startLatLng != null) drawStart()
            if (endLatLng != null) drawEnd()
        }

        etKeyword = findViewById(R.id.etKeyword)
        btnStart = findViewById(R.id.btnStart)
        tvStart = findViewById(R.id.tvStart)
        tvDest = findViewById(R.id.tvDest)
        rbSim = findViewById(R.id.rbSim)

        findViewById<Button>(R.id.btnSearch).setOnClickListener { onSearch() }
        findViewById<View>(R.id.btnLocate).setOnClickListener { requestLocation() }
        findViewById<Button>(R.id.btnTruckParam).setOnClickListener { showTruckDialog() }
        btnStart.setOnClickListener { onStartClick() }

        requestLocation()
    }

    // ---------------- 定位 ----------------
    private fun requestLocation() {
        try {
            val client = AMapLocationClient(this)
            locationClient = client
            val option = AMapLocationClientOption()
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
            option.setOnceLocation(true)
            option.setNeedAddress(true)
            client.setLocationOption(option)
            client.setLocationListener(locationListener)
            client.startLocation()
        } catch (e: Exception) {
            toast("定位初始化失败：${e.message}")
        }
    }

    private val locationListener = AMapLocationListener { location ->
        if (location == null) return@AMapLocationListener
        if (location.errorCode == 0) {
            val ll = NaviLatLng(location.latitude, location.longitude)
            startLatLng = ll
            val addr = location.address ?: ""
            tvStart.text = if (addr.isBlank()) "起点：${fmt(ll)}" else "起点：$addr"
            drawStart()
        } else {
            toast("定位失败(${location.errorCode})：${location.description}")
        }
    }

    private fun fmt(ll: NaviLatLng): String = "纬度 ${ll.latitude}, 经度 ${ll.longitude}"

    private fun drawStart() {
        val map = amap ?: return
        val ll = startLatLng ?: return
        startMarker?.remove()
        startMarker = map.addMarker(
            MarkerOptions().position(LatLng(ll.latitude, ll.longitude)).title("起点").snippet("起点")
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(ll.latitude, ll.longitude), 15f))
    }

    private fun drawEnd() {
        val map = amap ?: return
        val ll = endLatLng ?: return
        endMarker?.remove()
        endMarker = map.addMarker(
            MarkerOptions().position(LatLng(ll.latitude, ll.longitude)).title("终点").snippet("终点")
        )
    }

    // ---------------- 搜索 ----------------
    private fun onSearch() {
        val kw = etKeyword.text.toString().trim()
        if (kw.isEmpty()) {
            toast("请输入目的地")
            return
        }
        try {
            val query = PoiSearch.Query(kw, "", "")
            val search = PoiSearch(this, query)
            search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, errCode: Int) {
                    if (errCode == 0 && result != null) {
                        val pois = result.pois
                        if (pois != null && pois.isNotEmpty()) {
                            val item = pois[0]
                            val llp = item.latLonPoint
                            endLatLng = NaviLatLng(llp.latitude, llp.longitude)
                            tvDest.text = "终点：${item.title}"
                            drawEnd()
                        } else {
                            toast("未找到相关地点")
                        }
                    } else {
                        toast("搜索失败($errCode)")
                    }
                }

                override fun onPoiItemSearched(item: PoiItem?, errCode: Int) {}
            })
            search.searchPOI()
        } catch (e: Exception) {
            toast("搜索异常：${e.message}")
        }
    }

    // ---------------- 货车参数 ----------------
    private fun showTruckDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_truck, null)
        val etPlate = v.findViewById<EditText>(R.id.etPlate)
        val spnSize = v.findViewById<Spinner>(R.id.spnSize)
        val etLength = v.findViewById<EditText>(R.id.etLength)
        val etWidth = v.findViewById<EditText>(R.id.etWidth)
        val etHeight = v.findViewById<EditText>(R.id.etHeight)
        val etWeight = v.findViewById<EditText>(R.id.etWeight)
        val etLoad = v.findViewById<EditText>(R.id.etLoad)
        val etAxis = v.findViewById<EditText>(R.id.etAxis)
        val cbAvoid = v.findViewById<CheckBox>(R.id.cbAvoidRestrict)
        val cbLoad = v.findViewById<CheckBox>(R.id.cbLoadSwitch)

        val sizeLabels = listOf(
            getString(R.string.size_1), getString(R.string.size_2),
            getString(R.string.size_3), getString(R.string.size_4)
        )
        spnSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizeLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spnSize.setSelection(truckInfo.carSize - 1)

        etPlate.setText(truckInfo.plate)
        etLength.setText(truckInfo.length)
        etWidth.setText(truckInfo.width)
        etHeight.setText(truckInfo.height)
        etWeight.setText(truckInfo.weight)
        etLoad.setText(truckInfo.load)
        etAxis.setText(truckInfo.axis)
        cbAvoid.isChecked = truckInfo.avoidRestrict
        cbLoad.isChecked = truckInfo.loadSwitch

        AlertDialog.Builder(this)
            .setTitle(R.string.truck_title)
            .setView(v)
            .setPositiveButton(R.string.confirm) { _, _ ->
                truckInfo = truckInfo.copy(
                    plate = etPlate.text.toString().trim(),
                    carSize = spnSize.selectedItemPosition + 1,
                    length = etLength.text.toString().ifBlank { "12" },
                    width = etWidth.text.toString().ifBlank { "2.5" },
                    height = etHeight.text.toString().ifBlank { "4" },
                    weight = etWeight.text.toString().ifBlank { "10" },
                    load = etLoad.text.toString().ifBlank { "20" },
                    axis = etAxis.text.toString().ifBlank { "2" },
                    avoidRestrict = cbAvoid.isChecked,
                    loadSwitch = cbLoad.isChecked
                )
                TruckInfo.save(this, truckInfo)
                toast("货车参数已保存")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------- 导航 ----------------
    private fun onStartClick() {
        if (isNavigating) {
            naviController.stopNavigation()
            isNavigating = false
            updateStartButton()
            return
        }
        val s = startLatLng
        val e = endLatLng
        if (e == null) {
            toast(getString(R.string.toast_no_dest))
            return
        }
        if (s == null) {
            toast(getString(R.string.toast_no_start))
            return
        }
        pendingSimulate = rbSim.isChecked
        naviController.calculateTruckRoute(truckInfo, s, e, pendingSimulate)
    }

    private fun updateStartButton() {
        btnStart.text = getString(if (isNavigating) R.string.btn_stop else R.string.btn_start)
    }

    // ---------------- 生命周期 ----------------
    override fun onResume() {
        super.onResume()
        naviView.onResume()
    }

    override fun onPause() {
        super.onPause()
        naviView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        naviView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        naviView.onDestroy()
        naviController.release()
        locationClient?.onDestroy()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
