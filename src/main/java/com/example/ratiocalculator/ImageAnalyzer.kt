package com.example.ratiocalculator

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import java.lang.Math.round
import java.lang.Math.sqrt
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


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


class ImageAnalyzer(imageBitmap: Bitmap) {
    // var data: ByteArray=[]
    var imageBitmap: Bitmap
    var proccessedImages:LinkedList<Bitmap> = LinkedList<Bitmap>()
    var orginialImages:LinkedList<Bitmap> = LinkedList<Bitmap>()
    var returnProcessedImage:Boolean=false;
      var threshhold = 105.0
    init {
        this.imageBitmap = imageBitmap;

    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun  computeAll(){
        orginialImages= LinkedList<Bitmap>()
        proccessedImages= LinkedList<Bitmap>()
        var th= 1;
        returnProcessedImage=false
        for(th in (1..255)){
            this.threshhold=th.toDouble()
            orginialImages.add(analyze())
        }
        th=1
        returnProcessedImage=true
        for(th in (1..255)){
            this.threshhold=th.toDouble()
            proccessedImages.add(analyze())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun analyze(): Bitmap {
        if (!OpenCVLoader.initDebug()) return imageBitmap

        var originalImage = imageBitmap
        var originalImageMat = Mat(originalImage.width.toInt(), originalImage.height.toInt(), CV_8UC1);
        Utils.bitmapToMat(originalImage, originalImageMat)

        var imageGray = Mat(originalImageMat.width(), originalImageMat.height(), CvType.CV_8UC1)
        cvtColor(originalImageMat, imageGray, Imgproc.COLOR_RGB2GRAY)
        val kernel=  getStructuringElement(Imgproc.MORPH_RECT, Size(3.0,3.0))
        equalizeHist(imageGray, imageGray)
        var currentImage2:Bitmap= originalImage;
        Imgproc.GaussianBlur(imageGray, imageGray, Size(3.0, 3.0), 0.0)
      //  dilate(imageGray,imageGray,kernel,Point(-1.0,-1.0))
        Imgproc.threshold(imageGray, imageGray, threshhold, 255.0, Imgproc.THRESH_BINARY);
        dilate(imageGray,imageGray,kernel,Point(-1.0,-1.0))
        Imgproc.pyrDown(imageGray,imageGray,Size((imageGray.width()/2).toDouble(),
            (imageGray.height()/2).toDouble()))
        pyrUp(imageGray,imageGray,Size((imageGray.width()*2).toDouble(),
            (imageGray.height()*2).toDouble()))
     // adaptiveThreshold(imageGray,imageGray, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C,
         //   THRESH_BINARY_INV,31, 11.0)

        morphologyEx(imageGray,imageGray,Imgproc.MORPH_CLOSE,kernel)

        Canny(imageGray, imageGray, threshhold, 255.0)
       // dilate(imageGray,imageGray,kernel,Point(-1.0,-1.0))
        //erode(imageGray,imageGray,kernel,Point(-1.0,-1.0))
        dilate(imageGray,imageGray,kernel,Point(-1.0,-1.0))
      //  Imgproc.GaussianBlur(imageGray, imageGray, Size(5.0, 5.0), 0.0)

      //
        var contours = ArrayList<MatOfPoint>()
        var matOfPoint2f = MatOfPoint2f()
        var approxCurve = MatOfPoint2f()
 //try trheshold instead o daptive
        findContours(imageGray, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        for (contour in contours) {
            var rect = Imgproc.boundingRect(contour)
            var contourArea = Imgproc.contourArea(contour)
            matOfPoint2f.fromList(contour.toList())
            Imgproc.approxPolyDP(
                matOfPoint2f, approxCurve, Imgproc.arcLength(
                    matOfPoint2f, true
                ) * 0.03, true
            )
            val total = approxCurve.total()
            //       Imgproc.drawContours(originalImageMat,contours,-1,Scalar(0.0,0.0,255.0),3)

            if (total >= 4L && total <= 6) {
                var cos = ArrayList<Double>()
                var points = approxCurve.toArray()
                for (j in 2..(total)) {
                    cos.add(
                        angle(
                            points[((j % total).toInt())],
                            points[(j - 2).toInt()],
                            points[(j - 1).toInt()]
                        )
                    )
                }
                cos.sort()
                val minCos = cos[0]
                val maxCos = cos[1]
                val isRect = total == 4L && minCos >= -0.1 && maxCos <= 0.3
                val width:Double=rect.width.toDouble()
                val height:Double = rect.height.toDouble()
                if (isRect && contourArea> 180)  {
                    var ratio=width/height
                    if(ratio <1){ ratio= height/width}

                    if(ratio-sqrt(2.0)>0.06 && ratio- sqrt(2.0)<0.06){
                        rectangle(originalImageMat, rect.tl(), rect.br(), Scalar(200.0, 55.0, 30.0, .8), 5)
                      putText(
                            originalImageMat, ("Ratio 1:√2" ),
                            Point(rect.x.toDouble(), rect.y.toDouble()
                            ), FONT_HERSHEY_DUPLEX , 4.0, Scalar(0.0, 0.0, 0.0), 3)

                    }
                    else{
                   rectangle(originalImageMat, rect.tl(), rect.br(), Scalar(200.0, 55.0, 30.0, .8), 5)
                        ratio= round(ratio*100).toDouble()/100
                      putText(originalImageMat, ("Ratio 1:$ratio" ),
                            Point(rect.x.toDouble(), rect.y.toDouble()
                            ), FONT_HERSHEY_DUPLEX ,4.0, Scalar(0.0, 0.0,), 3)

                }}
            }
        }

        var currentImage: Bitmap = Bitmap.createBitmap(
            originalImageMat.width(),
            originalImageMat.height(),
            originalImage.config
        )
        if(returnProcessedImage){
            Utils.matToBitmap(imageGray, currentImage)
        }
        else {
            Utils.matToBitmap(originalImageMat, currentImage)
        }
        return currentImage


    }

    fun angle(p1: Point, p2: Point, p0: Point): Double {
        var dx1 = p1.x - p0.x;
        var dy1 = p1.y - p0.y;
        var dx2 = p2.x - p0.x;
        var dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt(
            (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10
        );
    }

}