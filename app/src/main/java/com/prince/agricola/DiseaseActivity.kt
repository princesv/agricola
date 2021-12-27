package com.prince.agricola

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import kotlinx.android.synthetic.main.activity_disease.*
import java.io.IOException
import java.util.jar.Manifest

class DiseaseActivity : AppCompatActivity() {
    private lateinit var mCatagorization:Catagorization
    private lateinit var mBitmap: Bitmap
    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2
    private val cameraRequestCode=5
    private val mInputSize=224
    private val mModelPath = "plant_disease_model.tflite"
    private val mLabelPath="plant_labels.txt"
    private val mSamplePath = "automn.jpg"
    var mDiseaseName="";
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease)
        when{
            ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_DENIED->{
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),cameraRequestCode)
            }
        }
        mCatagorization= Catagorization(assets,mModelPath,mLabelPath,mInputSize)
        resources.assets.open(mSamplePath).use {
            mBitmap=BitmapFactory.decodeStream(it)
            mBitmap=Bitmap.createScaledBitmap(mBitmap,mInputSize,mInputSize,true)
            val mImageView = findViewById<ImageView>(R.id.mImageViewPlant)
            mImageView.setImageBitmap(mBitmap)
        }
        val mCamera=findViewById<Button>(R.id.mCamera)
        mCamera.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent,mCameraRequestCode)
        }
        val mGallery=findViewById<Button>(R.id.mGallery)
        mGallery.setOnClickListener {
            val callGalleryIntent=Intent(Intent.ACTION_PICK)
            callGalleryIntent.type="image/*"
            startActivityForResult(callGalleryIntent,mGalleryRequestCode)
        }
        val mSearchRemedy=findViewById<Button>(R.id.mSearchRemedy)
        mSearchRemedy.setOnClickListener {
            val searchRemedyIntent=Intent(Intent.ACTION_VIEW)
            searchRemedyIntent.setData(Uri.parse("https://www.google.com/search?q="+mDiseaseName+" remedy"))
            startActivity(searchRemedyIntent)
        }
    }
   
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==mCameraRequestCode){
            if(resultCode== Activity.RESULT_OK&&data!=null){
                mBitmap=data.extras!!.get("data") as Bitmap
               // mBitmap=scaleImage(mBitmap)
                val mImageView = findViewById<ImageView>(R.id.mImageViewPlant)
                mImageView.setImageBitmap(mBitmap)
                val mText=findViewById<TextView>(R.id.mResult)
                mText.text = "Your photo image is set now"
            }else{
                Toast.makeText(this,"Camera cancel..",Toast.LENGTH_LONG).show()
            }
        }else if(requestCode==mGalleryRequestCode){
            if(data!=null){
                val uri=data.data
                try {
                    mBitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                }catch (e: IOException){
                    e.printStackTrace()
                }
              //  mBitmap=scaleImage(mBitmap)
                val mImageView = findViewById<ImageView>(R.id.mImageViewPlant)
                mImageView.setImageBitmap(mBitmap)
            }
        }
        val progDialog=ProgressDialog(this@DiseaseActivity)
        progDialog.setTitle("Please wait")
        progDialog.setMessage("Detecting plant disease...")
        progDialog.show()
        val handler=Handler()
        handler.postDelayed(Runnable {
            progDialog.dismiss()
            val mResultBox=findViewById<CardView>(R.id.mResultBox)
            mResultBox.visibility=View.VISIBLE
            val results=mCatagorization.recognizeImage(mBitmap).firstOrNull()
            val mResult=findViewById<TextView>(R.id.mResult)
            mResult.text="Plant disease detected:"+results?.title+"\n Confidence:"+results?.confidence
            mDiseaseName= results?.title.toString()
        },2000)
    }
    private fun scaleImage(bitmap: Bitmap): Bitmap{
        val originalWidth=bitmap!!.width
        val originalHeight=bitmap.height
        val scaleWidth=mInputSize.toFloat()
        val scaleHeight=mInputSize.toFloat()
        val matrix=Matrix()
        matrix.postScale(scaleWidth,scaleHeight)
        return Bitmap.createBitmap(bitmap,0,0,originalWidth,originalHeight,matrix,true)
    }
}
