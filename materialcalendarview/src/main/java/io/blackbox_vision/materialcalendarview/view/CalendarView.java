package io.blackbox_vision.materialcalendarview.view;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.blackbox_vision.materialcalendarview.R;
import io.blackbox_vision.materialcalendarview.internal.data.Day;
import io.blackbox_vision.materialcalendarview.internal.utils.CalendarUtils;


/**
 * CalendarView class
 *
 * @author jonatan.salas
 */
public final class CalendarView extends LinearLayout {

    /**
     * Indicates that the CalendarView is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the CalendarView is currently being dragged by the user.
     */
    int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the CalendarView is in the process of settling to a final position.
     */
    int SCROLL_STATE_SETTLING = 2;

    boolean USE_CACHE = false;
    int MIN_DISTANCE_FOR_FLING = 25; // dips
    int DEFAULT_GUTTER_SIZE = 16; // dips
    int MIN_FLING_VELOCITY = 400; // dips

    /**
     * Sentinel value for no current active pointer.
     */
    int INVALID_POINTER = -1;

    // If the CalendarView is at least this close to its final position, complete the scroll
    // on touch down and let the user interact with the content inside instead of
    // "catching" the flinging Calendar.
    int CLOSE_ENOUGH = 2; // dp

    private boolean scrollingCacheEnabled;
    private boolean isBeingDragged;
    private boolean isUnableToDrag;
    private int defaultGutterSize;
    private int touchSlop;

    /**
     * Position of the last motion event.
     */
    private float lastMotionX;
    private float lastMotionY;
    private float initialMotionX;
    private float initialMotionY;

    private Scroller scroller;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int activePointerId = INVALID_POINTER;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker velocityTracker;
    private int minimumVelocity;
    private int maximumVelocity;
    private int flingDistance;
    private int closeEnough;

    private int scrollState = SCROLL_STATE_IDLE;

    private final Runnable endScrollRunnable = () -> setScrollState(SCROLL_STATE_IDLE);

    // Gesture Detector used to handle Swipe gestures.
    private GestureDetectorCompat gestureDetector;

    //Listeners used by the Calendar...
    private OnMonthTitleClickListener onMonthTitleClickListener;
    private OnDateClickListener onDateClickListener;
    private OnDateLongClickListener onDateLongClickListener;
    private OnMonthChangeListener onMonthChangeListener;

    private Calendar calendar;
    private Date lastSelectedDay;

    //Customizable variables...
    private Typeface typeface;
    private int disabledDayBackgroundColor;
    private int disabledDayTextColor;
    private int calendarBackgroundColor;
    private int selectedDayBackground;
    private int weekLayoutBackgroundColor;
    private int titleBackgroundColor;
    private int selectedDayTextColor;
    private int titleTextColor;
    private int dayOfWeekTextColor;
    private int currentDayOfMonth;
    private int weekendColor;
    private int weekend;
    private int drawableColor;

    private boolean isOverflowDateVisible = true;
    private int firstDayOfWeek = Calendar.SUNDAY;
    private int currentMonthIndex = 0;

    // Day of weekend
    private int[] totalDayOfWeekend;

    // true for ordinary day, false for a weekend.
    private boolean isCommonDay;

    private View view;
    private HeaderView headerView;

    /**
     * Constructor with arguments. It receives a
     * Context used to get the resources.
     *
     * @param context - the context used to get the resources.
     */
    public CalendarView(Context context) {
        this(context, null);
    }

    /**
     * Constructor with arguments. It receives a
     * Context used to get the resources.
     *
     * @param context - the context used to get the resources.
     * @param attrs - attribute set with custom styles.
     */
    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        takeStyles(attrs);
        drawCalendar();
    }

    /***
     * Method that gets and set the attributes of the CalendarView class.
     *
     * @param attrs - Attribute set object with custom values to be setted
     */
    private void takeStyles(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MaterialCalendarView, 0, 0);

        final int white = ContextCompat.getColor(getContext(), android.R.color.white);
        final int black = ContextCompat.getColor(getContext(), android.R.color.black);
        final int dayDisableBackground = ContextCompat.getColor(getContext(), R.color.day_disabled_background_color);
        final int dayDisableTextColor = ContextCompat.getColor(getContext(), R.color.day_disabled_text_color);
        final int daySelectedBackground = ContextCompat.getColor(getContext(), R.color.selected_day_background);
        final int dayCurrent = ContextCompat.getColor(getContext(), R.color.current_day_of_month);
        final int endColor = ContextCompat.getColor(getContext(), R.color.weekend_color);

        try {
            drawableColor = a.getColor(R.styleable.MaterialCalendarView_drawableColor, black);
            calendarBackgroundColor = a.getColor(R.styleable.MaterialCalendarView_calendarBackgroundColor, white);
            titleBackgroundColor = a.getColor(R.styleable.MaterialCalendarView_titleLayoutBackgroundColor, white);
            titleTextColor = a.getColor(R.styleable.MaterialCalendarView_calendarTitleTextColor, white);
            weekLayoutBackgroundColor = a.getColor(R.styleable.MaterialCalendarView_weekLayoutBackgroundColor, white);
            dayOfWeekTextColor = a.getColor(R.styleable.MaterialCalendarView_dayOfWeekTextColor, black);
            disabledDayBackgroundColor = a.getColor(R.styleable.MaterialCalendarView_disabledDayBackgroundColor, dayDisableBackground);
            disabledDayTextColor = a.getColor(R.styleable.MaterialCalendarView_disabledDayTextColor, dayDisableTextColor);
            selectedDayBackground = a.getColor(R.styleable.MaterialCalendarView_selectedDayBackgroundColor, daySelectedBackground);
            selectedDayTextColor = a.getColor(R.styleable.MaterialCalendarView_selectedDayTextColor, white);
            currentDayOfMonth = a.getColor(R.styleable.MaterialCalendarView_currentDayOfMonthColor, dayCurrent);
            weekendColor = a.getColor(R.styleable.MaterialCalendarView_weekendColor, endColor);
            weekend = a.getInteger(R.styleable.MaterialCalendarView_weekend, 0);
        } finally {
            if (null != a) {
                a.recycle();
            }
        }
    }

    /**
     * This method init all necessary variables and Views that our Calendar is going to use.
     */
    private void drawCalendar() {
        calendar = Calendar.getInstance(Locale.getDefault());
        gestureDetector = new GestureDetectorCompat(getContext(), new CalendarGestureDetector());
        scroller = new Scroller(getContext(), null);

        //Variables associated to handle touch events..
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        final float density = getContext().getResources().getDisplayMetrics().density;

        //Variables associated to Swipe..
        touchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        minimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        flingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        closeEnough = (int) (CLOSE_ENOUGH * density);
        defaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);

        view = LayoutInflater.from(getContext()).inflate(R.layout.material_calendar_view, this, true);

        setFirstDayOfWeek(Calendar.SUNDAY);
        refreshCalendar(Calendar.getInstance(Locale.getDefault()));
    }

    /**
     * Display calendar title with next previous month button
     */
    private void drawHeaderView() {
        headerView = (HeaderView) view.findViewById(R.id.header_view);

        headerView.setTitle(CalendarUtils.getDateTitle(Locale.getDefault(), currentMonthIndex));
        headerView.setBackgroundColor(titleBackgroundColor);
        headerView.setNextButtonColor(drawableColor);
        headerView.setBackButtonColor(drawableColor);
        headerView.setTitleColor(titleTextColor);
        headerView.setTypeface(typeface);

        headerView.setOnTitleClickListener(() -> {
            if (onMonthTitleClickListener != null) {
                onMonthTitleClickListener.onMonthTitleClick(calendar.getTime());
                createDialogWithoutDateField(getContext());
            }
        });

        headerView.setOnNextButtonClickListener(v -> {
            currentMonthIndex++;
            headerView.setTitle(CalendarUtils.getDateTitle(Locale.getDefault(), currentMonthIndex));

            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.add(Calendar.MONTH, currentMonthIndex);

            refreshCalendar(calendar);

            if (onMonthChangeListener != null) {
                onMonthChangeListener.onMonthChange(calendar.getTime());
            }
        });

        headerView.setOnBackButtonClickListener(v -> {
            currentMonthIndex--;
            headerView.setTitle(CalendarUtils.getDateTitle(Locale.getDefault(), currentMonthIndex));

            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.add(Calendar.MONTH, currentMonthIndex);

            refreshCalendar(calendar);

            if (onMonthChangeListener != null) {
                onMonthChangeListener.onMonthChange(calendar.getTime());
            }
        });
    }

    /**
     * Initialize the calendar week layout, considers start day
     */
    private void drawWeekView() {
        TextView dayOfWeek;
        String dayOfTheWeekString;

        //Setting background color white
        View weekLayout = view.findViewById(R.id.week_layout);
        weekLayout.setBackgroundColor(weekLayoutBackgroundColor);

        final List<String> weekDaysArray = CalendarUtils.getShortWeekDays(Locale.getDefault());

        for (int i = 1; i < weekDaysArray.size(); i++) {
            dayOfTheWeekString = weekDaysArray.get(i);
            int length = dayOfTheWeekString.length() < 3 ? dayOfTheWeekString.length() : 3;
            dayOfTheWeekString = dayOfTheWeekString.substring(0, length).toUpperCase();
            dayOfWeek = (TextView) view.findViewWithTag(getContext().getString(R.string.day_of_week) + CalendarUtils.calculateWeekIndex(calendar, i));
            dayOfWeek.setText(dayOfTheWeekString);

            isCommonDay = true;

            if (totalDayOfWeekend().length != 0) {
                for (int weekend : totalDayOfWeekend()) {
                    if (i == weekend) {
                        dayOfWeek.setTextColor(weekendColor);
                        isCommonDay = false;
                    }
                }
            }

            if (isCommonDay) {
                dayOfWeek.setTextColor(dayOfWeekTextColor);
            }

            if (null != typeface) {
                dayOfWeek.setTypeface(typeface);
            }
        }
    }

    /**
     * Date Picker (Month & Year only)
     *
     * @param context
     * @author chris.chen
     */
    private void createDialogWithoutDateField(Context context) {
        calendar = Calendar.getInstance(Locale.getDefault());

        final int iYear = calendar.get(Calendar.YEAR);
        final int iMonth = calendar.get(Calendar.MONTH);
        final int iDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(context, R.style.CalendarViewTitle, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(
                    DatePicker datePicker,
                    int year,
                    int monthOfYear,
                    int dayOfMonth
            ) {

                int diffMonth = (year - iYear) * 12 + (monthOfYear - iMonth);

                currentMonthIndex = diffMonth;
                Calendar calendar = Calendar.getInstance(Locale.getDefault());

                calendar.add(Calendar.MONTH, currentMonthIndex);

                refreshCalendar(calendar);

                if (onMonthChangeListener != null) {
                    onMonthChangeListener.onMonthChange(calendar.getTime());
                }

            }
        }, iYear, iMonth, iDay);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int daySpinnerId = Resources.getSystem().getIdentifier("day", "id", "android");
            if (daySpinnerId != 0) {
                View daySpinner = dpd.getDatePicker().findViewById(daySpinnerId);
                if (daySpinner != null) {
                    daySpinner.setVisibility(View.GONE);
                }
            }

            int monthSpinnerId = Resources.getSystem().getIdentifier("month", "id", "android");
            if (monthSpinnerId != 0) {
                View monthSpinner = dpd.getDatePicker().findViewById(monthSpinnerId);
                if (monthSpinner != null) {
                    monthSpinner.setVisibility(View.VISIBLE);
                }
            }

            int yearSpinnerId = Resources.getSystem().getIdentifier("year", "id", "android");
            if (yearSpinnerId != 0) {
                View yearSpinner = dpd.getDatePicker().findViewById(yearSpinnerId);
                if (yearSpinner != null) {
                    yearSpinner.setVisibility(View.VISIBLE);
                }
            }

        } else { //Older SDK versions
            Field f[] = dpd.getDatePicker().getClass().getDeclaredFields();

            for (Field field : f) {
                if (field.getName().equals("mDayPicker") || field.getName().equals("mDaySpinner")) {
                    field.setAccessible(true);
                    Object dayPicker = null;

                    try {
                        dayPicker = field.get(dpd.getDatePicker());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    ((View) dayPicker).setVisibility(View.GONE);
                }

                if (field.getName().equals("mMonthPicker") || field.getName().equals("mMonthSpinner")) {
                    field.setAccessible(true);
                    Object monthPicker = null;

                    try {
                        monthPicker = field.get(dpd.getDatePicker());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    ((View) monthPicker).setVisibility(View.VISIBLE);
                }

                if (field.getName().equals("mYearPicker") || field.getName().equals("mYearSpinner")) {
                    field.setAccessible(true);
                    Object yearPicker = null;

                    try {
                        yearPicker = field.get(dpd.getDatePicker());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    ((View) yearPicker).setVisibility(View.VISIBLE);
                }
            }
        }

        dpd.show();
    }

    private void newSetDaysInCalendar() {
        final List<Day> days = CalendarUtils.obtainDays(calendar, currentMonthIndex);

        DayView textView;
        ViewGroup container;

        for (int i = 0; i < days.size(); i++) {
            Day day = days.get(i);

            int fixedIndex = i + 1;

            container = (ViewGroup) view.findViewWithTag(getContext().getString(R.string.day_of_month_container) + fixedIndex);
            textView = (DayView) view.findViewWithTag(getContext().getString(R.string.day_of_month_text) + fixedIndex);

            container.setOnClickListener(null);

            if (null != typeface) {
                textView.setTypeface(typeface);
            }

            textView.setText(String.valueOf(day.getDay()));
            textView.setVisibility(View.VISIBLE);

            if (day.isCurrentMonth()) {
                container.setOnClickListener(onDayOfMonthClickListener);
                container.setOnLongClickListener(onDayOfMonthLongClickListener);

                textView.setBackgroundColor(calendarBackgroundColor);

                isCommonDay = true;

                if (day.isWeekend()) {
                    textView.setTextColor(weekendColor);
                    isCommonDay = false;
                }

                if (totalDayOfWeekend().length != 0) {
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());

                    calendar.set(Calendar.DAY_OF_MONTH, day.getDay());
                    calendar.set(Calendar.MONTH, day.getMonth());
                    calendar.set(Calendar.YEAR, day.getYear());

                    for (int weekend : totalDayOfWeekend()) {
                        if (calendar.get(Calendar.DAY_OF_WEEK) == weekend) {
                            textView.setTextColor(weekendColor);
                            isCommonDay = false;
                        }
                    }
                }

                if (isCommonDay) {
                    textView.setTextColor(dayOfWeekTextColor);
                }

            } else {
                if (!isOverflowDateVisible) {
                    textView.setVisibility(View.INVISIBLE);
                } else {
                    textView.setVisibility(View.VISIBLE);
                    textView.setEnabled(false);

                    textView.setBackgroundColor(disabledDayBackgroundColor);
                    textView.setTextColor(disabledDayTextColor);
                }

                //TODO review
                //if (!isOverflowDateVisible()) {
                //    textView.setVisibility(View.GONE);
                //} else if (i >= 36 && ((float) days.size() / 7.0f) >= 1) {
                //    textView.setVisibility(View.GONE);
                //}
            }

            if (day.isCurrentDay()) {
                Calendar calendar = Calendar.getInstance(Locale.getDefault());

                calendar.set(Calendar.DAY_OF_MONTH, day.getDay());
                calendar.set(Calendar.MONTH, day.getMonth());
                calendar.set(Calendar.YEAR, day.getYear());

                setCurrentDay(calendar.getTime());
            }
        }
    }

    private void clearDayOfTheMonthStyle(Date currentDate) {
        if (currentDate != null) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setFirstDayOfWeek(firstDayOfWeek);
            calendar.setTime(currentDate);

            DayView dayView = findViewByCalendar(calendar);
            dayView.setBackgroundColor(calendarBackgroundColor);
            isCommonDay = true;

            if (totalDayOfWeekend().length != 0) {
                for (int weekend : totalDayOfWeekend()) {
                    if (calendar.get(Calendar.DAY_OF_WEEK) == weekend) {
                        dayView.setTextColor(weekendColor);
                        isCommonDay = false;
                    }
                }
            }

            if (isCommonDay) {
                dayView.setTextColor(dayOfWeekTextColor);
            }
        }
    }

    public DayView findViewByDate(@NonNull Date dateToFind) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(dateToFind);
        return (DayView) getView(getContext().getString(R.string.day_of_month_text), calendar);
    }

    private DayView findViewByCalendar(@NonNull Calendar calendarToFind) {
        return (DayView) getView(getContext().getString(R.string.day_of_month_text), calendarToFind);
    }

    private int getDayIndexByDate(Calendar calendar) {
        int monthOffset = CalendarUtils.getMonthOffset(calendar, firstDayOfWeek);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        return currentDay + monthOffset;
    }

    private View getView(String key, Calendar currentCalendar) {
        final int index = getDayIndexByDate(currentCalendar);
        return view.findViewWithTag(key + index);
    }

    public void refreshCalendar(Calendar c) {
        calendar = c;
        calendar.setFirstDayOfWeek(firstDayOfWeek);

        drawHeaderView();
        setTotalDayOfWeekend();
        drawWeekView();

        newSetDaysInCalendar();
    }

    private void setTotalDayOfWeekend() {
        int[] weekendDay = new int[Integer.bitCount(weekend)];
        char days[] = Integer.toBinaryString(weekend).toCharArray();
        int day = 1;
        int index = 0;

        for (int i = days.length - 1; i >= 0; i--) {
            if (days[i] == '1') {
                weekendDay[index] = day;
                index++;
            }

            day++;
        }

        totalDayOfWeekend = weekendDay;
    }

    private int[] totalDayOfWeekend() {
        return totalDayOfWeekend;
    }

    public void setCurrentDay(@NonNull Date todayDate) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(todayDate);

        if (CalendarUtils.isToday(calendar)) {
            final DayView dayOfMonth = findViewByCalendar(calendar);

            dayOfMonth.setTextColor(currentDayOfMonth);
            dayOfMonth.setBackgroundColor(selectedDayBackground);
        }
    }

    public void setDateAsSelected(Date currentDate) {
        Calendar currentCalendar = Calendar.getInstance(Locale.getDefault());
        currentCalendar.setFirstDayOfWeek(firstDayOfWeek);
        currentCalendar.setTime(currentDate);

        // Clear previous marks
        clearDayOfTheMonthStyle(lastSelectedDay);

        // Store current values as last values
        setLastSelectedDay(currentDate);

        // Mark current day as selected
        DayView view = findViewByCalendar(currentCalendar);
        view.setBackgroundColor(selectedDayBackground);
        view.setTextColor(selectedDayTextColor);
    }

    private OnLongClickListener onDayOfMonthLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            // Extract day selected
            ViewGroup dayOfMonthContainer = (ViewGroup) view;
            String tagId = (String) dayOfMonthContainer.getTag();
            tagId = tagId.substring(getContext().getString(R.string.day_of_month_container).length(), tagId.length());
            final TextView dayOfMonthText = (TextView) view.findViewWithTag(getContext().getString(R.string.day_of_month_text) + tagId);

            // Fire event
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(firstDayOfWeek);
            calendar.setTime(calendar.getTime());
            calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dayOfMonthText.getText().toString()));
            setDateAsSelected(calendar.getTime());

            //Set the current day color
            setCurrentDay(calendar.getTime());

            if (onDateLongClickListener != null) {
                onDateLongClickListener.onDateLongClick(calendar.getTime());
            }

            return false;
        }
    };

    private OnClickListener onDayOfMonthClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            // Extract day selected
            ViewGroup dayOfMonthContainer = (ViewGroup) view;
            String tagId = (String) dayOfMonthContainer.getTag();
            tagId = tagId.substring(getContext().getString(R.string.day_of_month_container).length(), tagId.length());

            final TextView dayOfMonthText = (TextView) view.findViewWithTag(getContext().getString(R.string.day_of_month_text) + tagId);

            // Fire event
            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(firstDayOfWeek);
            c.setTime(calendar.getTime());
            c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dayOfMonthText.getText().toString()));

            setDateAsSelected(c.getTime());

            //Set the current day color
            setCurrentDay(c.getTime());

            if (onDateClickListener != null) {
                onDateClickListener.onDateClick(calendar.getTime());
            }
        }
    };

    private boolean isGutterDrag(float x, float dx) {
        return (x < defaultGutterSize && dx > 0) || (x > getWidth() - defaultGutterSize && dx < 0);
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (scrollingCacheEnabled != enabled) {
            scrollingCacheEnabled = enabled;
            if (USE_CACHE) {
                final int size = getChildCount();
                for (int i = 0; i < size; ++i) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        child.setDrawingCacheEnabled(enabled);
                    }
                }
            }
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == activePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            lastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
            activePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);

            if (velocityTracker != null) {
                velocityTracker.clear();
            }
        }
    }

    private void setScrollState(int newState) {
        if (scrollState == newState) {
            return;
        }

        scrollState = newState;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();

        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    /**
     * Tests scroll ability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scroll ability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = scrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            scroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
        }

        if (needPopulate) {
            if (postEvents) {
                ViewCompat.postOnAnimation(this, endScrollRunnable);
            } else {
                endScrollRunnable.run();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(null != gestureDetector) {
            gestureDetector.onTouchEvent(ev);
            super.dispatchTouchEvent(ev);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            isBeingDragged = false;
            isUnableToDrag = false;
            activePointerId = INVALID_POINTER;
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (isBeingDragged) {
                return true;
            }
            if (isUnableToDrag) {
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * isBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. lastMotionY is set to the y value
                * of the down event.
                */
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float dx = x - lastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = Math.abs(y - initialMotionY);

                if (dx != 0 && !isGutterDrag(lastMotionX, dx) &&
                        canScroll(this, false, (int) dx, (int) x, (int) y)) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    lastMotionX = x;
                    lastMotionY = y;
                    isUnableToDrag = true;
                    return false;
                }
                if (xDiff > touchSlop && xDiff * 0.5f > yDiff) {
                    isBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    lastMotionX = dx > 0 ? initialMotionX + touchSlop :
                            initialMotionX - touchSlop;
                    lastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else if (yDiff > touchSlop) {
                    // The finger has moved enough in the vertical
                    // direction to be counted as a drag...  abort
                    // any attempt to drag horizontally, to work correctly
                    // with children that have scrolling containers.
                    isUnableToDrag = true;
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                lastMotionX = initialMotionX = ev.getX();
                lastMotionY = initialMotionY = ev.getY();
                activePointerId = MotionEventCompat.getPointerId(ev, 0);
                isUnableToDrag = false;

                scroller.computeScrollOffset();
                if (scrollState == SCROLL_STATE_SETTLING &&
                        Math.abs(scroller.getFinalX() - scroller.getCurrX()) > closeEnough) {
                    isBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else {
                    completeScroll(false);
                    isBeingDragged = false;
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return isBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    /**
     * CalendarGestureDetector class used to detect Swipes gestures.
     *
     * @author jonatan.salas
     */
    public final class CalendarGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > touchSlop && Math.abs(velocityX) > minimumVelocity && Math.abs(velocityX) < maximumVelocity) {
                        if (e2.getX() - e1.getX() > flingDistance) {
                            currentMonthIndex--;

                            headerView.setTitle(CalendarUtils.getDateTitle(Locale.getDefault(), currentMonthIndex));

                            Calendar calendar = Calendar.getInstance(Locale.getDefault());
                            calendar.add(Calendar.MONTH, currentMonthIndex);

                            refreshCalendar(calendar);

                            if (onMonthChangeListener != null) {
                                onMonthChangeListener.onMonthChange(calendar.getTime());
                            }

                        } else if(e1.getX() - e2.getX() > flingDistance) {
                            currentMonthIndex++;

                            headerView.setTitle(CalendarUtils.getDateTitle(Locale.getDefault(), currentMonthIndex));

                            Calendar calendar = Calendar.getInstance(Locale.getDefault());
                            calendar.add(Calendar.MONTH, currentMonthIndex);

                            refreshCalendar(calendar);

                            if (onMonthChangeListener != null) {
                                onMonthChangeListener.onMonthChange(calendar.getTime());
                            }
                        }
                    }
                }

                return true;

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    /**
     * Interface that define a method to
     * implement to handle a selected date event,
     *
     * @author jonatan.salas
     */
    public interface OnDateClickListener {

        /**
         * Method that lets you handle
         * when a user touches a particular date.
         *
         * @param selectedDate - the date selected by the user.
         */
        void onDateClick(@NonNull Date selectedDate);
    }

    /**
     * Interface that define a method to
     * implement to handle a selected date event,
     *
     * @author jonatan.salas
     */
    public interface OnDateLongClickListener {

        /**
         * Method that lets you handle
         * when a user touches a particular date.
         *
         * @param selectedDate - the date selected by the user.
         */
        void onDateLongClick(@NonNull Date selectedDate);
    }

    /**
     * Interface that define a method to implement to handle
     * a month changed event.
     *
     * @author jonatan.salas
     */
    public interface OnMonthChangeListener {

        /**
         * Method that lets you handle when a goes to back or next
         * month.
         *
         * @param monthDate - the date with the current month
         */
        void onMonthChange(@NonNull Date monthDate);
    }

    /**
     * Interface that define a method to implement to handle
     * a month title change event.
     *
     * @author chris.chen
     */
    public interface OnMonthTitleClickListener {

        void onMonthTitleClick(@NonNull Date monthDate);
    }

    /**
     *  Attributes setters and getters.
     */

    public void setOnMonthTitleClickListener(OnMonthTitleClickListener onMonthTitleClickListener) {
        this.onMonthTitleClickListener = onMonthTitleClickListener;
        invalidate();
    }

    public void setOnDateClickListener(OnDateClickListener onDateClickListener) {
        this.onDateClickListener = onDateClickListener;
        invalidate();
    }

    public void setOnDateLongClickListener(OnDateLongClickListener onDateLongClickListener) {
        this.onDateLongClickListener = onDateLongClickListener;
        invalidate();
    }

    public void setOnMonthChangeListener(OnMonthChangeListener onMonthChangeListener) {
        this.onMonthChangeListener = onMonthChangeListener;
        invalidate();
    }

    private void setLastSelectedDay(Date lastSelectedDay) {
        this.lastSelectedDay = lastSelectedDay;
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        invalidate();
    }

    public void setIsOverflowDateVisible(boolean isOverflowDateVisible) {
        this.isOverflowDateVisible = isOverflowDateVisible;
        invalidate();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
        invalidate();
    }

    public void setDisabledDayBackgroundColor(int disabledDayBackgroundColor) {
        this.disabledDayBackgroundColor = disabledDayBackgroundColor;
        invalidate();
    }

    public void setDisabledDayTextColor(int disabledDayTextColor) {
        this.disabledDayTextColor = disabledDayTextColor;
        invalidate();
    }

    public void setCalendarBackgroundColor(int calendarBackgroundColor) {
        this.calendarBackgroundColor = calendarBackgroundColor;
        invalidate();
    }

    public void setSelectedDayBackground(int selectedDayBackground) {
        this.selectedDayBackground = selectedDayBackground;
        invalidate();
    }

    public void setWeekLayoutBackgroundColor(int weekLayoutBackgroundColor) {
        this.weekLayoutBackgroundColor = weekLayoutBackgroundColor;
        invalidate();
    }

    public void setTitleBackgroundColor(int titleBackgroundColor) {
        this.titleBackgroundColor = titleBackgroundColor;
        invalidate();
    }

    public void setSelectedDayTextColor(int selectedDayTextColor) {
        this.selectedDayTextColor = selectedDayTextColor;
        invalidate();
    }

    public void setTitleTextColor(int titleTextColor) {
        this.titleTextColor = titleTextColor;
        invalidate();
    }

    public void setDayOfWeekTextColor(int dayOfWeekTextColor) {
        this.dayOfWeekTextColor = dayOfWeekTextColor;
        invalidate();
    }

    public void setCurrentDayOfMonth(int currentDayOfMonth) {
        this.currentDayOfMonth = currentDayOfMonth;
        invalidate();
    }

    public void setWeekendColor(int weekendColor) {
        this.weekendColor = weekendColor;
        invalidate();
    }

    public void setWeekend(int weekend) {
        this.weekend = weekend;
        invalidate();
    }

    public void setBackButtonColor(@ColorRes int colorId) {
        this.headerView.setBackButtonColor(ContextCompat.getColor(getContext(), colorId));
        invalidate();
    }

    public void setNextButtonColor(@ColorRes int colorId) {
        this.headerView.setNextButtonColor(ContextCompat.getColor(getContext(), colorId));
        invalidate();
    }

    public void setBackButtonDrawable(@DrawableRes int drawableId) {
        this.headerView.setBackButtonDrawable(ContextCompat.getDrawable(getContext(), drawableId));
        invalidate();
    }

    public void setNextButtonDrawable(@DrawableRes int drawableId) {
        this.headerView.setNextButtonDrawable(ContextCompat.getDrawable(getContext(), drawableId));
        invalidate();
    }

    public void setDrawableColor(@ColorRes int drawableColor) {
        this.drawableColor = drawableColor;
        setNextButtonColor(drawableColor);
        setBackButtonColor(drawableColor);
        invalidate();
    }

    public String getCurrentMonth() {
        return CalendarUtils.getCurrentMonth(currentMonthIndex);
    }

    public String getCurrentYear() {
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    public boolean isOverflowDateVisible() {
        return isOverflowDateVisible;
    }
}
