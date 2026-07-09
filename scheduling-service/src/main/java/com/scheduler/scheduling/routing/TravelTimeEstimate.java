package com.scheduler.scheduling.routing;

public record TravelTimeEstimate(Integer minutes, boolean known) {
    public static TravelTimeEstimate unknown() {
        return new TravelTimeEstimate(null, false);
    }

    public static TravelTimeEstimate known(int minutes) {
        return new TravelTimeEstimate(minutes, true);
    }
}
