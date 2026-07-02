package com.scheduler.commoncode.mappers;

import java.time.LocalTime;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Converts between String and LocalTime for MapStruct.
 */
@Component
public class LocalTimeMapper {

    @Named("stringToLocalTime")
    public LocalTime toLocalTime(String time) {
        return time == null ? null : LocalTime.parse(time);
    }

    @Named("localTimeToString")
    public String fromLocalTime(LocalTime localTime) {
        return localTime == null ? null : localTime.toString();
    }
}
