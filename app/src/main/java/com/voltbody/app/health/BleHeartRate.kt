package com.voltbody.app.health

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Cliente BLE de pulsometro (servicio GATT Heart Rate 0x180D, caracteristica 0x2A37).
 * Port de BLEHeartRateService.ts. state: disconnected | scanning | connecting | connected | error.
 */
class BleHeartRateClient(ctx: Context) {

    private val appCtx: Context = ctx.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private val _bpm = MutableStateFlow<Int?>(null)
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()
    private val _state = MutableStateFlow("disconnected")
    val state: StateFlow<String> = _state.asStateFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    /** true = sensor tocando piel, null = no reportado (equivale a HRPayload.contact de la web). */
    private val _contact = MutableStateFlow<Boolean?>(null)
    val contact: StateFlow<Boolean?> = _contact.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var scanCallback: ScanCallback? = null
    private var userStopped = false

    private val scanTimeout = Runnable {
        if (scanning) {
            stopScan()
            _state.value = "disconnected" // equivale a cancelar el selector sin error
        }
    }

    companion object {
        val HR_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val SCAN_TIMEOUT_MS = 20_000L

        /** Permisos runtime necesarios segun la version de Android (para pedirlos desde la UI). */
        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        fun hasPermissions(ctx: Context): Boolean = requiredPermissions().all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Parsea Heart Rate Measurement (Bluetooth spec): byte0 flags; bit0 = formato uint16;
         * bit1 = contacto soportado; bit2 = contacto detectado. Devuelve (bpm, contacto) o null si no valido.
         */
        fun parse(data: ByteArray): Pair<Int, Boolean?>? {
            if (data.size < 2) return null
            val flags = data[0].toInt() and 0xFF
            val is16 = (flags and 0x01) != 0
            val contactPresent = (flags and 0x02) != 0
            val contactDetected = (flags and 0x04) != 0
            val bpm = if (is16) {
                if (data.size < 3) return null
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else data[1].toInt() and 0xFF
            if (bpm <= 0 || bpm >= 300) return null
            return bpm to (if (contactPresent) contactDetected else null)
        }
    }

    private fun adapter(): BluetoothAdapter? =
        (appCtx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    fun start() {
        if (_state.value == "scanning" || _state.value == "connecting" || _state.value == "connected") return
        userStopped = false
        val ad = adapter()
        if (ad == null || !ad.isEnabled || !hasPermissions(appCtx)) {
            _state.value = "error"
            return
        }
        val scanner = ad.bluetoothLeScanner
        if (scanner == null) { _state.value = "error"; return }
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!scanning) return
                stopScan()
                connect(result.device)
            }
            override fun onScanFailed(errorCode: Int) {
                scanning = false
                _state.value = "error"
            }
        }
        scanCallback = cb
        try {
            val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE)).build())
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanning = true
            _state.value = "scanning"
            scanner.startScan(filters, settings, cb)
            main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS)
        } catch (_: Exception) {
            scanning = false
            _state.value = "error"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        main.removeCallbacks(scanTimeout)
        val cb = scanCallback
        if (scanning && cb != null) {
            try { adapter()?.bluetoothLeScanner?.stopScan(cb) } catch (_: Exception) { }
        }
        scanning = false
        scanCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        _state.value = "connecting"
        val nm: String? = try { device.name } catch (_: SecurityException) { null }
        _deviceName.value = nm ?: device.address
        try {
            gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                device.connectGatt(appCtx, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            else device.connectGatt(appCtx, false, gattCallback)
        } catch (_: Exception) {
            cleanup()
            _state.value = "error"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                val failedWhileConnecting = _state.value == "connecting" && status != BluetoothGatt.GATT_SUCCESS
                cleanup()
                main.post { _state.value = if (failedWhileConnecting && !userStopped) "error" else "disconnected" }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val ch = g.getService(HR_SERVICE)?.getCharacteristic(HR_MEASUREMENT)
            if (status != BluetoothGatt.GATT_SUCCESS || ch == null) {
                cleanup(); main.post { _state.value = "error" }; return
            }
            g.setCharacteristicNotification(ch, true)
            val desc = ch.getDescriptor(CCC_DESCRIPTOR)
            if (desc == null) { cleanup(); main.post { _state.value = "error" }; return }
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                g.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothGatt.GATT_SUCCESS
            else {
                @Suppress("DEPRECATION")
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(desc)
            }
            if (!ok) { cleanup(); main.post { _state.value = "error" } }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) main.post { _state.value = "connected" }
            else { cleanup(); main.post { _state.value = "error" } }
        }

        // API 33+
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handle(characteristic.uuid, value)
        }

        // API < 33
        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) handle(characteristic.uuid, characteristic.value ?: return)
        }

        private fun handle(uuid: UUID, value: ByteArray) {
            if (uuid != HR_MEASUREMENT) return
            val (bpm, contact) = parse(value) ?: return
            _bpm.value = bpm
            _contact.value = contact
        }
    }

    /** Detiene escaneo/conexion y libera el GATT. */
    fun stop() {
        userStopped = true
        stopScan()
        cleanup()
        _bpm.value = null
        _contact.value = null
        _deviceName.value = null
        _state.value = "disconnected"
    }

    @SuppressLint("MissingPermission")
    private fun cleanup() {
        val g = gatt
        gatt = null
        if (g != null) {
            try { g.disconnect() } catch (_: Exception) { }
            try { g.close() } catch (_: Exception) { }
        }
    }
}
