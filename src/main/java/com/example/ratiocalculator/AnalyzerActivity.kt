package com.example.ratiocalculator

import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import org.opencv.android.OpenCVLoader
import java.io.File
import java.net.URI

class AnalyzerActivity : AppCompatActivity() {

    lateinit var imageUri:Uri
    lateinit var analyzer: ImageAnalyzer
    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyzer)
        //imgData = intent.getByteArrayExtra("imageData")!!
        imageUri = Uri.parse( intent.getStringExtra("imagePath"))
        val image= File(imageUri.toString().replace("file:","")).inputStream()
        var bitmap= BitmapFactory.decodeStream(image)

        analyzer=  ImageAnalyzer(bitmap)
        var view= findViewById<ImageView>(R.id.analyzed_image_View)
        view.setImageBitmap(analyzer.analyze())
        view.visibility=View.VISIBLE


    }
}