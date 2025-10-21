package com.example.safeher.ui.resource


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.repository.HotlineRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResourceHubViewModel : ViewModel() {

    private val hotlineRepository =
        HotlineRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<HotlineCategory?>(null)
    val selectedCategory: StateFlow<HotlineCategory?> = _selectedCategory.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()


    val filteredHotlines: StateFlow<List<Hotline>> = combine(
        hotlineRepository.getHotlines(),
        _searchQuery,
        _selectedCategory,
        _favoriteIds
    ) { hotlines, query, category, favorites ->
        hotlines
            .filter { hotline ->
                val matchesSearch = query.isEmpty() ||
                        hotline.title.contains(query, ignoreCase = true) ||
                        hotline.description.contains(query, ignoreCase = true) ||
                        hotline.number.contains(query)

                val matchesCategory = category == null || hotline.category == category

                matchesSearch && matchesCategory
            }
            .sortedWith(compareBy(
                { !favorites.contains(it.id) },
                { it.title }
            ))
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        fetchFavoriteHotlines()
    }

    private fun fetchFavoriteHotlines() {
        viewModelScope.launch {
            val favorites = hotlineRepository.getFavoriteHotlines()
            _favoriteIds.value = favorites
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: HotlineCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onFavoriteToggle(hotlineId: String) {
        val currentFavorites = _favoriteIds.value
        val isCurrentlyFavorite = currentFavorites.contains(hotlineId)

        val newFavorites = if (isCurrentlyFavorite) {
            currentFavorites - hotlineId
        } else {
            currentFavorites + hotlineId
        }
        _favoriteIds.value = newFavorites

        viewModelScope.launch {
            if (isCurrentlyFavorite) {
                hotlineRepository.removeFavorite(hotlineId)
            } else {
                hotlineRepository.addFavorite(hotlineId)
            }
        }
    }
}