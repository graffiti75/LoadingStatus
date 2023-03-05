package br.android.cericatto.loadingstatus

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SnoozeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val detailsIntent = Intent(context as Activity, DetailActivity::class.java)
        detailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(detailsIntent)
//        Toast.makeText(context, "---=---", Toast.LENGTH_LONG).show()
    }
}