package com.urovorfid

import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

import com.ubx.usdk.RFIDSDKManager
import com.ubx.usdk.bean.ReadTag
import com.ubx.usdk.listener.DataCallback

import com.ubx.usdk.io.GripDeviceManager
import com.ubx.usdk.io.listener.KeyEventListener
import com.ubx.usdk.constant.BTKeyEvent

import com.ubx.usdk.listener.InitListener

import com.ubx.usdk.bean.Tag6C
import com.ubx.usdk.util.SoundTool

class UrovoRfidNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    companion object {
        var globalContext: ReactApplicationContext? = null
        
        fun sendHardwareTrigger(isDown: String) {
            try {
                globalContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    ?.emit("onHardwareTrigger", isDown)
                Log.d("UrovoGatilho", "Sinal $isDown disparado pro JS através do módulo!")
            } catch (e: Exception) {
                Log.e("UrovoGatilho", "Erro ao disparar pro JS", e)
            }
        }
    }

    private var isRadarWorking = false
    private var radarThread: Thread? = null

    init {
        globalContext = reactContext // Salva a conexão assim que o app abre
    }

    override fun getName(): String {
        return "UrovoRfidNative"
    }

    private fun sendEvent(eventName: String, data: String) {
        try {
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, data)
        } catch (e: Exception) {
            Log.e("UrovoRfidNative", "Erro ao enviar evento para o JS", e)
        }
    }

    private val rfidCallback = object : DataCallback {
        override fun onInventoryTag(tag: ReadTag?) {
            tag?.epcId?.let { epc ->
                Log.d("UrovoRfidNative", "🎯 NATIVO: Tag capturada pela antena: $epc")
                sendEvent("onRfidRead", epc)
            }
        }
        override fun onInventoryTagEnd() {}
    }

    @ReactMethod
    fun initAntenna(promise: Promise) {
        try {
            RFIDSDKManager.getInstance().init(reactApplicationContext, object : InitListener {
                override fun onStatus(status: Boolean) {
                    if (status) {
                        Log.d("UrovoRfidNative", "Placa RFID inicializada fisicamente com sucesso!")
                        
                        val rfidManager = RFIDSDKManager.getInstance().rfidManager
                        if (rfidManager != null) {
                            rfidManager.addDataCallback(rfidCallback)
                            rfidManager.setBeepEnable(true) 
                            
                            GripDeviceManager.getInstance().setKeyEventListener(object : KeyEventListener {
                                override fun event(keyCode: Int, isDown: Boolean) {
                                    if (keyCode == BTKeyEvent.BT_SCAN || keyCode == 515 || keyCode == 523) {
                                        sendEvent("onHardwareTrigger", if (isDown) "true" else "false")
                                    }
                                }
                            })

                            promise.resolve(true)
                        } else {
                            promise.reject("ERRO", "Placa ligou, mas o rfidManager continuou nulo.")
                        }
                    } else {
                        promise.reject("ERRO", "Falha ao ligar a placa RFID.")
                    }
                }
            })
        } catch (e: Exception) {
            promise.reject("ERRO", "Exceção ao tentar inicializar: ${e.message}")
        }
    }
}
