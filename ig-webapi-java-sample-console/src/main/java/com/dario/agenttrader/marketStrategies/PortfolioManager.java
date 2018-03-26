package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.EDIT_POSITION_INSTRUCTION;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.ENTER_MARKET_INSTRUCTION;


import com.dario.agenttrader.utility.IGClientUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        if(thereIsNoOtherPositionOnTheSameEPIC(signal.getEpic())){
            tradingAPI.createPosition(signal.getEpic(),signal.getDirection(),signal.getSize(),signal.getStopDistance());
            LOG.info("Position created:{}",signal);
        }else {
            List<String> oppositPositions =
                    findOpenPositionsOnOppositeDirectionOnTheSameEPIC(signal.getEpic(), signal.getDirection());

            if (!oppositPositions.isEmpty()) {
                String dealID = oppositPositions.get(0);
                tradingAPI.closeOpenPosition(dealID,signal.getEpic(),signal.getDirection(),signal.size);
            } else {
                LOG.info("Enter market signal for {} is ignored", signal.getEpic());
            }
        }
    }



    private boolean thereIsNoOtherPositionOnTheSameEPIC(String epic) {
        boolean thereIsNoOtherPositionOnThisEPIC = findPositionsOnTheSameEpic(epic).isEmpty();
        return thereIsNoOtherPositionOnThisEPIC;
    }

    private List<PositionSnapshot> findPositionsOnTheSameEpic(String epic) {
        Predicate<PositionSnapshot> checkIfMatchesEpic = new Predicate<PositionSnapshot>() {
            @Override
            public boolean test(PositionSnapshot p) {
                boolean test = epic.equalsIgnoreCase(p.getPositionsItem().getMarket().getEpic());
                return test;
            }
        };
        return positionSnapshots.stream().filter(checkIfMatchesEpic).collect(Collectors.toList());
    }

    private List<String> findOpenPositionsOnOppositeDirectionOnTheSameEPIC(String epic, Direction direction) {
        List<String> dealIDsOnTheOppositeDirection = new ArrayList<>();
        List<PositionSnapshot> matchingPositions = findPositionsOnTheSameEpic(epic);
        if(!matchingPositions.isEmpty()){
            Direction positionDirection = IGClientUtility.convertFromIGDirection(
                    matchingPositions.get(0).getPositionsItem().getPosition().getDirection()
            );

            if(direction.opposite().getDirection()==positionDirection.getDirection()){
                dealIDsOnTheOppositeDirection.add(
                        matchingPositions.get(0).getPositionsItem().getPosition().getDealId()
                );
            }
        }

        return dealIDsOnTheOppositeDirection;
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
