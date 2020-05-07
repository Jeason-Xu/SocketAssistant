package com.letter.socketassistant.connection

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

/**
 * USB转串口连接
 * @property context Context context
 * @property driver UsbSerialDriver USB串口driver
 * @property baudRate Int 波特率
 * @property dataBits Int 数据位
 * @property parity Int 校验
 * @property stopBits Int 停止位
 * @property maxPacketLen Int 最大包长度
 * @property packetTimeOut Long 包超时
 * @property port UsbSerialPort? USB串口端口
 * @constructor 构造一个USB串口连接
 *
 * @author Letter(NevermindZZT@gmail.com)
 * @since 1.0.1
 */
class UsbSerialConnection constructor(private val context: Context,
                                      private val driver: UsbSerialDriver,
                                      private val baudRate: Int = 115200,
                                      private val dataBits: Int = UsbSerialPort.DATABITS_8,
                                      private val parity: Int = UsbSerialPort.PARITY_NONE,
                                      private val stopBits: Int = UsbSerialPort.STOPBITS_1,
                                      private val maxPacketLen: Int = 1024,
                                      private val packetTimeOut: Long = 100)
    : AbstractConnection() {

    companion object {
        private const val TAG = "UsbSerialConnection"

        /**
         * 获取所有USB 串口设备
         * @param context Context context
         * @return List<UsbSerialDriver> 设备列表
         */
        fun getDrivers(context: Context): List<UsbSerialDriver> {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        }
    }

    private var port: UsbSerialPort ?= null

    init {
        name = "usb serial: ${driver.device.deviceName}"
    }

    override fun send(connection: AbstractConnection, bytes: ByteArray?) {
        port?.write(bytes, packetTimeOut.toInt())
    }

    override fun run() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            onDisConnectedListener?.invoke(this)
            return
        }
        port = driver.ports[0]
        port?.open(connection)
        port?.setParameters(baudRate, dataBits, stopBits, parity)
        val data = ByteArray(maxPacketLen)
        while (!isInterrupted) {
            try {
                val length = port?.read(data, packetTimeOut.toInt())
                if (length != null && length > 0) {
                    onReceivedListener?.invoke(this, data.sliceArray(IntRange(0, length - 1)))
                }
            } catch (e: Exception) {
                Log.w(TAG, "", e)
                break
            }
        }
        onDisConnectedListener?.invoke(this)
    }

    override fun disconnect() {
        super.disconnect()
        port?.close()
    }
}