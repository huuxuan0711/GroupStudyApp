package com.xmobile.project1groupstudyappnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.state.SearchUIState
import com.xmobile.project1groupstudyappnew.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel(){
    private val _searchState = MutableStateFlow<SearchUIState>(SearchUIState.Idle)
    val searchState: StateFlow<SearchUIState> = _searchState.asStateFlow()

    fun searchUser(query: String) {
        viewModelScope.launch {
            try {
                _searchState.value = SearchUIState.Loading
                val result = searchRepository.searchUser(query)
                result.onSuccess {
                    _searchState.value = SearchUIState.SuccessSearchUser(it)
                }.onFailure {
                    e -> _searchState.value = SearchUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _searchState.value = SearchUIState.Error(e.message.toString())
            }
        }
    }

    fun searchAll(query: String) {
        viewModelScope.launch {
            coroutineScope {
                launch { searchUser(query) }
                launch { searchGroup(query) }
                launch { searchTask(query) }
                launch { searchFile(query) }
            }
        }
    }


    fun searchGroup(query: String) {
        viewModelScope.launch {
            try {
                _searchState.value = SearchUIState.Loading
                val result = searchRepository.searchGroup(query)
                result.onSuccess {
                    _searchState.value = SearchUIState.SuccessSearchGroup(it)
                }.onFailure { e ->
                    _searchState.value = SearchUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _searchState.value = SearchUIState.Error(e.message.toString())
            }
        }
    }

    fun searchFile(query: String) {
        viewModelScope.launch {
            try {
                _searchState.value = SearchUIState.Loading
                val result = searchRepository.searchFile(query)
                result.onSuccess {
                    _searchState.value = SearchUIState.SuccessSearchFile(it)
                }.onFailure { e ->
                    _searchState.value = SearchUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _searchState.value = SearchUIState.Error(e.message.toString())
            }
        }
    }

    fun searchTask(query: String) {
        viewModelScope.launch {
            try {
                _searchState.value = SearchUIState.Loading
                val result = searchRepository.searchTask(query)
                result.onSuccess {
                    _searchState.value = SearchUIState.SuccessSearchTask(it)
                }.onFailure { e ->
                    _searchState.value = SearchUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _searchState.value = SearchUIState.Error(e.message.toString())
            }
        }
    }
}