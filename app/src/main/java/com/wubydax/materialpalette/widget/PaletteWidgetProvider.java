package com.wubydax.materialpalette.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.wubydax.materialpalette.MainActivity;
import com.wubydax.materialpalette.R;

/**
 * Created by Anna Berkovitch on 02/04/2016.
 * widget provider for palettes collection widget
 */
public class PaletteWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_main_layout);
            Intent openWidgetServiceIntent = new Intent(context, PaletteWidgetService.class);
            openWidgetServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            openWidgetServiceIntent.setData(Uri.parse(openWidgetServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            Intent templateIntent = new Intent(context, PaletteWidgetProvider.class);
            templateIntent.setAction(MainActivity.WIDGET_ACTION);
            PendingIntent itemTemplateIntent = PendingIntent.getBroadcast(context, 0, templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.widgetGrid, itemTemplateIntent);
            remoteViews.setRemoteAdapter(R.id.widgetGrid, openWidgetServiceIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WidgetProvider", "onReceive triggered");
        if(intent.getAction().equals(MainActivity.WIDGET_ACTION)) {
            intent.setClass(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        super.onReceive(context, intent);

    }
}
