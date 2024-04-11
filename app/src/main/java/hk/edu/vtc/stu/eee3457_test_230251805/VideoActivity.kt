package hk.edu.vtc.stu.eee3457_test_230251805

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.devbrackets.android.exomedia.BuildConfig
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.mapper.VideoInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class VideoActivity : AppCompatActivity(), View.OnClickListener {
    private var btnStartStream: Button? = null
    private var btnStopDownload: Button? = null
    private var etUrl: EditText? = null
    private var videoView: VideoView? = null
    private var pbLoading: ProgressBar? = null
    private var progressBar: ProgressBar? = null
    private var tvDownloadStatus: TextView? = null
    private var tvCommandOutput: TextView? = null
    private val compositeDisposable = CompositeDisposable()
    private val processId = "MyDlProcess"
    private var downloading = false
    private val callback: Function3<Float, Long, String, Unit> =
        { progress: Float, o2: Long?, line: String? ->
            runOnUiThread {
                progressBar!!.progress = progress.toInt()
                tvDownloadStatus!!.text = line
            }
            Unit
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        initViews()
        initListeners()
        val return_to_main = findViewById<ImageView>(R.id.return_to_main)
        return_to_main.setOnClickListener {
            startActivity(Intent(this@VideoActivity, MainActivity::class.java))
        }
    }

    private fun initViews() {
        btnStartStream = findViewById(R.id.btn_start_streaming)
        etUrl = findViewById(R.id.et_url)
        videoView = findViewById(R.id.video_view)
        pbLoading = findViewById(R.id.pb_status)
        btnStopDownload = findViewById(R.id.btn_stop_download)
        etUrl = findViewById(R.id.et_url)
        progressBar = findViewById(R.id.progress_bar)
        tvDownloadStatus = findViewById(R.id.tv_status)
        pbLoading = findViewById(R.id.pb_status)
        tvCommandOutput = findViewById(R.id.tv_command_output)
    }

    private fun initListeners() {
        btnStartStream!!.setOnClickListener(this)
        videoView!!.setOnPreparedListener { videoView!!.start() }
        btnStopDownload!!.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_start_streaming -> {
                startDownload()
                startStream()
            }

            R.id.btn_stop_download -> try {
                getInstance().destroyProcessById(processId)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    private fun startDownload() {
        if (downloading) {
            Toast.makeText(
                this@VideoActivity,
                "cannot start download. a download is already in progress",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!isStoragePermissionGranted) {
            Toast.makeText(
                this@VideoActivity,
                "grant storage permission and retry",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val url = etUrl!!.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(url)) {
            etUrl!!.error = getString(R.string.url_error)
            return
        }
        val request = YoutubeDLRequest(url)
        val youtubeDLDir = downloadLocation
        val config = File(youtubeDLDir, "config.txt")
        request.addOption("--no-mtime")
        request.addOption("--downloader", "libaria2c.so")
        request.addOption("--external-downloader-args", "aria2c:\"--summary-interval=1\"")
        request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
        request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")
        showStart()
        downloading = true
        val disposable = Observable.fromCallable {
            getInstance().execute(request, processId, callback)
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ youtubeDLResponse: YoutubeDLResponse ->
                pbLoading!!.visibility = View.GONE
                progressBar!!.progress = 100
                tvDownloadStatus!!.text = getString(R.string.download_complete)
                tvCommandOutput!!.text = youtubeDLResponse.out
                Toast.makeText(
                    this@VideoActivity,
                    "download successful",
                    Toast.LENGTH_LONG
                )
                    .show()
                downloading = false
            }) { e: Throwable ->
                if (BuildConfig.DEBUG) Log.e(
                    TAG,
                    "failed to download",
                    e
                )
                pbLoading!!.visibility = View.GONE
                tvDownloadStatus!!.text = getString(R.string.download_failed)
                tvCommandOutput!!.text = e.message
                Toast.makeText(this@VideoActivity, "download failed", Toast.LENGTH_LONG)
                    .show()
                downloading = false
            }
        compositeDisposable.add(disposable)
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private val downloadLocation: File
        private get() {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val youtubeDLDir = File(downloadsDir, "youtubedl-android")
            if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
            return youtubeDLDir
        }

    private fun showStart() {
        tvDownloadStatus!!.text = getString(R.string.download_start)
        progressBar!!.progress = 0
        pbLoading!!.visibility = View.VISIBLE
    }

    private fun startStream() {
        val url = etUrl!!.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(url)) {
            etUrl!!.error = getString(R.string.url_error)
            return
        }
        pbLoading!!.visibility = View.VISIBLE
        val disposable = Observable.fromCallable {
            val request = YoutubeDLRequest(url)
            // best stream containing video+audio
            request.addOption("-f", "best")
            getInstance().getInfo(request)
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ streamInfo: VideoInfo ->
                pbLoading!!.visibility = View.GONE
                val videoUrl = streamInfo.url
                if (TextUtils.isEmpty(videoUrl)) {
                    Toast.makeText(
                        this@VideoActivity,
                        "failed to get stream url",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    setupVideoView(videoUrl)
                }
            }) { e: Throwable? ->
                if (com.devbrackets.android.exomedia.BuildConfig.DEBUG) Log.e(
                    TAG,
                    "failed to get stream info",
                    e
                )
                pbLoading!!.visibility = View.GONE
                Toast.makeText(
                    this@VideoActivity,
                    "streaming failed. failed to get stream info",
                    Toast.LENGTH_LONG
                ).show()
            }
        compositeDisposable.add(disposable)
    }

    val isStoragePermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else {
            true
        }

    private fun setupVideoView(videoUrl: String?) {
        videoView!!.setVideoURI(Uri.parse(videoUrl))
    }

    companion object {
        private const val TAG = "StreamingExample"
    }
}
