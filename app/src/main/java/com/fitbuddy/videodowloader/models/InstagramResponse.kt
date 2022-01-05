package com.fitbuddy.videodowloader.models

import com.google.gson.annotations.SerializedName

data class InstagramResponse(
    val items: List<Data>
)

data class Data(
    @SerializedName("video_versions")
    val videos: List<Video>
)

data class Video(
    val url: String
)