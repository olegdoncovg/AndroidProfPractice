package com.bignerdranch.android.photogallery

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.LayoutInflater
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"

const val DEFAULT_PAGE_SIZE: Int = 10
const val DEFAULT_PAGE_NUMBER: Int = 1

class PhotoGalleryFragment : VisibleFragment() {
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var adapter: PhotoAdapter

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

        retainInstance = true
        setHasOptionsMenu(true)
        photoGalleryViewModel = ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)

        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler) { photoHolder, bitmap ->
            val drawable = BitmapDrawable(resources, bitmap)
            photoHolder.bindDrawable(drawable)
        }

        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycle.addObserver(
            thumbnailDownloader.viewLifecycleObserver
        )

        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        progressBar = view.findViewById(R.id.photo_search_progress)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        init()
        return view
    }

    private fun init() {
        // PagedList
        val dataSource = MyPositionalDataSource()

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(DEFAULT_PAGE_SIZE)
            .build()

        val pagedList: PagedList<GalleryItem?> = PagedList.Builder<Int, GalleryItem?>(dataSource, config)
            .setNotifyExecutor(MainThreadExecutor())
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()

        adapter = PhotoAdapter()
        adapter.submitList(pagedList);
        photoRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(
            thumbnailDownloader.viewLifecycleObserver
        )
        photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        listener.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        photoGalleryViewModel.galleryItemLiveData.observe(
//            viewLifecycleOwner,
//            Observer { galleryItems ->
//                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
//                progressBar.visibility = View.GONE
//            })
    }

    private inner class PhotoHolder(private val itemImageView: ImageView) :
        RecyclerView.ViewHolder(itemImageView), View.OnClickListener {
        private lateinit var galleryItem: GalleryItem

        init {
            itemView.setOnClickListener(this)
        }

        val bindDrawable: (Drawable) -> Unit = itemImageView::setImageDrawable

        fun bindGalleryItem(item: GalleryItem) {
            galleryItem = item
        }

        override fun onClick(view: View) {
            val intent = PhotoPageActivity.newIntent(requireContext(), galleryItem.photoPageUri)
            startActivity(intent)

////To use Chrome to open page
//            CustomTabsIntent.Builder()
//                .setToolbarColor(ContextCompat.getColor(
//                    requireContext(), R.color.purple_700))
//                .setShowTitle(true)
//                .build()
//                .launchUrl(requireContext(), galleryItem.photoPageUri)
        }
    }

    internal class MainThreadExecutor : Executor {
        private val mHandler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable?) {
            mHandler.post(command!!)
        }
    }

    internal class MyPositionalDataSource : PositionalDataSource<GalleryItem?>() {

        private val flickrFetchr = FlickrFetchr()
        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<GalleryItem?>
        ) {
            Log.d(
                TAG, "loadInitial, requestedStartPosition = " + params.requestedStartPosition +
                        ", requestedLoadSize = " + params.requestedLoadSize
            )
            flickrFetchr.searchPhotos(
                "forest",
                params.requestedLoadSize,
                params.requestedStartPosition / DEFAULT_PAGE_SIZE + 1,
                Consumer<List<GalleryItem>> {
                    callback.onResult(it, 0)
                })
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<GalleryItem?>) {
            Log.d(
                TAG,
                "loadRange, startPosition = " + params.startPosition + ", loadSize = " + params.loadSize
            )
            flickrFetchr.searchPhotos(
                "forest",
                params.loadSize,
                params.startPosition + 1,
                Consumer<List<GalleryItem>> {
                    callback.onResult(it)
                })
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GalleryItem>() {
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem) =
            oldItem === newItem
        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem) =
            oldItem == newItem
    }

    private inner class PhotoAdapter : PagedListAdapter<GalleryItem, PhotoHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_gallery, parent, false) as ImageView
            return PhotoHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val placeholder: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bill_up_close
            ) ?: ColorDrawable()
            holder.bindDrawable(placeholder)
            val item = getItem(position)
            if (item?.url != null) thumbnailDownloader.queueThumbnail(holder, item.url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(queryText: String): Boolean {
                    Log.d(TAG, "QueryTextSubmit: $queryText")
//                    photoGalleryViewModel.fetchPhotos(queryText)
                    clearForSearch()
                    return true
                }

                override fun onQueryTextChange(queryText: String): Boolean {
                    Log.d(TAG, "QueryTextChange: $queryText")
                    return false
                }
            })

            setOnSearchClickListener {
//                searchView.setQuery(photoGalleryViewModel.searchTerm, false)
            }
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) {
            R.string.stop_polling
        } else {
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
//                photoGalleryViewModel.fetchPhotos("")
                clearForSearch()
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                if (isPolling) {
                    WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                    QueryPreferences.setPolling(requireContext(), false)
                } else {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                    val periodicRequest = PeriodicWorkRequest
                        .Builder(PollWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance().enqueueUniquePeriodicWork(
                        POLL_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                    Log.d(TAG, "enqueueUniquePeriodicWork")
                    QueryPreferences.setPolling(requireContext(), true)
                }
                activity?.invalidateOptionsMenu()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearForSearch() {
//        photoRecyclerView.adapter = PhotoAdapter(emptyList())
        progressBar.visibility = View.VISIBLE

        hideSoftKeyboard()
    }

    private fun hideSoftKeyboard() {
        val view: View? = requireActivity().currentFocus
        if (view != null) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object {
        fun newInstance() = PhotoGalleryFragment()
    }
}