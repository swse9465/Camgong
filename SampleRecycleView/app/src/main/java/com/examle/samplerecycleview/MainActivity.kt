package com.examle.samplerecycleview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    //구글 로그인 부분
    private val RC_SIGN_IN = 9001// Google Login result
    private var googleSigninClient: GoogleSignInClient? = null// Google Api Client
    private var firebaseAuth: FirebaseAuth? = null// Firebase Auth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        TextView1.setText("${user.displayName}")

        //구글 로그인 부분
        // [START config_signin]
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // [END config_signin]
        googleSigninClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance();
        googleLoginBtn.setOnClickListener {
            val signInIntent = googleSigninClient?.getSignInIntent()
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

//        FirebaseAuth.getInstance().signOut(); //로그아웃시키려면 signOut을 호출

    }

    // [START onActivityResult]//구글 로그인 부분
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Google 로그인 인텐트 응답
        if (requestCode === RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {

            }
        }
    }
    // [START firebaseAuthWithGoogle]//구글 로그인 부분
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) {
                // 성공여부
                if (it.isSuccessful) {
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                    val user = firebaseAuth?.currentUser
                    if (user != null) {
                        writeNewUser(user.displayName.toString(),user.email.toString(),user.photoUrl.toString(),user.uid.toString()) //유저 네임,이메일,프로필,유저아이디 저장
//                        writeNewUser("sample02","sample02@gmail.com","sample02URL","sample02UID") //유저 네임,이메일,프로필,유저아이디 저장
                    }
                    val intent = Intent(this, `MainpageActivity`::class.java) //페이지 이동 부분
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // [END firebaseAuthWithGoogle]

//    private lateinit var database: DatabaseReference
    private fun writeNewUser(username: String, useremail: String, userphotourl: String?, useruid: String?) {

        val database = FirebaseDatabase.getInstance().reference //데이터를 읽거나 쓰기 위한 DatabaseReference의 인스턴스
        val user = User(username, useremail,userphotourl,useruid)
        if (useruid != null) {
            database.child("users").child(useruid).setValue(user)
        }

    }

}

data class User(
    var username: String? = "",
    var useremail: String? = "",
    var userphotourl: String? = "",
    var useruid: String? = ""
)