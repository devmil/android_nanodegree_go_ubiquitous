/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.sunshine.common.WeatherDataTelegram;
import com.example.android.sunshine.app.events.WeatherDataUpdatedEvent;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchface extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static String TAG = SunshineWatchface.class.getSimpleName();

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime = GregorianCalendar.getInstance(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundDarkPaint;
        Paint mBackgroundLightPaint;
        Paint mTextPaintLightBig;
        Paint mTextPaintLightSmall;
        Paint mTextPaintDarkBig;
        Paint mTextPaintDarkSmall;

        boolean mAmbient;

        Calendar mTime;

        float mXOffset;
        float mYOffset;

        WeatherDataTelegram mCurrentWeatherData;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            EventBus.getDefault().register(this);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchface.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchface.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundDarkPaint = new Paint();
            mBackgroundDarkPaint.setColor(resources.getColor(R.color.digital_background_dark));
            mBackgroundLightPaint = new Paint();
            mBackgroundLightPaint.setColor(resources.getColor(R.color.digital_background_light));

            mTextPaintLightBig = new Paint();
            mTextPaintLightBig.setTextAlign(Paint.Align.CENTER);
            mTextPaintLightBig = createTextPaint(resources.getColor(R.color.digital_text_light));
            mTextPaintLightSmall = new Paint();
            mTextPaintLightSmall.setTextAlign(Paint.Align.CENTER);
            mTextPaintLightSmall = createTextPaint(resources.getColor(R.color.digital_text_light));
            mTextPaintDarkBig = new Paint();
            mTextPaintDarkBig = createTextPaint(resources.getColor(R.color.digital_text_dark));
            mTextPaintDarkSmall = new Paint();
            mTextPaintDarkSmall = createTextPaint(resources.getColor(R.color.digital_text_dark));

            mTime = GregorianCalendar.getInstance();

            mCurrentWeatherData = PersistenceHelper.loadTelegram(getApplicationContext());
        }

        @Override
        public void onDestroy() {
            EventBus.getDefault().unregister(this);

            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        public void onEventMainThread(WeatherDataUpdatedEvent event) {
            Log.d(TAG, "received data changed event => updating");
            mCurrentWeatherData = PersistenceHelper.loadTelegram(getApplicationContext());
            invalidate();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime = GregorianCalendar.getInstance(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchface.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchface.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchface.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            float textSizeLightSmall = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_light_small_round : R.dimen.digital_text_size_light_small);
            float textSizeLightBig = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_light_big_round : R.dimen.digital_text_size_light_big);
            float textSizeDarkSmall = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_dark_small_round : R.dimen.digital_text_size_dark_small);
            float textSizeDarkBig = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_dark_big_round : R.dimen.digital_text_size_dark_big);

            mTextPaintLightSmall.setTextSize(textSizeLightSmall);
            mTextPaintLightBig.setTextSize(textSizeLightBig);
            mTextPaintDarkSmall.setTextSize(textSizeDarkSmall);
            mTextPaintDarkBig.setTextSize(textSizeDarkBig);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaintLightBig.setAntiAlias(!inAmbientMode);
                    mTextPaintLightSmall.setAntiAlias(!inAmbientMode);
                    mTextPaintDarkBig.setAntiAlias(!inAmbientMode);
                    mTextPaintDarkSmall.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        //instantiate it here even if it only gets used locally to avoid object instantiation in onDraw
        private Rect lTimeTextBounds = new Rect();
        private Rect lDateTextBounds = new Rect();
        private DateFormat lDateFormat = SimpleDateFormat.getDateInstance();
        private Rect lMiddleStripeRect = new Rect();

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            //1. calculate relevant sizes
            mTime = GregorianCalendar.getInstance();
            int hour = mTime.get(Calendar.HOUR_OF_DAY);
            int minutes = mTime.get(Calendar.MINUTE);

            String timeText = String.format("%d:%02d", hour, minutes);
            mTextPaintLightBig.getTextBounds(timeText, 0, timeText.length(), lTimeTextBounds);

            String dateText = lDateFormat.format(mTime.getTime());
            mTextPaintLightSmall.getTextBounds(dateText, 0, dateText.length(), lDateTextBounds);

            getMiddleStripeRect(bounds, lTimeTextBounds.height(), lDateTextBounds.height(), lMiddleStripeRect);

            drawBackgrounds(canvas, bounds, lMiddleStripeRect);

            drawTime(canvas, lMiddleStripeRect, timeText, lTimeTextBounds);
            drawDate(canvas, lMiddleStripeRect, lTimeTextBounds, dateText, lDateTextBounds);

            if(mCurrentWeatherData != null) {
                canvas.drawText(Integer.toString(mCurrentWeatherData.getWeatherConditionId()), mXOffset, bounds.bottom - 30, mTextPaintDarkBig);
            } else {
                canvas.drawText("no data", mXOffset, bounds.bottom - 30, mTextPaintDarkBig);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private static final int TIME_DATE_MARGIN_PX = 10;
        private static final int MIDDLE_STRIPE_MARGIN_PX = 20;

        private void getMiddleStripeRect(Rect canvasBounds, int timeTextHeight, int dateTextHeight, Rect outRect) {
            int centerY = canvasBounds.centerY();
            int middleStripeHeight = timeTextHeight + dateTextHeight + TIME_DATE_MARGIN_PX + 2 * MIDDLE_STRIPE_MARGIN_PX;
            int middleStripeTop = centerY - middleStripeHeight / 2;
            int middleStripeBottom = centerY + middleStripeHeight / 2;

            outRect.left= 0;
            outRect.top = middleStripeTop;
            outRect.right = canvasBounds.width();
            outRect.bottom = middleStripeBottom;
        }

        private void drawBackgrounds(Canvas canvas, Rect canvasBounds, Rect middleStripeRect) {

            canvas.drawRect(0, 0, canvasBounds.width(), canvasBounds.height(), mBackgroundLightPaint);

            canvas.drawRect(middleStripeRect, mBackgroundDarkPaint);
        }

        private void drawTime(Canvas canvas, Rect middleStripeRect, String timeText, Rect timeTextBounds) {

            int left = (middleStripeRect.width() - timeTextBounds.width()) / 2; //centered
            int top = middleStripeRect.top + MIDDLE_STRIPE_MARGIN_PX + timeTextBounds.height();

            canvas.drawText(timeText, left, top, mTextPaintLightBig);
        }

        private void drawDate(Canvas canvas, Rect middleStripeRect, Rect timeTextBounds, String dateText, Rect dateTextBounds) {
            int left = (middleStripeRect.width() - dateTextBounds.width()) / 2; //centered
            int top = MIDDLE_STRIPE_MARGIN_PX + middleStripeRect.top + timeTextBounds.height() + TIME_DATE_MARGIN_PX + dateTextBounds.height();

            canvas.drawText(dateText, left, top, mTextPaintLightSmall);
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchface.Engine> mWeakReference;

        public EngineHandler(SunshineWatchface.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchface.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
