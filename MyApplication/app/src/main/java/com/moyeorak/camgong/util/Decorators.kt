package com.moyeorak.camgong.util

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.*

// 토,일요일날 각각 파란색, 빨간색으로 표시하기 위한 데코레이터
public class SundayDecorator : DayViewDecorator {
    private val calendar: Calendar = Calendar.getInstance();

    override fun shouldDecorate(day: CalendarDay): Boolean {
        day.copyTo(calendar)
        var weekDay = calendar.get(Calendar.DAY_OF_WEEK)
        return weekDay == Calendar.SUNDAY
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(ForegroundColorSpan(Color.RED))
    }
}

public class SaturdayDecorator : DayViewDecorator {
    private val calendar: Calendar = Calendar.getInstance();

    override fun shouldDecorate(day: CalendarDay): Boolean {
        day.copyTo(calendar)
        var weekDay = calendar.get(Calendar.DAY_OF_WEEK)
        return weekDay == Calendar.SATURDAY
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(ForegroundColorSpan(Color.BLUE))
    }
}

public class AchiveDecorator(val date:CalendarDay, val type:Int) : DayViewDecorator{
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return date == day // 동일 날짜에 decorate
    }

    override fun decorate(view: DayViewFacade) {
        var color = Color.WHITE
        when(type){
            1 -> color = Color.rgb(183, 226, 226)
            2 -> color = Color.rgb(237, 172, 177)
            3 -> color = Color.LTGRAY
        }
//        view.addSpan(DotSpan(5F, color))
        view.addSpan(LineSpan(color))
    }
}