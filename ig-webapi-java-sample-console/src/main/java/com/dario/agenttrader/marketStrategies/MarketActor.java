package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.MarketUpdate;
import com.dario.agenttrader.dto.PriceTick;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.PushUserException;
import com.lightstreamer.ls_client.UpdateInfo;
import org.ta4j.core.BaseTick;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.dario.agenttrader.marketStrategies.MarketManager.*;


public class MarketActor extends AbstractActor {
    private final String epic;
    private final TradingAPI tradingAPI;
    private final Set<ActorRef> subscribers;
    private boolean isSubscribing = false;
    private HandyTableListenerAdapter lightStreamerListner;
    private MarketInfo staticMarketInfo = null;
    private PriceTick lastTick = null;
    private Instant staticMarketInfoTimeStamp = null;
    private Duration maxStaticMarketInfoAge = Duration.ofMinutes(120);
    private TimeSeries priceTimeSeries=null;



    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

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
            subscribeToChartUpdate();
            subscribeToPriceUpdate();
            setupPriceTimeSeries();
            isSubscribing = true;
        }catch(PushUserException pex){
                LOG.warning("Unable to subscribe to marketupdates",pex);
        }
    }

    private void setupPriceTimeSeries() {
        if(priceTimeSeries==null){
            priceTimeSeries=new BaseTimeSeries(epic);
            priceTimeSeries.setMaximumTickCount((int) Duration.ofDays(1).get(ChronoUnit.SECONDS));
        }
    }
//TODO should send MarketUpdate message
    private void subscribeToChartUpdate() throws Exception {
        HandyTableListenerAdapter subscriptionListner = new HandyTableListenerAdapter() {
            @Override
            public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                PriceTick chartPriceTick = IGClientUtility.extractMarketPriceTick(updateInfo);
                MarketUpdate<PriceTick> marketUpdate = new MarketUpdate(chartPriceTick,staticMarketInfo);
                LOG.info("Chart i {} s {} data {}", i, s, updateInfo);
                getSelf().tell(new MarketUpdated(epic, marketUpdate),getSelf());
            }
        };
        lightStreamerListner = subscriptionListner;
        tradingAPI.subscribeToLighstreamerChartUpdates(epic,lightStreamerListner);
    }
    private void subscribeToPriceUpdate() throws Exception {
//        tradingAPI.subscribeToLighstreamerPriceUpdates(
//                epic,
//                new HandyTableListenerAdapter() {
//                    @Override
//                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
//                        UpdateEvent updateEvent = new UpdateEvent(
//                                IGClientUtility.extractMarketUpdateKeyValues(updateInfo,i,s)
//                                ,UpdateEvent.MARKET_UPDATE);
//                        getSelf().tell(new MarketUpdated(epic, updateEvent),getSelf());
//                        LOG.debug("Chart i {} s {} data {}", i, s, updateInfo);
//                    }
//                }
//        );
    }

    @Override
    public void postStop() {
        if(lightStreamerListner!=null){
            try {
                tradingAPI.unsubscribeLightstreamerForListner(lightStreamerListner);
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
                .matchAny(this::onUnHandledMessage)
                .build();
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
        mupdated =  mergeMarketUpdate(mupdated);
        MarketUpdate<PriceTick> priceTickMarketUpdate = mupdated.getMarketupdate();
        MarketInfo marketInfo = getMarketInfo();
        priceTickMarketUpdate.setMarketInfo(marketInfo);
        MarketUpdated mupdatedWithStaticInfo = new MarketUpdated(epic,priceTickMarketUpdate);
        //change price subscription to minute resolution
        //addATickToPriceTimeSeries(marketInfo);
        subscribers.forEach(subscriber -> subscriber.tell(mupdatedWithStaticInfo,getSelf()));
    }

    private MarketInfo getMarketInfo() {
        MarketInfo marketInfo = new MarketInfo();
        marketInfo.setMarketName(staticMarketInfo.getMarketName());
        marketInfo.setMinNormalStopLimitDistance(staticMarketInfo.getMinNormalStopLimitDistance());
        marketInfo.setMinDealSize(staticMarketInfo.getMinDealSize());
        return marketInfo;
    }

    private void addATickToPriceTimeSeries(PriceTick priceTick) {
//TODO implement with chartsCandle

        LOG.info("Price Tick Count: {}",priceTimeSeries.getTickCount());
    }

    private void stopMarkeActorIfThereAreNoSubscribers() {
        if(subscribers.size()<1){
            getContext().stop(getSelf());
        }
    }

    private MarketUpdated mergeMarketUpdate(MarketUpdated<PriceTick> mupdated) {
        if(lastTick ==null){
            lastTick = mupdated.getMarketupdate().getUpdate();
        }else{
           PriceTick newPricTick = mupdated.getMarketupdate().getUpdate();
           newPricTick.mergeWithSnapshot(lastTick);
        }
        return  mupdated;
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

        ActorRef sender = getSender();
        ActorRef subscriber = msg.getSubscriber();
        subscribers.add(subscriber);
        getContext().watch(subscriber);

    }


    public static final class MarketUpdated<T>{
        private MarketInfo marketInfo;
        private String epic;
        private MarketUpdate<T> marketupdate;

        public MarketUpdated(String pepic,MarketInfo marketInfo){
            this.epic = pepic;
            this.marketInfo = marketInfo;
        }

        public MarketUpdated(String pEPIC,MarketUpdate mUpdate){
            marketupdate = mUpdate;
            this.epic=pEPIC;
        }

        public MarketUpdate<T> getMarketupdate() {
            return marketupdate;
        }

        public MarketInfo getMarketInfo() {
            return marketInfo;
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
}
