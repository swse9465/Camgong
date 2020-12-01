package com.moyeorak.camgong.tabviewpager

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.moyeorak.camgong.CustomDialog
import com.moyeorak.camgong.LoginActivity
import com.moyeorak.camgong.R
import com.moyeorak.camgong.TimerActivity
import com.moyeorak.camgong.models.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.layout_home.view.*
import java.util.*


class FragmentTabHome  : Fragment() {

    private var timer : Result = Result()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val username = FirebaseAuth.getInstance().currentUser
        val name = username?.displayName
        val view =inflater.inflate(R.layout.layout_home, container, false)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) +1
        val day = calendar.get(Calendar.DATE)
        view.homeDate.text = "$year.$month.$day"
        view.user.setText(name)

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
        val uid = FirebaseAuth.getInstance().uid
        val ref =FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/result")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.key.equals("result")) {
                    val result = snapshot.getValue(Result::class.java)
                    if (result != null) {
                        timer = Result(
                            result.focusStudyTime,
                            result.maxFocusStudyTime,
                            result.realStudyTime,
                            result.totalStudyTime
                        )
                        view.chronometer.base = SystemClock.elapsedRealtime() + timer.realStudyTime
                    }
                }

            }
        })
        view.btnStart.setOnClickListener {
            val intent = Intent(context, TimerActivity::class.java)
            /*
            intent.putParcelableArrayListExtra("focusStudyTime", ArrayList(timer.focusStudyTime))
            intent.putExtra("maxFocusStudyTime",timer.maxFocusStudyTime)
            intent.putExtra("realStudyTime",timer.realStudyTime)
            intent.putExtra("totalStudyTime",timer.totalStudyTime)*/
            context?.let { it1 ->
                CustomDialog(it1)
                    .setMessage("캠 스터디를 시작하시겠습니까?")
                    .setPositiveButton("OK") {
                        startActivity(intent)
                    }.setNegativeButton("CANCEL") {
                        null
                    }.show()
            }


        }

        view.btnMenu.setOnClickListener{
            view.drawer_layout.openDrawer(view.slide)
        }
        val firebaseAuth = FirebaseAuth.getInstance();

        view.logout.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            context?.let { it1 ->
                CustomDialog(it1)
                    .setMessage("로그아웃 하시겠습니까?")
                    .setPositiveButton("OK") {
                        FirebaseAuth.getInstance().signOut(); //로그아웃
                        startActivity(intent)
                        getActivity()?.finish()
                    }.setNegativeButton("CANCEL") {
                        null
                    }.show()
            }


        }

        view.delAcc.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            val UID =firebaseAuth!!.getCurrentUser()?.uid
            val database = FirebaseDatabase.getInstance().reference
            context?.let { it1 ->
                CustomDialog(it1)
                    .setMessage("탈퇴하시겠습니까?")
                    .setPositiveButton("OK") {
                        startActivity(intent)
                        getActivity()?.finish()
                        if (UID != null) {
                            FirebaseAuth.getInstance().signOut();
                            database.child("users").child(UID).removeValue()
                            firebaseAuth!!.getCurrentUser()?.delete() //회원탈퇴
                        }

                    }.setNegativeButton("CANCEL") {
                        null
                    }.show()
            }

        }

        return view
    }



}