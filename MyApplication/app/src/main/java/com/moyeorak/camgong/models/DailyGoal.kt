package com.moyeorak.camgong.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyGoal(
    var goalStatus: Boolean = false,
    var goalTime: Long = 0L
)
