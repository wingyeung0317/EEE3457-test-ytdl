package hk.edu.vtc.stu.eee3457_test_230251805

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import com.yausername.youtubedl_android.YoutubeDL.UpdateStatus
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var btnVideo: LinearLayout? = null
    private var btnUpdate: Button? = null
    private var progressBar: ProgressBar? = null
    private var updating = false
    private val compositeDisposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initListeners()

        val InchToCm_btn = findViewById<LinearLayout>(R.id.cToF_btn)
        val map_btn = findViewById<LinearLayout>(R.id.map_btn)
        InchToCm_btn.setOnClickListener(){
            startActivity(Intent(this@MainActivity, InchToCmActivity::class.java))
        }
        map_btn.setOnClickListener(){
            startActivity(Intent(this@MainActivity, VideoActivity::class.java))
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun initListeners() {
        btnVideo!!.setOnClickListener(this)
        btnUpdate!!.setOnClickListener(this)
    }

    private fun initViews() {
        btnUpdate = findViewById<Button>(R.id.btn_update)
        btnVideo = findViewById<LinearLayout>(R.id.map_btn)
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.map_btn -> {
                val i = Intent(
                    this@MainActivity,
                    VideoActivity::class.java
                )
                startActivity(i)
            }

            R.id.btn_update -> {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Update Channel")
                    .setItems(
                        arrayOf("Stable Releases", "Nightly Releases")
                    ) { dialogInterface: DialogInterface?, which: Int ->
                        if (which == 0) updateYoutubeDL(
                            UpdateChannel._STABLE
                        ) else updateYoutubeDL(UpdateChannel._NIGHTLY)
                    }
                    .create()
                dialog.show()
            }
        }
    }

    private fun updateYoutubeDL(updateChannel: UpdateChannel) {
        if (updating) {
            Toast.makeText(this@MainActivity, "Update is already in progress!", Toast.LENGTH_LONG)
                .show()
            return
        }
        updating = true
        progressBar!!.visibility = View.VISIBLE
        val disposable = Observable.fromCallable {
            getInstance().updateYoutubeDL(this, updateChannel)
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ status: UpdateStatus? ->
                progressBar!!.visibility = View.GONE
                when (status) {
                    UpdateStatus.DONE -> Toast.makeText(
                        this@MainActivity,
                        "Update successful " + getInstance().versionName(this),
                        Toast.LENGTH_LONG
                    ).show()

                    UpdateStatus.ALREADY_UP_TO_DATE -> Toast.makeText(
                        this@MainActivity,
                        "Already up to date " + getInstance().versionName(this),
                        Toast.LENGTH_LONG
                    ).show()

                    else -> Toast.makeText(
                        this@MainActivity,
                        status.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
                updating = false
            }) { e: Throwable? ->
                if (BuildConfig.DEBUG) Log.e(
                    TAG,
                    "failed to update",
                    e
                )
                progressBar!!.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "update failed",
                    Toast.LENGTH_LONG
                ).show()
                updating = false
            }
        compositeDisposable.add(disposable)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}