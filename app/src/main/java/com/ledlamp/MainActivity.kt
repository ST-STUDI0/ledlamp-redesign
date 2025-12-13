package com.ledlamp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.ledlamp.adapters.DeviceListAdapter
import com.ledlamp.bluetooth.BluetoothLeServiceSingle
import com.ledlamp.models.BleDevice
import com.ledlamp.views.ColorPickerView

class MainActivity : AppCompatActivity() {

    private lateinit var colorPicker: ColorPickerView
    private lateinit var selectedColorPreview: View
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessValueText: TextView
    private lateinit var connectionStatusText: TextView
    private lateinit var scanButton: MaterialButton
    
    private var bluetoothService: BluetoothLeServiceSingle? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val scannedDevices = mutableListOf<BleDevice>()
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private var scanDialog: AlertDialog? = null
    
    private var connectedDeviceName: String? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeServiceSingle.LocalBinder).getService()
            bluetoothService?.let {
                if (!it.initialize()) {
                    finish()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothLeServiceSingle.ACTION_GATT_CONNECTED -> {
                    updateConnectionStatus(true)
                }
                BluetoothLeServiceSingle.ACTION_GATT_DISCONNECTED -> {
                    updateConnectionStatus(false)
                    connectedDeviceName = null
                }
                BluetoothLeServiceSingle.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Toast.makeText(this@MainActivity, "Device ready", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startDeviceScan()
        } else {
            Toast.makeText(this, R.string.permission_bluetooth_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupColorPicker()
        setupPresets()
        setupBrightnessControl()
        setupTimerControls()
        setupBluetoothService()
    }

    private fun initializeViews() {
        colorPicker = findViewById(R.id.colorPicker)
        selectedColorPreview = findViewById(R.id.selectedColorPreview)
        brightnessSlider = findViewById(R.id.brightnessSlider)
        brightnessValueText = findViewById(R.id.brightnessValueText)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        scanButton = findViewById(R.id.scanButton)
        
        scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun setupColorPicker() {
        colorPicker.setOnColorChangeListener { color ->
            selectedColorPreview.setBackgroundColor(color)
            bluetoothService?.sendColorCommand(color)
        }
    }

    private fun setupPresets() {
        findViewById<MaterialButton>(R.id.brightLightButton).setOnClickListener {
            applyPreset(Color.WHITE, 100)
        }
        
        findViewById<MaterialButton>(R.id.warmLightButton).setOnClickListener {
            applyPreset(Color.rgb(255, 204, 153), 70)
        }
        
        findViewById<MaterialButton>(R.id.nightModeButton).setOnClickListener {
            applyPreset(Color.rgb(255, 107, 107), 30)
        }
    }

    private fun applyPreset(color: Int, brightness: Int) {
        colorPicker.setColor(color)
        selectedColorPreview.setBackgroundColor(color)
        brightnessSlider.value = brightness.toFloat()
        
        bluetoothService?.sendColorCommand(color)
        bluetoothService?.sendBrightnessCommand(brightness)
    }

    private fun setupBrightnessControl() {
        brightnessSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val brightnessPercent = value.toInt()
                brightnessValueText.text = getString(R.string.brightness_level, brightnessPercent)
                bluetoothService?.sendBrightnessCommand(brightnessPercent)
            }
        }
        
        // Initialize text
        brightnessValueText.text = getString(R.string.brightness_level, brightnessSlider.value.toInt())
    }

    private fun setupTimerControls() {
        findViewById<MaterialButton>(R.id.morningModeButton).setOnClickListener {
            // Morning mode: Gradual bright light
            Toast.makeText(this, "Morning mode activated", Toast.LENGTH_SHORT).show()
            applyPreset(Color.WHITE, 100)
        }
        
        findViewById<MaterialButton>(R.id.eveningModeButton).setOnClickListener {
            // Evening mode: Warm dimmed light
            Toast.makeText(this, "Evening mode activated", Toast.LENGTH_SHORT).show()
            applyPreset(Color.rgb(255, 204, 153), 40)
        }
        
        findViewById<MaterialButton>(R.id.timerOffButton).setOnClickListener {
            Toast.makeText(this, "Timer disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBluetoothService() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        val gattServiceIntent = Intent(this, BluetoothLeServiceSingle::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun checkPermissionsAndScan() {
        if (!bluetoothAdapter?.isEnabled!!) {
            Toast.makeText(this, R.string.enable_bluetooth, Toast.LENGTH_SHORT).show()
            return
        }
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needsPermission) {
            bluetoothPermissionLauncher.launch(permissions)
        } else {
            startDeviceScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDeviceScan() {
        if (scanning) return
        
        scannedDevices.clear()
        showDeviceListDialog()
        
        scanning = true
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        
        handler.postDelayed({
            scanning = false
            bluetoothLeScanner?.stopScan(scanCallback)
            updateDeviceList()
        }, 10000) // Scan for 10 seconds
        
        bluetoothLeScanner?.startScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name ?: "Unknown Device"
                val bleDevice = BleDevice(device, deviceName, device.address)
                
                if (scannedDevices.none { it.address == device.address }) {
                    scannedDevices.add(bleDevice)
                    runOnUiThread {
                        updateDeviceList()
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeviceListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_device_list, null)
        
        scanDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ ->
                stopScan()
                dialog.dismiss()
            }
            .create()
        
        scanDialog?.show()
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) {
            scanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private fun updateDeviceList() {
        scanDialog?.let { dialog ->
            val recyclerView = dialog.findViewById<RecyclerView>(R.id.deviceList)
            val progressBar = dialog.findViewById<ProgressBar>(R.id.scanningProgress)
            val noDevicesText = dialog.findViewById<TextView>(R.id.noDevicesText)
            
            progressBar?.visibility = if (scanning) View.VISIBLE else View.GONE
            
            if (scannedDevices.isEmpty() && !scanning) {
                noDevicesText?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
            } else {
                noDevicesText?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                
                recyclerView?.layoutManager = LinearLayoutManager(this)
                recyclerView?.adapter = DeviceListAdapter(scannedDevices) { device ->
                    connectToDevice(device)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun connectToDevice(device: BleDevice) {
        stopScan()
        connectedDeviceName = device.name
        bluetoothService?.connect(device.address)
        Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (connected) {
            connectionStatusText.text = getString(R.string.device_connected, connectedDeviceName ?: "Device")
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.connected))
            scanButton.text = getString(R.string.disconnect)
            scanButton.setOnClickListener {
                bluetoothService?.disconnect()
            }
        } else {
            connectionStatusText.text = getString(R.string.device_disconnected)
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.disconnected))
            scanButton.text = getString(R.string.scan_devices)
            scanButton.setOnClickListener {
                checkPermissionsAndScan()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothLeServiceSingle.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeServiceSingle.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeServiceSingle.ACTION_GATT_SERVICES_DISCOVERED)
        }
        registerReceiver(gattUpdateReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        unbindService(serviceConnection)
        bluetoothService = null
    }
}
