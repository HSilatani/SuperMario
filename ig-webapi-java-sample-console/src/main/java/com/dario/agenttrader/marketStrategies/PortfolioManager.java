package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.EDIT_POSITION_INSTRUCTION;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.ENTER_MARKET_INSTRUCTION;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PortfolioManager {

    private final static Logger LOG = LoggerFactory.getLogger(PortfolioManager.class);
    private final TradingAPI tradingAPI;

    private List<PositionSnapshot> positionSnapshots = new ArrayList<>();

    public PortfolioManager(TradingAPI ptradingAPI){
        tradingAPI=ptradingAPI;
    }

    public void processTradingSignal(StrategyActor.TradingSignal signal){
        try {
            if (EDIT_POSITION_INSTRUCTION.equalsIgnoreCase(signal.getInstruction())) {
                executeEditPositionSignal(signal);
            } else if (ENTER_MARKET_INSTRUCTION.equalsIgnoreCase(signal.getInstruction())) {
                executeEnterMarketSignal(signal);
            }
        }catch (Exception ex){
            ex.printStackTrace();
            LOG.warn("Unable to execute signal",ex);
        }
    }

    private void updatePositionsList() {
        List<PositionSnapshot> newPositionSnapshots = null;
        try {
            newPositionSnapshots = tradingAPI.listOpenPositions();
            positionSnapshots = newPositionSnapshots;
        } catch (Exception e) {
            LOG.warn("Unable to refresh list of positions",e);
            e.printStackTrace();
        }
    }

    private void executeEnterMarketSignal(StrategyActor.TradingSignal signal) throws Exception{
        LOG.info(signal.toString());
        updatePositionsList();
        if(isItAllowedToEnterMarket(signal.getEpic())){
            tradingAPI.createPosition(signal.getEpic(),signal.getDirection(),signal.getSize(),signal.getStopDistance());
            LOG.info("Position created:{}",signal);
        }else{
            LOG.info("Enter market signal for {} is ignored",signal.getEpic());
        }
    }

    private boolean isItAllowedToEnterMarket(String epic) {
        Predicate<PositionSnapshot> checkIfMatchesEpic = new Predicate<PositionSnapshot>() {
            @Override
            public boolean test(PositionSnapshot p) {
                boolean test = epic.equalsIgnoreCase(p.getPositionsItem().getMarket().getEpic());
                return test;
            }
        };
        boolean foundPositionOnEPIC =
                positionSnapshots.stream().anyMatch(checkIfMatchesEpic);
        return !foundPositionOnEPIC;
    }

    private void executeEditPositionSignal(StrategyActor.TradingSignal signal){
        try {
            tradingAPI.editPosition(signal.getDealId()
                    ,signal.getNewStopLevel()
                    ,signal.getNewLimitLevel());
        } catch (Exception e) {
            LOG.warn("Unable to execute EDIT Position",e);
            e.printStackTrace();
        }
    }

}
