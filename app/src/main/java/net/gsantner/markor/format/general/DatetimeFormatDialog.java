/*#######################################################
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatetimeFormatDialog {

    /**
     * @param activity {@link Activity} from which is {@link DatetimeFormatDialog} called
     * @param hlEditor {@link HighlightingEditor} which 'll add selected result to cursor position
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void showDatetimeFormatDialog(final Activity activity, final HighlightingEditor hlEditor) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.time_format_dialog, (ViewGroup) null);

        Locale _locale = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        String[] defaultDatetimeFormats = activity.getBaseContext().getResources().getStringArray(
                R.array.time_date_formats_array);

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        ;
        final EditText timeFormatEditText = viewRoot.findViewById(R.id.datetime_format_input);
        final TextView datetimeTextView = viewRoot.findViewById(R.id.format_example);
        final Button datePickButton = viewRoot.findViewById(R.id.date_format_picker);
        final Button timePickButton = viewRoot.findViewById(R.id.time_format_picker);
        final CheckBox formatInsteadCheckbox = viewRoot.findViewById(R.id.inser_format_check_box);
        formatInsteadCheckbox.setChecked(false);
        final CheckBox alwaysNowCheckBox = viewRoot.findViewById(R.id.always_use_current_time);
        alwaysNowCheckBox.setChecked(false);

        // combo box for format ->> we can write our own format or select one of default formats
        timeFormatEditText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= (v.getWidth() - ((EditText) v).getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    popupWindow.show();
                    return true;
                }
            }
            return false;
        });

        popupWindow.setAdapter(new SimpleAdapter(activity, createAdapterData(defaultDatetimeFormats),
                R.layout.extended_simple_list_item_2, new String[]{"format", "date"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ));
        popupWindow.setAnchorView(timeFormatEditText);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            timeFormatEditText.setText(defaultDatetimeFormats[position]);
            popupWindow.dismiss();
            setToNow(cal, alwaysNowCheckBox.isChecked());
            datetimeTextView.setText(parseDateTimeToCustomFromat(timeFormatEditText.getText().toString(), cal.getTimeInMillis()));
        });

        // check for changes in combo box every 2 sec(delay)
        timeFormatEditText.addTextChangedListener(new TextWatcher() {
            boolean isTyping = false;
            private final int DELAY = 2000;
            private long editTime = System.currentTimeMillis();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isTyping) {
                    isTyping = true;
                } else {
                    if (editTime + DELAY > System.currentTimeMillis()) {
                        isTyping = false;
                        setToNow(cal, alwaysNowCheckBox.isChecked());
                        datetimeTextView.setText(parseDateTimeToCustomFromat(timeFormatEditText.getText().toString(), cal.getTimeInMillis()));
                    }
                }
            }
        });

        // DatePicker Dialog
        datePickButton.setOnClickListener(button -> new DatePickerDialog(activity, (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    datetimeTextView.setText(parseDateTimeToCustomFromat(timeFormatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        );

        // TimePicker Dialog
        timePickButton.setOnClickListener(button -> new TimePickerDialog(activity, (timePicker, hour, min) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, min);
                    datetimeTextView.setText(parseDateTimeToCustomFromat(timeFormatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        );

        // hide buttons when both check box are checked
        View.OnClickListener onOptionsChangedListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean dateChangeable = !formatInsteadCheckbox.isChecked() && !alwaysNowCheckBox.isChecked();
                timePickButton.setEnabled(dateChangeable);
                datePickButton.setEnabled(dateChangeable);
                alwaysNowCheckBox.setEnabled(!formatInsteadCheckbox.isChecked());
            }
        };

        formatInsteadCheckbox.setOnClickListener(onOptionsChangedListener);
        alwaysNowCheckBox.setOnClickListener(onOptionsChangedListener);

        // set builder and implement buttons to discard and submit
        builder.setView(viewRoot)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        setToNow(cal, alwaysNowCheckBox.isChecked());
                        String text = parseDateTimeToCustomFromat(timeFormatEditText.getText().toString(), cal.getTimeInMillis());
                        datetimeTextView.setText(text);
                        hlEditor.insertOrReplaceTextOnCursor(getOutput(
                                formatInsteadCheckbox.isChecked(), text, timeFormatEditText.getText().toString())
                        );
                    }
                });

        builder.show();
    }

    /**
     * @param timeFormat {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime   {@link Long} selected _datetime in milisecond
     * @return formatted _datetime
     */
    private static String parseDateTimeToCustomFromat(String timeFormat, Long datetime) {
        try {
            DateFormat formatter = new SimpleDateFormat(timeFormat);
            return formatter.format(datetime);
        } catch (Exception e) {
            // ToDO: some exception handler about not acceptable format maybe??
            return null;
        }
    }

    /**
     * @param isUseFormatInstead {@link Boolean} information if we want _datetime or format
     * @param datetime           selected _datetime as {@link String} based on given format
     * @param format             {@link String} pattern used to convert _datetime into text output
     * @return @datetime or @format, based on @isUseFormatInstead
     */
    private static String getOutput(Boolean isUseFormatInstead, String datetime, String format) {
        return isUseFormatInstead != null && isUseFormatInstead ? format : datetime;
    }

    /**
     * @param defaultDatetimeFormats {@link String...} contains all default _datetime formats
     * @return extends {@link String...} with preview of given formats
     */
    private static String[] getFormatsAndDatetimeExamples(String[] defaultDatetimeFormats) {
        String[] result = new String[defaultDatetimeFormats.length * 2];
        for (int i = 0; i < defaultDatetimeFormats.length; i++) {
            result[i * 2] = defaultDatetimeFormats[i];
            result[(i * 2) + 1] = parseDateTimeToCustomFromat(
                    defaultDatetimeFormats[i], System.currentTimeMillis());
        }
        return result;
    }

    /**
     * @param defaultDatetimeFormatsWithExample {@link String...} contains all default _datetime formats with preview
     * @return {@link List} of mapped pairs ->> format + preview
     */
    private static List<Map<String, String>> createAdapterData(String[] defaultDatetimeFormatsWithExample) {
        List<Map<String, String>> formatAndDatetimeExample = new ArrayList<>();
        String[] defaultFormatsWithExample = getFormatsAndDatetimeExamples(defaultDatetimeFormatsWithExample);

        for (int i = 0; i < defaultFormatsWithExample.length; i++) {
            Map<String, String> pair = new HashMap<>(2);
            pair.put("format", defaultFormatsWithExample[i++]);
            pair.put("date", defaultFormatsWithExample[i]);
            formatAndDatetimeExample.add(pair);
        }
        return formatAndDatetimeExample;
    }

    /**
     * set cal to current time if doIt is set
     */
    private static void setToNow(final Calendar cal, boolean doIt) {
        if (doIt) {
            cal.setTime(new Date());
        }
    }
}