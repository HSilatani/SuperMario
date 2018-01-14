package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.dto.MarketUpdate;
import com.dario.agenttrader.tradingservices.IGClient;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;


import java.util.HashSet;
import java.util.Set;

import static com.dario.agenttrader.marketStrategies.MarketManager.*;

public class MarketActor extends AbstractActor {
    private final String epic;
    private final IGClient igClient;
    private final Set<ActorRef> subscribers;



    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    public MarketActor(String pEPIC,IGClient pIGClient){
        subscribers = new HashSet<>();
        epic = pEPIC;
        igClient = pIGClient;
    }

    public static Props props(String pEPIC,IGClient pIGClient){
        return Props.create(MarketActor.class,pEPIC,pIGClient);

    }
    @Override
    public void preStart() throws Exception {
        subscribeToMarketUpdates();
        LOG.info("Market {} registered", epic);
    }

    private void subscribeToMarketUpdates() throws Exception{
        igClient.subscribeToLighstreamerChartUpdates(
                    epic,
                    new HandyTableListenerAdapter() {
                        @Override
                        public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                            MarketUpdate marketUpdate = new MarketUpdate(
                                    IGClientUtility.extractMarketUpdateKeyValues(updateInfo,i,s)
                            );
                            getSelf().tell(new MarketUpdated(epic,marketUpdate),getSelf());
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
                .matchAny(this::onUnHandledMessage)
                .build();
    }

    private void onMarketUpdate(MarketUpdated mupdated) {
        subscribers.forEach(subscriber -> subscriber.tell(mupdated,getSelf()));
    }

    private void onTerminated(Terminated t) {
        ActorRef actor = t.getActor();
        subscribers.remove(actor);
    }

    private void onUnHandledMessage(Object msg) {
        LOG.info("Ignoring message {}",msg);
    }

    private void onSubscribeToMarket(SubscribeToMarketUpdate msg) throws IllegalArgumentException{
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
        private MarketUpdate marketUpdate;
        private String epic;

        public MarketUpdated(String pepic,MarketUpdate mupdate){
            this.epic = pepic;
            this.marketUpdate = mupdate;
        }
        public MarketUpdate getMarketUpdate() {
            return marketUpdate;
        }

        public String getEpic() {
            return epic;
        }
    }

}
