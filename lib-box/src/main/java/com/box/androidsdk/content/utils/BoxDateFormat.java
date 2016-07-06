package com.box.androidsdk.content.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Contains methods for parsing and formatting dates for use with the Box API.
 */
public final class BoxDateFormat {
    private static final ThreadLocal<DateFormat> THREAD_LOCAL_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        }
    };

    private static final ThreadLocal<DateFormat> THREAD_LOCAL_ROUND_TO_DAY_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private static final ThreadLocal<DateFormat> THREAD_LOCAL_HEADER_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        }
    };

    private static final SimpleDateFormat format =
            new SimpleDateFormat("yyyy-MM-dd");

    private BoxDateFormat() { }

    /**
     * Parses a date string returned by the Box API into a {@link java.util.Date} object.
     * @param  dateString     a string containing the date.
     * @return                the parsed date.
     * @throws java.text.ParseException if the string cannot be parsed into a valid date.
     */
    public static Date parse(String dateString) throws ParseException {
        return THREAD_LOCAL_DATE_FORMAT.get().parse(dateString);
    }

    /**
     * Formats a date as a string that can be sent to the Box API.
     * @param  date the date to format.
     * @return      a string containing the formatted date.
     */
    public static String format(Date date) {
        String format = THREAD_LOCAL_DATE_FORMAT.get().format(date);
        // Java 6 does not have a convenient way of having the colon in the timezone offset
        return format.substring(0,22) + ":" + format.substring(22);
    }


    /**
     * Parses a date string returned by the Box API into a {@link java.util.Date} object.
     * @param  dateString     a string containing the date.
     * @return                the parsed date.
     * @throws java.text.ParseException if the string cannot be parsed into a valid date.
     */
    public static Date parseRoundToDay(String dateString) throws ParseException {
        return THREAD_LOCAL_ROUND_TO_DAY_DATE_FORMAT.get().parse(dateString);
    }

    /**
     * Formats a date as a string that can be sent to the Box API.
     * @param  date the date to format.
     * @return      a string containing the formatted date.
     */
    public static String formatRoundToDay(Date date) {
        return THREAD_LOCAL_ROUND_TO_DAY_DATE_FORMAT.get().format(date);
    }

    /**
     * Parses a date string returned by the Box API inside headers into a {@link java.util.Date} object.
     * @param  dateString     a string containing the date.
     * @return                the parsed date.
     * @throws java.text.ParseException if the string cannot be parsed into a valid date.
     */
    public static Date parseHeaderDate(String dateString) throws ParseException {
        return THREAD_LOCAL_HEADER_DATE_FORMAT.get().parse(dateString);
    }

    /**
     * Get a String to represent a time range.
     *
     * @param fromDate can use null if don't want to specify this.
     * @param toDate can use null if don't want to specify this.
     * @return The string will be time strings separated by a comma.
     * Trailing "from_date," and leading ",to_date" are also accepted if one of the date is null.
     * Returns null if both dates are null.
     */
    public static String getTimeRangeString(Date fromDate, Date toDate) {
        if (fromDate == null && toDate == null) {
            return null;
        }

        StringBuilder sbr = new StringBuilder();
        if (fromDate != null) {
            sbr.append(format(fromDate));
        }
        sbr.append(",");
        if (toDate != null) {
            sbr.append(format(toDate));
        }
        return sbr.toString();
    }

    /**
     * Get back a from date and to date given a timeRangeString created from BoxDateForm.getTimeRangeString().
     * @param timeRangeString a timeRangeString created from BoxDateForm.getTimeRangeString()
     * @return an array of the from date and to date with Date[0] being from and Date[1] being to. Values can potentially be null.
     */
    public static Date[] getTimeRangeDates(String timeRangeString){
       if (SdkUtils.isEmptyString(timeRangeString)){
           return null;
       }
       String[] dateStrings = timeRangeString.split(",");
        Date[] dates = new Date[2];
        try {
            dates[0] = parse(dateStrings[0]);
        } catch (ParseException e){
        }catch (ArrayIndexOutOfBoundsException e){
        }
        try {
            dates[1] = parse(dateStrings[1]);
        } catch (ParseException e ){
        } catch (ArrayIndexOutOfBoundsException e){
        }
        return dates;

    }

    public static Date convertToDay(Date date) throws ParseException {
        Calendar calendar = Calendar.getInstance((TimeZone.getTimeZone("PST")));
        calendar.setTime(date);
        return parseRoundToDay(formatRoundToDay(calendar.getTime()));
    }
}
