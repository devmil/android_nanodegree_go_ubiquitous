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
}
