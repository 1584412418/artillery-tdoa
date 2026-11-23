package com.artillery.tdoa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rgMode: RadioGroup
    private lateinit var rbMaster: RadioButton
    private lateinit var rbSlave: RadioButton
    private lateinit var etMasterIp: EditText
    private lateinit var etTemp: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnNtpSync: Button
    private lateinit var btnGps: Button
    private lateinit var btnManualCoord: Button
    private lateinit var btnTemplate: Button
    private lateinit var btnHistory: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLocResult: TextView

    private var isMasterMode = false
    private var running = false

    private lateinit var ntpSync: NtpTimeSync
    private lateinit var locHelper: LocationHelper
    private var currentNtpTs: Long = 0L
    private var stationX: Double = 0.0
    private var stationY: Double = 0.0

    private var masterListener: MasterUdpListener? = null
    private var slaveReporter: SlaveReporter? = null
    private var audioDetector: AudioDetector? = null

    private val permissionList = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindView()
        initModule()
        checkPermission()
        bindEvent()
    }

    private fun bindView() {
        rgMode = findViewById(R.id.rg_mode)
        rbMaster = findViewById(R.id.rb_master)
        rbSlave = findViewById(R.id.rb_slave)
        etMasterIp = findViewById(R.id.et_master_ip)
        etTemp = findViewById(R.id.et_temp)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        btnNtpSync = findViewById(R.id.btn_ntp_sync)
        btnGps = findViewById(R.id.btn_gps)
        btnManualCoord = findViewById(R.id.btn_manual_coord)
        btnTemplate = findViewById(R.id.btn_template)
        btnHistory = findViewById(R.id.btn_history)
        tvStatus = findViewById(R.id.tv_status)
        tvLocResult = findViewById(R.id.tv_loc_result)
    }

    private fun initModule() {
        ntpSync = NtpTimeSync()
        ntpSync.onResult = { ok, ts ->
            if (ok && ts != null) {
                currentNtpTs = ts
                updateStatus("NTP同步成功：${ntpSync.formatTime(ts)}")
            } else {
                currentNtpTs = System.currentTimeMillis()
                updateStatus("NTP同步失败，使用本地时间")
            }
        }

        locHelper = LocationHelper(this, 38)
        locHelper.onGaussResult = { gx, gy ->
            stationX = gx
            stationY = gy
            runOnUiThread {
                Toast.makeText(this@MainActivity, "GPS高斯坐标：X=%.2f Y=%.2f".format(gx, gy), Toast.LENGTH_SHORT).show()
                tvLocResult.text = "本站高斯坐标：X=%.2f  Y=%.2f".format(gx, gy)
            }
        }
        locHelper.onError = { msg ->
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }

        val pref = StationPref(this)
        stationX = pref.getX()
        stationY = pref.getY()
        tvLocResult.text = "本站高斯坐标：X=%.2f  Y=%.2f".format(stationX, stationY)
    }

    private fun bindEvent() {
        rgMode.setOnCheckedChangeListener { _, checkedId ->
            isMasterMode = checkedId == R.id.rb_master
        }

        btnStart.setOnClickListener { startWork() }
        btnStop.setOnClickListener { stopWork() }

        btnNtpSync.setOnClickListener {
            ntpSync.sync()
        }

        btnGps.setOnClickListener {
            if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                locHelper.start()
                Toast.makeText(this, "正在获取GPS...", Toast.LENGTH_SHORT).show()
            } else {
                requestPermission()
            }
        }

        btnManualCoord.setOnClickListener {
            startActivity(Intent(this, ManualStationActivity::class.java))
        }

        btnTemplate.setOnClickListener {
            startActivity(Intent(this, TemplateManagerActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun startWork() {
        if (running) return
        val masterIp = etMasterIp.text.toString().trim()
        if (!isMasterMode && masterIp.isEmpty()) {
            Toast.makeText(this, "请填写主台IP", Toast.LENGTH_SHORT).show()
            return
        }
        val tempText = etTemp.text.toString().trim()
        val temp = tempText.toDoubleOrNull() ?: 15.0

        if (isMasterMode) {
            masterListener = MasterUdpListener(9000)
            masterListener?.onLocationResult = { (x, y) ->
                runOnUiThread {
                    tvLocResult.text = "定位结果(平滑后)：X=%.2f  Y=%.2f".format(x, y)
                    HistoryDb(this).addRecord(x, y, System.currentTimeMillis())
                }
            }
            masterListener?.start()
            updateStatus("主台已启动，等待属台上报")
        } else {
            slaveReporter = SlaveReporter(masterIp, 9000, "SLAVE_${System.currentTimeMillis()%10000}")
            slaveReporter?.start()

            audioDetector = AudioDetector(this)
            audioDetector?.onTrigger = {
                slaveReporter?.reportTrigger(stationX, stationY, currentNtpTs)
                runOnUiThread { Toast.makeText(this@MainActivity, "检测到爆炸，已上报", Toast.LENGTH_SHORT).show() }
            }
            audioDetector?.start()
            updateStatus("属台已启动，监听声音")
        }
        running = true
    }

    private fun stopWork() {
        if (!running) return
        masterListener?.stop()
        masterListener = null
        slaveReporter?.stop()
        slaveReporter = null
        audioDetector?.stop()
        audioDetector = null
        locHelper.stop()
        running = false
        updateStatus("已停止")
    }

    private fun updateStatus(text: String) {
        runOnUiThread { tvStatus.text = "状态：$text" }
    }

    private fun checkPermission(): Boolean {
        for (p in permissionList) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun checkSinglePermission(perm: String): Boolean {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissionList, 1001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "缺少必要权限，功能受限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val pref = StationPref(this)
        stationX = pref.getX()
        stationY = pref.getY()
        tvLocResult.text = "本站高斯坐标：X=%.2f  Y=%.2f".format(stationX, stationY)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWork()
    }
}
