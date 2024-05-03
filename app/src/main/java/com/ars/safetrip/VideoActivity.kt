package com.ars.safetrip

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.ars.safetrip.data.SafeTripAdapter
import com.ars.safetrip.database.AppDataBase
import com.ars.safetrip.database.SafeTripHistory
import com.ars.safetrip.databinding.ActivityVideoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityVideoBinding
    private lateinit var db: AppDataBase
    private lateinit var safeTripList: ArrayList<SafeTripHistory>
    private lateinit var safeTripAdapter: SafeTripAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var dateStart : Date

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        viewBinding.ibVideoStart.setOnClickListener { captureVideo() }

        viewBinding.ibHistoric.setOnClickListener { showHistoricModal() }

        safeTripList = ArrayList()
        cameraExecutor = Executors.newSingleThreadExecutor()

        initDatabase()

        viewBinding.ibAlarm1.setOnClickListener {
            toggleAlarm(viewBinding.ibAlarm1, R.raw.music_1)
            viewBinding.ibWarning.apply { isVisible = !isVisible }
            viewBinding.ibDanger.apply { isVisible = false }
        }

        viewBinding.ibAlarm2.setOnClickListener {
            toggleAlarm(viewBinding.ibAlarm2, R.raw.music_2)
            viewBinding.ibDanger.apply { isVisible = !isVisible }
            viewBinding.ibWarning.apply { isVisible = false }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.pvFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.ibVideoStart.isEnabled = false
        viewBinding.ibHistoric.isActivated = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {

                        dateStart = Date(System.currentTimeMillis())

                        viewBinding.ibHistoric.apply {
                            isVisible = false
                        }
                        viewBinding.ibAlarm1.apply {
                            isVisible = true
                        }
                        viewBinding.ibAlarm2.apply {
                            isVisible = true
                        }

                        viewBinding.ibVideoStart.apply {
                            contentDescription = getString(R.string.stop_capture)
                            setImageResource(R.drawable.ic_stop_video)
                            layoutParams.width = resources.getDimensionPixelSize(R.dimen.button_100)
                            layoutParams.height = resources.getDimensionPixelSize(R.dimen.button_100)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }

                        if (viewBinding.ibAlarm1.isActivated || viewBinding.ibAlarm2.isActivated) {
                            stopAlarm()
                        }

                        viewBinding.ibHistoric.apply {
                            isVisible = true
                        }
                        viewBinding.ibAlarm1.apply {
                            isVisible = false
                        }
                        viewBinding.ibAlarm2.apply {
                            isVisible = false
                        }

                        viewBinding.ibVideoStart.apply {
                            contentDescription = getString(R.string.start_capture)
                            setImageResource(R.drawable.ic_start_video)
                            layoutParams.width = resources.getDimensionPixelSize(R.dimen.button_150)
                            layoutParams.height = resources.getDimensionPixelSize(R.dimen.button_150)
                            isEnabled = true
                        }

                        val dateEnd = Date(System.currentTimeMillis())
                        val tempo = (dateEnd.time - dateStart.time).toInt() / (1000 * 60)
                        val safeTripHistory = SafeTripHistory(
                            dateStart = dateStart,
                            dateEnd = dateEnd,
                            travelMinutes = tempo,
                            travelOccurrences = 0)

                        saveData(safeTripHistory)
                    }
                }
            }
    }

    private fun initDatabase() {
        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java, "database-history"
        )
            .allowMainThreadQueries()
            .build()
    }

    private fun saveData(value: SafeTripHistory) {
        try {
            db.safeTripHistory().insertAll(value)
        } catch (e: Error) {
            db.safeTripHistory().getAll()
            db.safeTripHistory().insertAll(value)
        }
    }

    private fun loadData() {
        safeTripList.clear()
        safeTripList.addAll(db.safeTripHistory().getAll() as ArrayList<SafeTripHistory>)
    }

    private fun setupRecyclerView(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(this)

        safeTripList = ArrayList()
        safeTripAdapter = SafeTripAdapter(safeTripList)
        rv.adapter = safeTripAdapter
    }

    private fun showHistoricModal() {
        val dialogView = layoutInflater.inflate(R.layout.historic_layout, null)

        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_history)
        setupRecyclerView(rv)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.show()

        val closeButton = dialogView.findViewById<ImageButton>(R.id.bt_close)
        closeButton.setOnClickListener { alertDialog.dismiss() }

        loadData()
    }

    private fun toggleAlarm(button: ImageButton, soundResource: Int) {
        stopAlarm()
        if (!button.isActivated) {
            startAlarm(soundResource)
        }

        button.isActivated = !button.isActivated
    }

    private fun startAlarm(soundResource: Int) {
        if (soundResource != 0) {
            mediaPlayer = MediaPlayer.create(this, soundResource)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}