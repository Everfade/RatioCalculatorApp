package com.example.ratiocalculator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.activity_analyzer.*
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import java.io.File
import java.net.URI

class AnalyzerActivity : AppCompatActivity() {

    lateinit var imageUri:Uri
    lateinit var analyzer: ImageAnalyzer
    @RequiresApi(Build.VERSION_CODES.N)
    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyzer)
        //imgData = intent.getByteArrayExtra("imageData")!!
        imageUri = Uri.parse( intent.getStringExtra("imagePath"))
        val image= File(imageUri.toString().replace("file:","")).inputStream()
        var bitmap= BitmapFactory.decodeStream(image).rotate(90f)

        analyzer=  ImageAnalyzer(bitmap)
        var view= findViewById<ImageView>(R.id.analyzed_image_View)
        view.setImageBitmap(analyzer.analyze())
        view.visibility=View.VISIBLE
        var textView = findViewById<TextView>(R.id.textViewSeekBar)
        MobileAds.initialize(this@AnalyzerActivity)
        val adRequest= AdRequest.Builder().build()

        adView3.loadAd(adRequest)
        seekBar2.setOnSeekBarChangeListener( object: OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                analyzer.threshhold= seekBar2.progress.toDouble()
                textView.setText("Threshold: ${seekBar2.progress}")
                view.setImageResource(0)

                    view.setImageBitmap(analyzer.analyze())

            }
            override fun onStartTrackingTouch(  seekBar:SeekBar) {
            }
            override fun onStopTrackingTouch(  seekBar:SeekBar) {
                analyzer.threshhold= seekBar2.progress.toDouble()
                textView.setText("Threshold: ${seekBar2.progress}")
                view.setImageResource(0)

                view.setImageBitmap(analyzer.analyze())
            }
        });
        showProcessedImageToggle.setOnCheckedChangeListener{_,isChecked-> analyzer.returnProcessedImage=isChecked
            view.setImageBitmap(analyzer.analyze())}



    }
    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}




