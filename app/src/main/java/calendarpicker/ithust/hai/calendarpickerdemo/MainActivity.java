package calendarpicker.ithust.hai.calendarpickerdemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import ithust.hai.calendarpicker.CalendarPickerView;
import ithust.hai.calendarpicker.DefaultDayViewAdapter;
import ithust.hai.calendarpicker.SelectionMode;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SampleTimesSquareActivi";
    private CalendarPickerView calendar;
    private AlertDialog theDialog;
    private CalendarPickerView dialogView;
    private final Set<Button> modeButtons = new LinkedHashSet<Button>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 100);

        final Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);

        calendar = findViewById(R.id.calendar_view);
        calendar.init(new Date(), nextYear.getTime()) //
                .inMode(SelectionMode.SINGLE) //
                .withSelectedDate(new Date());

        initButtonListeners(nextYear, lastYear);
    }

    private void initButtonListeners(final Calendar nextYear, final Calendar lastYear) {
        final Button single = findViewById(R.id.button_single);
        final Button multi = findViewById(R.id.button_multi);
        final Button highlight = findViewById(R.id.button_highlight);
        final Button range = findViewById(R.id.button_range);
        final Button displayOnly = findViewById(R.id.button_display_only);
        final Button dialog = findViewById(R.id.button_dialog);
        final Button customized = findViewById(R.id.button_customized);
        final Button decorator = findViewById(R.id.button_decorator);
        final Button hebrew = findViewById(R.id.button_hebrew);
        final Button arabic = findViewById(R.id.button_arabic);
        final Button arabicDigits = findViewById(R.id.button_arabic_with_digits);
        final Button customView = findViewById(R.id.button_custom_view);

        modeButtons.addAll(Arrays.asList(single, multi, range, displayOnly, decorator, customView));

        single.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsEnabled(single);

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                calendar.init(new Date(), nextYear.getTime()) //
                        .inMode(SelectionMode.SINGLE) //
                        .withSelectedDate(new Date());
            }
        });

        multi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsEnabled(multi);

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                Calendar today = Calendar.getInstance();
                ArrayList<Date> dates = new ArrayList<Date>();
                for (int i = 0; i < 5; i++) {
                    today.add(Calendar.DAY_OF_MONTH, 3);
                    dates.add(today.getTime());
                }
                calendar.init(new Date(), nextYear.getTime()) //
                        .inMode(SelectionMode.MULTIPLE) //
                        .withSelectedDates(dates);
            }
        });

        highlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setButtonsEnabled(highlight);

                Calendar c = Calendar.getInstance();
                c.setTime(new Date());

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                calendar.init(getDateWithYear(2000), getDateWithYear(2020))
                        .inMode(SelectionMode.SINGLE).withSelectedDate(c.getTime());
            }
        });

        range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsEnabled(range);

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                Calendar today = Calendar.getInstance();
                ArrayList<Date> dates = new ArrayList<Date>();
                today.add(Calendar.DATE, 3);
                dates.add(today.getTime());
                today.add(Calendar.DATE, 5);
                dates.add(today.getTime());
                calendar.init(new Date(), nextYear.getTime()) //
                        .inMode(SelectionMode.RANGE) //
                        .withSelectedDates(dates);
            }
        });

        displayOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsEnabled(displayOnly);

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                calendar.init(new Date(), nextYear.getTime()) //
                        .inMode(SelectionMode.SINGLE) //
                        .withSelectedDate(new Date()) //
                        .displayOnly();
            }
        });

        dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = "I'm a dialog!";
                showCalendarInDialog(title, R.layout.dialog);
                dialogView.init(lastYear.getTime(), nextYear.getTime()) //
                        .withSelectedDate(new Date());
            }
        });

        customized.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarInDialog("Pimp my calendar!", R.layout.dialog_customized);
                dialogView.init(lastYear.getTime(), nextYear.getTime()).withSelectedDate(new Date());
            }
        });

        decorator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsEnabled(decorator);

                calendar.setCustomDayView(new DefaultDayViewAdapter());
                calendar.init(lastYear.getTime(), nextYear.getTime()) //
                        .inMode(SelectionMode.SINGLE) //
                        .withSelectedDate(new Date());
            }
        });

        hebrew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarInDialog("I'm Hebrew!", R.layout.dialog);
                dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("iw", "IL")) //
                        .withSelectedDate(new Date());
            }
        });

        arabic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarInDialog("I'm Arabic!", R.layout.dialog);
                dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("ar", "EG")) //
                        .withSelectedDate(new Date());
            }
        });

        arabicDigits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarInDialog("I'm Arabic with Digits!", R.layout.dialog_digits);
                dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("ar", "EG")) //
                        .withSelectedDate(new Date());
            }
        });

        customView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setButtonsEnabled(customView);

                calendar.setCustomDayView(new SampleDayViewAdapter());
                calendar.init(lastYear.getTime(), nextYear.getTime())
                        .inMode(SelectionMode.SINGLE)
                        .withSelectedDate(new Date());
            }
        });

        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Selected time in millis: " + calendar.getSelectedDate().getTime());
                String toast = "Selected: " + calendar.getSelectedDate().getTime();
                Toast.makeText(MainActivity.this, toast, LENGTH_SHORT).show();
            }
        });
    }

    private void showCalendarInDialog(String title, int layoutResId) {
        dialogView = (CalendarPickerView) getLayoutInflater().inflate(layoutResId, null, false);
        theDialog = new AlertDialog.Builder(this) //
                .setTitle(title)
                .setView(dialogView)
                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        theDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Log.d(TAG, "onShow: fix the dimens!");
                dialogView.fixDialogDimens();
            }
        });
        theDialog.show();
    }

    private void setButtonsEnabled(Button currentButton) {
        for (Button modeButton : modeButtons) {
            modeButton.setEnabled(modeButton != currentButton);
        }
    }

    private Date getDateWithYear(int year) {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean applyFixes = theDialog != null && theDialog.isShowing();
        if (applyFixes) {
            Log.d(TAG, "Config change: unfix the dimens so I'll get remeasured!");
            dialogView.unfixDialogDimens();
        }
        super.onConfigurationChanged(newConfig);
        if (applyFixes) {
            dialogView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Config change done: re-fix the dimens!");
                    dialogView.fixDialogDimens();
                }
            });
        }
    }
}