package com.ledlamp.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class BluetoothLeServiceSingle : Service() {

    companion object {
        private const val TAG = "BluetoothLeService"
        
        const val ACTION_GATT_CONNECTED = "com.ledlamp.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.ledlamp.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.ledlamp.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.ledlamp.ACTION_DATA_AVAILABLE"
        
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeServiceSingle = this@BluetoothLeServiceSingle
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        close()
        return super.onUnbind(intent)
    }

    fun initialize(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    Log.i(TAG, "Connected to GATT server.")
                    bluetoothGatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                    Log.i(TAG, "Disconnected from GATT server.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                Log.w(TAG, "Characteristic write failed: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                connectionState = STATE_CONNECTING
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
        }
    }

    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    fun writeCharacteristic(serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray): Boolean {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(serviceUuid) ?: return false
            val characteristic = service.getCharacteristic(characteristicUuid) ?: return false
            
            characteristic.value = value
            return gatt.writeCharacteristic(characteristic)
        } ?: return false
    }

    fun sendColorCommand(color: Int): Boolean {
        // Extract RGB components
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        
        // Create command byte array (format may vary based on LED lamp protocol)
        val command = byteArrayOf(
            0x56.toByte(), // Start byte (example)
            red.toByte(),
            green.toByte(),
            blue.toByte(),
            0xAA.toByte()  // End byte (example)
        )
        
        Log.d(TAG, "Sending color command: R=$red, G=$green, B=$blue")
        
        // Note: Replace with actual service and characteristic UUIDs from your LED lamp
        // This is a placeholder UUID
        val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        
        return writeCharacteristic(serviceUuid, characteristicUuid, command)
    }

    fun sendBrightnessCommand(brightness: Int): Boolean {
        // Create brightness command (format may vary based on LED lamp protocol)
        val command = byteArrayOf(
            0x56.toByte(), // Start byte
            0x01.toByte(), // Brightness command
            brightness.toByte(),
            0xAA.toByte()  // End byte
        )
        
        Log.d(TAG, "Sending brightness command: $brightness")
        
        val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val characteristicUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        
        return writeCharacteristic(serviceUuid, characteristicUuid, command)
    }

    fun getConnectionState(): Int = connectionState
    
    fun isConnected(): Boolean = connectionState == STATE_CONNECTED
}
