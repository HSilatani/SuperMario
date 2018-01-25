package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.UpdateEvent;
import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.PushUserException;
import com.lightstreamer.ls_client.UpdateInfo;


import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dario.agenttrader.marketStrategies.MarketManager.*;


//TODO migrate to SubscriberActorRegistery
public class MarketActor extends AbstractActor {
    private final String epic;
    private final TradingAPI tradingAPI;
    private final Set<ActorRef> subscribers;
    private boolean isSubscribing = false;
    private MarketInfo staticMarketInfo = null;
    private Instant staticMarketInfoTimeStamp = null;
    private Duration maxStaticMarketInfoAge = Duration.ofMinutes(120);



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
            isSubscribing = true;
        }catch(PushUserException pex){
                LOG.warning("Unable to subscribe to marketupdates",pex);
        }
    }

    private void subscribeToChartUpdate() throws Exception {
        tradingAPI.subscribeToLighstreamerChartUpdates(
                    epic,
                    new HandyTableListenerAdapter() {
                        @Override
                        public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                            Map<String,String> updateMap = IGClientUtility.extractMarketUpdateKeyValues(updateInfo,i,s);
                            UpdateEvent updateEvent = new UpdateEvent(updateMap,UpdateEvent.MARKET_UPDATE);
                            LOG.info("Chart i {} s {} data {}", i, s, updateInfo);
                            getSelf().tell(new MarketUpdated(epic, updateEvent),getSelf());
                        }
                    }
                    );
    }
    private void subscribeToPriceUpdate() throws Exception {
        tradingAPI.subscribeToLighstreamerPriceUpdates(
                epic,
                new HandyTableListenerAdapter() {
                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        UpdateEvent updateEvent = new UpdateEvent(
                                IGClientUtility.extractMarketUpdateKeyValues(updateInfo,i,s)
                                ,UpdateEvent.MARKET_UPDATE);
                        getSelf().tell(new MarketUpdated(epic, updateEvent),getSelf());
                        LOG.debug("Chart i {} s {} data {}", i, s, updateInfo);
                    }
                }
        );
    }

    @Override
    public void postStop() {
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
        updateStaticMarketInfo();
        MarketInfo marketInfo = new MarketInfo(mupdated.getUpdateEvent());
        marketInfo.setMarketName(staticMarketInfo.getMarketName());
        marketInfo.setMinNormalStopLimitDistance(staticMarketInfo.getMinNormalStopLimitDistance());
        marketInfo.setMinDealSize(staticMarketInfo.getMinDealSize());
        MarketUpdated mupdatedWithStaticInfo = new MarketUpdated(epic,marketInfo);
        subscribers.forEach(subscriber -> subscriber.tell(mupdatedWithStaticInfo,getSelf()));
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


    public static final class MarketUpdated{
        private MarketInfo marketInfo;
        private String epic;

        public MarketUpdated(String pepic,MarketInfo marketInfo){
            this.epic = pepic;
            this.marketInfo = marketInfo;
        }

        public MarketUpdated(String epic,UpdateEvent updateEvent){
            this(epic,new MarketInfo(updateEvent));
        }
        public UpdateEvent getUpdateEvent() {
            return marketInfo.getUpdateEvent();
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
