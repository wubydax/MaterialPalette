package com.wubydax.materialpalette.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by Anna Berkovitch on 02/04/2016.
 * Remote view binding service
 */
public class PaletteWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new PaletteWidgetViewFactory(getApplicationContext());
    }
}
