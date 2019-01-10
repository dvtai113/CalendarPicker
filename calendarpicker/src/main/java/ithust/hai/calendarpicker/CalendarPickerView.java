
package ithust.hai.calendarpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Android component to allow picking a date from a calendar view (a list of months).  Must be
 * initialized after inflation with {@link #init(Date, Date)} and can be customized with any of the
 * {@link FluentInitializer} methods returned.  The currently selected date can be retrieved with
 * {@link #getSelectedDate()}.
 */
public class CalendarPickerView extends RecyclerView {
    private final CalendarPickerView.MonthAdapter adapter;
    private final MonthView.Listener listener = new CellClickedListener();
    private final List<MonthCellDescriptor> selectedCells = new ArrayList<>();
    private final List<Calendar> selectedCals = new ArrayList<>();
    private Locale locale;
    private DateFormat monthNameFormat;
    private DateFormat weekdayNameFormat;
    private DateFormat fullDateFormat;
    private Calendar minCal;
    private Calendar maxCal;
    private Calendar monthCounter;
    private Calendar today;
    private boolean displayOnly;
    private byte selectionMode;
    private int dayBackgroundResId;
    private int dayTextColorResId;
    private int titleTextStyle;
    private boolean displayDayNamesHeaderRow;
    private boolean displayAlwaysDigitNumbers;

    private OnDateSelectedListener dateListener;
    private DateSelectableFilter dateConfiguredListener;
    private OnInvalidDateSelectedListener invalidDateListener =
            new DefaultOnInvalidDateSelectedListener();
    private CellClickInterceptor cellClickInterceptor;
    private DayViewAdapter dayViewAdapter = new DefaultDayViewAdapter();

    private int totalMonth;

    public CalendarPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarPickerView);
        final int bg = a.getColor(R.styleable.CalendarPickerView_android_background,
                res.getColor(R.color.calendar_bg));
        dayBackgroundResId = a.getResourceId(R.styleable.CalendarPickerView_tsquare_dayBackground,
                R.drawable.calendar_bg_selector);
        dayTextColorResId = a.getResourceId(R.styleable.CalendarPickerView_tsquare_dayTextColor,
                R.color.calendar_text_selector);
        titleTextStyle = a.getResourceId(R.styleable.CalendarPickerView_tsquare_titleTextStyle,
                R.style.CalendarTitle);
        displayDayNamesHeaderRow =
                a.getBoolean(R.styleable.CalendarPickerView_tsquare_displayDayNamesHeaderRow, true);
        displayAlwaysDigitNumbers =
                a.getBoolean(R.styleable.CalendarPickerView_tsquare_displayAlwaysDigitNumbers, false);
        a.recycle();

        setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MonthAdapter();
        setBackgroundColor(bg);
        locale = Locale.getDefault();
        today = Calendar.getInstance(locale);
        minCal = Calendar.getInstance(locale);
        maxCal = Calendar.getInstance(locale);
        monthCounter = Calendar.getInstance(locale);
        weekdayNameFormat = new SimpleDateFormat(context.getString(R.string.day_name_format), locale);
        monthNameFormat = new SimpleDateFormat(context.getString(R.string.month_name_format), locale);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);

        if (isInEditMode()) {
            Calendar nextYear = Calendar.getInstance(locale);
            nextYear.add(Calendar.YEAR, 1);

            init(new Date(), nextYear.getTime()) //
                    .withSelectedDate(new Date());
        }
    }

    public FluentInitializer init(Date minDate, Date maxDate, Locale locale) {
        if (minDate == null || maxDate == null) {
            throw new IllegalArgumentException(
                    "minDate and maxDate must be non-null.  " + dbg(minDate, maxDate));
        }
        if (minDate.after(maxDate)) {
            throw new IllegalArgumentException(
                    "minDate must be before maxDate.  " + dbg(minDate, maxDate));
        }
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null.");
        }

        // Make sure that all calendar instances use the same time zone and locale.
        this.locale = locale;
        today = Calendar.getInstance(locale);
        minCal = Calendar.getInstance(locale);
        maxCal = Calendar.getInstance(locale);
        monthCounter = Calendar.getInstance(locale);
        weekdayNameFormat = new SimpleDateFormat(getContext().getString(R.string.day_name_format), locale);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);

        this.selectionMode = SelectionMode.SINGLE;
        // Clear out any previously-selected dates/cells.
        selectedCals.clear();
        selectedCells.clear();

        // Clear previous state.
        minCal.setTime(minDate);
        maxCal.setTime(maxDate);
        setMidnight(minCal);
        setMidnight(maxCal);
        displayOnly = false;

        // maxDate is exclusive: bump back to the previous day so if maxDate is the first of a month,
        // we don't accidentally include that month in the view.
        maxCal.add(MINUTE, -1);

        // Now iterate between minCal and maxCal and build up our list of months to show.
        monthCounter.setTime(minCal.getTime());
        final int maxMonth = maxCal.get(MONTH);
        final int maxYear = maxCal.get(YEAR);
        totalMonth = 0;
        while ((monthCounter.get(MONTH) <= maxMonth // Up to, including the month.
                || monthCounter.get(YEAR) < maxYear) // Up to the year.
                && monthCounter.get(YEAR) < maxYear + 1) { // But not > next yr.
            totalMonth++;
            monthCounter.add(MONTH, 1);
        }

        validateAndUpdate();
        return new FluentInitializer();
    }

    public FluentInitializer init(Date minDate, Date maxDate) {
        return init(minDate, maxDate, Locale.getDefault());
    }

    public class FluentInitializer {
        /**
         * Override the {@link SelectionMode} from the default ({@link SelectionMode#SINGLE}).
         */
        public FluentInitializer inMode(byte mode) {
            selectionMode = mode;
            validateAndUpdate();
            return this;
        }

        /**
         * Set an initially-selected date.  The calendar will scroll to that date if it's not already
         * visible.
         */
        public FluentInitializer withSelectedDate(Date selectedDates) {
            return withSelectedDates(Collections.singletonList(selectedDates));
        }

        /**
         * Set multiple selected dates.  This will throw an {@link IllegalArgumentException} if you
         * pass in multiple dates and haven't already called {@link #(SelectionMode)}.
         */
        public FluentInitializer withSelectedDates(Collection<Date> selectedDates) {
            if (selectionMode == SelectionMode.SINGLE && selectedDates.size() > 1) {
                throw new IllegalArgumentException("SINGLE mode can't be used with multiple selectedDates");
            }
            if (selectionMode == SelectionMode.RANGE && selectedDates.size() > 2) {
                throw new IllegalArgumentException(
                        "RANGE mode only allows two selectedDates.  You tried to pass " + selectedDates.size());
            }
            if (selectedDates != null) {
                for (Date date : selectedDates) {
                    selectDate(date);
                }
            }
            scrollToSelectedDates();

            validateAndUpdate();
            return this;
        }

        @SuppressLint("SimpleDateFormat")
        public FluentInitializer setShortWeekdays(String[] newShortWeekdays) {
            DateFormatSymbols symbols = new DateFormatSymbols(locale);
            symbols.setShortWeekdays(newShortWeekdays);
            weekdayNameFormat =
                    new SimpleDateFormat(getContext().getString(R.string.day_name_format), symbols);
            return this;
        }

        public FluentInitializer displayOnly() {
            displayOnly = true;
            return this;
        }
    }

    private void validateAndUpdate() {
        if (getAdapter() == null) {
            setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    private void scrollToSelectedMonth(final int selectedIndex) {
        scrollToSelectedMonth(selectedIndex, false);
    }

    private void scrollToSelectedMonth(final int selectedIndex, final boolean smoothScroll) {
        post(new Runnable() {
            @Override
            public void run() {
                Logr.d("Scrolling to position %d", selectedIndex);

                if (smoothScroll) {
                    smoothScrollToPosition(selectedIndex);
                } else {
                    scrollToPosition(selectedIndex);
                }
            }
        });
    }

    private void scrollToSelectedDates() {
        int selectedIndex = NO_POSITION;
        int todayIndex = NO_POSITION;
        Calendar today = Calendar.getInstance(locale);
        MonthDescriptor month;
        for (int c = 0; c < totalMonth; c++) {
            monthCounter.setTime(minCal.getTime());
            monthCounter.add(MONTH, c);
            month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), monthNameFormat.format(monthCounter.getTime()));
            if (selectedIndex == NO_POSITION) {
                for (Calendar selectedCal : selectedCals) {
                    if (sameMonth(selectedCal, month)) {
                        selectedIndex = c;
                        break;
                    }
                }
                if (selectedIndex == NO_POSITION && todayIndex == NO_POSITION && sameMonth(today, month)) {
                    todayIndex = c;
                }
            }
        }
        if (selectedIndex != NO_POSITION) {
            scrollToSelectedMonth(selectedIndex);
        } else if (todayIndex != NO_POSITION) {
            scrollToSelectedMonth(todayIndex);
        }
    }

    public boolean scrollToDate(Date date) {
        Integer selectedIndex = null;

        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(date);
        MonthDescriptor month;
        for (int c = 0; c < totalMonth; c++) {
            monthCounter.setTime(minCal.getTime());
            monthCounter.add(MONTH, c);
            month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), monthNameFormat.format(monthCounter.getTime()));
            if (sameMonth(cal, month)) {
                selectedIndex = c;
                break;
            }
        }
        if (selectedIndex != null) {
            scrollToSelectedMonth(selectedIndex);
            return true;
        }
        return false;
    }

    public void fixDialogDimens() {
        Logr.d("Fixing dimensions to h = %d / w = %d", getMeasuredHeight(), getMeasuredWidth());
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = getMeasuredHeight();
        getLayoutParams().width = getMeasuredWidth();
        // Post this runnable so it runs _after_ the dimen changes have been applied/re-measured.
        post(new Runnable() {
            @Override
            public void run() {
                Logr.d("Dimens are fixed: now scroll to the selected date");
                scrollToSelectedDates();
            }
        });
    }

    /**
     * This method should only be called if the calendar is contained in a dialog, and it should only
     * be called when the screen has been rotated and the dialog should be re-measured.
     */
    public void unfixDialogDimens() {
        Logr.d("Reset the fixed dimensions to allow for re-measurement");
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = LayoutParams.MATCH_PARENT;
        getLayoutParams().width = LayoutParams.MATCH_PARENT;
        requestLayout();
    }

    public Date getSelectedDate() {
        return (selectedCals.size() > 0 ? selectedCals.get(0).getTime() : null);
    }

    public List<Date> getSelectedDates() {
        List<Date> selectedDates = new ArrayList<>();
        for (MonthCellDescriptor cal : selectedCells) {
            selectedDates.add(new Date(cal.getTime()));
        }
        Collections.sort(selectedDates);
        return selectedDates;
    }

    /**
     * Returns a string summarizing what the client sent us for init() params.
     */
    private static String dbg(Date minDate, Date maxDate) {
        return "minDate: " + minDate + "\nmaxDate: " + maxDate;
    }

    /**
     * Clears out the hours/minutes/seconds/millis of a Calendar.
     */
    static void setMidnight(Calendar cal) {
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.set(MILLISECOND, 0);
    }

    private class CellClickedListener implements MonthView.Listener {
        @Override
        public void handleClick(MonthCellDescriptor cell) {
            Date clickedDate = new Date(cell.getTime());

            if (cellClickInterceptor != null && cellClickInterceptor.onCellClicked(clickedDate)) {
                return;
            }
            if (!betweenDates(clickedDate, minCal, maxCal) || !isDateSelectable(clickedDate)) {
                if (invalidDateListener != null) {
                    invalidDateListener.onInvalidDateSelected(clickedDate);
                }
            } else {
                boolean wasSelected = doSelectDate(clickedDate, cell);

                if (dateListener != null) {
                    if (wasSelected) {
                        dateListener.onDateSelected(clickedDate);
                    } else {
                        dateListener.onDateUnselected(clickedDate);
                    }
                }
            }
        }
    }

    /**
     * Select a new date.  Respects the {@link SelectionMode} this CalendarPickerView is configured
     * with: if you are in {@link SelectionMode#SINGLE}, the previously selected date will be
     * un-selected.  In {@link SelectionMode#MULTIPLE}, the new date will be added to the list of
     * selected dates.
     * <p>
     * If the selection was made (selectable date, in range), the view will scroll to the newly
     * selected date if it's not already visible.
     *
     * @return - whether we were able to set the date
     */
    public boolean selectDate(Date date) {
        return selectDate(date, false);
    }

    /**
     * Select a new date.  Respects the {@link SelectionMode} this CalendarPickerView is configured
     * with: if you are in {@link SelectionMode#SINGLE}, the previously selected date will be
     * un-selected.  In {@link SelectionMode#MULTIPLE}, the new date will be added to the list of
     * selected dates.
     * <p>
     * If the selection was made (selectable date, in range), the view will scroll to the newly
     * selected date if it's not already visible.
     *
     * @return - whether we were able to set the date
     */
    public boolean selectDate(Date date, boolean smoothScroll) {
        validateDate(date);

        MonthCellWithMonthIndex monthCellWithMonthIndex = getMonthCellWithIndexByDate(date);
        if (monthCellWithMonthIndex == null || !isDateSelectable(date)) {
            return false;
        }
        boolean wasSelected = doSelectDate(date, monthCellWithMonthIndex.cell);
        if (wasSelected) {
            scrollToSelectedMonth(monthCellWithMonthIndex.monthIndex, smoothScroll);
        }
        return wasSelected;
    }

    private void validateDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Selected date must be non-null.");
        }
        if (date.before(minCal.getTime()) || date.after(maxCal.getTime())) {
            throw new IllegalArgumentException(String.format(
                    "SelectedDate must be between minDate and maxDate."
                            + "%nminDate: %s%nmaxDate: %s%nselectedDate: %s", minCal.getTime(), maxCal.getTime(),
                    date));
        }
    }

    private boolean doSelectDate(Date date, MonthCellDescriptor cell) {
        Calendar newlySelectedCal = Calendar.getInstance(locale);
        newlySelectedCal.setTime(date);
        // Sanitize input: clear out the hours/minutes/seconds/millis.
        setMidnight(newlySelectedCal);

        // Clear any remaining range state.
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        switch (selectionMode) {
            case SelectionMode.RANGE:
                if (selectedCals.size() > 1) {
                    // We've already got a range selected: clear the old one.
                    clearOldSelections();
                } else if (selectedCals.size() == 1 && newlySelectedCal.before(selectedCals.get(0))) {
                    // We're moving the start of the range back in time: clear the old start date.
                    clearOldSelections();
                } else if (selectedCals.size() == 1 && newlySelectedCal.equals(selectedCals.get(0))) {
                    // no need perform action when user select current
                    return false;
                }

                break;

            case SelectionMode.MULTIPLE:
                date = applyMultiSelect(date, newlySelectedCal);
                break;

            case SelectionMode.SINGLE:
                clearOldSelections();
                break;
        }

        if (date != null) {
            // Select a new cell.
            if (selectedCells.size() == 0 || selectedCells.get(0).getTime() != cell.getTime()) {
                selectedCells.add(cell);
                cell.setSelected(true);
            }
            selectedCals.add(newlySelectedCal);

            if (selectionMode == SelectionMode.RANGE && selectedCells.size() > 1) {
                // Select all days in between start and end.
                Calendar calStart = Calendar.getInstance(locale);
                calStart.setTimeInMillis(selectedCells.get(0).getTime());
                Calendar calEnd = Calendar.getInstance(locale);
                calEnd.setTimeInMillis(selectedCells.get(1).getTime());

                // get position of selection range, example min calendar 1/2019, user select date 2 -> 9/2/2019 so start and end month position is 1
                int startMonth = 0, endMonth = 0;
                for (int index = 0; index < totalMonth; index++) {
                    monthCounter.setTime(minCal.getTime());
                    monthCounter.add(MONTH, index);
                    if (sameMonth(calStart, monthCounter)) {
                        startMonth = index;
                    }
                    if (sameMonth(calEnd, monthCounter)) {
                        endMonth = index;
                        break;
                    }
                }

                MonthDescriptor month;

                // check start day and end day of week
                for (int index = startMonth; index <= endMonth; index++) {
                    monthCounter.setTime(minCal.getTime());
                    monthCounter.add(MONTH, index);
                    month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), monthNameFormat.format(monthCounter.getTime()));

                    for (List<MonthCellDescriptor> week : getMonthCells(month, monthCounter)) {
                        for (int dayIndex = 0, size = week.size(); dayIndex < size; dayIndex++) {
                            MonthCellDescriptor singleCell = week.get(dayIndex);
                            if (singleCell.getTime() > calStart.getTimeInMillis()
                                    && singleCell.getTime() < calEnd.getTimeInMillis()
                                    && singleCell.isSelectable()) {
                                singleCell.setSelected(false);
                                singleCell.setRangeState(RangeState.MIDDLE);
                                selectedCells.add(singleCell);
                            }
                        }
                    }
                }
            }
        }

        // Update the adapter.
        validateAndUpdate();
        return date != null;
    }

    private void clearOldSelections() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            // De-select the currently-selected cell.
            selectedCell.setSelected(false);

            if (dateListener != null) {
                Date selectedDate = new Date(selectedCell.getTime());

                if (selectionMode == SelectionMode.RANGE) {
                    int index = selectedCells.indexOf(selectedCell);
                    if (index == 0 || index == selectedCells.size() - 1) {
                        dateListener.onDateUnselected(selectedDate);
                    }
                } else {
                    dateListener.onDateUnselected(selectedDate);
                }
            }
        }
        selectedCells.clear();
        selectedCals.clear();
    }

    private Date applyMultiSelect(Date date, Calendar selectedCal) {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            if (selectedCell.getTime() == date.getTime()) {
                // De-select the currently-selected cell.
                selectedCell.setSelected(false);
                selectedCells.remove(selectedCell);
                date = null;
                break;
            }
        }
        for (Calendar cal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                selectedCals.remove(cal);
                break;
            }
        }
        return date;
    }

    public void clearSelectedDates() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        clearOldSelections();
        validateAndUpdate();
    }

    /**
     * Hold a cell with a month-index.
     */
    private static class MonthCellWithMonthIndex {
        MonthCellDescriptor cell;
        int monthIndex;

        MonthCellWithMonthIndex(MonthCellDescriptor cell, int monthIndex) {
            this.cell = cell;
            this.monthIndex = monthIndex;
        }
    }

    /**
     * Return cell and month-index (for scrolling) for a given Date.
     */
    private MonthCellWithMonthIndex getMonthCellWithIndexByDate(Date date) {
        int index = 0;
        Calendar searchCal = Calendar.getInstance(locale);
        searchCal.setTime(date);
        Calendar actCal = Calendar.getInstance(locale);

        MonthDescriptor month;
        for (int i = 0; i < totalMonth; i++) {
            monthCounter.setTime(minCal.getTime());
            monthCounter.add(MONTH, i);
            month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), monthNameFormat.format(monthCounter.getTime()));
            if (sameMonth(searchCal, month)) {
                for (List<MonthCellDescriptor> weekCells : getMonthCells(month, monthCounter)) {
                    for (MonthCellDescriptor actCell : weekCells) {
                        actCal.setTimeInMillis(actCell.getTime());
                        if (sameDate(actCal, searchCal) && actCell.isSelectable()) {
                            return new MonthCellWithMonthIndex(actCell, index);
                        }
                    }
                    index++;
                }
            }
        }
        return null;
    }

    private class MonthAdapter extends RecyclerView.Adapter<MonthViewHolder> {

        @Override
        public int getItemCount() {
            return totalMonth;
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MonthView monthView = MonthView.create(parent, LayoutInflater.from(parent.getContext()), weekdayNameFormat, listener, today,
                    dayBackgroundResId, dayTextColorResId, titleTextStyle,
                    displayDayNamesHeaderRow, displayAlwaysDigitNumbers, locale, dayViewAdapter);
            return new MonthViewHolder(monthView);
        }

        @Override
        public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
            MonthView monthView = (MonthView) holder.itemView;

            // calculate current month
            monthCounter.setTime(minCal.getTime());
            monthCounter.add(MONTH, position);
            Date date = monthCounter.getTime();
            MonthDescriptor month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), monthNameFormat.format(date));
            monthView.init(month, getMonthCells(month, monthCounter), displayOnly);
        }
    }

    private static class MonthViewHolder extends ViewHolder {

        private MonthViewHolder(View itemView) {
            super(itemView);
        }
    }

    private List<List<MonthCellDescriptor>> getMonthCells(MonthDescriptor month, Calendar startCal) {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(startCal.getTime());
        List<List<MonthCellDescriptor>> cells = new ArrayList<>();
        cal.set(DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(DAY_OF_WEEK);
        int offset = cal.getFirstDayOfWeek() - firstDayOfWeek;
        if (offset > 0) {
            offset -= 7;
        }
        cal.add(Calendar.DATE, offset);

        Calendar minSelectedCal = minDate(selectedCals);
        Calendar maxSelectedCal = maxDate(selectedCals);

        while ((cal.get(MONTH) < month.getMonth() + 1 || cal.get(YEAR) < month.getYear()) //
                && cal.get(YEAR) <= month.getYear()) {
            Logr.d("Building week row starting at %s", cal.getTime());
            List<MonthCellDescriptor> weekCells = new ArrayList<>();
            cells.add(weekCells);
            for (int c = 0; c < 7; c++) {
                boolean isCurrentMonth = cal.get(MONTH) == month.getMonth();

                boolean isSelected = isCurrentMonth && containsDate(selectedCals, cal);
                boolean isSelectable = isCurrentMonth && betweenDates(cal, minCal, maxCal) && isDateSelectable(cal.getTime());
                boolean isToday = sameDate(cal, today);

                byte rangeState = RangeState.NONE;
                if (selectedCals.size() > 1 && isCurrentMonth && selectionMode != SelectionMode.MULTIPLE) {
                    if (sameDate(minSelectedCal, cal)) {
                        if (c != 6) {
                            // if min calendar at the end of week should no mark first
                            rangeState = RangeState.FIRST;
                        }
                    } else if (sameDate(maxDate(selectedCals), cal)) {
                        if (c != 0) {
                            // if max calendar at the start of week should no mark first
                            rangeState = RangeState.LAST;
                        }
                    } else if (betweenDates(cal, minSelectedCal, maxSelectedCal)) {
                        // check start and end of week to draw background
                        if (c == 0) {
                            rangeState = RangeState.START_WEEK;
                        } else if (c == 6) {
                            rangeState = RangeState.END_WEEK;
                        } else {
                            rangeState = RangeState.MIDDLE;
                        }
                    }
                }

                weekCells.add(new MonthCellDescriptor(cal.getTimeInMillis(), isCurrentMonth, isSelectable, isSelected, isToday, isCurrentMonth ? cal.get(DAY_OF_MONTH) : 0, rangeState));
                if (isCurrentMonth && selectionMode != SelectionMode.MULTIPLE) {
                    if (cal.get(DAY_OF_MONTH) == cal.getActualMaximum(DAY_OF_MONTH) && (rangeState == RangeState.FIRST || rangeState == RangeState.MIDDLE)) {
                        // if first day of month is last selected day, should mark all next item
                        for (c = c + 1; c < 7; c++) {
                            weekCells.add(new MonthCellDescriptor(cal.getTimeInMillis(), false, false, false, isToday, 0, c == 6 ? RangeState.END_WEEK : RangeState.MIDDLE));
                            cal.add(DATE, 1);
                        }
                        break;
                    } else if (cal.get(DAY_OF_MONTH) == cal.getActualMinimum(DAY_OF_MONTH) && (rangeState == RangeState.LAST || rangeState == RangeState.MIDDLE)) {
                        // if last day selected is start of month should mark range state for previous item
                        for (int i = 0; i < c; i++) {
                            weekCells.get(i).setRangeState(i == 0 ? RangeState.START_WEEK : RangeState.MIDDLE);
                        }
                    }
                }
                cal.add(DATE, 1);
            }
        }
        return cells;
    }

    private boolean containsDate(List<Calendar> selectedCals, Date date) {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(date);
        return containsDate(selectedCals, cal);
    }

    private static boolean containsDate(List<Calendar> selectedCals, Calendar cal) {
        for (Calendar selectedCal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                return true;
            }
        }
        return false;
    }

    private static Calendar minDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(0);
    }

    private static Calendar maxDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(selectedCals.size() - 1);
    }

    private static boolean sameDate(Calendar cal, Calendar selectedDate) {
        return cal.get(MONTH) == selectedDate.get(MONTH)
                && cal.get(YEAR) == selectedDate.get(YEAR)
                && cal.get(DAY_OF_MONTH) == selectedDate.get(DAY_OF_MONTH);
    }

    private static boolean betweenDates(Calendar cal, Calendar minCal, Calendar maxCal) {
        final Date date = cal.getTime();
        return betweenDates(date, minCal, maxCal);
    }

    static boolean betweenDates(Date date, Calendar minCal, Calendar maxCal) {
        final Date min = minCal.getTime();
        return (date.equals(min) || date.after(min)) // >= minCal
                && date.before(maxCal.getTime()); // && < maxCal
    }

    private static boolean sameMonth(Calendar cal, MonthDescriptor month) {
        return (cal.get(MONTH) == month.getMonth() && cal.get(YEAR) == month.getYear());
    }

    private boolean sameMonth(Calendar first, Calendar second) {
        return (first.get(MONTH) == second.get(MONTH) && first.get(YEAR) == second.get(YEAR));
    }

    private boolean isDateSelectable(Date date) {
        return dateConfiguredListener == null || dateConfiguredListener.isDateSelectable(date);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        dateListener = listener;
    }

    /**
     * Set a listener to react to user selection of a disabled date.
     *
     * @param listener the listener to set, or null for no reaction
     */
    public void setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener listener) {
        invalidDateListener = listener;
    }

    /**
     * Set a listener used to discriminate between selectable and unselectable dates. Set this to
     * disable arbitrary dates as they are rendered.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setDateSelectableFilter(DateSelectableFilter listener) {
        dateConfiguredListener = listener;
    }

    /**
     * Set an adapter used to initialize {@link CalendarCellView} with custom layout.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setCustomDayView(DayViewAdapter dayViewAdapter) {
        this.dayViewAdapter = dayViewAdapter;
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Set a listener to intercept clicks on calendar cells.
     */
    public void setCellClickInterceptor(CellClickInterceptor listener) {
        cellClickInterceptor = listener;
    }

    /**
     * Interface to be notified when a new date is selected or unselected. This will only be called
     * when the user initiates the date selection.  If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnDateSelectedListener(OnDateSelectedListener)
     */
    public interface OnDateSelectedListener {
        void onDateSelected(Date date);

        void onDateUnselected(Date date);
    }

    /**
     * Interface to be notified when an invalid date is selected by the user. This will only be
     * called when the user initiates the date selection. If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener)
     */
    public interface OnInvalidDateSelectedListener {
        void onInvalidDateSelected(Date date);
    }

    /**
     * Interface used for determining the selectability of a date cell when it is configured for
     * display on the calendar.
     *
     * @see #setDateSelectableFilter(DateSelectableFilter)
     */
    public interface DateSelectableFilter {
        boolean isDateSelectable(Date date);
    }

    /**
     * Interface to be notified when a cell is clicked and possibly intercept the click.  Return true
     * to intercept the click and prevent any selections from changing.
     *
     * @see #setCellClickInterceptor(CellClickInterceptor)
     */
    public interface CellClickInterceptor {
        boolean onCellClicked(Date date);
    }

    private class DefaultOnInvalidDateSelectedListener implements OnInvalidDateSelectedListener {
        @Override
        public void onInvalidDateSelected(Date date) {
            String errMessage =
                    getResources().getString(R.string.invalid_date, fullDateFormat.format(minCal.getTime()),
                            fullDateFormat.format(maxCal.getTime()));
            Toast.makeText(getContext(), errMessage, Toast.LENGTH_SHORT).show();
        }
    }
}