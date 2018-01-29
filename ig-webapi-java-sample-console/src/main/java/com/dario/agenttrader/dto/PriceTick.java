package com.dario.agenttrader.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PriceTick {


    BigDecimal bid;
    BigDecimal offer;
    BigDecimal lastTradePrice;
    BigDecimal lastTradeVolume;
    String utm;
    BigDecimal dayOpenMid;
    BigDecimal dayPercentageChangeMid;
    BigDecimal dayHigh;
    BigDecimal dayLow;

    public PriceTick(BigDecimal bid, BigDecimal offer, BigDecimal lastTradePrice, BigDecimal plastTradeVolume, String utm, BigDecimal dayOpenMid, BigDecimal dayPercentageChangeMid, BigDecimal dayHigh, BigDecimal dayLow) {
        this.bid = bid;
        this.offer = offer;
        this.lastTradePrice = lastTradePrice;
        this.lastTradeVolume = plastTradeVolume;
        this.utm = utm;
        this.dayOpenMid = dayOpenMid;
        this.dayPercentageChangeMid = dayPercentageChangeMid;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
    }

    public PriceTick(Double pbid, Double poffer, Double plastTradePrice, Double plastTradeVolume, String utm, Double pdayOpenMid, Double pdayPercentageChangeMid, Double pdayHigh, Double pdayLow) {
        Optional.ofNullable(pbid).ifPresent(b->bid=BigDecimal.valueOf(b));
        Optional.ofNullable(poffer).ifPresent(o->offer=BigDecimal.valueOf(o));
        Optional.ofNullable(plastTradePrice).ifPresent(l->lastTradePrice=BigDecimal.valueOf(l));
        Optional.ofNullable(plastTradeVolume).ifPresent(l->lastTradeVolume=BigDecimal.valueOf(l));
        this.utm = utm;
        Optional.ofNullable(pdayOpenMid).ifPresent(d->dayOpenMid=BigDecimal.valueOf(d));
        Optional.ofNullable(pdayPercentageChangeMid).ifPresent(d->dayPercentageChangeMid=BigDecimal.valueOf(d));
        Optional.ofNullable(pdayHigh).ifPresent(d->dayHigh=BigDecimal.valueOf(d));
        Optional.ofNullable(pdayLow).ifPresent(d-> dayLow=BigDecimal.valueOf(d));
    }

    public BigDecimal getBid() {
        return bid;
    }

    public BigDecimal getOffer() {
        return offer;
    }

    public BigDecimal getLastTradePrice() {
        return lastTradePrice;
    }

    public BigDecimal getGetLastTradeVolume() {
        return lastTradeVolume;
    }

    public String getUtm() {
        return utm;
    }

    public BigDecimal getDayOpenMid() {
        return dayOpenMid;
    }

    public BigDecimal getDayPercentageChangeMid() {
        return dayPercentageChangeMid;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
    }

    public void mergeWithSnapshot(PriceTick lastTick) {
        bid = Optional.ofNullable(this.bid).orElse(lastTick.bid);
        offer= Optional.ofNullable(this.offer).orElse(lastTick.offer);
        lastTradePrice= Optional.ofNullable(this.lastTradePrice).orElse(lastTick.lastTradePrice);
        lastTradeVolume= Optional.ofNullable(this.lastTradeVolume).orElse(lastTick.lastTradeVolume);
        dayOpenMid= Optional.ofNullable(this.dayOpenMid).orElse(lastTick.dayOpenMid);
        dayPercentageChangeMid= Optional.ofNullable(this.dayPercentageChangeMid).orElse(lastTick.dayPercentageChangeMid);
        dayHigh= Optional.ofNullable(this.dayHigh).orElse(lastTick.dayHigh);
        dayLow= Optional.ofNullable(this.dayLow).orElse(lastTick.dayLow);
    }
}
