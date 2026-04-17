package com.example.simsekolah.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TugasModel(
    val deadline: String,
    val time: String,
    val title: String,
    val description: String
) : Parcelable