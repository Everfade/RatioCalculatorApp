package com.example.ratiocalculator

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import org.opencv.core.Scalar

import org.opencv.core.Core
import org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX
import java.io.File
import java.lang.Exception


private var ByteBuffer.data: ByteArray
    get() = data
    set(value) {
        data = value
    }

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()    // Rewind the buffer to zero
    var data = ByteArray(remaining())
    get(data)   // Copy the buffer into a byte array
    return data // Return the byte array
}


class ImageAnalyzer(  imageBitmap: Bitmap) {
    // var data: ByteArray=[]
    var imageBitmap:Bitmap

    init {
       this.imageBitmap=imageBitmap;

    }

      fun drawText(colorImage:Mat,ofs: Point, text: String) {
        Imgproc.putText(colorImage, text, ofs, FONT_HERSHEY_SIMPLEX , 0.5, Scalar(25.0, 255.0, 25.0))
    }


    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun analyze(): Bitmap? {
     OpenCVLoader.initDebug( )


            var path="/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk";
            Log.e("run",path)
            //System.load(File(Environment.getExternalStorageDirectory().toString() + path).absolutePath)




        var originalImage = imageBitmap
            //BitmapFactory.decodeByteArray(data,0,data.size)
        //btm to matrix
        var originalImageMat = Mat(originalImage.width.toInt(),originalImage.height.toInt(), CV_8UC1);
        Utils.bitmapToMat(originalImage,originalImageMat)
        //convert to grayscale
        var imageGray= Mat(originalImageMat.width(),originalImageMat.height(), CvType.CV_8UC1)
        Imgproc.cvtColor(originalImageMat,imageGray,Imgproc.COLOR_RGB2GRAY)
        Imgproc.equalizeHist(imageGray,imageGray)
        Imgproc.GaussianBlur(imageGray,imageGray, Size(5.0,5.0),0.0,0.0,Core.BORDER_DEFAULT)
        val threshhold=100.0
        Imgproc.Canny(imageGray,imageGray,threshhold,threshhold*3)
        Imgproc.threshold(imageGray, imageGray, 0.0, 255.0, Imgproc.THRESH_BINARY);

        var contours= ArrayList<MatOfPoint>()
        var matOfPoint2f= MatOfPoint2f()
        var approxCurve= MatOfPoint2f()

        Imgproc.findContours(imageGray,contours,Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE)
        for(contour in contours){
            var rect= Imgproc.boundingRect(contour)
            var contourArea=Imgproc.contourArea(contour)
            matOfPoint2f.fromList(contour.toList())
            Imgproc.approxPolyDP(matOfPoint2f,approxCurve,Imgproc.arcLength(
                matOfPoint2f,true)*0.02,true)
            val total= approxCurve.total()
            if(total>=4L && total <=6){
                var cos = ArrayList<Double>()
                var points=approxCurve.toArray()
                for(j in 2..(total)){
                    cos.add(angle(points[((j % total).toInt())]
                        , points[(j-2).toInt()],points[(j-1).toInt()]))
                }
                cos.sort()
                val minCos= cos[0]
                val maxCos= cos[1]
                val isRect= total==4L && minCos >= -0.1 && maxCos<=0.3
                if(isRect){
                    val ratio=Math.abs(rect.width/rect.height)
                    Log.e("run","ratio: "+ratio)

                    Imgproc.rectangle(originalImageMat, rect.tl(), rect.br(),   Scalar(
                        200.0, 55.0, 50.0, .8),4);

                }
            }


        }

        //View current result
        var currentImage:Bitmap=Bitmap.createBitmap(originalImageMat.width(),originalImageMat.height(),originalImage.config)
        Utils.matToBitmap(originalImageMat,currentImage)
            return currentImage
        /*

        var mYuv =   Mat(2400, 2400, CvType.CV_8UC1);
        mYuv.put(0, 0, data);
        var  mRgba =mYuv;
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
        var bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),
        Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        var result:Bitmap;
        try {
            result = findRectangle(bitmap);
            if (result != null) {
                return result;
            }
        } catch (  ex:Exception) {
            ex.printStackTrace();
        }

        val w = 1200
        val h =  1200

        var conf = Bitmap.Config.ARGB_8888; // see other conf types
      return Bitmap.createBitmap(w, h, conf)

*/
/*
        var bm = BitmapFactory.decodeByteArray(data, 0, data.size)
        val mat = Mat()
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        val grayBitmap = bm.copy(bm.config, true)
        Utils.matToBitmap(mat, grayBitmap)
        bm= findRectangle(grayBitmap)
        return bm
*/
return null
    }

    private fun  findRectangle(image: Bitmap): Bitmap {
        var tempor = Mat()
        var src = Mat()
        Utils.bitmapToMat(image, tempor)
        Imgproc.cvtColor(tempor, src, Imgproc.COLOR_BGR2RGB);
        var blurred = src.clone()
        Imgproc.medianBlur(src, blurred, 9);
        var gray0 = Mat(blurred.size(), CvType.CV_8U);
        var gray = Mat()
        var contours = ArrayList<MatOfPoint>();
        var blurredChannel = ArrayList<Mat>();
        blurredChannel.add(blurred);
        var gray0Channel = ArrayList<Mat>();
        gray0Channel.add(gray0);
        var approxCurve = MatOfPoint2f();

        var maxArea = 0;
        var maxId = -1;

        for (c in 0..3) {
     //    var ch = charArrayOf(  );
         //   Core.mixChannels(blurredChannel, gray0Channel,   MatOfInt(c,0));

            var thresholdLevel = 2;
            for (t in 1..thresholdLevel) {
            if (t == 0) {

                Imgproc.Canny(gray0, gray, 10.0, 20.0, 3, true); // true ?
                Imgproc.dilate(gray, gray, Mat()); // 1
                //
            } else {
                Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel.toDouble(),
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    (src.width() + src.height()) / 20, t.toDouble()
                );
            }

            Imgproc.findContours(gray, contours,   Mat(),
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            for (contour in contours) run {
                    var temp =   MatOfPoint2f (*contour.toArray());

                    var area = Imgproc.contourArea(contour);
                    approxCurve =  MatOfPoint2f ();
                    Imgproc.approxPolyDP(
                        temp, approxCurve,
                        Imgproc.arcLength(temp, true) * 0.02, true
                    );

                    if (approxCurve.total() == 4L && area >= maxArea) {
                        var maxCosine = 0;

                        var curves = approxCurve . toList ();
                        for ( j in 2..4) {

                            var cosine = Math. abs(angle(
                                curves.get(j % 4),
                                curves.get(j - 2), curves.get(j - 1)
                            ));
                            maxCosine = Math.max(maxCosine.toDouble(), cosine).toInt();
                        }

                        if (maxCosine < 0.3) {
                            maxArea = area.toInt();
                            maxId = contours.indexOf(contour);
                        }
                    }
                }
        }
        }
        if (maxId >= 0) {
            var rect = Imgproc.boundingRect(contours.get(maxId));

            Imgproc.rectangle(src, rect.tl(), rect.br(),   Scalar(
                255.0, 0.0, 0.0, .8), 4);
            var mDetectedWidth = rect.width;
            var mDetectedHeight = rect.height;

            Log.d(TAG, "Rectangle width :"+mDetectedWidth+ " Rectangle height :"+mDetectedHeight);
        }
      var  bmp:Bitmap = Bitmap.createBitmap(src.cols(), src.rows(),
            Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bmp);

        return bmp

    }

    fun     angle(  p1:Point,   p2:Point,  p0:Point): Double {
        var dx1 = p1.x - p0.x;
        var dy1 = p1.y - p0.y;
        var dx2 = p2.x - p0.x;
        var dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)/ Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

}