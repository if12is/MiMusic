package it.vfsfitvnm.vimusic.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import it.vfsfitvnm.vimusic.MainActivity
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.utils.intent

class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, "MiMusic", "", true))
        }
    }

    companion object {
        fun update(context: Context, title: String?, artist: String?, playing: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildRemoteViews(context, title ?: "MiMusic", artist.orEmpty(), playing)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildRemoteViews(
            context: Context,
            title: String,
            artist: String,
            playing: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_play,
                if (playing) R.drawable.pause else R.drawable.play
            )

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 0, context.intent<MainActivity>(), flags)
            )
            views.setOnClickPendingIntent(
                R.id.widget_play,
                PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(if (playing) "it.vfsfitvnm.vimusic.pause" else "it.vfsfitvnm.vimusic.play")
                        .setPackage(context.packageName),
                    flags
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                PendingIntent.getBroadcast(
                    context,
                    2,
                    Intent("it.vfsfitvnm.vimusic.next").setPackage(context.packageName),
                    flags
                )
            )
            return views
        }
    }
}
