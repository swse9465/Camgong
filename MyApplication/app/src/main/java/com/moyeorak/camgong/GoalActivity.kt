package com.moyeorak.camgong

import android.app.AlertDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TimePicker
import com.moyeorak.camgong.models.DailyGoal
import com.moyeorak.camgong.util.TimeCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_goal.*

class GoalActivity : AppCompatActivity() {

    // Log의 TAG
    companion object {
        private const val TAG = "GoalActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "user doesn't exist")
        } else {
            val uid = user.uid
            val database = Firebase.database
            val today = TimeCalculator().today()
            val myRef = database.getReference("calendar/$uid/$today")
            myRef.child("/dailyGoal/goalTime").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val time = snapshot.getValue<Long>()
                    if (time == null) {
                        time_picker.hour = 0
                        time_picker.minute = 0
                    } else {
                        time_picker.hour = (time / (60*60 * 1000)).toInt()
                        time_picker.minute = (time % (60*60 * 1000)/(60 * 1000)).toInt()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
            // Set a click listener for set time button widget
            button_set.setOnClickListener {
                goalWrite(myRef, time_picker)
                CustomDialog(this)
                    .setMessage("목표를 저장했습니다")
                    .setPositiveButton("OK") { finish()
                    }.show()
            }
        }


        time_picker.setIs24HourView(true)
    }

    private fun goalWrite(myRef: DatabaseReference, timePicker: TimePicker) {
        // [START write_message]
        val destination = myRef.child("/dailyGoal")
        val time = String.format("%02d:%02d:00", timePicker.hour, timePicker.minute)
        val goalTime = TimeCalculator().stringToLong(time)
        val goal = DailyGoal(false, goalTime)
        destination.setValue(goal)
        // [END write_message]
    }
}