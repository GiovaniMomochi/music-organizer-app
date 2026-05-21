package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Music>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(music: Music)

    @Update
    suspend fun update(music: Music)

    @Delete
    suspend fun delete(music: Music)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Music>)
}
