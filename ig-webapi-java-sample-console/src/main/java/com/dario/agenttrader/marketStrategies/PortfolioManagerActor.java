package com.dario.agenttrader.marketStrategies;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;

import java.util.ArrayList;
import java.util.List;

import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.EDIT_POSITION_INSTRUCTION;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.ENTER_MARKET_INSTRUCTION;

public class PortfolioManagerActor extends AbstractActor{

    private final LoggingAdapter LOG = Logging.getLogger(getContext().getSystem(),this);

    private final TradingAPI tradingAPI;

    private List<PositionSnapshot> positionSnapshots = new ArrayList<>();

    public PortfolioManagerActor(TradingAPI pTradingAPI){
        tradingAPI = pTradingAPI;
    }

    public static final  Props props(TradingAPI ptradingAPI){
        return Props.create(PortfolioManagerActor.class,ptradingAPI);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StrategyActor.TradingSignal.class,this::onTradingSignal)
                .build();
    }

    private void updatePositionsList() {
        List<PositionSnapshot> newPositionSnapshots = null;
        try {
            newPositionSnapshots = tradingAPI.listOpenPositions();
            positionSnapshots = newPositionSnapshots;
        } catch (Exception e) {
            LOG.warning("Unable to refresh list of positions",e);
            e.printStackTrace();
        }
    }

    private void onTradingSignal(StrategyActor.TradingSignal signal) {
        try {


            if (EDIT_POSITION_INSTRUCTION.equalsIgnoreCase(signal.getInstruction())) {
                executeEditPositionSignal(signal);
            } else if (ENTER_MARKET_INSTRUCTION.equalsIgnoreCase(signal.getInstruction())) {
                executeEnterMarketSignal(signal);
            }
        }catch (Exception ex){
            ex.printStackTrace();
            LOG.warning("Unable to execute signal",ex);
        }

    }

    private void executeEnterMarketSignal(StrategyActor.TradingSignal signal) throws Exception{
        LOG.info(signal.toString());
        updatePositionsList();
        if(!isItAllowedToEnterMarket(signal.getEpic())){
            tradingAPI.createPosition(signal.getEpic(),signal.getDirection(),signal.getSize(),signal.getStopDistance());
        }else{
            LOG.info("Enter market signal for {} is ignore",signal.getEpic());
        }
    }

    private boolean isItAllowedToEnterMarket(String epic) {
        //TODO: ENTER MARKET if there is no other position open on this market
        return false;
    }

    private void executeEditPositionSignal(StrategyActor.TradingSignal signal){
        try {
            tradingAPI.editPosition(signal.getDealId()
                   ,signal.getNewStopLevel()
                   ,signal.getNewLimitLevel());
        } catch (Exception e) {
            LOG.warning("Unable to execute EDIT Position",e);
            e.printStackTrace();
        }
    }
}
