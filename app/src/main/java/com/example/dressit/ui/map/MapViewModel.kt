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
    
    // מיפוי של פוסטים לפי מזהה, לשימוש עם המרקרים
    private val _postMap = MutableLiveData<Map<String, Post>>()
    val postMap: LiveData<Map<String, Post>> = _postMap

    init {
        loadPosts()
    }

    private fun loadPosts() {
        launchWithLoading {
            repository.getAllPosts()
                .onEach { posts ->
                    // פילטור פוסטים ללא מיקום
                    val postsWithLocation = posts.filter { it.latitude != null && it.longitude != null }
                    _posts.value = postsWithLocation
                    
                    // בניית מיפוי של פוסטים לפי מזהה
                    _postMap.value = postsWithLocation.associateBy { it.id }
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