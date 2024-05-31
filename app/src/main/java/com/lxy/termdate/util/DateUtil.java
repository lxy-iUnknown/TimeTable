package com.lxy.termdate.util;

import com.lxy.termdate.BuildConfig;
import com.lxy.termdate.contract.Contract;
import com.lxy.termdate.contract.Operator;
import com.lxy.termdate.contract.Value;

import java.time.LocalDate;

public class DateUtil {
    private static final int DAYS_IN_WEEK = 7;
    private static final int MONDAY = 1;
    private static final int SUNDAY = DAYS_IN_WEEK;

    /**
     * Get day-of-week(0 to 6) from days since Unix epoch. Modified from {@link LocalDate#getDayOfWeek()}
     *
     * @param epochDay Days since Unix epoch
     * @return Day of week
     */
    private static long dayOfWeekFromEpochDay(long epochDay) {
        return (epochDay + 3) % DAYS_IN_WEEK;
    }

    /**
     * Calculate week count, assuming start >= 0, end>= 0 and start <= end<br>
     * This algorithm is modified from this <a href="https://blog.csdn.net/xqyunyun/article/details/115191308">week count algorithm</a><br>
     * The following code is equivalent to: <br>
     * <code>
     * // Days between start date and previous first day of week <br>
     * int d1 = start - dayOfWeekFromEpochDay(start) + MONDAY; <br>
     * // Days between end date and following first day of week <br>
     * int d2 = end + SUNDAY - dayOfWeekFromEpochDay(end); <br>
     * return 1 + (d2 - d1) / DAYS_IN_WEEK; <br>
     * </code>
     *
     * @param start Start date (days since Unix epoch)
     * @param end   End date (days since Unix epoch)
     * @return Week count
     */
    private static long weekCountInternal(long start, long end) {
        if (BuildConfig.DEBUG) {
            var startDate = new Value<>("startDate", start);
            var endDate = new Value<>("endDate", end);
            Contract.requireOperation(startDate, Value.ZERO_L, Operator.GE);
            Contract.requireOperation(endDate, Value.ZERO_L, Operator.GE);
            Contract.requireOperation(startDate, endDate, Operator.LE);
        }
        return 1 + ((end - dayOfWeekFromEpochDay(end)) -
                (start - dayOfWeekFromEpochDay(start)) +
                (SUNDAY - MONDAY)) / DAYS_IN_WEEK;
    }

    public static long now() {
        final long DIVIDEND = 24 * 60 * 60 * 1000;

        var date = System.currentTimeMillis() / DIVIDEND;
        if (date < BuildConfig.MIN_DATE) {
            date = BuildConfig.MIN_DATE;
        }
        if (date > BuildConfig.MAX_DATE) {
            date = BuildConfig.MAX_DATE;
        }
        return date;
    }

    public static boolean isEven(long start, long end) {
        return (weekCount(start, end) % 2) != 0;
    }

    public static long weekCount(long start, long end) {
        if (start <= end) {
            return weekCountInternal(start, end);
        }
        return -weekCountInternal(end, start);
    }
}
