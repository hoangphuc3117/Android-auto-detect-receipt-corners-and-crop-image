package ppapps.cropreceiptdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v4.content.PermissionChecker
import android.util.Log
import ppapps.cropreceiptdemo.cropreceipt.CropReceiptActivity
import ppapps.cropreceiptdemo.reviewreceipt.ReviewReceiptActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        val KEY_RECEIPT_PATH = "RECEIPT_PATH"
        val IMAGE_PATH = Environment
                .getExternalStorageDirectory().path + "/scanSample"
    }

    val REQUEST_PERMISSION_CAMERA = 101
    val REQUEST_RECEIPT_CAPTURE = 100
    val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 102
    val REQUEST_PICK_IMAGE = 103
    lateinit var mTvCaptureReceipt: TextView
    lateinit var mTvChooseGallery: TextView
    lateinit var mCurrentPhotoPath: String
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
    }

    private fun initViews() {
        mTvCaptureReceipt = findViewById(R.id.tv_capture_receipt)
        mTvChooseGallery = findViewById(R.id.tv_choose_gallery)

        mTvCaptureReceipt.setOnClickListener(this)
        mTvChooseGallery.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            mTvCaptureReceipt.id -> {
                checkCameraPermission()
            }
            mTvChooseGallery.id -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_IMAGE)
            }
        }
    }

    fun startScanCamera() {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val file = createImageFile()
        val isDirectoryCreated = file.parentFile.mkdirs()
        Log.d("", "openCamera: isDirectoryCreated: " + isDirectoryCreated)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val tempFileUri = FileProvider.getUriForFile(applicationContext,
                    "com.scanlibrary.provider", // As defined in Manifest
                    file)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri)
        } else {
            val tempFileUri = Uri.fromFile(file)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri)
        }
        startActivityForResult(cameraIntent, REQUEST_RECEIPT_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RECEIPT_CAPTURE && resultCode == Activity.RESULT_OK) {
            startCropActivity(fileUri!!)
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri = data!!.getData() as Uri
            startCropActivity(imageUri)
        }
    }

    private fun checkCameraPermission() {
        if (!(PermissionChecker.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            val permissions = arrayOf(Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CAMERA)
        } else {
            checkExternalPermission()
        }
    }

    private fun checkExternalPermission() {
        if (!(PermissionChecker.checkSelfPermission(baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
        } else {
            startScanCamera()
        }
    }

    fun startReviewReceiptActivity(receiptPath: Uri) {
        val intent = Intent(this, ReviewReceiptActivity::class.java)
        intent.putExtra(KEY_RECEIPT_PATH, receiptPath)
        startActivity(intent)

    }

    fun startCropActivity(receiptPath: Uri) {
        val intent = Intent(this, CropReceiptActivity::class.java)
        intent.putExtra(KEY_RECEIPT_PATH, receiptPath)
        startActivity(intent)
    }

    private fun createImageFile(): File {
        clearTempImages()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val file = File(IMAGE_PATH, "IMG_" + timeStamp +
                ".jpg")
        fileUri = Uri.fromFile(file)
        return file
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                val grantedExternal = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (grantedExternal) {
                    // process our code
                    startScanCamera()
                } else {
                }
            }
            REQUEST_PERMISSION_CAMERA -> {
                val grantedCamera = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (grantedCamera) {
                    checkExternalPermission()
                } else {

                }
            }
            else ->
                //                mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun clearTempImages() {
        try {
            val tempFolder = File(IMAGE_PATH)
            for (f in tempFolder.listFiles())
                f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
