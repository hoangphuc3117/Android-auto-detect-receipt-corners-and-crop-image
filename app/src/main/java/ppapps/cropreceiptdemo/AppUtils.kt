package ppapps.cropreceiptdemo

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by phuchoang on 11/5/17.
 */
class AppUtils {
    companion object {
        @Throws(IOException::class)
        fun getBitmap(selectedimg: Uri?, activity: Activity): Bitmap {
            val options = BitmapFactory.Options()
            options.inSampleSize = 3
            var fileDescriptor: AssetFileDescriptor? = null
            fileDescriptor = activity.contentResolver.openAssetFileDescriptor(selectedimg!!, "r")
            return BitmapFactory.decodeFileDescriptor(
                    fileDescriptor!!.fileDescriptor, null, options)
        }

        fun saveBitmapToFile(bm: Bitmap): String {
            val file = getOutputMediaFile()
            try {
                file!!.createNewFile()
                val fos = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
            } catch (e: Exception) {
                Log.e("Error", e.message)
                //            e.printStackTrace();
            }

            return file!!.getAbsolutePath()
        }

        fun getOutputMediaFile(): File? {
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp")
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.
            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
            // Create a media file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val mediaFile: File
            mediaFile = File(mediaStorageDir.path + File.separator +
                    "IMG_" + timeStamp + ".jpg")
            return mediaFile
        }
    }


}