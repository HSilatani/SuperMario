package com.dario.agenttrader.dto;

import java.math.BigDecimal;
import java.util.Optional;


public class PriceCandle implements Price<PriceCandle>{
    public static final String SECOND = "SECOND";
    public static final String ONE_MINUTE = "1MINUTE";
    public static final String FIVE_MINUTE="5MINUTE";
    public static final String HOUR = "HOUR ";

    private BigDecimal LTV;
    private BigDecimal TTV;
    private String UTM;
    private BigDecimal DAY_OPEN_MID;
    private BigDecimal DAY_NET_CHG_MID;
    private BigDecimal DAY_PERC_CHG_MID;
    private BigDecimal DAY_HIGH;
    private BigDecimal DAY_LOW;
    private BigDecimal OFR_OPEN;
    private BigDecimal OFR_HIGH;
    private BigDecimal OFR_LOW;
    private BigDecimal OFR_CLOSE;
    private BigDecimal BID_OPEN;
    private BigDecimal BID_HIGH;
    private BigDecimal BID_LOW;
    private BigDecimal BID_CLOSE;
    private BigDecimal LTP_OPEN;
    private BigDecimal LTP_HIGH;
    private BigDecimal LTP_LOW;
    private BigDecimal LTP_CLOSE;
    private BigDecimal CONS_END;
    private BigDecimal CONS_TICK_COUNT;

    public PriceCandle(BigDecimal lastTradeVolume, BigDecimal incrementalValue, String UTM, BigDecimal DAY_OPEN_MID, BigDecimal DAY_NET_CHG_MID, BigDecimal DAY_PERC_CHG_MID, BigDecimal DAY_HIGH, BigDecimal DAY_LOW, BigDecimal OFR_OPEN, BigDecimal OFR_HIGH, BigDecimal OFR_LOW, BigDecimal OFR_CLOSE, BigDecimal BID_OPEN, BigDecimal BID_HIGH, BigDecimal BID_LOW, BigDecimal BID_CLOSE, BigDecimal LTP_OPEN, BigDecimal LTP_HIGH, BigDecimal LTP_LOW, BigDecimal LTP_CLOSE, BigDecimal CONS_END, BigDecimal CONS_TICK_COUNT) {
        this.LTV = lastTradeVolume;
        this.TTV = incrementalValue;
        this.UTM = UTM;
        this.DAY_OPEN_MID = DAY_OPEN_MID;
        this.DAY_NET_CHG_MID = DAY_NET_CHG_MID;
        this.DAY_PERC_CHG_MID = DAY_PERC_CHG_MID;
        this.DAY_HIGH = DAY_HIGH;
        this.DAY_LOW = DAY_LOW;
        this.OFR_OPEN = OFR_OPEN;
        this.OFR_HIGH = OFR_HIGH;
        this.OFR_LOW = OFR_LOW;
        this.OFR_CLOSE = OFR_CLOSE;
        this.BID_OPEN = BID_OPEN;
        this.BID_HIGH = BID_HIGH;
        this.BID_LOW = BID_LOW;
        this.BID_CLOSE = BID_CLOSE;
        this.LTP_OPEN = LTP_OPEN;
        this.LTP_HIGH = LTP_HIGH;
        this.LTP_LOW = LTP_LOW;
        this.LTP_CLOSE = LTP_CLOSE;
        this.CONS_END = CONS_END;
        this.CONS_TICK_COUNT = CONS_TICK_COUNT;
    }
    public PriceCandle(Double lastTradeVolume, Double TTV, String UTM, Double DAY_OPEN_MID, Double DAY_NET_CHG_MID, Double DAY_PERC_CHG_MID, Double DAY_HIGH, Double DAY_LOW, Double OFR_OPEN, Double OFR_HIGH, Double OFR_LOW, Double OFR_CLOSE, Double BID_OPEN, Double BID_HIGH, Double BID_LOW, Double BID_CLOSE, Double LTP_OPEN, Double LTP_HIGH, Double LTP_LOW, Double LTP_CLOSE, Double CONS_END, Double CONS_TICK_COUNT) {
        Optional.ofNullable(lastTradeVolume).ifPresent(l->this.LTV=BigDecimal.valueOf(l));
        Optional.ofNullable(TTV).ifPresent(t->this.TTV =BigDecimal.valueOf(t));
        this.UTM = UTM;
        Optional.ofNullable(DAY_OPEN_MID).ifPresent(d-> this.DAY_OPEN_MID =BigDecimal.valueOf(d));
        Optional.ofNullable(DAY_NET_CHG_MID).ifPresent(d->this.DAY_NET_CHG_MID = BigDecimal.valueOf(d));
        Optional.ofNullable(DAY_PERC_CHG_MID).ifPresent(d->this.DAY_PERC_CHG_MID = BigDecimal.valueOf(d));
        Optional.ofNullable(DAY_HIGH).ifPresent(d->this.DAY_HIGH = BigDecimal.valueOf(d));
        Optional.ofNullable(DAY_LOW).ifPresent(d->this.DAY_LOW = BigDecimal.valueOf(d));
        Optional.ofNullable( OFR_OPEN).ifPresent(o->this.OFR_OPEN = BigDecimal.valueOf(o));
        Optional.ofNullable(OFR_HIGH).ifPresent(o-> this.OFR_HIGH = BigDecimal.valueOf(o));
        Optional.ofNullable( OFR_LOW).ifPresent(o->this.OFR_LOW =BigDecimal.valueOf(o));
        Optional.ofNullable( OFR_CLOSE).ifPresent(o->this.OFR_CLOSE =BigDecimal.valueOf(o));
        Optional.ofNullable( BID_OPEN).ifPresent(b->this.BID_OPEN=BigDecimal.valueOf(b));
        Optional.ofNullable( BID_HIGH).ifPresent(b->this.BID_HIGH =BigDecimal.valueOf(b));
        Optional.ofNullable( BID_LOW).ifPresent(b->this.BID_LOW =BigDecimal.valueOf(b));
        Optional.ofNullable( BID_CLOSE).ifPresent(b->this.BID_CLOSE =BigDecimal.valueOf(b));
        Optional.ofNullable( LTP_OPEN).ifPresent(l->this.LTP_OPEN =BigDecimal.valueOf(l));
        Optional.ofNullable( LTP_HIGH).ifPresent(l->this.LTP_HIGH = BigDecimal.valueOf(l));
        Optional.ofNullable( LTP_LOW).ifPresent(l->this.LTP_LOW =BigDecimal.valueOf(l));
        Optional.ofNullable( LTP_CLOSE).ifPresent(l->this.LTP_CLOSE = BigDecimal.valueOf(l) );
        Optional.ofNullable( CONS_END).ifPresent(c-> this.CONS_END =BigDecimal.valueOf(c) );
        Optional.ofNullable( CONS_TICK_COUNT).ifPresent(c-> this.CONS_TICK_COUNT =BigDecimal.valueOf(c));
    }
    public BigDecimal getLastTradeVolume() {
        return LTV;
    }

    public BigDecimal getIncrementalValue() {
        return TTV;
    }

    public String getUTM() {
        return UTM;
    }

    public BigDecimal getDAY_OPEN_MID() {
        return DAY_OPEN_MID;
    }

    public BigDecimal getDAY_NET_CHG_MID() {
        return DAY_NET_CHG_MID;
    }

    public BigDecimal getDAY_PERC_CHG_MID() {
        return DAY_PERC_CHG_MID;
    }

    public BigDecimal getDAY_HIGH() {
        return DAY_HIGH;
    }

    public BigDecimal getDAY_LOW() {
        return DAY_LOW;
    }

    public BigDecimal getOFR_OPEN() {
        return OFR_OPEN;
    }

    public BigDecimal getOFR_HIGH() {
        return OFR_HIGH;
    }

    public BigDecimal getOFR_LOW() {
        return OFR_LOW;
    }

    public BigDecimal getOFR_CLOSE() {
        return OFR_CLOSE;
    }

    public BigDecimal getBID_OPEN() {
        return BID_OPEN;
    }

    public BigDecimal getBID_HIGH() {
        return BID_HIGH;
    }

    public BigDecimal getBID_LOW() {
        return BID_LOW;
    }

    public BigDecimal getBID_CLOSE() {
        return BID_CLOSE;
    }

    public BigDecimal getLTP_OPEN() {
        return LTP_OPEN;
    }

    public BigDecimal getLTP_HIGH() {
        return LTP_HIGH;
    }

    public BigDecimal getLTP_LOW() {
        return LTP_LOW;
    }

    public BigDecimal getLTP_CLOSE() {
        return LTP_CLOSE;
    }

    public BigDecimal getCONS_END() {
        return CONS_END;
    }

    public BigDecimal getCONS_TICK_COUNT() {
        return CONS_TICK_COUNT;
    }

    @Override
    public void mergeWithSnapshot(PriceCandle lastCandle) {
        this.LTV = Optional.ofNullable(this.LTV).orElse(lastCandle.LTV);
        this.TTV = Optional.ofNullable(this.TTV).orElse(lastCandle.TTV);
        this.DAY_OPEN_MID = Optional.ofNullable(this.DAY_OPEN_MID).orElse(lastCandle.DAY_OPEN_MID);
        this.DAY_NET_CHG_MID = Optional.ofNullable(this.DAY_NET_CHG_MID).orElse(lastCandle.DAY_NET_CHG_MID);
        this.DAY_PERC_CHG_MID = Optional.ofNullable(this.DAY_PERC_CHG_MID).orElse(lastCandle.DAY_PERC_CHG_MID);
        this.DAY_HIGH = Optional.ofNullable(this.DAY_HIGH).orElse(lastCandle.DAY_HIGH);
        this.DAY_LOW = Optional.ofNullable(this.DAY_LOW).orElse(lastCandle.DAY_LOW);
        this.OFR_OPEN = Optional.ofNullable(this.OFR_OPEN).orElse(lastCandle.OFR_OPEN);
        this.OFR_HIGH = Optional.ofNullable(this.OFR_HIGH).orElse(lastCandle.OFR_HIGH);
        this.OFR_LOW = Optional.ofNullable(this.OFR_LOW).orElse(lastCandle.OFR_LOW);
        this.OFR_CLOSE = Optional.ofNullable(this.OFR_CLOSE).orElse(lastCandle.OFR_CLOSE);
        this.BID_OPEN = Optional.ofNullable(this.BID_OPEN).orElse(lastCandle.BID_OPEN);
        this.BID_HIGH = Optional.ofNullable(this.BID_HIGH).orElse(lastCandle.BID_HIGH);
        this.BID_LOW = Optional.ofNullable(this.BID_LOW).orElse(lastCandle.BID_LOW);
        this.BID_CLOSE = Optional.ofNullable(this.BID_CLOSE).orElse(lastCandle.BID_CLOSE);
        this.LTP_OPEN = Optional.ofNullable(this.LTP_OPEN).orElse(lastCandle.LTP_OPEN);
        this.LTP_HIGH = Optional.ofNullable(this.LTP_HIGH).orElse(lastCandle.LTP_HIGH);
        this.LTP_LOW = Optional.ofNullable(this.LTP_LOW).orElse(lastCandle.LTP_LOW);
        this.LTP_CLOSE = Optional.ofNullable(this.LTP_CLOSE).orElse(lastCandle.LTP_CLOSE);
        this.CONS_END = Optional.ofNullable(this.CONS_END).orElse(lastCandle.CONS_END);
        this.CONS_TICK_COUNT = Optional.ofNullable(this.CONS_TICK_COUNT).orElse(lastCandle.CONS_TICK_COUNT);
    }

}
