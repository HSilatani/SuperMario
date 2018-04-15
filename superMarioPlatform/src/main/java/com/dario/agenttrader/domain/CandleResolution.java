package com.dario.agenttrader.domain;

import com.dario.agenttrader.dto.PriceCandle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CandleResolution {
    private String candleInterval;
    private Duration candleBarDuration;

    private static Map<Integer,String> candleIntervals = new HashMap();
    static {
        candleIntervals.put(1,PriceCandle.ONE_MINUTE);
        candleIntervals.put(2,PriceCandle.TWO_MINUTE);
        candleIntervals.put(3,PriceCandle.THREE_MINUTE);
        candleIntervals.put(5,PriceCandle.FIVE_MINUTE);
        candleIntervals.put(60,PriceCandle.HOUR);
    }

    private static final CandleResolution FIVE_MINUTE_RESOLUTION = new CandleResolution(5);
    private static final CandleResolution THREE_MINUTE_RESOLUTION = new CandleResolution(3);
    private static final CandleResolution TWO_MINUTE_RESOLUTION = new CandleResolution(2);
    private static final CandleResolution ONE_MINUTE_RESOLUTION = new CandleResolution(1);
    private static final CandleResolution ONE_HOUR_RESOLUTION = new CandleResolution(60);

    public static CandleResolution fiveMinuteResolution(){
        return FIVE_MINUTE_RESOLUTION;
    }
    public static CandleResolution twoMinuteResolution() {
        return TWO_MINUTE_RESOLUTION;
    }

    public static CandleResolution threeMinuteResolution() {
        return THREE_MINUTE_RESOLUTION;
    }

    public static CandleResolution oneMinuteResolution(){
        return ONE_MINUTE_RESOLUTION;
    }

    public static CandleResolution oneHourResolution(){
        return ONE_HOUR_RESOLUTION;
    }

    private CandleResolution(int interval){
        candleBarDuration = Duration.ofMinutes(interval);
        candleInterval = candleIntervals.get(interval);

    }

    public String getCandleInterval() {
        return candleInterval;
    }

    public Duration getCandleBarDuration() {
        return candleBarDuration;
    }

    public static CandleResolution findMatchingResolutionOrDefaultToFiveMin(String subscriptionKey) {
        CandleResolution candleResolution=CandleResolution.fiveMinuteResolution();
        Integer candleInterval = candleIntervals.entrySet().stream()
                .filter(e ->subscriptionKey.contains(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(5);

        candleResolution= new CandleResolution(candleInterval);


        return candleResolution;
    }


}
