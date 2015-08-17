package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.android.sunshine.common.WeatherDataTelegram;
import com.example.android.sunshine.common.WeatherUnit;

public final class PersistenceHelper {

    private static String PREF_NAME = "sunshinewatchfacestate";

    private static String KEY_CONDITION = "WEATHER_CONDITION";
    private static String KEY_TEMP_MIN = "WEATHER_TEMP_MIN";
    private static String KEY_TEMP_MAX = "WEATHER_TEMP_MAX";
    private static String KEY_TEMP_UNIT = "WEATHER_TEMP_UNIT";

    private PersistenceHelper() {
    }

    public static void persistTelegram(Context context, WeatherDataTelegram telegram) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putInt(KEY_CONDITION, telegram.getWeatherConditionId())
                .putFloat(KEY_TEMP_MAX, (float)telegram.getTemperatureMax())
                .putFloat(KEY_TEMP_MIN, (float)telegram.getTemperatureMin())
                .putInt(KEY_TEMP_UNIT, telegram.getWeatherUnit().getValue())
                .apply();
    }

    public static WeatherDataTelegram loadTelegram(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if(!prefs.contains(KEY_CONDITION)) {
            return null;
        }

        return new WeatherDataTelegram(
                WeatherUnit.fromValue(prefs.getInt(KEY_TEMP_UNIT, 0)),
                prefs.getInt(KEY_CONDITION, -1),
                prefs.getFloat(KEY_TEMP_MAX, 0),
                prefs.getFloat(KEY_TEMP_MIN, 0)
        );
    }
}
