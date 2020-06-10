/*
 * Copyright (C) 2017 The OmniROM Project
 * Copyright (C) 2020 RevengeOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnijaws.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.Context;

import org.omnirom.omnijaws.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class WeatherAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherAppWidgetProvider";
    private static final boolean LOGGING = false;
    private static final String WEATHER_UPDATE = "org.omnirom.omnijaws.WEATHER_UPDATE";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (LOGGING) {
            Log.i(TAG, "onEnabled");
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (LOGGING) {
            Log.i(TAG, "onDisabled");
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int id : appWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onDeleted: " + id);
            }
            WeatherAppWidgetConfigure.clearPrefs(context, id);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        int i = 0;
        for (int oldWidgetId : oldWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onRestored " + oldWidgetId + " " + newWidgetIds[i]);
            }
            WeatherAppWidgetConfigure.remapPrefs(context, oldWidgetId, newWidgetIds[i]);
            i++;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (LOGGING) {
            Log.i(TAG, "onReceive: " + action);
        }
        if (action.equals(WEATHER_UPDATE)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            updateAllWeather(context);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        if (LOGGING) {
            Log.i(TAG, "onAppWidgetOptionsChanged");
        }
        updateWeather(context, appWidgetManager, appWidgetId);
    }

    public static void updateAfterConfigure(Context context, int appWidgetId) {
        if (LOGGING) {
            Log.i(TAG, "updateAfterConfigure");
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateWeather(context, appWidgetManager, appWidgetId);
    }

    public static void updateAllWeather(Context context) {
        if (LOGGING) {
            Log.i(TAG, "updateAllWeather at = " + new Date());
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            ComponentName componentName = new ComponentName(context, WeatherAppWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            for (int appWidgetId : appWidgetIds) {
                updateWeather(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static void updateWeather(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        if (LOGGING) {
            Log.i(TAG, "updateWeather " + appWidgetId);
        }
        OmniJawsClient weatherClient = new OmniJawsClient(context);
        weatherClient.queryWeather();
        final ContentResolver resolver = context.getContentResolver();
        String iconPack = Settings.System.getString(resolver,Settings.System.OMNIJAWS_WEATHER_ICON_PACK);
        if (!TextUtils.isEmpty(iconPack)) {
            weatherClient.loadIconPackage(iconPack);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean calenderEvents = prefs.getBoolean(WeatherAppWidgetConfigure.KEY_CALENDER_EVENTS + "_" + appWidgetId, true);

        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.weather_appwidget);

        Uri.Builder timeUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time");
        ContentUris.appendId(timeUri, System.currentTimeMillis());
        Intent calIntent = new Intent(Intent.ACTION_VIEW)
                .setData(timeUri.build())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        widget.setOnClickPendingIntent(R.id.date_text,
                PendingIntent.getActivity(context, 0, calIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        OmniJawsClient.WeatherInfo weatherData = weatherClient.getWeatherInfo();
        if (weatherData == null) {
            Log.e(TAG, "updateWeather weatherData == null");
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
            return;
        }
        if (LOGGING) {
            Log.i(TAG, "updateWeather " + weatherData.toString());
        }

        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minHeight = context.getResources().getDimensionPixelSize(R.dimen.weather_widget_height);
        int minWidth = context.getResources().getDimensionPixelSize(R.dimen.weather_widget_width);

        int currentHeight = minHeight;
        int currentWidth = minWidth;

        if (newOptions != null) {
            currentHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight);
            currentWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth);
        }

        WeatherAppWidgetEvents events = new WeatherAppWidgetEvents(context);

        Drawable d = weatherClient.getWeatherConditionImage(weatherData.conditionCode);
        BitmapDrawable bd = overlay(context.getResources(), d);

        if (events.showEvent() && calenderEvents) {
            widget.setViewVisibility(R.id.event_line, View.VISIBLE);
            widget.setViewVisibility(R.id.condition_line, View.GONE);
            widget.setTextViewText(R.id.event_text, events.formatedEvent());
            widget.setTextViewText(R.id.event_duration, events.EventDuration());
            widget.setImageViewBitmap(R.id.event_current_image, bd.getBitmap());
            widget.setTextViewText(R.id.event_current_text, weatherData.temp + weatherData.tempUnits);
        } else {
            widget.setViewVisibility(R.id.event_line, View.GONE);
            widget.setViewVisibility(R.id.condition_line, View.VISIBLE);
            widget.setImageViewBitmap(R.id.current_image, bd.getBitmap());
            widget.setTextViewText(R.id.current_text, weatherData.temp + weatherData.tempUnits);
        }

        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private static BitmapDrawable overlay(Resources resources, Drawable image) {
        if (image instanceof VectorDrawable) {
            image = applyTint(image);
        }
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final float density = resources.getDisplayMetrics().density;
        final int imageWidth = image.getIntrinsicWidth();
        final int imageHeight = image.getIntrinsicHeight();
        final int height = imageHeight;
        final int width = imageWidth;

        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        image.setBounds(0, 0, imageWidth, imageHeight);
        image.draw(canvas);

        return shadow(resources, bmp);
    }

    private static Drawable applyTint(Drawable icon) {
        icon = icon.mutate();
        icon.setTint(Color.WHITE);
        return icon;
    }

    public static BitmapDrawable shadow(Resources resources, Bitmap b) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        BlurMaskFilter blurFilter = new BlurMaskFilter(5, BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setMaskFilter(blurFilter);

        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bmResult);
        canvas.drawBitmap(b, 0, 0, null);

        return new BitmapDrawable(resources, bmResult);
    }
}
