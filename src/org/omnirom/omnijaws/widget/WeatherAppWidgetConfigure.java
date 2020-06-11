/*
 *  Copyright (C) 2017 The OmniROM Project
 *  Copyright (C) 2020 RevengeOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.omnijaws.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.ListView;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.app.ActionBar;

import org.omnirom.omnijaws.R;

public class WeatherAppWidgetConfigure extends PreferenceActivity {
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public static final String KEY_CALENDER_EVENTS = "show_calender_events";
    public static final String KEY_SHOW_GREETINGS = "show_greeting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID,
        // finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.weather_appwidget_configure);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ListView lv = getListView();
        lv.setDivider(new ColorDrawable(Color.TRANSPARENT));
        lv.setDividerHeight(0);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        initPreference(KEY_CALENDER_EVENTS, prefs.getBoolean(KEY_CALENDER_EVENTS + "_" + mAppWidgetId, true));
        initPreference(KEY_SHOW_GREETINGS, prefs.getBoolean(KEY_SHOW_GREETINGS + "_" + mAppWidgetId, true));

    }

    private void initPreference(String key, boolean value) {
        SwitchPreference b = (SwitchPreference) findPreference(key);
        b.setKey(key + "_" + String.valueOf(mAppWidgetId));
        b.setDefaultValue(value);
        b.setChecked(value);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(b.getKey(), value).commit();
    }

    public void handleOkClick(View v) {
        WeatherAppWidgetProvider.updateAfterConfigure(this, mAppWidgetId);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    public static void clearPrefs(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_CALENDER_EVENTS + "_" + id).commit();
        prefs.edit().remove(KEY_SHOW_GREETINGS + "_" + id).commit();
    }

    public static void remapPrefs(Context context, int oldId, int newId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean oldBoolean = prefs.getBoolean(KEY_CALENDER_EVENTS + "_" + oldId, false);	
        prefs.edit().putBoolean(KEY_CALENDER_EVENTS + "_" + newId, oldBoolean).commit();	
        prefs.edit().remove(KEY_CALENDER_EVENTS + "_" + oldId).commit();

        oldBoolean = prefs.getBoolean(KEY_SHOW_GREETINGS + "_" + oldId, false);	
        prefs.edit().putBoolean(KEY_SHOW_GREETINGS + "_" + newId, oldBoolean).commit();	
        prefs.edit().remove(KEY_SHOW_GREETINGS + "_" + oldId).commit();

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return false;
    }

}