package com.bignerdranch.android.criminalintent

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

private const val ARG_PATH = "ARG_PATH"

class DialogFragment : Fragment() {
    private var path: String? = null
    private lateinit var bigImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            path = it.getString(ARG_PATH)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bigImage = view.findViewById(R.id.bigImage) as ImageView

        val myBitmap = BitmapFactory.decodeFile(path)
        bigImage.setImageBitmap(myBitmap)
    }

    companion object {
        @JvmStatic
        fun newInstance(path: String) =
            DialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                }
            }
    }
}