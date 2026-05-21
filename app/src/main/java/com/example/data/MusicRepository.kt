package com.example.data

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {
    val allSongs: Flow<List<Music>> = musicDao.getAllSongs()

    suspend fun insert(music: Music) = musicDao.insert(music)

    suspend fun update(music: Music) = musicDao.update(music)

    suspend fun delete(music: Music) = musicDao.delete(music)

    suspend fun clearAndRestore(songs: List<Music>) {
        musicDao.deleteAllSongs()
        musicDao.insertAll(songs)
    }
}
