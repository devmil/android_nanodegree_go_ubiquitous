package com.example.android.sunshine.common;

public enum WeatherUnit {
    Celcius(1, "°C"),
    Fahrenheit(2, "°F");

    private int mValue;
    private String mSuffix;

    WeatherUnit(int value, String suffix) {
        mValue = value;
        mSuffix = suffix;
    }

    public int getValue() {
        return mValue;
    }

    public String getmSuffix() {
        return mSuffix;
    }

    public static WeatherUnit fromValue(int value) {
        switch(value) {
            case 1:
                return Celcius;
            case 2:
                return Fahrenheit;
            default:
                return Celcius;
        }
    }
}
