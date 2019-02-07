package com.checkmarx.engine.utils;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import com.checkmarx.engine.rest.model.ScanRequest;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;

public class ScanUtils {

    private ScanUtils() {
        // static class
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return new org.joda.time.format.DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                // append Z if offset is zero (UTC)
                //.appendTimeZoneOffset("Z", true, 2, 2)
                // create formatter
                .toFormatter();
    }

    public static final DateTimeFormatter FORMATTER = ScanUtils.getDateTimeFormatter();

    public static String printDate(DateTime date) {
        if (date == null) return null;
        return date.toInstant().toString(FORMATTER);
    }

    public static void sortQueue(List<ScanRequest> queue) {

        final Ordering<ScanRequest> byQueuedOn = Ordering
                .natural()
                .nullsFirst()
                .onResultOf(new Function<ScanRequest, DateTime>() {
                    @Override
                    public DateTime apply(ScanRequest input) {
                        return input.getQueuedOn();
                    };
                });

        final Ordering<ScanRequest> byDateCreated = Ordering
                .natural()
                .nullsFirst()
                .onResultOf(new Function<ScanRequest, DateTime>() {
                    @Override
                    public DateTime apply(ScanRequest input) {
                        return input.getDateCreated();
                    };
                });

        queue.sort(byQueuedOn.thenComparing(byDateCreated));
    }

}