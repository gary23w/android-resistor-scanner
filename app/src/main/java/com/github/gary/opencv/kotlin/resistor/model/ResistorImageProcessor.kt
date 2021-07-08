package com.github.gary.opencv.kotlin.resistor.model

/**
 * Created by parth on 05/05/15.
 */
import android.util.SparseIntArray
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.line
import org.opencv.imgproc.Imgproc.putText
import org.opencv.imgproc.Moments
import java.util.*


class ResistorImageProcessor {
    private val _locationValues = SparseIntArray(4)
    fun processFrame(frame: CvCameraViewFrame): Mat {
        val imageMat: Mat = frame.rgba()
        val cols: Int = imageMat.cols()
        val rows: Int = imageMat.rows()
        val subMat: Mat = imageMat.submat(rows / 2, rows / 2 + 30, cols / 2 - 50, cols / 2 + 50)
        val filteredMat = Mat()
        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGBA2BGR)
        Imgproc.bilateralFilter(subMat, filteredMat, 5, 80.0, 80.0)
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2HSV)
        findLocations(filteredMat)
        if (_locationValues.size() >= 3) {
            // recover the resistor value by iterating through the centroid locations
            // in an ascending manner and using their associated colour values
            val k_tens = _locationValues.keyAt(0)
            val k_units = _locationValues.keyAt(1)
            val k_power = _locationValues.keyAt(2)
            var value = 10 * _locationValues[k_tens] + _locationValues[k_units]
            value *= Math.pow(10.0, _locationValues[k_power].toDouble()).toInt()
            val valueStr: String
            valueStr =
                if (value >= 1e3 && value < 1e6) (value / 1e3).toString() + " KOhm" else if (value >= 1e6) (value / 1e6).toString() + " MOhm" else "$value Ohm"
            if (value <= 1e9) putText(
                imageMat, valueStr, Point(10.0, 100.0), Core.FONT_HERSHEY_COMPLEX,
                    2.0, Scalar(255.0, 0.0, 0.0, 255.0), 3
            )
        }
        val color = Scalar(255.0, 0.0, 0.0, 255.0)
        line(
            imageMat,
            Point((cols / 2 - 50).toDouble(), (rows / 2).toDouble()),
            Point((cols / 2 + 50).toDouble(), (rows / 2).toDouble()),
            color,
            2
        )
        return imageMat
    }

    // find contours of colour bands and the x-coords of their centroids
    private fun findLocations(searchMat: Mat) {
        _locationValues.clear()
        val areas = SparseIntArray(4)
        for (i in 0 until NUM_CODES) {
            val mask = Mat()
            val contours: List<MatOfPoint> = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            if (i == 2) {
                // combine the two ranges for red
                Core.inRange(
                    searchMat,
                    LOWER_RED1,
                    UPPER_RED1,
                    mask
                )
                val rmask2 = Mat()
                Core.inRange(
                    searchMat,
                    LOWER_RED2,
                    UPPER_RED2,
                    rmask2
                )
                Core.bitwise_or(mask, rmask2, mask)
            } else Core.inRange(
                searchMat,
                COLOR_BOUNDS[i][0],
                COLOR_BOUNDS[i][1],
                mask
            )
            Imgproc.findContours(
                mask,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            for (contIdx in contours.indices) {
                var area: Int
                area = Imgproc.contourArea(contours[contIdx]).toInt()
                if (area > 20){
                    val M: Moments = Imgproc.moments(contours[contIdx])
                    val cx = (M.get_m10().toInt() / M.get_m00().toInt())

                    // if a colour band is split into multiple contours
                    // we take the largest and consider only its centroid
                    var shouldStoreLocation = true
                    for (locIdx in 0 until _locationValues.size()) {
                        if (Math.abs(_locationValues.keyAt(locIdx) - cx) < 10) {
                            if (areas[_locationValues.keyAt(locIdx)] > area) {
                                shouldStoreLocation = false
                                break
                            } else {
                                _locationValues.delete(_locationValues.keyAt(locIdx))
                                areas.delete(_locationValues.keyAt(locIdx))
                            }
                        }
                    }
                    if (shouldStoreLocation) {
                        areas.put(cx, area)
                        _locationValues.put(cx, i)
                    }
                }
            }
        }
    }

    companion object {
        private const val NUM_CODES = 10

        // HSV colour bounds
        private val COLOR_BOUNDS: Array<Array<Scalar>> =
            arrayOf<Array<Scalar>>(
                arrayOf<Scalar>(Scalar(0.0, 0.0, 0.0), Scalar(180.0, 250.0, 50.0)),
                arrayOf<Scalar>(Scalar(0.0, 90.0, 10.0), Scalar(15.0, 250.0, 100.0)),
                arrayOf<Scalar>(Scalar(0.0, 0.0, 0.0), Scalar(0.0, 0.0, 0.0)),
                arrayOf<Scalar>(Scalar(4.0, 100.0, 100.0), Scalar(9.0, 250.0, 150.0)),
                arrayOf<Scalar>(Scalar(20.0, 130.0, 100.0), Scalar(30.0, 250.0, 160.0)),
                arrayOf<Scalar>(Scalar(45.0, 50.0, 60.0), Scalar(72.0, 250.0, 150.0)),
                arrayOf<Scalar>(Scalar(80.0, 50.0, 50.0), Scalar(106.0, 250.0, 150.0)),
                arrayOf<Scalar>(Scalar(130.0, 40.0, 50.0), Scalar(155.0, 250.0, 150.0)),
                arrayOf<Scalar>(Scalar(0.0, 0.0, 50.0), Scalar(180.0, 50.0, 80.0)),
                arrayOf<Scalar>(Scalar(0.0, 0.0, 90.0), Scalar(180.0, 15.0, 140.0))
            )

        // red wraps around in HSV, so we need two ranges
        private val LOWER_RED1: Scalar = Scalar(0.0, 65.0, 100.0)
        private val UPPER_RED1: Scalar = Scalar(2.0, 250.0, 150.0)
        private val LOWER_RED2: Scalar = Scalar(171.0, 65.0, 50.0)
        private val UPPER_RED2: Scalar = Scalar(180.0, 250.0, 150.0)
    }
}