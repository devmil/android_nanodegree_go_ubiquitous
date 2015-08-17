package com.example.android.sunshine.common;

public class WeatherDataTelegram {
    private WeatherUnit mWeatherUnit;
    private int mWeatherConditionId;
    private double mTemperatureMax;
    private double mTemperatureMin;

    public WeatherDataTelegram(WeatherUnit weatherUnit, int weatherConditionId, double temperatureMax, double temperatureMin) {
        this.mWeatherUnit = weatherUnit;
        this.mWeatherConditionId = weatherConditionId;
        this.mTemperatureMax = temperatureMax;
        this.mTemperatureMin = temperatureMin;
    }

    public WeatherUnit getWeatherUnit() {
        return mWeatherUnit;
    }

    public int getWeatherConditionId() {
        return mWeatherConditionId;
    }

    public double getTemperatureMax() {
        return mTemperatureMax;
    }

    public double getTemperatureMin() {
        return mTemperatureMin;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(!(o instanceof WeatherDataTelegram)) {
            return false;
        }
        WeatherDataTelegram castedObj = (WeatherDataTelegram)o;
        return mWeatherUnit.getValue() == castedObj.mWeatherUnit.getValue()
                && mWeatherConditionId == castedObj.mWeatherConditionId
                && mTemperatureMax == castedObj.mTemperatureMax
                && mTemperatureMin == castedObj.mTemperatureMin;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result |= mWeatherUnit.getValue();
        result |= mWeatherConditionId;
        result |= Double.valueOf(mTemperatureMax).hashCode();
        result |= Double.valueOf(mTemperatureMin).hashCode();

        return result;
    }
}
