package br.android.cericatto.loadingstatus

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.android.cericatto.loadingstatus.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_CODE = 10
        const val FILE_NAME = "file_name"
        const val STATUS = "status"
    }

    private lateinit var notificationManager: NotificationManager
    private var downloadID: Long = 0
    private var currentUrl = ""
    private var status = ""

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
            showTopSnackBar(getString(R.string.permission_explanation))
        }
    }

    //--------------------------------------------------
    // Activity Life Cycle
    //--------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        if (radioButtonsAreNotChecked()) {
            binding.customButton.isEnabled = false
            showTopSnackBar()
        }

        binding.customButton.setOnClickListener {
            checkDownloadConditions()
        }

        binding.radioGroup.setOnCheckedChangeListener {  group, checkedId ->
            var checked = false
            when (checkedId) {
                R.id.firstRadioButton -> {
                    currentUrl = UrlState.GLIDE.url
                    checked = true
                }
                R.id.secondRadioButton -> {
                    currentUrl = UrlState.LOAD_APP.url
                    checked = true
                }
                R.id.thirdRadioButton -> {
                    currentUrl = UrlState.RETROFIT.url
                    checked = true
                }
            }
            if (checked)
                binding.customButton.isEnabled = true
        }
    }

    private fun radioButtonsAreNotChecked() : Boolean {
        val first = binding.firstRadioButton.isChecked
        val second = binding.secondRadioButton.isChecked
        val third = binding.thirdRadioButton.isChecked
        return !first && !second && !third
    }

    private fun showTopSnackBar() {
        showTopSnackBar(getString(R.string.select_radio_button))
    }

    private fun showTopSnackBar(text: String) {
        val snack = Snackbar.make(
            findViewById(R.id.parentLayout),
            text,
            Snackbar.LENGTH_LONG
        )
        val view = snack.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        view.setBackgroundColor(getColor(R.color.grey_500))
        snack.show()
    }

    private fun checkDownloadConditions() {
        if (radioButtonsAreNotChecked()) {
            showTopSnackBar()
        } else {
            val isConnected = isNetworkConnected()
            updateConnectionStatus(isConnected)
            if (isConnected) {
                binding.customButton.isEnabled = false
                download()
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        binding.apply {
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

    private fun checkAndroidApiVersion() {
        val belowApi33 = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        var visibility = View.VISIBLE
        if (belowApi33) {
            visibility = View.GONE
        } else {
            if (notificationPermissionEnabled()) {
                visibility = View.GONE
            } else {
                binding.customButton.visibility = View.GONE
            }
        }
        binding.requestPermissionButton.visibility = visibility
    }

    private fun hidePermissionButton() {
        binding.apply {
            customButton.visibility = View.VISIBLE
            requestPermissionButton.visibility = View.GONE
        }
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
        binding.requestPermissionButton.setOnClickListener {
            when {
                shouldShowRequestPermissionRationale(notificationPermission) -> {
                    showSettingsSnackBar()
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

    private fun showSettingsSnackBar() {
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
                getString(R.string.download_completed),
                applicationContext,
                createPendingIntent()
            )
        }
    }

    private fun createPendingIntent() : PendingIntent {
        var intentFlagTypeUpdateCurrent = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentFlagTypeUpdateCurrent = PendingIntent.FLAG_IMMUTABLE
        }

        val contentIntent = Intent(applicationContext, DetailActivity::class.java)
        contentIntent.putExtra(FILE_NAME, getFileName())
        contentIntent.putExtra(STATUS, status)
        return PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            contentIntent,
            intentFlagTypeUpdateCurrent
        )
    }

    //--------------------------------------------------
    // Download Manager Methods
    //--------------------------------------------------

    private fun getFileName() : String {
        val text = currentUrl.split("/")
        return text[text.size - 1]
    }

    /**
     * Source:
     * https://medium.com/@aungkyawmyint_26195/downloading-file-properly-in-android-d8cc28d25aca
     */
    private fun download() {
        val request = DownloadManager.Request(Uri.parse(currentUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "udacity")
            .setTitle(getFileName())
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
                val columnStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when (columnStatus) {
                    DownloadManager.STATUS_FAILED -> {
                        finishDownload = true
                        status = getString(R.string.status_failed)
                        binding.customButton.isEnabled = true
                    }
                    DownloadManager.STATUS_PAUSED -> {}
                    DownloadManager.STATUS_PENDING -> {}
                    DownloadManager.STATUS_RUNNING -> {
                        progress = statusRunning(cursor, progress)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 100
                        finishDownload = true
                        status = getString(R.string.status_success)
                        binding.customButton.isEnabled = true
                        sendNotification()
                    }
                }
            }
        }
    }

    private fun statusRunning(cursor: Cursor, progress: Int): Int {
        var currentProgress = progress
        val columnTotalSizeBytes = DownloadManager.COLUMN_TOTAL_SIZE_BYTES
        val columnBytesDownloadedSoFar = DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
        val total = cursor.getLong(cursor.getColumnIndex(columnTotalSizeBytes))
        if (total >= 0) {
            val downloaded = cursor.getLong(cursor.getColumnIndex(columnBytesDownloadedSoFar))
            currentProgress = (downloaded * 100L / total).toInt()
        }
        return currentProgress
    }
}