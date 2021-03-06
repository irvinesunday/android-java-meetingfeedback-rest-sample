/*
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license.
 * See LICENSE in the project root for license information.
 */
package com.microsoft.office365.meetingfeedback.model.outlook;

import com.microsoft.office365.meetingfeedback.model.DataStore;
import com.microsoft.office365.meetingfeedback.model.authentication.AuthenticationManager;
import com.microsoft.office365.meetingfeedback.model.meeting.DateRange;
import com.microsoft.office365.meetingfeedback.model.outlook.payload.Envelope;
import com.microsoft.office365.meetingfeedback.model.outlook.payload.Event;
import com.microsoft.office365.meetingfeedback.model.request.RESTHelper;
import com.microsoft.office365.meetingfeedback.util.FormatUtil;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CalendarService {

    private CalendarInterface mCalendarClient;
    private DataStore mDataStore;
    private List<Event> mAccumulatedEvents;

    public CalendarService(AuthenticationManager authenticationManager, DataStore dataStore) {
        mDataStore = dataStore;
        RestAdapter restAdapter = new RESTHelper(authenticationManager).getRestAdapter();
        mCalendarClient = restAdapter.create(CalendarInterface.class);
    }

    public void fetchEvents(final Callback<Void> callback) {
        getEvents(new Callback<Envelope<Event>>() {
            @Override
            public void success(Envelope envelope, Response response) {
                mAccumulatedEvents = envelope.mValues;
                mDataStore.setEvents(mAccumulatedEvents);
                if(null != callback) {
                    callback.success(null, response);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if(null != callback) {
                    callback.failure(error);
                }
            }
        });
    }

    private void getEvents(Callback<Envelope<Event>> callback) {
        DateRange dateRange = getDateRange();
        String startDateTime = FormatUtil.convertDateToUrlString(dateRange.mStart.getTime());
        String endDateTime = FormatUtil.convertDateToUrlString(dateRange.mEnd.getTime());
        String preferredTimezone = "outlook.timezone=\"" + TimeZone.getDefault().getID() + "\"";

        mCalendarClient.getEvents(
                "application/json",
                preferredTimezone,
                startDateTime,
                endDateTime,
                "subject,start,end,organizer,isOrganizer,attendees,bodyPreview,iCalUID",
                "start/datetime desc",
                "150",
                callback
        );
    }

    private DateRange getDateRange() {
        Calendar end = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -28);
        return new DateRange(start, end);
    }
}
