package com.example.dressit.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.FirebaseRepository
import com.example.dressit.ui.base.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MapViewModel : BaseViewModel() {
    private val repository = FirebaseRepository()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    init {
        loadPosts()
    }

    private fun loadPosts() {
        launchWithLoading {
            repository.getAllPosts()
                .onEach { posts ->
                    // Filter out posts without location
                    _posts.value = posts.filter { it.latitude != null && it.longitude != null }
                }
                .catch { exception ->
                    handleError(exception as Exception)
                }
                .launchIn(viewModelScope)
        }
    }

    fun refreshPosts() {
        loadPosts()
    }
} 