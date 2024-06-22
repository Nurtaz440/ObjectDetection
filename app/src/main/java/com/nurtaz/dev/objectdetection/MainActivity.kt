package com.nurtaz.dev.objectdetection

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.nurtaz.dev.objectdetection.databinding.ActivityMainBinding
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private  var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!
    private val CAMERA_PERMISSION_CODE = 123
    private val READ_STORAGE_PERMISSION_CODE = 113
    private val WRITE_EXTERNAL_STORAGE_CODE = 133


    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var inputImage: InputImage
    private lateinit var imageLabler : ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageLabler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object :ActivityResultCallback<ActivityResult?>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result!!.data
                    try {
                        val photo = data!!.extras!!.get("data") as Bitmap
                        binding.ivImage.setImageBitmap(photo)
                        inputImage = InputImage.fromBitmap(photo,0)
                        proccessImage()
                    }catch (e:Exception){
                        Log.d("TAg" ,"onActivityResult: ${e.message}")
                    }
                }

            }
        )
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object :ActivityResultCallback<ActivityResult?>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result!!.data
                    try {
                        inputImage = InputImage.fromFilePath(this@MainActivity, data!!.data!!)
                        binding.ivImage.setImageURI(data!!.data)
                        proccessImage()
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }

            }
        )

        binding.btnChoosePicture.setOnClickListener {
            val options = arrayOf("camera", "gallery")
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Pick a option")

            builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                if (which == 0) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                } else {
                val storageIntent = Intent()
                    storageIntent.setType("image/*")
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storageIntent)

                }
            })
            builder.show()

        }

    }

    private fun proccessImage() {
        imageLabler.process(inputImage)
            .addOnSuccessListener {
                var result = ""
                for (label in it) {
                  //  val text = label.text
//                    val confidence = label.confidence
//                    val index = label.index
                    result = result + "\n"+ label.text
                }
                binding.textView.text = result
            }
            .addOnFailureListener {
                Log.d("TAg" ,"proccessImage: ${it.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        checkSelfPermission(android.Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
    }

    private fun checkSelfPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    READ_STORAGE_PERMISSION_CODE
                )
            } else {
                Toast.makeText(this@MainActivity, "Camera Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkSelfPermission(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE_CODE
                )
            } else {
                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}