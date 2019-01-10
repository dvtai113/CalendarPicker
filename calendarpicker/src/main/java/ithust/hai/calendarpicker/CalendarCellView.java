// Copyright 2013 Square, Inc.

package ithust.hai.calendarpicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CalendarCellView extends FrameLayout {
    private static final int[] STATE_SELECTABLE = {
            R.attr.tsquare_state_selectable
    };
    private static final int[] STATE_CURRENT_MONTH = {
            R.attr.tsquare_state_current_month
    };
    private static final int[] STATE_TODAY = {
            R.attr.tsquare_state_today
    };

    private boolean isSelectable = false;
    private boolean isCurrentMonth = false;
    private boolean isToday = false;
    private TextView dayOfMonthTextView;

    public CalendarCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSelectable(boolean isSelectable) {
        if (this.isSelectable != isSelectable) {
            this.isSelectable = isSelectable;
            refreshDrawableState();
        }
    }

    public void setCurrentMonth(boolean isCurrentMonth) {
        if (this.isCurrentMonth != isCurrentMonth) {
            this.isCurrentMonth = isCurrentMonth;
            refreshDrawableState();
        }
    }

    public void setToday(boolean isToday) {
        if (this.isToday != isToday) {
            this.isToday = isToday;
            refreshDrawableState();
        }
    }

    public void setRangeState(byte rangeState) {
        switch (rangeState) {
            case RangeState.NONE:
                if (!isSelected()) {
                    setBackgroundColor(Color.WHITE);
                } else {
                    setBackgroundResource(R.drawable.calendar_date_picker_selected);
                }
                break;
            case RangeState.START_WEEK:
                setBackgroundResource(R.drawable.calendar_date_picker_selected_start_week);
                break;
            case RangeState.END_WEEK:
                setBackgroundResource(R.drawable.calendar_date_picker_selected_end_week);
                break;
            case RangeState.MIDDLE:
                setBackgroundResource(R.drawable.calendar_date_picker_selected_middle);
                break;
            case RangeState.FIRST:
                setBackgroundResource(R.drawable.calendar_date_picker_selected_left);
                break;
            case RangeState.LAST:
                setBackgroundResource(R.drawable.calendar_date_picker_selected_right);
                break;
        }
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    public boolean isSelectable() {
        return isSelectable;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 5);

        if (isSelectable) {
            mergeDrawableStates(drawableState, STATE_SELECTABLE);
        }

        if (isCurrentMonth) {
            mergeDrawableStates(drawableState, STATE_CURRENT_MONTH);
        }

        if (isToday) {
            mergeDrawableStates(drawableState, STATE_TODAY);
            if (dayOfMonthTextView != null) {
                dayOfMonthTextView.setTypeface(null, Typeface.BOLD);
            }
        } else {
            if (dayOfMonthTextView != null) {
                dayOfMonthTextView.setTypeface(null, Typeface.NORMAL);
            }
        }

        return drawableState;
    }

    public void setDayOfMonthTextView(TextView textView) {
        dayOfMonthTextView = textView;
    }

    public TextView getDayOfMonthTextView() {
        return dayOfMonthTextView;
    }
}