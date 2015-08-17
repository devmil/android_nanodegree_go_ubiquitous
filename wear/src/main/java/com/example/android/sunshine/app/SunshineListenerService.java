package com.example.android.sunshine.app;

import android.os.Binder;
import android.util.Log;

import com.example.android.sunshine.common.Protocol;
import com.example.android.sunshine.common.WeatherDataTelegram;
import com.example.android.sunshine.app.events.WeatherDataUpdatedEvent;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

import de.greenrobot.event.EventBus;

public class SunshineListenerService extends WearableListenerService {

    private static final String TAG = SunshineListenerService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        long token = Binder.clearCallingIdentity();

        try {

            final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

            for(DataEvent event : events) {
                if(event.getType() == DataEvent.TYPE_CHANGED) {
                    String path = event.getDataItem().getUri().getPath();
                    if(Protocol.PATH_WEATHER_DATA.equals(path)) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        persistDataAndNotify(dataMapItem);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void persistDataAndNotify(DataMapItem dataMapItem) {
        WeatherDataTelegram telegram = Protocol.telegramFromData(dataMapItem.getDataMap());

        PersistenceHelper.persistTelegram(this, telegram);

        EventBus.getDefault().post(new WeatherDataUpdatedEvent());
    }
}
