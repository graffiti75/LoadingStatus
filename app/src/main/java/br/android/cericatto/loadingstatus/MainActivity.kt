package br.android.cericatto.loadingstatus

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

enum class Urls(val url: String) {
    GLIDE("https://github.com/bumptech/glide"),
    LOAD_APP("https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter"),
    RETROFIT("https://github.com/square/retrofit"),
    TEST("https://drive.google.com/u/0/uc?id=0B7-BCrhhCGUsR1JCV245aXp6QVk" +
        "&export=download&resourcekey=0-bAKkWj8f99fa_rMFanGPiQ")
}

class MainActivity : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager
    private var downloadID: Long = 0
    private var currentUrl = ""

    /**
     * Source:
     * https://www.droidcon.com/2022/03/21/notification-runtime-permission-android13/
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            hidePermissionButton()
        } else {
            // Explain to the user that the feature is unavailable because the features requires
            // a permission that the user has denied. At the same time, respect the user's decision.
            // Don't link to system settings in an effort to convince the user to change their
            // decision.
            Toast.makeText(this, getText(R.string.permission_explanation), Toast.LENGTH_LONG).show()
        }
    }

    //--------------------------------------------------
    // Activity Life Cycle
    //--------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndroidApiVersion()
        initButtonListeners()
        initNotifications()
    }

    override fun onResume() {
        super.onResume()
        if (notificationPermissionEnabled()) {
            hidePermissionButton()
        }
    }

    //--------------------------------------------------
    // Layout Methods
    //--------------------------------------------------

    private fun initButtonListeners() {
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

    private fun checkAndroidApiVersion() {
        val belowApi33 = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        var visibility = View.VISIBLE
        if (belowApi33) {
            visibility = View.GONE
        } else {
            if (notificationPermissionEnabled()) {
                visibility = View.GONE
            } else {
                customButton.visibility = View.GONE
            }
        }
        requestPermissionButton.visibility = visibility
    }

    private fun hidePermissionButton() {
        customButton.visibility = View.VISIBLE
        requestPermissionButton.visibility = View.GONE
    }

    //--------------------------------------------------
    // Notification Methods
    //--------------------------------------------------

    private fun initNotifications() {
        createChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name)
        )

        initNotificationManager()
        requestPermissionListener()
    }

    private fun initNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private fun requestPermissionListener() {
        val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
        requestPermissionButton.setOnClickListener {
            when {
                shouldShowRequestPermissionRationale(notificationPermission) -> {
                    showSnackBar()
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(notificationPermission)
                    }
                }
            }
        }
    }

    private fun notificationPermissionEnabled() : Boolean {
        val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
        val permissionStatus = ContextCompat.checkSelfPermission(this, notificationPermission)
        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }

    private fun showSnackBar() {
        Snackbar.make(
            findViewById(R.id.parentLayout),
            getText(R.string.notification_blocked),
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            // Responds to click on the action
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }.show()
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            .apply {
                setShowBadge(false)
            }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Time for breakfast"

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun sendNotification() {
        if (notificationPermissionEnabled()) {
            notificationManager.sendNotification(
                getText(R.string.download_completed).toString(),
                this
            )
        }
    }

    //--------------------------------------------------
    // Download Manager Methods
    //--------------------------------------------------

    /**
     * Source:
     * https://medium.com/@aungkyawmyint_26195/downloading-file-properly-in-android-d8cc28d25aca
     */
    private fun download() {
        val text = currentUrl.split("/")
        val title = text[text.size - 1]
        val request = DownloadManager.Request(Uri.parse(currentUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "udacity")
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
                        finishDownload = true
                        sendNotification()
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
}