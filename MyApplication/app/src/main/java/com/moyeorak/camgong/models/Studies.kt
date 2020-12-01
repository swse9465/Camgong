package com.moyeorak.camgong.models


data class Study(
    var startTime: String = "",
    var endTime: String = "",
    var realStudy: MutableList<RealStudy> = mutableListOf()
)

data class RealStudy(
    var realStudyStartTime: String = "",
    var realStudyEndTime: String = ""
)
