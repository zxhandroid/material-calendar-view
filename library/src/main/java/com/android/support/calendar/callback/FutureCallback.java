package com.android.support.calendar.callback;

import android.util.MonthDisplayHelper;

import com.android.support.calendar.model.DayTime;
import com.android.support.calendar.util.CalendarUtility;
import com.android.support.calendar.util.ThreadUtility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @author jonatan.salas
 */
public class FutureCallback implements ThreadUtility.CallBack<List<DayTime>> {
    private Calendar calendar;
    private int index;

    public FutureCallback(final Calendar calendar, final int index) {
        this.calendar = calendar;
        this.index = index;
    }

    @Override
    public List<DayTime> execute() {
        MonthDisplayHelper displayHelper = new MonthDisplayHelper(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.getFirstDayOfWeek());
        final List<DayTime> dayTimeList = new ArrayList<>(42);

        for (int i = 0; i < 6; i++) {
            int n[] = displayHelper.getDigitsForRow(i);

            for (int d = 0; d < 7; d++) {
                if (displayHelper.isWithinCurrentMonth(i, d)) {
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.set(Calendar.DAY_OF_MONTH, n[d]);
                    calendar.add(Calendar.MONTH, index);

                    if (n[d] == calendar.get(Calendar.DAY_OF_MONTH) && CalendarUtility.isWeekend(calendar) && index == 0) {
                        final DayTime dayTime = new DayTime()
                                .setDay(n[d])
                                .setMonth(calendar.get(Calendar.MONTH))
                                .setYear(calendar.get(Calendar.YEAR))
                                .setCurrentDay(true)
                                .setCurrentMonth(true)
                                .setCurrentYear(true)
                                .setWeekend(true)
                                .setEventList(null);

                        dayTimeList.add(dayTime);
                    } else if (n[d] == calendar.get(Calendar.DAY_OF_MONTH) && index == 0) {
                        final DayTime dayTime = new DayTime()
                                .setDay(n[d])
                                .setMonth(calendar.get(Calendar.MONTH))
                                .setYear(calendar.get(Calendar.YEAR))
                                .setCurrentDay(true)
                                .setCurrentMonth(true)
                                .setCurrentYear(true)
                                .setWeekend(false)
                                .setEventList(null);

                        dayTimeList.add(dayTime);
                    } else if (CalendarUtility.isWeekend(calendar)) {
                        final DayTime dayTime = new DayTime()
                                .setDay(n[d])
                                .setMonth(calendar.get(Calendar.MONTH))
                                .setYear(calendar.get(Calendar.YEAR))
                                .setCurrentDay(false)
                                .setCurrentMonth(true)
                                .setCurrentYear(true)
                                .setWeekend(true)
                                .setEventList(null);

                        dayTimeList.add(dayTime);
                    } else {
                        final DayTime dayTime = new DayTime()
                                .setDay(n[d])
                                .setMonth(calendar.get(Calendar.MONTH))
                                .setYear(calendar.get(Calendar.YEAR))
                                .setCurrentDay(false)
                                .setCurrentMonth(true)
                                .setCurrentYear(true)
                                .setWeekend(false)
                                .setEventList(null);

                        dayTimeList.add(dayTime);
                    }

                } else {
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.set(Calendar.DAY_OF_MONTH, n[d]);
                    calendar.add(Calendar.MONTH, index);

                    final DayTime dayTime = new DayTime()
                            .setDay(n[d])
                            .setMonth(calendar.get(Calendar.MONTH))
                            .setYear(calendar.get(Calendar.YEAR))
                            .setCurrentDay(false)
                            .setCurrentMonth(false)
                            .setCurrentYear(true)
                            .setWeekend(false)
                            .setEventList(null);

                    dayTimeList.add(dayTime);
                }
            }
        }

        return dayTimeList;
    }
}
