package com.moyeorak.camgong.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Result (
    var focusStudyTime: MutableList<FocusStudyTime> = mutableListOf(),
    var maxFocusStudyTime: Long = 0L,
    var realStudyTime: Long = 0L,
    var totalStudyTime: Long = 0L
)
data class FocusStudyTime(
    var startTime: String = "",
    var endTime:String = ""
)
