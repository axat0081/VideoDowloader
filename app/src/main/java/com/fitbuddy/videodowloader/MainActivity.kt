package com.fitbuddy.videodowloader

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.fitbuddy.videodowloader.databinding.ActivityMainBinding
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

const val BASE_URL = "https://instagram-unofficial-api.heroku.com/unofficial/api/video"
const val ROOT_DIR = "My Story Saver/facebook/"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val videoUrl = MutableLiveData<String>(null)
    private val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSourceFactory(this, "exoplayer-sample")
    }
    private lateinit var exoplayer: SimpleExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, 1)
            }
        }
        videoUrl.observe(this) {
            if (it != null) {
                binding.downloadButton.isVisible = true
                exoplayer = SimpleExoPlayer.Builder(this).build()
                val mediaSource = buildMediaSource(Uri.parse(it))
                exoplayer.prepare(mediaSource)
                exoplayer.playWhenReady = true
                binding.exoplayerView.player = exoplayer
            } else {
                binding.downloadButton.isVisible = false
            }
        }
        binding.getVideoButton.setOnClickListener {
            val url = binding.urlEditText.text.toString()
            Toast.makeText(this@MainActivity, "Extracting video", Toast.LENGTH_SHORT).show()
            this.lifecycleScope.launchWhenStarted {
                withContext(Dispatchers.IO) {
                    try {
                        val fbDoc = Jsoup.connect(url).get()
                        runOnUiThread {
                            videoUrl.value = fbDoc.select("meta[property=\"og:video\"]")
                                .last().attr("content")
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Log.e("error", e.localizedMessage ?: "An error occurred")
                            Toast.makeText(
                                this@MainActivity,
                                e.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        binding.downloadButton.setOnClickListener {
            download(
                videoUrl.value!!,
                ROOT_DIR,
                this@MainActivity,
                "fb_" + System.currentTimeMillis() + ".mp4"
            )
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)

    private fun download(
        downloadPath: String,
        destPath: String,
        context: Context,
        fileName: String
    ) {
        try {
            val uri = Uri.parse(downloadPath)
            val request = DownloadManager.Request(uri)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(fileName)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                destPath + fileName
            )
            val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.e("error", e.localizedMessage ?: "An error occurred")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoplayer.release()
    }
}