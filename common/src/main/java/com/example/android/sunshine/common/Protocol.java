package com.example.android.sunshine.common;

import com.google.android.gms.wearable.DataMap;

import java.util.Calendar;

public final class Protocol {

    private Protocol() {
    }

    public static String PATH_WEATHER_DATA = "/weatherdata";

    private static String KEY_CONDITION = "WEATHER_CONDITION";
    private static String KEY_TEMP_MIN = "WEATHER_TEMP_MIN";
    private static String KEY_TEMP_MAX = "WEATHER_TEMP_MAX";
    private static String KEY_TEMP_UNIT = "WEATHER_TEMP_UNIT";

    //TODO: delete
    private static String DUMMY_SALT_DATA = "DUMMY_SALT_DATA";

    public static WeatherDataTelegram telegramFromData(DataMap dm) {
        int conditionId = dm.getInt(KEY_CONDITION);
        double tempMin = dm.getDouble(KEY_TEMP_MIN);
        double tempMax = dm.getDouble(KEY_TEMP_MAX);
        int unitValue = dm.getInt(KEY_TEMP_UNIT);

        return new WeatherDataTelegram(
                WeatherUnit.fromValue(unitValue),
                conditionId,
                tempMax,
                tempMin
        );
    }

    public static void addTelegramToData(WeatherDataTelegram telegram, DataMap dm) {
        dm.putInt(KEY_CONDITION, telegram.getWeatherConditionId());
        dm.putDouble(KEY_TEMP_MIN, telegram.getTemperatureMin());
        dm.putDouble(KEY_TEMP_MAX, telegram.getTemperatureMax());
        dm.putInt(KEY_TEMP_UNIT, telegram.getWeatherUnit().getValue());
        dm.putLong(DUMMY_SALT_DATA, Calendar.getInstance().getTimeInMillis());
    }
}
