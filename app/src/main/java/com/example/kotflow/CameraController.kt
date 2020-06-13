package com.example.kotflow

import android.annotation.SuppressLint
import android.hardware.camera2.*
import android.view.Surface
import android.view.SurfaceHolder
import timber.log.Timber


class CameraController(private val manager: CameraManager) {
    var cameraID: String? = chooseBackCamera()

    @SuppressLint("MissingPermission")
    fun start(outSurface: Surface) {
        cameraID?.let{
            val stateCallback = SurfaceConnectCallback{device -> createCameraPreviewSession(device, outSurface)}
            manager.openCamera(it, stateCallback, null)
        }
    }

    private fun chooseBackCamera(): String? {
        for (cameraId in manager.cameraIdList) {
            val characteristics =
                manager.getCameraCharacteristics(cameraId)

            // We don't use a front facing camera in this sample.
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }

            return cameraId
        }

        return null
    }

    private fun createCameraPreviewSession(cameraDevice: CameraDevice, surface: Surface) {
        Timber.d("Create preview session stub.")

        // We set up a CaptureRequest.Builder with the output Surface.
        val previewRequestBuilder = cameraDevice.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        )
        previewRequestBuilder.addTarget(surface)

        // Here, we create a CameraCaptureSession for camera preview.
        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    Timber.d("Camera capture session configured")

                    // Auto focus should be continuous for camera preview.
                    previewRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )

                    cameraCaptureSession.setRepeatingRequest(
                        previewRequestBuilder.build(),
                        object : CameraCaptureSession.CaptureCallback() {},
                        null)
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Timber.d("Camera configure attempt failed.")
                }
            }, null
        )
    }
}

class SurfaceConnectCallback(val createSessionImpl: (cameraDevice: CameraDevice) -> Unit) : CameraDevice.StateCallback() {
    override fun onOpened(cameraDevice: CameraDevice) {
        createSessionImpl(cameraDevice)
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
        cameraDevice.close()
    }

    override fun onError(cameraDevice: CameraDevice, error: Int) {
        onDisconnected(cameraDevice)
    }
}
