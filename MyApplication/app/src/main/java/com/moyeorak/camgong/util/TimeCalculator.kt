package com.moyeorak.camgong.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.moyeorak.camgong.models.FocusStudyTime
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
class TimeCalculator {
    // 시간 계산
    private val current = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val timeFormatters = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * today() : 오늘 날짜
     * currentTime : 현재시간
     * subTime : 두 시각 사이의 시간 계산
     * addTime : 시간 List를 모두 더함
     * dateToString : LocalDate의 날짜를 String으로 변환
     * timeToString : LocalTime의 시간을 String으로 변환
     * stringToDate : String형의 날짜를 LocalDate형으로 변환
     * stringToTime :
     * msToStringTime : Long형의 millisecond를 String형 시간(시:분:초.ms)으로 변환
     */
    fun today(): String? {
        return current.format(dateFormatter)
    }
    fun currentTime(): String{
        return current.format(timeFormatter)
    }
    fun subTime(startTime: String, endTime: String): Long {
        val start = LocalTime.parse(startTime, shortTimeFormatter)
        val end = LocalTime.parse(endTime, shortTimeFormatter)
        return abs(Duration.between(start,end).seconds)
    }
//    fun addTime(times: Map<String, FocusStudyTime>){
//
//
//    }
    fun dateToString(date: LocalDate): String{
        return date.format(dateFormatter)
    }
//    fun timeToString(time: LocalTime): String{
//        return time.format(timeFormatter)
//    }
    fun stringToDate(date: String): LocalDate {
        return LocalDate.parse(date, dateFormatter)
    }
//    fun stringToTime(time: String): LocalTime {
//        return LocalTime.parse(time, timeFormatter)
//    }
    fun msToStringTime(milliSec: Long): String{
        val ms = abs(milliSec)
        return String.format(
            "%02d:%02d:%02d",
            ms / (60*60*1000),
            ms % (60*60*1000) / (60*1000),
            ms % (60*1000) / 1000
        )
    }
    fun stringToLong(time: String):Long{
        var localtime: LocalTime
        if (time.length <9){
            localtime = LocalTime.parse(time,timeFormatters)
        }else{
            localtime = LocalTime.parse(time,timeFormatter)
        }
        val hours = localtime.hour.toLong()*60*60*1000
        val min = localtime.minute*60*1000
        val sec = localtime.second*1000
        val milli = localtime.nano/1000000
        return hours+min+sec+milli
    }

    // time1 / time2 * 100의 결과를 소수점 2번째 반올림한 결과 반환
    // time1 / time2 * 100의 결과를 소수점 2번째 반올림한 결과 반환
    fun getPercent(time1:Long, time2:Long): Double{
        var percent = time1.toDouble() / time2 * 100
        percent = (percent*100).roundToInt() / 100.0

        return percent
    }
}

