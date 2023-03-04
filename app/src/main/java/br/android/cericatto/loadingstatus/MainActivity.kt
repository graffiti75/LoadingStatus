package br.android.cericatto.loadingstatus

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.activity_main.*

enum class Urls(val url: String) {
    GLIDE("https://github.com/bumptech/glide"),
    LOAD_APP("https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter"),
    RETROFIT("https://github.com/square/retrofit"),
    TEST("https://drive.google.com/u/0/uc?id=0B7-BCrhhCGUsR1JCV245aXp6QVk&export=download&resourcekey=0-bAKkWj8f99fa_rMFanGPiQ")
}

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0
    private var currentUrl = ""

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    companion object {
        private const val CHANNEL_ID = "channelId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        customButton.setOnClickListener {
            checkRadioButtons()
        }

        radioGroup.setOnCheckedChangeListener {  group, checkedId ->
            when (checkedId) {
                R.id.firstRadioButton -> {
                    currentUrl = Urls.GLIDE.url
                }
                R.id.secondRadioButton -> {
                    currentUrl = Urls.LOAD_APP.url
                }
                R.id.thirdRadioButton -> {
                    currentUrl = Urls.RETROFIT.url
                }
            }
        }
    }

    /*
    override fun onDestroy() {
        super.onDestroy()
        // using broadcast method
        unregisterReceiver(onDownloadComplete)
    }
     */

    /*
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            // Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(this@MainActivity, "Download Completed", Toast.LENGTH_SHORT).show()
            }
        }
    }
     */

    private fun checkRadioButtons() {
        val first = firstRadioButton.isChecked
        val second = secondRadioButton.isChecked
        val third = thirdRadioButton.isChecked
        if (!first && !second && !third) {
            Toast.makeText(this, getString(R.string.select_radio_button), Toast.LENGTH_LONG).show()
        } else {
            val isConnected = isNetworkConnected()
            updateConnectionStatus(isConnected)
            if (isConnected) {
                download()
            }
        }
    }

    /**
     * Source:
     * https://medium.com/@aungkyawmyint_26195/downloading-file-properly-in-android-d8cc28d25aca
     */
    private fun download() {
//        var fileName = URL.substring(URL.lastIndexOf('/') + 1)
//        fileName = fileName.substring(0, 1).uppercase(Locale.getDefault()) + fileName.substring(1)
//        val file: File = Util.createDocumentFile(fileName, this)

        val text = currentUrl.split("/")
        val title = text[text.size - 1]
        val request = DownloadManager.Request(Uri.parse(currentUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "udacity")
//            .setDestinationUri(Uri.fromFile(file))
            .setTitle(title)
            .setDescription(getString(R.string.app_description))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        // Enqueue puts the download request in the queue.
        downloadID = downloadManager.enqueue(request)

        // Using query method
        processDownload(downloadManager)
    }

    private fun processDownload(downloadManager: DownloadManager) {
        var finishDownload = false
        var progress = 0
        while (!finishDownload) {
            val cursor: Cursor = downloadManager.query(DownloadManager.Query().setFilterById(
                downloadID)
            )
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_FAILED -> {
                        finishDownload = true
                    }
                    DownloadManager.STATUS_PAUSED -> {}
                    DownloadManager.STATUS_PENDING -> {}
                    DownloadManager.STATUS_RUNNING -> {
                        progress = statusRunning(cursor, progress)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 100
                        // If you use AsyncTask: publishProgress(100);
                        finishDownload = true
                        Toast.makeText(
                            this@MainActivity, "Download Completed", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun statusRunning(cursor: Cursor, progress: Int): Int {
        var currentProgress = progress
        Log.i("udacity", "----- Running -----")
        val columnTotalSizeBytes = DownloadManager.COLUMN_TOTAL_SIZE_BYTES
        val columnBytesDownloadedSoFar = DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
        val total = cursor.getLong(cursor.getColumnIndex(columnTotalSizeBytes))
        Log.i("udacity", "Total: $total")
        if (total >= 0) {
            val downloaded = cursor.getLong(cursor.getColumnIndex(columnBytesDownloadedSoFar))
            Log.i("udacity", "Downladed: $downloaded")
            currentProgress = (downloaded * 100L / total).toInt()
            // If you use downloadmanger in async task, here you can use like this to display
            // progress. Don't forget to do the division in long to get more digits rather than
            // double.
            // publishProgress((int) ((downloaded * 100L) / total));
            Log.i("udacity", "Progress: $progress")
        }
        return currentProgress
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (connected) {
            customButton.visibility = View.VISIBLE
            radioGroup.visibility = View.VISIBLE
            downloadImageView.visibility = View.VISIBLE
            connectionImageView.visibility = View.GONE
        } else {
            customButton.visibility = View.GONE
            radioGroup.visibility = View.GONE
            downloadImageView.visibility = View.GONE
            connectionImageView.visibility = View.VISIBLE
        }
    }
}