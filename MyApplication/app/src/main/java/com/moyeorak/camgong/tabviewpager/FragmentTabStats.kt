package com.moyeorak.camgong.tabviewpager

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.moyeorak.camgong.GoalActivity
import com.moyeorak.camgong.R
import com.moyeorak.camgong.models.*
import com.moyeorak.camgong.util.TimeCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_stats.*
import kotlinx.android.synthetic.main.layout_stats.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


// DATE class를 사용하기 위한 API제한
class FragmentTabStats : Fragment() {
    // database에 연결
    private lateinit var database: DatabaseReference

    // Log의 TAG
    private val TAG = "FragmentTabStats"
    var calendar: Calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val date = calendar.get(Calendar.DATE)
    val today = TimeCalculator().today()
    var resultValueEventListener: ValueEventListener? = null
    var dailyGoalValueEventListener: ValueEventListener? = null
    var studiesValueEventListener: ValueEventListener? = null


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_stats, container, false)
        // user의 uid를 확인하기 위해 우선 user가 존재하는지 확인
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "user doesn't exist")
        } else {
            val uid = user.uid
            val database = Firebase.database
            updateData(database, uid)
            view.calendarText.text = "$year.$month.$date"

            val tvDate = view.calendarText

            val myDatePicker =
                OnDateSetListener { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateLabel()
                    updateData(database, uid)
                }

            tvDate.setOnClickListener {
                context?.let { it1 ->
                    DatePickerDialog(
                        it1, R.style.DatePickerTheme,
                        myDatePicker,
                        calendar[Calendar.YEAR],
                        calendar[Calendar.MONTH],
                        calendar[Calendar.DAY_OF_MONTH]
                    ).show()
                }
            }
        }

        // 테이블 롱클릭 -> 버튼 클릭 이벤트로 교체
        view.btn_setting_goal.setOnClickListener {
            startActivity(Intent(this.activity, GoalActivity::class.java))
        }

        return view
    }

    private fun updateLabel() {
        val myFormat = "yyyy.MM.dd"
        val sdf = SimpleDateFormat(myFormat, Locale.KOREA)
        calendarText.text = sdf.format(calendar.time)
    }

    private fun updateData(database: FirebaseDatabase, uid: String) {
        val myFormat = "yyyyMMdd"
        val sdf = SimpleDateFormat(myFormat, Locale.KOREA)
        val day = sdf.format(calendar.time)
        val myRef = database.getReference("calendar/$uid/$day")
        var dailyReal = 0L
        var dailyGoalTime = 0L
        var dailyGoalStatus: Boolean

        if (resultValueEventListener != null) myRef.removeEventListener(resultValueEventListener!!)
        if (dailyGoalValueEventListener != null) myRef.removeEventListener(
            dailyGoalValueEventListener!!
        )
        if (studiesValueEventListener != null) myRef.removeEventListener(studiesValueEventListener!!)
        if (btn_setting_goal != null && day == today ) btn_setting_goal.visibility = View.VISIBLE
        else if(btn_setting_goal != null && day != today) btn_setting_goal.visibility = View.INVISIBLE
        // [START read_message]

        resultValueEventListener =
            myRef.child("/result").addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue<Result>()
                    if (value == null) {
                        Log.d(TAG, "Result가 없음")
                        timeText.text = "00:00:00"
                        realTime.text = "00:00:00"
//                        recommendTime.text = "00:00:00"
                        maxFocusTime.text = "00:00:00"
                    } else {
                        val total = TimeCalculator().msToStringTime(value.totalStudyTime).substring(
                            0,
                            8
                        )
                        // 총 공부 시간
                        timeText.text = total
                        // 실제 공부한 시간
                        dailyReal = value.realStudyTime

                        dailyGoalStatus = dailyGoalTime + dailyReal <= 0
                        myRef.child("/dailyGoal/goalStatus")
                            .setValue(dailyGoalStatus)

                        if (dailyGoalStatus) imageGoal.visibility = View.VISIBLE
                        else imageGoal.visibility = View.INVISIBLE

                        val real = TimeCalculator().msToStringTime(value.realStudyTime).substring(
                            0,
                            8
                        )
                        realTime.text = real
                        //최대 공부 시간 : maxFocusStudyTime
                        val max =
                            TimeCalculator().msToStringTime(value.maxFocusStudyTime).substring(
                                0,
                                8
                            )
                        maxFocusTime.text = max

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })
        dailyGoalValueEventListener =
            myRef.child("/dailyGoal/goalTime").addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue<Long>()
                    if (value == null) {
                        Log.d(TAG, "DailyGoal이 없음")
                        goalTime.text = "00:00:00"
                    } else {
                        val goal = TimeCalculator().msToStringTime(value).substring(0, 8)
                        goalTime.text = goal
                        dailyGoalTime = value
                        dailyGoalStatus = dailyGoalTime + dailyReal <= 0
                        myRef.child("/dailyGoal/goalStatus")
                            .setValue(dailyGoalStatus)
                        if (dailyGoalStatus) imageGoal.visibility = View.VISIBLE
                        else imageGoal.visibility = View.INVISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })
        studiesValueEventListener =
            myRef.child("/studies").addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue<ArrayList<Study>>()
                    var tableRow = TableRow(context)
                    table_layout.addView(tableRow)
//                    if(tableRow != null)
                    (tableRow.parent as ViewGroup).removeAllViews()
                    if (value == null) {
                        Log.d(TAG, "Studies가 없음")
                        for (time in 0..23) {
                            tableRow = TableRow(context)
                            val lp: TableLayout.LayoutParams = TableLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            lp.setMargins(2, 2, 2, 2)
                            tableRow.layoutParams = lp
                            for (i in 0..6) {
                                val textView = TextView(context)
                                if (i == 0) {
                                    textView.text = "${time}시"
                                } else {
                                    textView.text = ""
                                }
                                textView.setBackgroundColor(Color.WHITE)
                                textView.gravity = Gravity.CENTER
                                textView.height = (resources.displayMetrics.density * 30).toInt()
//                                textView.setMarginLeft((resources.displayMetrics.density*3).toInt())
                                tableRow.addView(textView)
                            }
                            table_layout.addView(tableRow)
                        }

                    } else {
                        // list : 00:00:00 ~23:59:59
                        // 10분단위로 체크
                        // 0시 0분-0시 10분 : tr0_td1
                        val colorTable = Array(24) {
                            arrayOfNulls<String>(
                                7
                            )
                        }
                        for (study in value) {
                            val startHour = study.startTime.substring(0, 2).toInt()  // 00~23 시간
                            val startMin = (study.startTime.substring(3, 5)
                                .toInt() + 5) / 10  // 0~4 : td1, 5~14 : td2, 55~59 : X =>1~6
                            val endHour = study.endTime.substring(0, 2).toInt()
                            val endMin = (study.endTime.substring(3, 5)
                                .toInt() + 5) / 10  //0~4 : X, 5~14 : td
                            for (array in colorTable.indices) {
                                for (item in colorTable[startHour].indices) {
                                    if (item > startMin) {
                                        colorTable[startHour][item] = "#7AF0FF"
                                    }
                                }
                                if (array in startHour + 1 until endHour) {
                                    for (item in 1 until colorTable[array].size) {
                                        colorTable[array][item] = "#7AF0FF"
                                    }

                                }
                                for (item in colorTable[endHour].indices) {
                                    if (item > endMin) {
                                        colorTable[endHour][item] = "#FFFFFF"
                                    }
                                }
                            }
                            val real: MutableList<RealStudy> = study.realStudy
                            for (time in real) {
                                val rsHour = time.realStudyStartTime.substring(0, 2).toInt()
                                val rsMin =
                                    (time.realStudyStartTime.substring(3, 5).toInt() + 5) / 10
                                val reHour = time.realStudyEndTime.substring(0, 2).toInt()
                                val reMin = (time.realStudyEndTime.substring(3, 5).toInt() + 5) / 10
                                            Log.d(TAG,"$rsHour , $reHour , $rsMin , $reMin")
                                if (rsHour == reHour) {
                                    for (i in rsMin+1 until reMin + 1) {
                                        colorTable[rsHour][i] = "#34C7DA"
                                    }
                                } else if (rsHour < reHour) {
                                    for (i in rsMin+1 until colorTable[rsHour].size) {
                                        colorTable[rsHour][i] = "#34C7DA"
                                    }
                                    for (i in rsHour+1 until reHour) {
                                        for (j in 1 until colorTable[i].size) {
                                            colorTable[i][j] = "#34C7DA"
                                        }
                                    }
                                    for (i in 1 until reMin+1) {
                                        colorTable[reHour][i] = "#34C7DA"
                                    }
                                }
                            }
                        }

                        for (time in 0..23) {
                            tableRow = TableRow(context)
                            val lp: TableLayout.LayoutParams = TableLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            lp.setMargins(2, 2, 2, 2)
                            tableRow.layoutParams = lp
                            for (i in 0..6) {
                                val textView = TextView(context)
                                if (i == 0) {
                                    textView.text = "${time}시"
                                } else {
                                    textView.text = ""
                                }
                                if (colorTable[time][i] != null) textView.setBackgroundColor(
                                    Color.parseColor(
                                        colorTable[time][i]
                                    )
                                )
                                else {
                                    textView.setBackgroundColor(Color.WHITE)
                                }
                                textView.gravity = Gravity.CENTER
                                textView.height = (resources.displayMetrics.density * 30).toInt()
                                tableRow.addView(textView)
                            }
                            table_layout.addView(tableRow)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })
        val ref = database.getReference("calendar/$uid")
        val array: Array<Int> = Array(24) { 0 }
        ref.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null || snapshot.value!! == "") {
                    Log.d(TAG, "아무런 값이 없음")
                } else {
                    for (i in 1..7) {
                        val date = TimeCalculator().stringToDate(day).minusDays(i.toLong())
                        val studies =
                            snapshot.child("/${TimeCalculator().dateToString(date)}/studies")
                                .getValue<ArrayList<Study>>()
                        if (studies != null) {
                            studies.forEach {
                                for (time in it.realStudy) {
                                    array[time.realStudyStartTime.substring(0, 2).toInt()]++
                                    array[time.realStudyEndTime.substring(0, 2).toInt()]++
                                }
//                                if (it.realStudy != null) {
//                                }
                            }
                        } else {
                            Log.d(TAG, "study 없음....")
                        }
                    }
                    var max1 = 0
                    var max2 = 0
                    var max3 = 0
                    var maxValue1 = 0
                    var maxValue2 = 0
                    var maxValue3 = 0
                    for (i in array.indices) {
                        if (array[i] > maxValue1) {
                            max3 = max2
                            maxValue3 = maxValue2
                            max2 = max1
                            maxValue2 = maxValue1
                            max1 = i
                            maxValue1 = array[i]
                        } else if (array[i] > maxValue2) {
                            max3 = max2
                            maxValue3 = maxValue2
                            max2 = i
                            maxValue2 = array[i]
                        } else if (array[i] > maxValue3) {
                            max3 = i
                            maxValue3 = array[i]
                        }
                    }
                    if (maxValue1 != 0) {
                        recommendTime1.textSize = 24F
                        recommendTime1.text = "${max1}시~${max1 + 1}시"
                    }
                    else {
                        recommendTime1.textSize = 18F
                        recommendTime1.text = "기록이 없습니다"
                    }
                    if (maxValue2 != 0) recommendTime2.text = "${max2}시~${max2 + 1}시"
                    else recommendTime2.text = " "
                    if (maxValue3 != 0) recommendTime3.text = "${max3}시~${max3 + 1}시"
                    else recommendTime3.text = " "
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
        // [END read_message]
    }
}