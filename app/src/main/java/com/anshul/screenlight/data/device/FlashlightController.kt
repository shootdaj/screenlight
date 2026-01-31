package com.anshul.screenlight.data.device

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for LED flashlight/torch using CameraManager.
 *
 * Provides toggle and state tracking for device torch.
 * No CAMERA permission required for setTorchMode() since API 23.
 */
@Singleton
class FlashlightController @Inject constructor(
    private val cameraManager: CameraManager
) {
    private val _torchState = MutableStateFlow(false)

    /**
     * Current torch state as StateFlow.
     * Automatically updated via TorchCallback when torch state changes.
     */
    val torchState: StateFlow<Boolean> = _torchState.asStateFlow()

    /**
     * Camera ID with flash capability, or null if none available.
     */
    private val cameraId: String? = findFlashCamera()

    init {
        // Register callback to track torch state changes
        // (can be triggered by other apps or system)
        try {
            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == this@FlashlightController.cameraId) {
                        _torchState.value = enabled
                    }
                }
            }, null)
        } catch (e: Exception) {
            // Callback registration can fail on some devices, silently continue
        }
    }

    /**
     * Check if device has flash capability.
     */
    val hasFlash: Boolean
        get() = cameraId != null

    /**
     * Toggle torch on/off.
     *
     * If torch is currently on, turns it off. If off, turns it on.
     * Fails silently if camera is in use or flash unavailable.
     */
    fun toggleTorch() {
        setTorch(!_torchState.value)
    }

    /**
     * Set torch to specific state.
     *
     * @param enabled true to turn on, false to turn off
     */
    fun setTorch(enabled: Boolean) {
        val id = cameraId ?: return

        try {
            cameraManager.setTorchMode(id, enabled)
            // State will be updated via TorchCallback
        } catch (e: CameraAccessException) {
            // Camera in use by another app, silently fail
        } catch (e: IllegalArgumentException) {
            // Invalid camera ID, silently fail
        }
    }

    /**
     * Find first camera with flash capability.
     *
     * @return Camera ID with flash, or null if none available
     */
    private fun findFlashCamera(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            null
        }
    }
}
