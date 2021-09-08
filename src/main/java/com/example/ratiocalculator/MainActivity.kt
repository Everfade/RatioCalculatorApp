package com.example.ratiocalculator
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.example.ratiocalculator.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_analyzer.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
   private lateinit var binding: ActivityMainBinding
    private var cameraAvailable = false
    private var imageCapture = ImageCapture.Builder().build()
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    lateinit var takenImage: Image;
    lateinit var imagePath:String;


    private val pickImage = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_RatioCalculator)
        super.onCreate(savedInstanceState)

      binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar4)
        toolbar4.setTitle("Ratio Calculator")
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
       // setupActionBarWithNavController(navController, appBarConfiguration)
        binding.fab.setOnClickListener { view ->
         //   Snackbar.make(view, "Starting Camera", Snackbar.LENGTH_LONG)
               // .setAction("Action", null).show()
            // Request camera permissions
            if (allPermissionsGranted() && isStoragePermissionGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
            select_from_galary_button.visibility=View.INVISIBLE;
            fab.visibility=View.INVISIBLE
            adView2.visibility=View.INVISIBLE

            camera_capture_button.setOnClickListener { takePhoto() }
        }
        analyze_image_button.setOnClickListener {
            val intent = Intent(this@MainActivity, AnalyzerActivity::class.java);
            intent.putExtra("imagePath",imagePath)
            startActivity(intent)
        }
        viewFinder.visibility= View.INVISIBLE
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        // IMAGE FROM GALLERY
        select_from_galary_button.setOnClickListener{
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        MobileAds.initialize(this@MainActivity)
        val adRequest= AdRequest.Builder().build()
        adView2.loadAd(adRequest)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== RESULT_OK && requestCode==pickImage){
            imagePath=data?.data.toString()
            var temp=getRealPathFromURI(imagePath)
            if(temp != null){
                imagePath=temp
                val image= File(imagePath.replace("file:","")).inputStream()
                var bitmap= BitmapFactory.decodeStream(image).rotate(90f)
                capturedImageView.setImageBitmap(bitmap)
                capturedImageView.visibility=View.VISIBLE
                analyze_image_button.visibility=View.VISIBLE;

            }
            else
            {
                // error loading Image
            }
        }
    }
      @RequiresApi(Build.VERSION_CODES.O)
      fun getRealPathFromURI(contentUri: String): String? {
        var proj={MediaStore.Images.Media.DATA};
        var cursor = contentResolver.query(Uri.parse( contentUri),null,null,null)
        var column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
          if (cursor != null) {
              cursor.moveToFirst()
        return column_index?.let { cursor.getString(it) };
          };
          return null
    }
    private fun takePhoto() {
        if (!cameraAvailable) return
        val imageCapture = imageCapture
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.GERMAN).format(System.currentTimeMillis()) + ".jpg")
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                takenImage= image.image!!
                val buffer= takenImage!!.planes[0].buffer
                val data =buffer.toByteArray()
                var bm= BitmapFactory.decodeByteArray(data,0, data.size).rotate(90f)
                var view= findViewById<ImageView>(R.id.capturedImageView)
                view.setImageBitmap(bm)
                view.visibility=View.VISIBLE
                fab.visibility=View.VISIBLE
                select_from_galary_button.visibility=View.VISIBLE
                camera_capture_button.visibility=View.INVISIBLE;
            }
        })
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                @SuppressLint("RestrictedApi")
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    imagePath=savedUri.toString()
                    val msg = "Photo capture succeeded: $savedUri"
                  //  Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    cameraExecutor.shutdownNow()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    cameraProvider.shutdown()
                    viewFinder.visibility = View.INVISIBLE
                    analyze_image_button.visibility=View.VISIBLE
                    viewFinder
                    cameraAvailable = false
                    adView2.visibility=View.VISIBLE

                }
            }
        )
    }
    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
     fun   saveImage(  bitmap:Bitmap,     name:String)       {
        var saved:Bitmap;
         val IMAGES_FOLDER_NAME="RatioAnalyzerImages"
        var fos: OutputStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var resolver =  getApplicationContext().getContentResolver()
            var contentValues =   ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + IMAGES_FOLDER_NAME);
            var imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = imageUri?.let { resolver.openOutputStream(it) }!!;
        } else {
            var imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + IMAGES_FOLDER_NAME;
            var file =   File(imagesDir);
            if (!file.exists()) {
                file.mkdir();
            }
            var image =   File(imagesDir, name + ".png");
            fos =   FileOutputStream(image)
        }
         bitmap.compress( Bitmap.CompressFormat.PNG, 100, fos);
         saved= bitmap;
        fos.flush();
        fos.close();
    }
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        var data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }
    private fun startCamera() {
        cameraAvailable = true
        camera_capture_button.visibility=View.VISIBLE;
        analyze_image_button.visibility=View.INVISIBLE
        var view= findViewById<ImageView>(R.id.capturedImageView)
        view.visibility=View.INVISIBLE
        viewFinder.visibility = View.VISIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
       fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this,arrayOf( Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

  //  override fun onSupportNavigateUp(): Boolean {
  //      val navController = findNavController(R.id.nav_host_fragment_content_main)
        //return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
   // }
}