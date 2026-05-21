package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Music
import com.example.data.MusicJson
import com.example.data.MusicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(private val repository: MusicRepository) : ViewModel() {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, MusicJson::class.java)
    private val adapter = moshi.adapter<List<MusicJson>>(listType)

    // Raw list from database
    val songs: StateFlow<List<Music>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User search input
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered and A-Z Sorted list
    val filteredSortedSongs: StateFlow<List<Music>> = combine(songs, searchQuery) { list, query ->
        val trimmedQuery = query.trim()
        val filtered = if (trimmedQuery.isEmpty()) {
            list
        } else {
            list.filter {
                it.artist.contains(trimmedQuery, ignoreCase = true) ||
                it.song.contains(trimmedQuery, ignoreCase = true)
            }
        }
        // Exiba a lista sempre em Ordem Alfabética Automática (A-Z pelo Artista), e depois pela música como critério secundário
        filtered.sortedWith(
            compareBy<Music> { it.artist.lowercase() }
                .thenBy { it.song.lowercase() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state states for notifications and dialogs
    private val _duplicateArtistSong = MutableStateFlow<Pair<String, String>?>(null)
    val duplicateArtistSong: StateFlow<Pair<String, String>?> = _duplicateArtistSong.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissDuplicateAlert() {
        _duplicateArtistSong.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun setSuccessMessage(message: String) {
        _successMessage.value = message
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    /**
     * Adicionar com Validação Inteligente:
     * - Se o Artista + Música já existir em formato case-insensitive exato, bloqueia e avisa.
     * - Permite variações (se houver strings de diferença).
     */
    fun addMusic(artist: String, song: String): Boolean {
        val cleanArtist = artist.trim()
        val cleanSong = song.trim()

        if (cleanArtist.isEmpty() || cleanSong.isEmpty()) {
            _errorMessage.value = "Por favor, preencha o nome do artista e da música."
            return false
        }

        // Bloqueio de Duplicatas: Artista + Música exatamente igual (case-insensitive)
        val isDuplicate = songs.value.any {
            it.artist.trim().equals(cleanArtist, ignoreCase = true) &&
            it.song.trim().equals(cleanSong, ignoreCase = true)
        }

        if (isDuplicate) {
            _duplicateArtistSong.value = Pair(cleanArtist, cleanSong)
            return false
        }

        viewModelScope.launch {
            repository.insert(Music(artist = cleanArtist, song = cleanSong))
            _successMessage.value = "Música adicionada com sucesso!"
        }
        return true
    }

    fun deleteMusic(music: Music) {
        viewModelScope.launch {
            repository.delete(music)
            _successMessage.value = "Música excluída!"
        }
    }

    fun updateMusic(music: Music, newArtist: String, newSong: String): Boolean {
        val cleanArtist = newArtist.trim()
        val cleanSong = newSong.trim()

        if (cleanArtist.isEmpty() || cleanSong.isEmpty()) {
            _errorMessage.value = "O nome do artista e da música não podem ficar vazios."
            return false
        }

        // Duplicate checks (ignoring target item itself during edit validation)
        val isDuplicate = songs.value.any {
            it.id != music.id &&
            it.artist.trim().equals(cleanArtist, ignoreCase = true) &&
            it.song.trim().equals(cleanSong, ignoreCase = true)
        }

        if (isDuplicate) {
            _duplicateArtistSong.value = Pair(cleanArtist, cleanSong)
            return false
        }

        viewModelScope.launch {
            repository.update(music.copy(artist = cleanArtist, song = cleanSong))
            _successMessage.value = "Música editada com sucesso!"
        }
        return true
    }

    /**
     * Serializa toda a biblioteca para texto JSON
     */
    fun getBackupJsonString(): String {
        val exportList = songs.value.map { MusicJson(it.artist, it.song) }
        return adapter.toJson(exportList)
    }

    /**
     * Lê JSON importado e substitui toda a lista local. Erros mostram aviso.
     */
    fun restoreFromJsonString(jsonString: String): Boolean {
        if (jsonString.isBlank()) {
            _errorMessage.value = "O arquivo de backup está vazio."
            return false
        }
        return try {
            val importedList = adapter.fromJson(jsonString)
            if (importedList != null) {
                viewModelScope.launch {
                    val dbList = importedList.map {
                        Music(
                            artist = it.artist.trim(),
                            song = it.song.trim()
                        )
                    }
                    repository.clearAndRestore(dbList)
                    _successMessage.value = "Biblioteca restaurada com sucesso!"
                }
                true
            } else {
                _errorMessage.value = "Format de arquivo inválido."
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Erro ao restaurar backup. Verifique a estrutura do arquivo."
            false
        }
    }
}

class MusicViewModelFactory(private val repository: MusicRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
