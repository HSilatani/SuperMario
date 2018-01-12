package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.dario.agenttrader.tradingservices.IGClient;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;

public class MarketManager extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    public static final Props props(){
        return Props.create(MarketManager.class);
    }
    @Override
    public void preStart() {
        LOG.info("MarketManager created");
    }

    @Override
    public void postStop() {
        LOG.info("MarketManager stopped");
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubscribeToMarketUpdate.class,this::onSubscribeToMarket)
                .build();
    }

    private void onSubscribeToMarket(SubscribeToMarketUpdate msg) {
      IGClient igClient =   IGClient.getInstance();

      String tradeableEpic = msg.getEpic();
      if (tradeableEpic != null) {
          try {
              igClient.subscribeToLighstreamerChartUpdates(tradeableEpic,
                        new HandyTableListenerAdapter(){
                            @Override
                            public void onUpdate(int i, String s, UpdateInfo updateInfo){
                                 LOG.info("Chart i {} s {} data {}", i, s, updateInfo);
                            }
                        }
                        );
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
    }

    public static final class SubscribeToMarketUpdate {
        private final String epic;
        public SubscribeToMarketUpdate(String pepic) {
            epic = pepic;
        }

        public String getEpic() {
            return epic;
        }
    }
}
