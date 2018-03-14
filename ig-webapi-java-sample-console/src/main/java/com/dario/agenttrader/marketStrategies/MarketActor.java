package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.*;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.Calculator;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.PricesItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.PushUserException;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.dario.agenttrader.marketStrategies.MarketManager.*;

//TODO: Cant recover for bad input or failure
public class MarketActor extends AbstractActor {

    private final String epic;
    private final TradingAPI tradingAPI;
    private final Set<ActorRef> subscribers;
    private boolean isSubscribing = false;
    private HandyTableListenerAdapter lightStreamerChartTickListner;
    private HandyTableListenerAdapter lightStreamerChartCandleListner;
    private MarketInfo staticMarketInfo = null;
    private PriceTick lastTick = null;
    private PriceCandle lastCandle = null;
    private Instant staticMarketInfoTimeStamp = null;
    private Duration maxStaticMarketInfoAge = Duration.ofMinutes(120);
    private TimeSeries priceTimeSeries=null;
    private BaseBar currentBar=null;



    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);
    private final Logger PRICE_LOGGER = LoggerFactory.getLogger("PRICE_LOGGER");

    public MarketActor(String pEPIC,TradingAPI ptradingAPI){
        subscribers = new HashSet<>();
        epic = pEPIC;
        tradingAPI = ptradingAPI;
    }

    public static Props props(String pEPIC,TradingAPI pTradingAPI){
        return Props.create(MarketActor.class,pEPIC,pTradingAPI);

    }
    @Override
    public void preStart() throws Exception {
        LOG.info("Market {} registered", epic);
    }

    private void subscribeToMarketUpdates() throws Exception{
        try {
            subscribeToChartTickUpdate();
            subscribeToChartCandleUpdate();
            setupPriceTimeSeries();
            retrieveHistoricPrices();
            isSubscribing = true;
        }catch(PushUserException pex){
                LOG.warning("Unable to subscribe to marketupdates for EPIC={} , Exception={}",epic,pex);
                pex.printStackTrace();
        }
    }

    private void retrieveHistoricPrices() {
        List<PricesItem> historicPrices = new ArrayList<>();

        try {
            historicPrices = tradingAPI.getHistoricPrices(epic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(historicPrices!=null) {
            historicPrices.forEach(p -> {
                PriceCandle pCandle = IGClientUtility.extractMarketPriceCandle(p);
                addATickToPriceTimeSeries(pCandle);
            });
        }

    }

    private void setupPriceTimeSeries() {
        if(priceTimeSeries==null){
            priceTimeSeries=new BaseTimeSeries(epic);
            priceTimeSeries.setMaximumBarCount((int) Duration.ofDays(1).get(ChronoUnit.SECONDS));
        }
    }
    private void subscribeToChartTickUpdate() throws Exception {
        HandyTableListenerAdapter subscriptionListner = new HandyTableListenerAdapter() {
            @Override
            public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                PriceTick chartPriceTick = IGClientUtility.extractMarketPriceTick(updateInfo);
                MarketUpdate<PriceTick> marketUpdate = new MarketUpdate(chartPriceTick,staticMarketInfo);
                LOG.info("Chart i {} s {} data {}", i, s, updateInfo);
                getSelf().tell(new MarketUpdated(epic, marketUpdate),getSelf());
            }
        };
        lightStreamerChartTickListner = subscriptionListner;
        tradingAPI.subscribeToLighstreamerChartUpdates(epic, lightStreamerChartTickListner);
    }
    private void subscribeToChartCandleUpdate() throws Exception {

        HandyTableListenerAdapter subsrciptionListener = new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        PriceCandle chartCandle = IGClientUtility.extractMarketPriceCandle(updateInfo);
                        MarketUpdate<PriceCandle> marketUpdate = new MarketUpdate(chartCandle,staticMarketInfo);
                        LOG.debug("ChartCandle i {} s {} data {}", i, s, updateInfo);
                        getSelf().tell(new MarketUpdated(epic, marketUpdate),getSelf());
                    }
                };
        lightStreamerChartCandleListner = subsrciptionListener;
        tradingAPI.subscribeToLighstreamerChartCandleUpdates(epic,PriceCandle.FIVE_MINUTE,lightStreamerChartCandleListner);
    }

    @Override
    public void postStop() {
        if(lightStreamerChartTickListner !=null){
            try {
                tradingAPI.unsubscribeLightstreamerForListner(lightStreamerChartTickListner);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(lightStreamerChartCandleListner !=null){
            try {
                tradingAPI.unsubscribeLightstreamerForListner(lightStreamerChartCandleListner);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LOG.info("Market {} unregistered", epic);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubscribeToMarketUpdate.class,this::onSubscribeToMarket)
                .match(MarketUpdated.class,this::onMarketUpdate)
                .match(Terminated.class,this::onTerminated)
                .match(MarketManager.ResetLSSubscriptions.class,this::onResetLSSubscription)
                .match(GiveMarketInfo.class,this::onGiveMarketInfo)
                .match(ListSubscribers.class,this::onListSubscribers)
                .matchAny(this::onUnHandledMessage)
                .build();
    }

    private void onListSubscribers(ListSubscribers listSubscribers) {
        if(!this.epic.equalsIgnoreCase(listSubscribers.getEpic())){
            LOG.warning("ListSubscriber request: Bad request from {} for epic {}. expecting epic{} ",
                    getSender(),listSubscribers.getEpic(),epic);
        }
        SubscribersList subscribersList =
                new SubscribersList(epic,subscribers.toArray(new ActorRef[subscribers.size()]));

        getSender().tell(subscribersList,getSelf());
    }

    private void onGiveMarketInfo(GiveMarketInfo giveMarketInfo) throws Exception{
        updateStaticMarketInfo();

        getSender().tell(new MarketInfoResponse(staticMarketInfo),getSelf());
    }

    private void updateStaticMarketInfo() throws Exception {
        boolean isLastUpdateExpired = isLastUpdateExpired();

        if(isLastUpdateExpired){
            staticMarketInfo = tradingAPI.getMarketInfo(epic);
            staticMarketInfoTimeStamp = Instant.now();
        }
    }

    private boolean isLastUpdateExpired() {
        Optional<MarketInfo> optStaticMarketInfo = Optional.ofNullable(staticMarketInfo);
        Optional<Instant> optLastStaticMarketTimeStamp = Optional.ofNullable(staticMarketInfoTimeStamp);

        boolean isLastUpdateExpired = !optStaticMarketInfo.isPresent();
        isLastUpdateExpired = isLastUpdateExpired && !optLastStaticMarketTimeStamp.isPresent();
        isLastUpdateExpired =
                (!isLastUpdateExpired)?Instant.now().isAfter(staticMarketInfoTimeStamp.plus(maxStaticMarketInfoAge)):isLastUpdateExpired;
        return isLastUpdateExpired;
    }

    private void onResetLSSubscription(ResetLSSubscriptions reset) throws Exception{
      subscribeToMarketUpdates();
    }
    private void onMarketUpdate(MarketUpdated mupdated) throws Exception{
        stopMarkeActorIfThereAreNoSubscribers();
        updateStaticMarketInfo();
        mergePriceWithSnapshot((Price)mupdated.getMarketupdate().getUpdate());
        updatePriceTimeSeriese(mupdated);
        MarketUpdated mupdatedWithStaticInfo = createMArketUpdatedMessage(mupdated);
        subscribers.forEach(subscriber -> subscriber.tell(mupdatedWithStaticInfo,getSelf()));
    }

    private void updatePriceTimeSeriese(MarketUpdated mupdated) {
        if(mupdated.getMarketupdate().getUpdate() instanceof PriceCandle){
            addATickToPriceTimeSeries((PriceCandle)mupdated.getMarketupdate().getUpdate());
        }
    }

    private MarketUpdated createMArketUpdatedMessage(MarketUpdated mupdated) {
        MarketUpdate priceMarketUpdate = mupdated.getMarketupdate();
        priceMarketUpdate.setTimeSeries(priceTimeSeries);
        MarketInfo marketInfo = createMarketInfo();
        priceMarketUpdate.setMarketInfo(marketInfo);
        return new MarketUpdated(epic,priceMarketUpdate);
    }


    private MarketInfo createMarketInfo() {
        MarketInfo marketInfo = new MarketInfo();
        marketInfo.setMarketName(staticMarketInfo.getMarketName());
        marketInfo.setMinNormalStopLimitDistance(staticMarketInfo.getMinNormalStopLimitDistance());
        marketInfo.setMinDealSize(staticMarketInfo.getMinDealSize());
        return marketInfo;
    }

    private void addATickToPriceTimeSeries(PriceCandle priceCandle) {
        //BigDecimal hiring = Calculator.convertStrToBigDecimal(priceCandle.getUTM()).get();
        //Instant fromEpochMilli = Instant.ofEpochMilli(Long.valueOf(hiring.longValue()));
        //ZonedDateTime zt = ZonedDateTime.ofInstant(Instant.from(fromEpochMilli), ZoneId.of("Europe/London"));
        ZonedDateTime barOpenTime = Calculator.zonedDateTimeFromString(priceCandle.getUTM());
        BaseBar newbar = new BaseBar(
                Duration.ofMinutes(5)
                ,barOpenTime
                ,Decimal.valueOf(priceCandle.getBID_OPEN().doubleValue())
                ,Decimal.valueOf(priceCandle.getBID_HIGH().doubleValue())
                ,Decimal.valueOf(priceCandle.getBID_LOW().doubleValue())
                ,Decimal.valueOf(priceCandle.getBID_CLOSE().doubleValue())
                ,Decimal.valueOf(Optional.ofNullable(priceCandle.getLastTradeVolume()).orElse(BigDecimal.ZERO).doubleValue()));

        if(currentBar!=null){
            if(newbar.getEndTime().isAfter(currentBar.getEndTime())){
                registerLastBarInPriceTimeSeries(currentBar);
                currentBar=newbar;
            }
        }else if(currentBar == null){
            currentBar = newbar;
        }

        LOG.info("EPIC= {} Price Tick Count: {}",epic,priceTimeSeries.getBarCount());
    }


    private void registerLastBarInPriceTimeSeries(Bar bar) {
        try{
            priceTimeSeries.addBar(bar);
            PRICE_LOGGER.info("EPIC {}:{}-{},CLOSE {}, OPEN {},Max {} , Min {}"
                    ,epic,
                    bar.getBeginTime()
                    ,bar.getEndTime()
                    ,bar.getClosePrice()
                    ,bar.getOpenPrice()
                    ,bar.getMaxPrice()
                    ,bar.getMinPrice()
            );
        }catch(Exception ex){
            LOG.warning("Unable to add price tick. lsat tick:{} , new tick:{}",
                    lastCandle.getBID_CLOSE(),bar.getClosePrice());
            LOG.warning("unable to add price tick to timeseries",ex);
        }
    }

    private void stopMarkeActorIfThereAreNoSubscribers() {
        if(subscribers.size()<1){
            getContext().stop(getSelf());
        }
    }

    private Price mergePriceWithSnapshot(Price price){
        if( price instanceof PriceTick){
            lastTick=Optional.ofNullable(lastTick).orElse((PriceTick)price);
            price.mergeWithSnapshot(lastTick);
        }else if(price instanceof PriceCandle){
            lastCandle = Optional.ofNullable(lastCandle).orElse((PriceCandle)price);
            ((PriceCandle) price).mergeWithSnapshot(lastCandle);
        }
        return price;
    }

    private void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        subscribers.remove(actor);
    }

    private void onUnHandledMessage(Object msg) {
        LOG.info("Ignoring message {}",msg);
    }

    private void onSubscribeToMarket(SubscribeToMarketUpdate msg) throws Exception{
        if(!isSubscribing){
            subscribeToMarketUpdates();
        }
        String tradeableEpic = msg.getEpic();
        if(!epic.equalsIgnoreCase(tradeableEpic)){
            throw new IllegalArgumentException(
                    "Not responsible for {"+tradeableEpic+"} - expected {"+epic+"}");
        }

        ActorRef subscriber = msg.getSubscriber();
        subscribers.add(subscriber);
        getContext().watch(subscriber);

    }


    public static final class MarketUpdated<T>{
        private String epic;
        private MarketUpdate<T> marketupdate;


        public MarketUpdated(String pEPIC,MarketUpdate mUpdate){
            marketupdate = mUpdate;
            this.epic=pEPIC;
        }

        public MarketUpdate<T> getMarketupdate() {
            return marketupdate;
        }

        public String getEpic() {
            return epic;
        }
    }

    public static final class GiveMarketInfo{
        private final String epic;
        public GiveMarketInfo(String pEPIC){
            epic=pEPIC;
        }

        public String getEpic() {
            return epic;
        }
    }

    public static final class MarketInfoResponse{
        private final MarketInfo marketInfo;

        MarketInfoResponse(MarketInfo minfo){
            marketInfo = minfo;
        }

        public MarketInfo getMarketInfo() {
            return marketInfo;
        }
    }

    public static final class ListSubscribers{
        private final String epic;

        public ListSubscribers(String epic) {
            this.epic = epic;
        }

        public String getEpic() {
            return epic;
        }
    }

    public static final class SubscribersList {
        private final ActorRef[] subscribers;
        private final String epic;
        public SubscribersList(String pEPIC, ActorRef[] actorRefs) {
            epic = pEPIC;
            subscribers = actorRefs;
        }

        public ActorRef[] getSubscribers() {
            return subscribers;
        }

        public String getEpic() {
            return epic;
        }

        public List<ActorRef> getSubscribersList() {
            return Arrays.asList(subscribers);
        }
    }
}
