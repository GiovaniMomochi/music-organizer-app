package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "songs")
data class Music(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val artist: String,
    val song: String
)

@JsonClass(generateAdapter = true)
data class MusicJson(
    val artist: String,
    val song: String
)
