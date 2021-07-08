package com.github.gary.opencv.kotlin.resistor.model

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Camera
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import org.opencv.android.JavaCameraView


/**
 * Created by parth on 05/05/15.
 */
public class ResistorCameraView : JavaCameraView  {
    constructor(context: Context?, cameraId: Int) : super(context, cameraId) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    protected var _zoomControl: SeekBar? = null
    fun setZoomControl(zoomControl: SeekBar?) {
        _zoomControl = zoomControl
    }

    protected fun enableZoomControls(params: Camera.Parameters) {
        val settings: SharedPreferences = getContext().getSharedPreferences("ZoomCtl", 0)

        // set zoom level to previously set level if available, otherwise maxZoom
        val maxZoom = params.maxZoom
        val currentZoom = settings.getInt("ZoomLvl", maxZoom)
        params.zoom = currentZoom
        if (_zoomControl == null) return
        _zoomControl!!.max = maxZoom
        _zoomControl!!.progress = currentZoom
        _zoomControl!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                val params: Camera.Parameters = mCamera.getParameters()
                params.zoom = progress
                mCamera.setParameters(params)
                if (settings != null) {
                    val editor = settings.edit()
                    editor.putInt("ZoomLvl", progress)
                    editor.apply()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    // zoom in and enable flash
    protected override fun initializeCamera(width: Int, height: Int): Boolean {
        val ret: Boolean = super.initializeCamera(width, height)
        val params: Camera.Parameters = mCamera.getParameters()
        val FocusModes = params.supportedFocusModes
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        } else if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        val FlashModes = params.supportedFlashModes
        if (FlashModes != null && FlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        }
        if (params.isZoomSupported) enableZoomControls(params)
        mCamera.setParameters(params)
        return ret
    }
}