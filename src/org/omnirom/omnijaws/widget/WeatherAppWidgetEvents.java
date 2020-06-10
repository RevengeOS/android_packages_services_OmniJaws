/*
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

import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.content.ContentResolver;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WeatherAppWidgetEvents {
    private String name;
    private long starting;
    private String location;
    private long ending;
    private long hourInMillis = 3600000;

    public WeatherAppWidgetEvents(Context context){

        int index = 0;

        final ContentResolver resolver = context.getContentResolver();

        String[] projection = new String[] { CalendarContract.Events.CALENDAR_ID, CalendarContract.Events.TITLE, 
            CalendarContract.Events.DESCRIPTION, CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND, 
            CalendarContract.Events.ALL_DAY, CalendarContract.Events.EVENT_LOCATION };

        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY,0);
        startTime.set(Calendar.MINUTE,0);
        startTime.set(Calendar.SECOND, 0);

        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.DATE, 1);

        String selection = "(( " + CalendarContract.Events.DTSTART + " >= " + startTime.getTimeInMillis() + " ) AND ( " + CalendarContract.Events.DTSTART + " <= " + endTime.getTimeInMillis() + " ) AND ( deleted != 1 ))";
        Cursor cursor = resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, null, null);

        List<String> nameStrings = new ArrayList<>();
        List<String> locStrings = new ArrayList<>();
        List<Long> startLongs = new ArrayList<>();
        List<Long> endlLongs = new ArrayList<>();

        if (cursor!=null && cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                nameStrings.add(cursor.getString(1));
                locStrings.add(cursor.getString(6));
                startLongs.add(cursor.getLong(3));
                endlLongs.add(cursor.getLong(4));
            } while ( cursor.moveToNext());

            for (int i = 0; i < endlLongs.size(); i++) {
                if (endlLongs.get(i) > System.currentTimeMillis()){
                    index = i;
                    break;
                }
            }
    
            name = nameStrings.get(index);
            location = locStrings.get(index);
            starting = startLongs.get(index);
            ending = endlLongs.get(index);
        }


        cursor.close();

    }

    public String formatedEvent(){

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String formattedDate = sdf.format(starting);
        String formattedEvent;
        if (isEventHappening()){
            formattedEvent = ("Now: " + name);
        } else {
            formattedEvent = (name + " at " + formattedDate);
        }

        return formattedEvent;
    }

    public String EventDuration(){ 

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String start = sdf.format(starting);
        String end = sdf.format(ending);
        String duration = (start + " - " + end);

        return duration;
    }

    public boolean showEvent(){
        if (name != null && name != "" && ending > System.currentTimeMillis() && System.currentTimeMillis() > (starting - hourInMillis)){
            return true;
        }
        return false;
    }

    private boolean isEventHappening(){
        if (System.currentTimeMillis() > starting && ending > System.currentTimeMillis()){
            return true;
        }
        return false;
    }

    public String getlocation(){
        return location;
    }
    
}