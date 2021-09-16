package com.bignerdranch.android.criminalintent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import java.io.File
import java.text.DateFormat
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
const val REQUEST_DATE_KEY = "REQUEST_DATE_KEY"
const val REQUEST_DATE_BUNDLE = "REQUEST_DATE_BUNDLE"

class CrimeFragment : Fragment() {

    interface Callbacks {
        fun onShowImage(path: String)
    }

    private var callbacks: Callbacks? = null
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
//    private lateinit var callButton: Button

    private val getContent = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri: Uri? ->
        // Указать, для каких полей ваш запрос должен возвращать значения.
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        // Выполняемый здесь запрос — contactUri похож на предложение "where"
        val cursor = contactUri?.let {
            requireActivity().contentResolver.query(it, queryFields, null, null, null)
        }
        cursor?.use {
            // Verify cursor contains at least one result
            if (it.count != 0) {
            // Первый столбец первой строки данных — это имя вашего подозреваемого.
                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
    }

    private val getPhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { sucess: Boolean? ->
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            updatePhotoView()
//            photoView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
//                override fun onGlobalLayout() {
//                    if (photoView.width > 0 && photoView.height > 0) {
//                        photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//
//                        updatePhotoView()
//                    }
//                }
//            })
        }

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
//        callButton = view.findViewById(R.id.crime_call) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        photoFile)
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                //setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent ->
                startActivity(Intent.createChooser(intent, "Gmail"))
            }
        }

//        callButton.setOnClickListener {
//            val phone: String? = getPhoneNumber(crime.suspect)
//            callNumber(phone)
//        }

        suspectButton.apply {
            setOnClickListener { getContent.launch() }

            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)

            Log.e(TAG, "suspectButton.apply: resolvedActivity222=" + resolvedActivity)
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }
            setOnClickListener {
                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                getPhoto.launch(photoUri)
            }
        }

        photoView.setOnClickListener {
            callbacks?.onShowImage(photoFile.absolutePath)
        }

        setFragmentResultListener(REQUEST_DATE_KEY) { _, bundle ->
            // We use a String here, but any type that can be put in a Bundle is supported
            val result = bundle.getLong(REQUEST_DATE_BUNDLE)
            // Do something with the result
            crime.date = Date(result)
            updateUI()
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        callbacks = null
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = DateFormat.getDateInstance(DateFormat.LONG).format(crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path,  requireActivity())//photoView.width, photoView.height)
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription = getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.getDateInstance(DateFormat.LONG).format(crime.date)
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report,
            crime.title, dateString, solvedString, suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
//
//    val requestPermissionLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ) { isGranted: Boolean ->
//            Log.i(TAG, "checkPermission-registerForActivityResult: isGranted=$isGranted")
//            if (isGranted) {
//                // Permission is granted. Continue the action or workflow in your
//                // app.
//            } else {
//                // Explain to the user that the feature is unavailable because the
//                // features requires a permission that the user has denied. At the
//                // same time, respect the user's decision. Don't link to system
//                // settings in an effort to convince the user to change their
//                // decision.
//            }
//        }
//
//    //Manifest.permission.READ_CONTACTS
//    fun checkPermission(permission: String): Boolean {
//        when {
//            ContextCompat.checkSelfPermission(
//                requireContext(),
//                permission
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // You can use the API that requires the permission.
//                Log.i(TAG, "checkPermission-checkSelfPermission: true")
//                return true
//            }
//            shouldShowRequestPermissionRationale(permission) -> {
//                // In an educational UI, explain to the user why your app requires this
//                // permission for a specific feature to behave as expected. In this UI,
//                // include a "cancel" or "no thanks" button that allows the user to
//                // continue using your app without granting the permission.
////            showInContextUI(...)
//                Log.i(TAG, "checkPermission-shouldShowRequestPermissionRationale: false")
//                return false
//            }
//            else -> {
//                Log.i(TAG, "checkPermission-else: false")
//                // You can directly ask for the permission.
//                // The registered ActivityResultCallback gets the result of this request.
//                requestPermissionLauncher.launch(permission)
//                return false
//            }
//        }
//    }
//
//    private fun callNumber(phone: String?) {
//        if (!checkPermission(Manifest.permission.CALL_PHONE)) return
//        if (phone != null) {
//            val intent = Intent(Intent.ACTION_CALL)
//            intent.data = Uri.parse("tel:$phone")
//            requireContext().startActivity(intent)
//        }
//    }
//
//    private fun getPhoneNumber(nameRequest: String): String? {
//        if (!checkPermission(Manifest.permission.READ_CONTACTS)) return null
//        var number: String? = null
//
//        val activity = requireActivity()
//        val cr: ContentResolver = activity.getContentResolver()
//        val cur = cr.query(
//            ContactsContract.Contacts.CONTENT_URI,
//            null, null, null, null
//        )
//
//        if (cur?.count ?: 0 > 0) {
//            while (cur != null && cur.moveToNext()) {
//
//                val columnIndex = cur.getColumnIndex(ContactsContract.Contacts._ID)
//                if (columnIndex < 0)
//                    continue
//                val id = cur.getString(columnIndex)
//                val columnIndex1 = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
//                if (columnIndex1 < 0)
//                    continue
//                val name = cur.getString(columnIndex1)
//                val columnIndex2 =
//                    cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
//                if (columnIndex2 < 0)
//                    continue
//                if (cur.getInt(columnIndex2) > 0) {
//                    val pCur = cr.query(
//                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                        null,
//                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                        arrayOf(id),
//                        null
//                    )
//                    if (pCur != null)
//                        while (pCur.moveToNext()) {
//                            val columnIndex3 = pCur.getColumnIndex(
//                                ContactsContract.CommonDataKinds.Phone.NUMBER
//                            )
//                            if (columnIndex3 < 0)
//                                continue
//                            val phoneNo = pCur.getString(
//                                columnIndex3
//                            )
//                            Log.i(TAG, "callButton.setOnClickListener: Name: $name")
//                            Log.i(TAG, "callButton.setOnClickListener: Phone Number: $phoneNo")
//                            if (name == nameRequest) {
//                                number = phoneNo
//                            }
//                        }
//                    pCur!!.close()
//                }
//            }
//        }
//        cur?.close()
//        return number
//    }
}
