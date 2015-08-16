package com.example.android.sunshine.app.wear;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.common.Protocol;
import com.example.android.sunshine.common.WeatherDataTelegram;
import com.example.android.sunshine.common.WeatherUnit;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;

public class WearUpdateHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = WearUpdateHelper.class.getSimpleName();

    private static final String[] WEAR_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    private GoogleApiClient mGoogleApiClient;
    private WeatherDataTelegram mLastSentTelegram;

    private WeakReference<Context> mContextRef;

    public WearUpdateHelper(Context context) {
        mContextRef = new WeakReference<>(context);

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(mGoogleApiClient != null
                && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void doUpdate() {
        Log.i(LOG_TAG, "Updating wear");

        if(mGoogleApiClient == null
                || !mGoogleApiClient.isConnected()) {
            Log.i(LOG_TAG, "No Google API connection => returning");
            return;
        }

        Context context = mContextRef.get();
        if(context == null) {
            return;
        }

        String locationQuery = Utility.getPreferredLocation(context);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        // we'll query our contentProvider, as always
        Cursor cursor = context.getContentResolver().query(weatherUri, WEAR_WEATHER_PROJECTION, null, null, null);

        Log.i(LOG_TAG, "Querying weather data for wear");

        if (cursor.moveToFirst()) {
            Log.i(LOG_TAG, "Got data");
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);

            boolean sendUpdate = true;

            WeatherDataTelegram telegram = new WeatherDataTelegram(
                    WeatherUnit.Celcius,
                    weatherId,
                    high,
                    low
            );

            //just to be sure not to use more power than needed
            if (telegram.equals(mLastSentTelegram)) {
                sendUpdate = false;
            }

            if(sendUpdate) {
                Log.i(LOG_TAG, "sending update to wear");
                PutDataMapRequest mapRequest = PutDataMapRequest.create(Protocol.PATH_WEATHER_DATA);
                Protocol.addTelegramToData(telegram, mapRequest.getDataMap());
                PutDataRequest request = mapRequest.asPutDataRequest();

                Wearable.DataApi.putDataItem(mGoogleApiClient, request);

                mLastSentTelegram = telegram;
            } else {
                Log.i(LOG_TAG, "no wear update needed");
            }

        }
        cursor.close();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
