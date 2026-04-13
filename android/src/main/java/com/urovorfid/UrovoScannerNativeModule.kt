package com.urovorfid

import android.util.Log
import android.device.ScanManager
import android.device.scanner.configuration.Triggering

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class UrovoScannerNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private var scanManager: ScanManager? = null

    override fun getName(): String {
        return "UrovoScannerNative"
    }

    init {
        try {
            scanManager = ScanManager()
        } catch (e: Exception) {
            Log.e("UrovoScanner", "Dispositivo não suporta ScanManager nativo", e)
        }
    }

    // ==========================================
    // CONTROLE NATIVO DO SCANNER (LASER)
    // ==========================================
    @ReactMethod
    fun configureScanner(isOpen: Boolean, sound: Boolean, vibrate: Boolean, triggerLock: Boolean, promise: Promise) {
        try {
            val sm = scanManager ?: ScanManager().also { scanManager = it }

            if (isOpen) {
                sm.openScanner()
            } else {
                sm.closeScanner()
            }

            val idArray = intArrayOf(6, 9, 7, 10, 13)
            
            val soundVal = if (sound) 1 else 0
            val vibVal = if (vibrate) 1 else 0
            val lockVal = if (triggerLock) 1 else 0

            val valArray = intArrayOf(soundVal, soundVal, vibVal, vibVal, lockVal)

            sm.setParameterInts(idArray, valArray)

            promise.resolve(true)
        } catch (e: Exception) {
            Log.e("UrovoScanner", "Falha ao configurar Scanner Nativamente", e)
            promise.reject("ERRO", e.message)
        }
    }

    @ReactMethod
    fun setScannerTriggerMode(mode: String, promise: Promise) {
        try {
            val scanManager = ScanManager()
            
            when (mode) {
                "HOST" -> {
                    // Padrão: Segura para ler, solta para desligar
                    scanManager.setTriggerMode(Triggering.HOST)
                }
                "CONTINUOUS" -> {
                    // Contínuo: Laser sempre aceso (mãos livres)
                    scanManager.setTriggerMode(Triggering.CONTINUOUS)
                }
                "PULSE" -> {
                    // Pulso: Aperta uma vez e solta, ele fica aceso até ler algo
                    scanManager.setTriggerMode(Triggering.PULSE)
                }
                else -> {
                    scanManager.setTriggerMode(Triggering.HOST)
                }
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERRO", "Falha ao mudar modo do Scanner: ${e.message}")
        }
    }
}