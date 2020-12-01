package com.moyeorak.camgong

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Chronometer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.moyeorak.camgong.models.*
import com.moyeorak.camgong.timecamera.CameraSourcePreview
import com.moyeorak.camgong.timecamera.GraphicOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.FaceDetector.Builder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.android.synthetic.main.layout_home.view.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log


class TimerActivity : AppCompatActivity() {
    private val TAG = "FaceTracker"

    private var mCameraSource: CameraSource? = null

    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay? = null

    private val RC_HANDLE_GMS = 9001
    // permission request codes need to be < 256
    private val RC_HANDLE_CAMERA_PERM = 2
    private var noStudyTime: Long = 0
    private var lasttime: Long = 0
    private var flag: Boolean = false
    private var noFlag: Boolean = true
    private var timer : Result = Result()
    private var calendar: Calendar = Calendar.getInstance()
    private var study1 :Study = Study()
    private var realStudy1 :RealStudy = RealStudy()
    private var studies1 : ArrayList<Study> = arrayListOf()
    private var starthour : Int = 0
    private var startminute : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 상태바 숨기기
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // 화면 켜짐 유지
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_timer)
        calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) +1
        val day = calendar.get(Calendar.DATE)
        var date = "$year"
        if(month<10)
        {
            date+="0$month"
        }else
        {
            date+="$month"
        }
        if(day<10)
        {
            date+="0$day"
        }else
        {
            date+="$day"
        }
        // result 내용들 가지고 오기
        var intent = getIntent()
        val uid = FirebaseAuth.getInstance().uid
        val ref =FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/result")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.key.equals("result")) {
                    val result = snapshot.getValue(Result::class.java)
                    if(result!=null)
                    {
                        timer = Result(result.focusStudyTime,result.maxFocusStudyTime,result.realStudyTime,result.totalStudyTime)
                        chronometer.base=SystemClock.elapsedRealtime()+timer.realStudyTime
                        totalStudy?.base = SystemClock.elapsedRealtime()+timer.totalStudyTime
                        lasttime = timer.realStudyTime
                    }
                }

            }
        })
        //Studies 내용들 가지고 오기
        val ref2 =FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/studies")
        ref2.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.key.equals("studies")) {
                    val tmp = snapshot.getValue()
                    if(tmp!=null)
                    {
                        studies1 = snapshot.getValue<ArrayList<Study>>()!!
                    }
//                    for(p0 in snapshot.children)
//                    {
//                        val p1 = p0.getValue(Study::class.java)
//                        if(p1 != null)
//                        {
//                            studies1.studies.add(p1)
//                        }
//                    }
                }

            }
        })
        chronometer.base=SystemClock.elapsedRealtime()+timer.realStudyTime
        totalStudy?.base = SystemClock.elapsedRealtime()+timer.totalStudyTime
        focusStudy.base =SystemClock.elapsedRealtime()+0
        lasttime = timer.realStudyTime
        totalStudy?.start()
        starthour = calendar.get(Calendar.HOUR_OF_DAY)
        startminute = calendar.get(Calendar.MINUTE)
        study1.startTime=String.format("%02d:%02d",starthour,startminute)
        realStudy1.realStudyStartTime =String.format("%02d:%02d",starthour,startminute)
        mPreview = findViewById<View>(R.id.preview) as CameraSourcePreview
        mGraphicOverlay = findViewById<View>(R.id.faceOverlay) as GraphicOverlay

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }

        btnPause.setOnClickListener {
            storeDB()
            CustomDialog(this)
                .setMessage("기록되었습니다")
                .setPositiveButton("나가기") { finish()
                }.setNegativeButton("계속하기") {null
                }.show()

        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                storeDB()
                CustomDialog(this)
                    .setMessage("기록되었습니다")
                    .setPositiveButton("나가기") { finish()
                    }.setNegativeButton("계속하기") {null
                    }.show()


            }
        }
        return true
    }
    private fun storeDB(){
        val intent = Intent(this, TimerActivity::class.java)
        chronometer.stop();
        calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) +1
        val day = calendar.get(Calendar.DATE)
        var date = String.format("%d%02d%02d",year,month,day)
        Log.d("날짜",""+date)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val uid = FirebaseAuth.getInstance().uid
        Log.d("타이머", "" + uid)
        chronometer.stop()
        totalStudy?.stop()
        timer.realStudyTime = chronometer.base-SystemClock.elapsedRealtime()
        timer.totalStudyTime = totalStudy.base - SystemClock.elapsedRealtime()
        if(!flag)
        {
            timer.realStudyTime = lasttime
        }
        var ref =FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/result")
        ref.setValue(timer)

        study1.endTime=String.format("%02d:%02d",hour,minute)

        if(flag)
        {
            realStudy1.realStudyEndTime = String.format("%02d:%02d",hour,minute)
            val tmp = RealStudy(realStudy1.realStudyStartTime,realStudy1.realStudyEndTime)
            val fstudyTime = focusStudy.base-SystemClock.elapsedRealtime()
            focusStudy.stop()
            if(timer.maxFocusStudyTime > fstudyTime)
            {
                timer.maxFocusStudyTime = fstudyTime
            }
            if(fstudyTime<(-300000))
            {
                study1.realStudy?.add(tmp)
            }
        }

        studies1.add(study1)
        ref =FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/studies")
        ref.setValue(studies1)

    }
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
        val thisActivity: Activity = this
        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(
                thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }
        Snackbar.make(
            mGraphicOverlay!!, R.string.permission_camera_rationale,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.ok, listener)
            .show()
    }


    private fun createCameraSource() {
        val context: Context = applicationContext

        val detector: FaceDetector = Builder(context)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setProminentFaceOnly(true)
            .build()

        detector.setProcessor(

            object : Detector.Processor<Face>{
                override fun release() {
                    //mGraphicOverlay?.clear()
                }

                override fun receiveDetections(detections: Detections<Face>) {
                    //mGraphicOverlay?.clear()
                    var faces = detections?.detectedItems


                    if(faces.size()>0)
                    {
                        if(faces.valueAt(0).isLeftEyeOpenProbability<0.1&&faces.valueAt(0).isRightEyeOpenProbability<0.1)
                        { // 자고 있나?
                            if(noFlag)
                            {
                                noStudy.base = SystemClock.elapsedRealtime()+0
                                noStudy.start()
                                noFlag = false
                            }else if((-60000)>(noStudy.base-SystemClock.elapsedRealtime()))
                            {
                                if(flag)
                                {
                                    lasttime = chronometer.base-SystemClock.elapsedRealtime()
                                    chronometer.stop()
                                    flag = false
                                    calendar = Calendar.getInstance()
                                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                                    val minute = calendar.get(Calendar.MINUTE)
                                    realStudy1.realStudyEndTime =String.format("%02d:%02d",hour,minute)
                                    val tmp = RealStudy(realStudy1.realStudyStartTime,realStudy1.realStudyEndTime)
                                    val fstudyTime = focusStudy.base-SystemClock.elapsedRealtime()
                                    focusStudy.stop()
                                    if(timer.maxFocusStudyTime > fstudyTime)
                                    {
                                        timer.maxFocusStudyTime = fstudyTime
                                    }
                                    if(fstudyTime<(-300000))
                                    {
                                        study1.realStudy?.add(tmp)
                                    }
                                }
                            }
                        }
                        else if(!flag){
                            // 안 자고 있는데 그 전에 공부 안 하는 중이 카운트 됐었음
                            chronometer.base = SystemClock.elapsedRealtime()+lasttime
                            focusStudy.base =SystemClock.elapsedRealtime()+0
                            noStudy.base =SystemClock.elapsedRealtime()+0
                            chronometer.start()
                            focusStudy.start()
                            noStudy.stop()
                            flag = true
                            noFlag = true
                            calendar = Calendar.getInstance()
                            starthour = calendar.get(Calendar.HOUR_OF_DAY)
                            startminute = calendar.get(Calendar.MINUTE)
                            realStudy1.realStudyStartTime =String.format("%02d:%02d",starthour,startminute)

                        }else{
                            noStudy.base = SystemClock.elapsedRealtime()+0
                        }

                        //val graphic = FaceGraphic(mGraphicOverlay, faces.valueAt(0))
                        // mGraphicOverlay?.add(graphic)


                    }
                    else{
                        // 얼굴이 화면에 없음

                        if(noFlag)
                        {
                            noStudy.base = SystemClock.elapsedRealtime()+0
                            noStudy.start()
                            noFlag = false
                        }else if((-60000)>(noStudy.base-SystemClock.elapsedRealtime()))
                        {
                            if(flag)
                            {
                                lasttime = chronometer.base-SystemClock.elapsedRealtime()
                                chronometer.stop()
                                flag = false
                                calendar = Calendar.getInstance()
                                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                                val minute = calendar.get(Calendar.MINUTE)
                                realStudy1.realStudyEndTime =String.format("%02d:%02d",hour,minute)
                                val tmp = RealStudy(realStudy1.realStudyStartTime,realStudy1.realStudyEndTime)
                                study1.realStudy?.add(tmp)
                                val fstudyTime = focusStudy.base-SystemClock.elapsedRealtime()
                                focusStudy.stop()
                                if(timer.maxFocusStudyTime > fstudyTime)
                                {
                                    timer.maxFocusStudyTime = fstudyTime
                                }
                                if(fstudyTime<(-300000))
                                {
                                    study1.realStudy?.add(tmp)          ///////////////////////////////////////////////////////////////
                                }
                            }
                        }
                    }

                }
            }
        )
        if (!detector.isOperational()) {

            Log.w(TAG, "Face detector dependencies are not yet available.")
        }
        mCameraSource = CameraSource.Builder(context, detector)
            .setRequestedPreviewSize(640, 480)
            .setFacing(CameraSource.CAMERA_FACING_FRONT)
            .setRequestedFps(30.0f)
            .build()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        mPreview!!.stop()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mCameraSource != null) {
            mCameraSource!!.release()
        }
    }

    private fun startCameraSource() {

        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            applicationContext
        )
        if (code != ConnectionResult.SUCCESS) {
            val dlg: Dialog =
                GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        if (mCameraSource != null) {
            try {
                mPreview!!.start(mCameraSource, mGraphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            createCameraSource()
            return
        }
        Log.e(
            TAG, "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)"
        )
        val listener =
            DialogInterface.OnClickListener { dialog, id -> finish() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Face Tracker sample")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok, listener)
            .show()
    }
}