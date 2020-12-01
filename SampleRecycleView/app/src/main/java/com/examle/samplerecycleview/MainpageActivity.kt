package com.examle.samplerecycleview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_mainpage.*

class `MainpageActivity` : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainpage)

        val user = FirebaseAuth.getInstance().currentUser //파이어베이스 사용자에 저장된 유저정보를 불러온다.
//        if (user != null) {
//            welcomeText.setText("${user.displayName} 님 환영합니다.")
//        }

        val database : FirebaseDatabase = FirebaseDatabase.getInstance()   //데이터 베이스에 저장된 users데이터 읽는 방법
        val userRef : DatabaseReference = database.getReference("users").child("${user?.uid}").child("username")
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot?.value
                welcomeText.setText("${value} 님 환영합니다.")
            }

            override fun onCancelled(error: DatabaseError) {
                println("Failed to read value.")
            }
        })


        logout.setOnClickListener{
            FirebaseAuth.getInstance().signOut(); //구글 로그아웃
            val intent = Intent(this, `MainActivity`::class.java) //페이지 이동 부분
            startActivity(intent)
        }


    }


}