package com.github.gary.opencv.kotlin.resistor.view

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceView
import android.view.View
import android.widget.SeekBar
import com.github.gary.opencv.kotlin.resistor.R
import com.github.gary.opencv.kotlin.resistor.model.ResistorCameraView
import com.github.gary.opencv.kotlin.resistor.model.ResistorImageProcessor
//import kotlinx.android.synthetic.main.activity_main.toolbar
import org.opencv.core.Mat

import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader



class MainActivity : AppCompatActivity(), CvCameraViewListener2 {
    companion object {
        init {
            OpenCVLoader.initDebug()
        }
    }

    private var _resistorCameraView: ResistorCameraView? = null
    private var _resistorProcessor: ResistorImageProcessor? = null
    private val _loaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> _resistorCameraView?.enableView()
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(android.Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this, permissions,0)
        setContentView(R.layout.activity_main)
        _resistorCameraView = findViewById<View>(R.id.ResistorCameraView) as ResistorCameraView
        _resistorCameraView!!.setVisibility(SurfaceView.VISIBLE)
        _resistorCameraView!!.setZoomControl(findViewById<View>(R.id.CameraZoomControls) as SeekBar)
        _resistorCameraView!!.setCvCameraViewListener(this)
        _resistorProcessor = ResistorImageProcessor()
        val settings = getPreferences(0)
        if (!settings.getBoolean("shownInstructions", false)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title)
                    .setNeutralButton(R.string.dialog_ok,
                            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                    .create().show()
            val editor = settings.edit()
            editor.putBoolean("shownInstructions", true)
            editor.apply()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (_resistorCameraView != null) _resistorCameraView!!.disableView()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (_resistorCameraView != null) _resistorCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}
    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        return _resistorProcessor!!.processFrame(inputFrame)
    }

    public override fun onResume() {
        super.onResume()
        _loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }
}
