package com.dario.agenttrader.marketStrategies;

import akka.actor.AbstractActor;
import akka.actor.ActorLogging;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MarketStrategyManager  extends AbstractActor{

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(MarketStrategyManager.class);
    }

    @Override
    public void preStart() {
        log.info("Market Strategy Manager started");
    }

    @Override
    public void postStop() {
        log.info("Market Strategy Manager stopped");

    }

    // No need to handle any messages
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }
}
