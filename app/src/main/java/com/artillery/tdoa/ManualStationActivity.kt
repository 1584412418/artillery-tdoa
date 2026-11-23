package com.artillery.tdoa

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ManualStationActivity : AppCompatActivity() {
    private lateinit var etX: EditText
    private lateinit var etY: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var stationPref: StationPref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_station)

        etX = findViewById(R.id.et_x)
        etY = findViewById(R.id.et_y)
        btnSave = findViewById(R.id.btn_save)
        btnBack = findViewById(R.id.btn_back)
        stationPref = StationPref(this)

        val x = stationPref.getX()
        val y = stationPref.getY()
        if (x != 0.0 || y != 0.0) {
            etX.setText("%.2f".format(x))
            etY.setText("%.2f".format(y))
        }

        btnSave.setOnClickListener {
            val sx = etX.text.toString().toDoubleOrNull()
            val sy = etY.text.toString().toDoubleOrNull()
            if (sx == null || sy == null) {
                Toast.makeText(this, "坐标格式错误", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            stationPref.saveXY(sx, sy)
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
