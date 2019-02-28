package com.byd.vtdr2.widget;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

public class MyTextClock extends android.widget.TextClock {

    public MyTextClock(Context context) {
        super(context);
        setLocaleDateFormat();
    }

    public MyTextClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLocaleDateFormat();
    }

    public MyTextClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLocaleDateFormat();
    }

    private void setLocaleDateFormat() {
        // You can change language from here
       /* Locale currentLocale = new Locale("","MA");
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getDefault(), currentLocale);

        String dayName = cal.getDisplayName(cal.DAY_OF_WEEK, Calendar.LONG, currentLocale);
        String monthName = cal.getDisplayName(cal.MONTH, Calendar.LONG, currentLocale);

//        this.setFormat12Hour("'" + dayName + "'\n'" + monthName + "' dd");
        this.setFormat24Hour("'" + dayName  + monthName + "' dd");*/
        this.setFormat24Hour(getDateFormate(this.getContext()));
    }

    private String getDateFormate(Context context) {
        return Settings.System.getString(context.getContentResolver(),
                Settings.System.DATE_FORMAT);

    }
}
