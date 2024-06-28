package com.blockstream.green.devices

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.isBonded
import com.blockstream.common.extensions.isJade
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.utils.Loggable
import com.blockstream.jade.connection.JadeBleConnection
import com.btchip.comm.LedgerDeviceBLE
import com.juul.kable.Bluetooth
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.peripheral
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference


class DeviceManagerAndroid constructor(
    scope: ApplicationScope,
    val context: Context,
    sessionManager: SessionManager,
    bluetoothManager: BluetoothManager,
    val usbManager: UsbManager
): DeviceManager(scope, sessionManager, bluetoothManager, SupportedBleUuid) {

    private var onPermissionSuccess: WeakReference<(() -> Unit)>? = null
    private var onPermissionError: WeakReference<((throwable: Throwable?) -> Unit)>? = null

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            logger.i { "onReceive: ${intent.action}" }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                scanUsbDevices()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                scanUsbDevices()
            } else if (ACTION_USB_PERMISSION == intent.action) {

                val device: UsbDevice? = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)

                if (device != null && (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) || hasPermissions(device))) {
                    device.apply {
                        logger.i { "Permission granted for device $device" }
                        onPermissionSuccess?.get()?.invoke()
                    }
                } else {
                    logger.i { "Permission denied for device $device" }
                    onPermissionError?.get()?.invoke(null)
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().also {
            it.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            it.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            it.addAction(ACTION_USB_PERMISSION)
            it.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        scanUsbDevices()

        Bluetooth.availability.onEach {
            it
        }
    }

    override fun advertisedDevice(advertisement: PlatformAdvertisement) {
        val isJade = advertisement.isJade

        // Jade is added in Common code
        if (isJade) {
            super.advertisedDevice(advertisement)
        } else {

            val peripheral = scope.peripheral(advertisement)

            val device = AndroidDevice.fromScan(
                deviceManager = this,
                peripheral = peripheral,
                bleService = advertisement.uuids.first().let {
                    ParcelUuid(it)
                },
                isBonded = advertisement.isBonded()
            )

            addBluetoothDevice(device)
        }
    }

    fun hasPermissions(device: UsbDevice) = usbManager.hasPermission(device)

    fun askForPermissions(device: UsbDevice, onSuccess: (() -> Unit), onError: ((throwable: Throwable?) -> Unit)? = null) {
        onPermissionSuccess = WeakReference(onSuccess)
        onPermissionError = onError?.let { WeakReference(it) }
        val permissionIntent = PendingIntent.getBroadcast(context, 748, Intent(ACTION_USB_PERMISSION).also {
            // Cause of FLAG_IMMUTABLE OS won't give us the Extra Device
            it.putExtra(UsbManager.EXTRA_DEVICE, device)
        }, FLAG_IMMUTABLE)
        usbManager.requestPermission(device, permissionIntent)
    }

    fun refreshDevices(){
        logger.i { "Refresh device list" }

        bleDevices.value = listOf()
        scanUsbDevices()
    }

    fun scanUsbDevices() {
        logger.i { "Scan for USB devices" }

        val newUsbDevices = usbManager.deviceList.values

        // Disconnect devices
        val oldDevices = usbDevices.value.filter {
            if(newUsbDevices.contains(it.toAndroidDevice()?.usbDevice)){
                true
            }else{
                it.toAndroidDevice()?.offline()
                false
            }
        }

        val newDevices = mutableListOf<AndroidDevice>()
        for (usbDevice in newUsbDevices){
            if(oldDevices.find { it.toAndroidDevice()?.usbDevice == usbDevice } == null){
                AndroidDevice.fromUsbDevice(this, usbDevice)?.let{
                    newDevices += it
                }
            }
        }

        usbDevices.value = oldDevices + newDevices
    }

    companion object : Loggable() {
        private const val ACTION_USB_PERMISSION = "com.blockstream.green.USB_PERMISSION"

        // Supported BLE Devices
        private val SupportedBleUuid = listOf(LedgerDeviceBLE.SERVICE_UUID.toString(), JadeBleConnection.JADE_SERVICE)
    }
}