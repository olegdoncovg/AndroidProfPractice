package com.bignerdranch.android.photogallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

//var DEFAULT_PAGE_SIZE: Int = 10
//var DEFAULT_PAGE_NUMBER: Int = 1

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
//    val galleryItemLiveData: LiveData<List<GalleryItem>>
//    var pageSize: Int = DEFAULT_PAGE_SIZE
//    var pageNumber: Int = DEFAULT_PAGE_NUMBER
//
//    private val flickrFetchr = FlickrFetchr()
//    private val mutableSearchTerm = MutableLiveData<String>()
//    val searchTerm: String get() = mutableSearchTerm.value ?: ""
//
//    init {
//        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)
//
//        galleryItemLiveData = Transformations.switchMap(mutableSearchTerm) { searchTerm ->
//            if (searchTerm.isBlank()) {
//                flickrFetchr.fetchPhotos()
//            } else {
//                flickrFetchr.searchPhotos(searchTerm, pageSize, pageNumber)
//            }
//        }
//    }
//
//    fun fetchPhotos(query: String = "") {
//        QueryPreferences.setStoredQuery(app, query)
//        mutableSearchTerm.value = query
//    }
}