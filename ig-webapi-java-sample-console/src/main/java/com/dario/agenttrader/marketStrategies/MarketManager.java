package com.dario.agenttrader.marketStrategies;


import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.dario.agenttrader.utility.ActorRegistery;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.stop;

public class MarketManager extends AbstractActorWithTimers{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final ActorRegistery marketManagerRegistry = new ActorRegistery();

    private final TradingAPI tradingAPI;

    private Instant lastHeartBeat=null;

    private Duration maxDelayFromLastHeartBeat = Duration.ofMillis(10000);

    public MarketManager(TradingAPI pTradingAPI){
        tradingAPI = pTradingAPI;
    }

    public static final Props props(TradingAPI ptradingAPI){
        return Props.create(MarketManager.class,ptradingAPI);
    }
    @Override
    public void preStart() throws Exception{
        LOG.info("MarketManager created");
        subscribeToLighstreamerHeartbeat();
        startHeartBeatTimer();
    }

    private static final class HeartBeatTimer{};
    public static final class ResetLSSubscriptions{};
    private void startHeartBeatTimer() {
        getTimers().startPeriodicTimer(
                "LIGHTSTREAMERHB",new HeartBeatTimer(),
                scala.concurrent.duration.Duration.create(15000,TimeUnit.MILLISECONDS));
    }

    @Override
    public void postStop() {
        LOG.info("MarketManager stopped");
    }

//    private static SupervisorStrategy strategy =
//            new OneForOneStrategy(1, Duration.create(1, TimeUnit.MINUTES),
//                    DeciderBuilder
//                            .match(Exception.class,
//                                    e -> e.getCause().getMessage().contains("Insufficient permissions"),e-> stop())
//                            .matchAny(o -> escalate()).build());
//
//    public SupervisorStrategy supervisorStrategy() {
//        return strategy;
//    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubscribeToMarketUpdate.class,this::onSubscribeToMarket)
                .match(Terminated.class,this::onTerminated)
                .match(HeartBeatTimer.class,this::onHeartbeatTimer)
                .match(ResetLSSubscriptions.class,this::onResetLSSubscriptions)
                .match(ListMarkets.class,this::onListMarkets)
                .build();
    }

    private void onListMarkets(ListMarkets listMarket) {
        Set<String> uniqIds = marketManagerRegistry.getUniqIds();
        ListOfMarkets listOfMArkets = new ListOfMarkets(uniqIds);
        getSender().tell(listOfMArkets,getSelf());
    }

    private void onResetLSSubscriptions(ResetLSSubscriptions resetMsg) {
        try {
            tradingAPI.disconnect();
        }catch(Exception ex){
            LOG.warning("Subscription failure",ex);
        }
        try {
            tradingAPI.connect();
            subscribeToLighstreamerHeartbeat();
            getContext().getChildren().forEach(actor -> actor.tell(resetMsg, getSelf()));
        }catch(Exception ex){
            LOG.warning("Subscription failure",ex);
        }
    }

    private void onHeartbeatTimer(HeartBeatTimer hb) {
        Optional<Instant> lastHB = Optional.ofNullable(lastHeartBeat);
        lastHeartBeat = lastHB.orElse(Instant.now());
        Duration durationSinceLastHB = Duration.between(lastHeartBeat,Instant.now());
        int intIsMaxDelayBreached = durationSinceLastHB.compareTo(maxDelayFromLastHeartBeat);
        if(intIsMaxDelayBreached>-1){
            LOG.warning("HEARBEAT Abscence - {} since last HEARTBEAT",durationSinceLastHB);
            getSelf().tell(new ResetLSSubscriptions(),getSelf());
        }

    }

    private void onTerminated(Terminated t) {
        ActorRef terminatedActor = t.getActor();
        String marketId = marketManagerRegistry.removeActor(terminatedActor);
        LOG.info("Market actor for {} is terminated",marketId);
    }

    private void onSubscribeToMarket(SubscribeToMarketUpdate msg) {

      Props props = MarketActor.props(msg.getEpic(),tradingAPI);
      CreateMarket createMarket = new CreateMarket(msg.getEpic());
      ActorRef marketActor = marketManagerRegistry.registerActorIfAbscent(
              getContext(),props,msg.getEpic(),createMarket);

      marketActor.forward(msg,getContext());

    }

    private void subscribeToLighstreamerHeartbeat() throws Exception {
      LOG.info("Subscribing to Lightstreamer heartbeat");
      tradingAPI.subscribeToLighstreamerHeartbeat(new HandyTableListenerAdapter() {
         @Override
         public void onUpdate(int i, String s, UpdateInfo updateInfo) {
            lastHeartBeat = Instant.now();
            LOG.debug("Heartbeat = " + updateInfo);
         }
      });
    }

    public static final class ListMarkets{

    }

    public static final class ListOfMarkets{
        private Set<String>  markets;

        public ListOfMarkets(Set<String> markets) {
            this.markets = markets;
        }

        public Set<String> getMarkets() {
            return markets;
        }
    }

    public static final class SubscribeToMarketUpdate {
        private final String epic;
        private final ActorRef subscriber;
        public SubscribeToMarketUpdate(String pepic,ActorRef pobserver) {
            this.subscriber = pobserver;
            epic = pepic;
        }

        public ActorRef getSubscriber() {
            return subscriber;
        }

        public String getEpic()
        {
            return epic;
        }
    }

    public static final class CreateMarket{
        private final String epic;
        CreateMarket(String pepic){
            this.epic = pepic;
        }

        public String getEpic() {
            return epic;
        }
    }
}
