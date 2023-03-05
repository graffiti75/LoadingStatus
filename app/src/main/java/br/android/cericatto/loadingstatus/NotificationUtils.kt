package br.android.cericatto.loadingstatus

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat

private const val NOTIFICATION_ID = 0

/**
 * Builds and delivers the notification.
 *
 * @param messageBody the message to be showed.
 * @param applicationContext activity context.
 */
fun NotificationManager.sendNotification(
	messageBody: String,
	applicationContext: Context,
	statusPendingIntent: PendingIntent
) {
	val bigImage = BitmapFactory.decodeResource(
		applicationContext.resources,
		R.drawable.ic_baseline_cloud_download_24
	)
	val bigPicStyle = NotificationCompat.BigPictureStyle()
		.bigPicture(bigImage)
		.bigLargeIcon(null)

	// Build the notification
	val builder = NotificationCompat.Builder(
		applicationContext,
		applicationContext.getString(R.string.notification_channel_id)
	)
	.setSmallIcon(R.drawable.ic_assistant_black_24dp)
	.setContentTitle(applicationContext.getString(R.string.notification_title))
	.setContentText(messageBody)
	.setContentIntent(statusPendingIntent)
	.setAutoCancel(true)
	.setStyle(bigPicStyle)
	.setLargeIcon(bigImage)
	.addAction(
		R.drawable.ic_baseline_cloud_download_24,
		applicationContext.getString(R.string.details),
		statusPendingIntent
	)
	.setPriority(NotificationCompat.PRIORITY_LOW)

	notify(NOTIFICATION_ID, builder.build())
}
