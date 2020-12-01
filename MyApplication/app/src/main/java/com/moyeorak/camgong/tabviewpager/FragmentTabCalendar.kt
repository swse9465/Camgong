
package com.moyeorak.camgong.tabviewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.moyeorak.camgong.R
import com.moyeorak.camgong.models.DailyGoal
import com.moyeorak.camgong.models.Result
import com.moyeorak.camgong.util.AchiveDecorator
import com.moyeorak.camgong.util.SaturdayDecorator
import com.moyeorak.camgong.util.SundayDecorator
import com.moyeorak.camgong.util.TimeCalculator
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.prolificinteractive.materialcalendarview.*
import kotlinx.android.synthetic.main.layout_calendar.view.*
import java.util.*
import kotlin.collections.ArrayList


class FragmentTabCalendar : Fragment() {
    private val tabTextList = arrayListOf("일간", "주간", "월간")
    // 주, 월의 공부시간 정보 저장할 배열, 총시간 / 실제 공부시간 / 최대 집중시간 / 휴식시간
    var dailyInfo = mutableListOf<Long>(0, 0, 0, 0)
    var monthlyInfo = mutableListOf<Long>(0, 0, 0, 0)
    var weeklyInfo = mutableListOf<Long>(0, 0, 0, 0)
    var weekOfMonth = CalendarDay.today()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_calendar, container, false)

        // 캘린더 초기 설정
        initCalendar(view)

        // 목표치 달성했는지 판단하여 달력에 표시하는메서드
        isAchived(view, CalendarDay.today())

        // 일, 주, 월 공부시간 정보 출력
        getweeklyInfo(view, CalendarDay.today())
        getmonthlyInfo(view, CalendarDay.today())
        getDayInfo(view, CalendarDay.today())

        // 일,주,월 버튼 클릭 이벤트
        view.btnDaily.setOnClickListener {
            displayDaily(view)
            initBtn(view)
            view.btnDaily.setBackground(resources.getDrawable(R.drawable.button_custom))
        }
        view.btnWeekly.setOnClickListener{
            for (i in 0..2)
                weeklyInfo[i] = 0
            getweeklyInfo(view, weekOfMonth)
            initBtn(view)
            view.btnWeekly.setBackground(resources.getDrawable(R.drawable.button_custom))
        }
        view.btnMonthly.setOnClickListener{
            displayMonthly(view)
            initBtn(view)
            view.btnMonthly.setBackground(resources.getDrawable(R.drawable.button_custom))
        }

        // 선택된 날짜 변경 리스너
        view.calendar.setOnDateChangedListener(object : OnDateSelectedListener {
            override fun onDateSelected(
                widget: MaterialCalendarView,
                date: CalendarDay,
                selected: Boolean
            ) {
                // 파이어베이스에서 값 불러와서 textView에 표시 + 비율을 받아서 차트 그리기
                weekOfMonth = date
                for (i in 0..2)
                    dailyInfo[i] = 0
                getDayInfo(view, date)
                initBtn(view)
                view.btnDaily.setBackground(resources.getDrawable(R.drawable.button_custom))
            }
        })// end of setOnDateChangedListener

        // 캘린더 월 변경 리스너 -> isAchived() 호출
        view.calendar.setOnMonthChangedListener(object : OnMonthChangedListener {
            override fun onMonthChanged(
                widget: MaterialCalendarView,
                date: CalendarDay
            ) { // 달이 변경되었을때
                isAchived(view, date) // 목표 달성여부 달력에 표시

                // 달이 바뀌면 일, 주별 출력정보 + 차트는 초기화 시킴
                for (i in 0..2) {
                    dailyInfo[i] = 0
                    weeklyInfo[i] = 0
                    monthlyInfo[i] = 0
                }

                getmonthlyInfo(view, date) // 해당 월의 공부정보 가져옴
                initBtn(view)
                view.btnMonthly.setBackground(resources.getDrawable(R.drawable.button_custom))
            }
        })// end of setOnMonthChangedListener

        displayDaily(view)
        return view
    }

    private fun initBtn(view: View) {
        view.btnDaily.setBackground(resources.getDrawable(R.drawable.button_custom2))
        view.btnWeekly.setBackground(resources.getDrawable(R.drawable.button_custom2))
        view.btnMonthly.setBackground(resources.getDrawable(R.drawable.button_custom2))
    }

    private fun initCalendar(view: View) {
        // 캘린더 선택설정
        view.calendar.selectionMode = MaterialCalendarView.SELECTION_MODE_SINGLE

        // 캘린더 기본설정
        view.calendar.state().edit()
            .setFirstDayOfWeek(Calendar.SUNDAY)
            .setMinimumDate(CalendarDay.from(2000, 0, 1))
            .setMaximumDate(CalendarDay.from(2100, 11, 31))
            .setCalendarDisplayMode(CalendarMode.MONTHS)
            .commit()

        // 달력 꾸미기(토/일요일 색상 변경)
        view.calendar.addDecorators(
            SundayDecorator(),
            SaturdayDecorator()
        )
        view.calendar.setDateSelected(CalendarDay.today(), true)
    }

    // calendarDay를 받아서 기본 YYYYMM, true일 경우 YYYYMMDD
    private fun getStringDate(date: CalendarDay, flag: Boolean = false): String {
        var res : String = "${date.year}"

        if(date.month < 9) res += "0${date.month+1}"
        else res += "${date.month+1}"

        if(flag){
            if(date.day < 10) res += "0${date.day}"
            else res += "${date.day}"
        }

        return res
    }

    // 일별공부비율과 휴식비율을 받아서 차트를 만듦
    private fun drawPieChart(view: View, realTime: Long, breakTime: Long) {
        val pieChart = view.piechart
        val time = ArrayList<PieEntry>()
        val listColors = ArrayList<Int>()

        // 여기 value에 데이터 값 넣어주세요
        time.add(PieEntry(realTime.toFloat(), "공부"))
        listColors.add(resources.getColor(R.color.btnBackColor))
        time.add(PieEntry(breakTime.toFloat(), "휴식"))
        listColors.add(resources.getColor(R.color.cancelBtnBackColor))

        val dataSet = PieDataSet(time, "");
        dataSet.colors = listColors
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0F, 40F)
        dataSet.selectionShift = 5f
        dataSet.valueFormatter = PercentFormatter(pieChart)

        val pieData = PieData(dataSet)
        pieData.setValueTextSize(17f)
        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.description.isEnabled = false
        pieChart.animateXY(1000, 1000);
        pieChart.setRotationEnabled(false)
    }

    // 1일 공부정보 가져옴
    fun getDayInfo(view: View, calendarDay: CalendarDay) {
        val uid = FirebaseAuth.getInstance().uid
        val date = getStringDate(calendarDay, true)
        val ref = FirebaseDatabase.getInstance().getReference("/calendar/$uid/$date/result")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // result부분 가져오기
                if (snapshot.key.equals("result")) {
                    // DB에서 해당 값 가져오기, Result 클래스에 맞게 자동으로 저장됨
                    val data = snapshot.getValue(Result::class.java)

                    if (data != null) {
                        val tc = TimeCalculator()

                        // 총 공부시간, 실제 공부시간, 최대집중시간, 휴식시간
                        dailyInfo[0] = data.totalStudyTime
                        dailyInfo[1] = data.realStudyTime
                        dailyInfo[2] = data.maxFocusStudyTime
                        dailyInfo[3] = data.totalStudyTime - data.realStudyTime

                    } else { // 정보가 없을경우 모든 값을 없음으로 처리
                    }
                }// end of outer if()

                // 휴식, 공부 비율 그래프로 출력
                displayDaily(view)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Failed to read Value")
            }

        })

    } // end of getDate()

    private fun displayDefault(view: View){
        drawPieChart(view, 0, 100)
        view.msg1.text = "데이터가 존재하지 않습니다!"
        view.msg2.text = ""
        view.msg3.text = ""
        view.msg4.text = ""
        view.result1.text = ""
        view.result2.text = ""
        view.result3.text = ""
        view.result4.text = ""
    }

    private fun displayDaily(view: View) {
        if(dailyInfo[0] == 0L){
            displayDefault(view)
        }else{
            val tc = TimeCalculator()
            dailyInfo[3] = dailyInfo[0] - dailyInfo[1]
            var real = dailyInfo[1]
            var rest = dailyInfo[3]
            if(real < 0) real *= -1
            if(rest < 0) rest *= -1
            drawPieChart(view, real, rest)
            view.msg1.text = "총 공부 시간"
            view.msg2.text = "실제 공부 시간"
            view.msg3.text = "최대 집중시간"
            view.msg4.text = "휴식시간"
            view.result1.text = "${tc.msToStringTime(dailyInfo[0])}\n"
            view.result2.text = "${tc.msToStringTime(dailyInfo[1])}\n"
            view.result3.text = "${tc.msToStringTime(dailyInfo[2])}\n"
            view.result4.text = "${tc.msToStringTime(dailyInfo[3])}\n"
        }
    }

    // 주별 확인
    private fun getweeklyInfo(view: View, calendarDay: CalendarDay) {
        val uid = FirebaseAuth.getInstance().uid
        val ins = FirebaseDatabase.getInstance()

        // 요일 확인하여 그 주의 일요일에해당하는 날짜 선택 (firstDayOfWeek가 계속 1로 나오네...)
        val month = getStringDate(calendarDay)
        var DoW = calendarDay.calendar.get(Calendar.DAY_OF_WEEK)
        var day = calendarDay.day
        when(DoW){
            2 -> day -= 1
            3 -> day -= 2
            4 -> day -= 3
            5 -> day -= 4
            6 -> day -= 5
            7 -> day -= 6
            else -> null
        }

        for (i in day .. day+6){
            var date = month
            if(i<10) date += "0$i"
            else date += "$i"
            var ref = ins.getReference("/calendar/$uid/$date/result")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.key.equals("result")) {
                        val data = snapshot.getValue(Result::class.java)
                        if (data != null) {
                            weeklyInfo[0] += data.totalStudyTime
                            weeklyInfo[1] += data.realStudyTime
                            weeklyInfo[2] += data.maxFocusStudyTime
                            weeklyInfo[3] += data.totalStudyTime - data.realStudyTime
                        } else {
                        }
                    }
                    if (i == day + 6) {
                        displayWeekly(view)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(view.context, "주별 계산 실패\n$date", Toast.LENGTH_SHORT).show()
                }

            })// end of ref

        }

    }// end of getweeklyInfo()

    // 1주일의 공부 정보 View에 표시
    private fun displayWeekly(view: View) {
        if(weeklyInfo[0] == 0L) {
            displayDefault(view)
        }
        else{
            val tc = TimeCalculator()
            weeklyInfo[3] = weeklyInfo[0] - weeklyInfo[1]
            var real = weeklyInfo[1]
            var rest = weeklyInfo[3]
            if(real < 0) real *= -1
            if(rest < 0) rest *= -1
            drawPieChart(view, real, rest)
            view.msg1.text = "총 공부 시간"
            view.msg2.text = "실제 공부 시간"
            view.msg3.text = "최대 집중시간"
            view.msg4.text = "휴식시간"
            view.result1.text = "${tc.msToStringTime(weeklyInfo[0])}\n"
            view.result2.text = "${tc.msToStringTime(weeklyInfo[1])}\n"
            view.result3.text = "${tc.msToStringTime(weeklyInfo[2])}\n"
            view.result4.text = "${tc.msToStringTime(weeklyInfo[3])}\n"
        }
    }

    // 1달의 공부 정보를 가져와서 표시
    private fun getmonthlyInfo(view: View, current: CalendarDay) {
        val uid = FirebaseAuth.getInstance().uid
        val ins = FirebaseDatabase.getInstance()
        val month = getStringDate(current)

        val tc = TimeCalculator()
        for (i in 1..31){
            var date = month
            if(i<10) date += "0$i"
            else date += "$i"
            var ref = ins.getReference("/calendar/$uid/$date/result")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.key.equals("result")) {
                        val data = snapshot.getValue(Result::class.java)

                        if (data != null) {
                            monthlyInfo[0] += data.totalStudyTime
                            monthlyInfo[2] += data.maxFocusStudyTime
                            monthlyInfo[1] += data.realStudyTime
                            monthlyInfo[3] += data.totalStudyTime - data.realStudyTime
                        } else {
                        }
                    }
                    if (i == 31) {
                        displayMonthly(view)
                    }
                }// end of onDataChange

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(view.context, "월별 계산 실패\n$date", Toast.LENGTH_SHORT).show()
                }
            })
        }// end of for
    }// end of getmonthlyInfo()

    // 1달의 공부 정보 View에 표시
    private fun displayMonthly(view: View) {
        if(monthlyInfo[0] == 0L) {
            displayDefault(view)
        }
        else{
            val tc = TimeCalculator()
            monthlyInfo[3] = monthlyInfo[0] - monthlyInfo[1]
            var real = monthlyInfo[1]
            var rest = monthlyInfo[3]
            if(real < 0) real *= -1
            if(rest < 0) rest *= -1
            drawPieChart(view, real, rest)

            view.msg1.text = "총 시간"
            view.msg2.text = "실제 시간"
            view.msg3.text = "최대 집중시간"
            view.msg4.text = "휴식 시간"
            view.result1.text = "${tc.msToStringTime(monthlyInfo[0])}\n"
            view.result2.text = "${tc.msToStringTime(monthlyInfo[1])}\n"
            view.result3.text = "${tc.msToStringTime(monthlyInfo[2])}\n"
            view.result4.text = "${tc.msToStringTime(monthlyInfo[3])}\n"
        }
    }

    // 각 일자의 목표 달성여부를 판단하여 달력에 표시 -> 미완성
    // 실행되면 1~31일 훑어서 goalstatus 값에 따라 잘 가져오는것 까지 확인하였음 -> 값에따라 캘린더의 해당날짜에 표시
    private fun isAchived(view: View, current: CalendarDay) {
        val uid = FirebaseAuth.getInstance().uid
        val ins = FirebaseDatabase.getInstance()
        val month = getStringDate(current)
        for (i in 1..31){
            var date = month  // YYYYMMDD
            if(i<10) date += "0$i"
            else date += "$i"
            var ref = ins.getReference("/calendar/$uid/$date/dailyGoal")
            val day = CalendarDay.from(current.year, current.month, i) // 현재 조회중인 날짜

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.key.equals("dailyGoal")) {
                        val data = snapshot.getValue(DailyGoal::class.java)
                        var str = "기본값"
                        if (data != null) { // 목표가 설정되어있을경우
                            if (data.goalStatus) { // 달성한 경우
                                // 파란색으로 해당날짜에 표시
                                view.calendar.addDecorators(AchiveDecorator(day, 1))
                            } else { // 달성 못한 경우
                                // 빨간색으로 해당날짜에 표시
                                view.calendar.addDecorators(AchiveDecorator(day, 2))
                            }
                        } else { // 목표가 없는 경우, 회색으로 해당날짜에 표시(삭제됨)
                            view.calendar.addDecorators(AchiveDecorator(day, 3))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(view.context, "불러오기 실패\n$date", Toast.LENGTH_SHORT).show()
                }

            })
        }// end of for
    } // end of isAchived()

}