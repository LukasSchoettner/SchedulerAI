package com.scheduler.commoncode.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.google.protobuf.Timestamp;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Converts between String/LocalDateTime and gRPC Timestamp for MapStruct.
 */
@Component
public class DateMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Named("stringToLocalDateTime")
    public LocalDateTime asLocalDateTime(String date) {
        return date == null ? null : LocalDateTime.parse(date, FORMATTER);
    }

    @Named("localDateTimeToString")
    public String asString(LocalDateTime date) {
        return date == null ? null : date.format(FORMATTER);
    }

    @Named("localDateTimeToTimestamp")
    public Timestamp asTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        long seconds = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        int nanos = localDateTime.getNano();
        return Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }

    @Named("timestampToLocalDateTime")
    public LocalDateTime asLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos(),
                ZoneOffset.UTC
        );
    }

    @Named("timestampToString")
    public String asString(Timestamp timestamp) {
        if (timestamp == null) return null;
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
                timestamp.getSeconds(), timestamp.getNanos(), ZoneOffset.UTC
        );
        return ldt.format(FORMATTER);
    }

    @Named("stringToTimestamp")
    public Timestamp asTimestamp(String date) {
        if (date == null) return null;
        LocalDateTime localDateTime = LocalDateTime.parse(date, FORMATTER);
        long seconds = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        int nanos = localDateTime.getNano();
        return Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }
}