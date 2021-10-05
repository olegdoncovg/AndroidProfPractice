package com.bignerdranch.android.photogallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "PhotoGalleryFragment"

class PhotoGalleryFragment : Fragment() {
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var photoRecyclerView: RecyclerView

    private val listener: OnChangeListener = OnChangeListener()

    private inner class OnChangeListener : OnGlobalLayoutListener {
        private var recyclerViewWidth = 0
        private var spanCount: Int = 0
        override fun onGlobalLayout() {
            val w = photoRecyclerView.width
            if (w > 0 && w != recyclerViewWidth) {
                recyclerViewWidth = w
                val newSpanCount: Int =
                    1f.coerceAtLeast(w / resources.getDimension(R.dimen.min_column_width)).toInt()

                if (spanCount != newSpanCount) {
                    spanCount = newSpanCount;
                    photoRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
                }
            }
        }

        fun clear() {
            recyclerViewWidth = 0
            spanCount = 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoGalleryViewModel = ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        listener.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer { galleryItems ->
                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
            })
    }

    private class PhotoHolder(itemTextView: TextView) : RecyclerView.ViewHolder(itemTextView) {
        val bindTitle: (CharSequence) -> Unit = itemTextView::setText
    }

    private class PhotoAdapter(private val galleryItems: List<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PhotoHolder {
            val textView = TextView(parent.context)
            return PhotoHolder(textView)
        }

        override fun getItemCount(): Int = galleryItems.size
        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]
            holder.bindTitle(galleryItem.title)
        }
    }


    companion object {
        fun newInstance() = PhotoGalleryFragment()
    }
}