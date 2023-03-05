package br.android.cericatto.loadingstatus

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat

// Notification ID.
private const val NOTIFICATION_ID = 0
private const val REQUEST_CODE = 0
//private val FLAGS = 0

// TODO: Step 1.1 extension function to send messages (GIVEN)
/**
 * Builds and delivers the notification.
 *
 * @param messageBody the message to be showed.
 * @param applicationContext activity context.
 */
fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {
    // Create the content intent for the notification, which launches this activity
	val contentIntent = Intent(applicationContext, MainActivity::class.java)

	var intentFlagTypeUpdateCurrent = PendingIntent.FLAG_UPDATE_CURRENT
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
		intentFlagTypeUpdateCurrent = PendingIntent.FLAG_IMMUTABLE
	}

	val contentPendingIntent = PendingIntent.getActivity(
		applicationContext,
		NOTIFICATION_ID,
		contentIntent,
		intentFlagTypeUpdateCurrent
	)

	val eggImage = BitmapFactory.decodeResource(
		applicationContext.resources,
		R.drawable.cooked_egg
	)
	val bigPicStyle = NotificationCompat.BigPictureStyle()
		.bigPicture(eggImage)
		.bigLargeIcon(null)

	var intentFlagTypeFlags = 0
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
		intentFlagTypeFlags = PendingIntent.FLAG_IMMUTABLE
	}
//	val snoozeIntent = Intent(applicationContext, SnoozeReceiver::class.java)
//	val snoozePendingIntent: PendingIntent = PendingIntent.getBroadcast(
//		applicationContext,
//		REQUEST_CODE,
//		snoozeIntent,
//		intentFlagTypeFlags
//	)

    // Build the notification
	val builder = NotificationCompat.Builder(
		applicationContext,
		applicationContext.getString(R.string.notification_channel_id)
	)

		.setSmallIcon(R.drawable.cooked_egg)
		.setContentTitle(applicationContext.getString(R.string.notification_title))
		.setContentText(messageBody)

		.setContentIntent(contentPendingIntent)
		.setAutoCancel(true)
	
		.setStyle(bigPicStyle)
		.setLargeIcon(eggImage)

//		.addAction(
//			R.drawable.egg_icon,
//			applicationContext.getString(R.string.snooze),
//			snoozePendingIntent
//		)

		.setPriority(NotificationCompat.PRIORITY_LOW)
	notify(NOTIFICATION_ID, builder.build())
}

fun NotificationManager.cancelNotifications() {
	cancelAll()
}