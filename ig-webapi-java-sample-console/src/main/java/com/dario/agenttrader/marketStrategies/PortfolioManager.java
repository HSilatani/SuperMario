package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.EDIT_POSITION_INSTRUCTION;
import static com.dario.agenttrader.marketStrategies.StrategyActor.TradingSignal.ENTER_MARKET_INSTRUCTION;


import com.dario.agenttrader.tradingservices.TradingDataStreamingService;
import com.dario.agenttrader.utility.IGClientUtility;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PortfolioManager {

    private final static Logger LOG = LoggerFactory.getLogger(PortfolioManager.class);
    public static final String DEAL_REFERENCE = "dealReference";
    public static final String DEAL_ID = "dealId";
    private final TradingAPI tradingAPI;
    private final TradingDataStreamingService tradingDataStreamingService =TradingDataStreamingService.getInstance();
    private volatile boolean isSubscribedtoconfirms = false;
    private List<PositionSnapshot> positionSnapshots = new ArrayList<>();
    private Instant lastPositionListRefresh = null;
    private Duration positionRefreshTimeOut = Duration.ofSeconds(180);
    private volatile Set<String> confirmUpdateConsumers = new ConcurrentSkipListSet();
    private volatile Instant lastCreatePositionTimeStamp = null;
    private Duration confirmTimeOut = Duration.ofSeconds(2);

    public PortfolioManager(TradingAPI ptradingAPI){
        tradingAPI=ptradingAPI;
    }

    public void processTradingSignal(StrategyActor.TradingSignal signal){
        subscribeToConfirms();
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

    private void subscribeToConfirms() {
        if(!isSubscribedtoconfirms){
            Consumer<UpdateInfo> consumer = u->
            {
                String updateStr = u.getNewValue("CONFIRMS");
                if (updateStr != null) {
                    Map update = new HashMap();
                    try {
                        update = IGClientUtility.flatConfirmMessageMap(updateStr);
                    }catch (Exception e){
                        LOG.warn("Unable to parse CONFIRM message",e);
                    }
                    this.confirmUpdateConsumers.remove(update.get(DEAL_REFERENCE));
                    LOG.info("Trade confirm update data Ref:{}-ID:{},OutstandingConfirms:{}"
                            , update.get(DEAL_REFERENCE)
                            ,update.get(DEAL_ID)
                            ,this.confirmUpdateConsumers.size());
                }
            };
            try {
                tradingDataStreamingService.subscribeToConfirms(this.toString(),consumer);
                isSubscribedtoconfirms=true;
            } catch (Exception e) {
                LOG.warn("Unable to subscribe to CONFIRMS",e);
            }
        }
    }

    public void updatePositionList(List<PositionSnapshot> psnapsots) {
        positionSnapshots=psnapsots;
    }
    private void refreshPositionsList() {
        boolean isRefreshTimOutExpired = true;
        if(lastPositionListRefresh!=null){
            Duration durationSinceLastRefresh = Duration.between(Instant.now(),lastPositionListRefresh);
            isRefreshTimOutExpired = durationSinceLastRefresh.compareTo(positionRefreshTimeOut)>0;
        }

        if(isRefreshTimOutExpired) {
            reloadPositionListFromBroker();
        }
    }

    private void reloadPositionListFromBroker() {
        List<PositionSnapshot> newPositionSnapshots = null;
        try {
            newPositionSnapshots = tradingAPI.listOpenPositions();
            positionSnapshots = newPositionSnapshots;
            this.confirmUpdateConsumers.clear();
            lastPositionListRefresh= Instant.now();
        } catch (Exception e) {
            LOG.warn("Unable to refresh list of positions", e);
            e.printStackTrace();
        }
    }

    private Object enterMarketLock = new Object();

    private void executeEnterMarketSignal(StrategyActor.TradingSignal signal) throws Exception{
        synchronized (enterMarketLock) {
            if (stillWaitingForConfirm()) {
                LOG.info("Cant execute while waiting for confirm. Waiting since{}", lastCreatePositionTimeStamp.toString());
                return;
            }
            LOG.info(signal.toString());
            refreshPositionsList();

            if (thereIsNoOtherPositionOnTheSameEPIC(signal.getEpic())) {
                String dealRef = tradingAPI.createPosition(signal.getEpic(), signal.getDirection(), signal.getSize(), signal.getStopDistance());
                setWaitForConfirmMechnism(dealRef);
                LOG.info("Position created:{}", signal);
            } else {
                List<String> oppositPositions =
                        findOpenPositionsOnOppositeDirectionOnTheSameEPIC(signal.getEpic(), signal.getDirection());

                if (!oppositPositions.isEmpty()) {
                    String dealID = oppositPositions.get(0);
                    LOG.info("Closing position:{} on EPIC:{}", dealID, signal.getEpic());
                    tradingAPI.closeOpenPosition(dealID, signal.getEpic(), signal.getDirection(), signal.size);

                } else {
                    LOG.info("Enter market signal for {} is ignored", signal.getEpic());
                }
            }
        }
    }

    private boolean stillWaitingForConfirm() {
        boolean waitingForConfirm = true;
        waitingForConfirm = confirmUpdateConsumers.size()>0;

        if(waitingForConfirm ){
            if(lastCreatePositionTimeStamp!=null) {
                Duration durationSinceLastPositionCreation = Duration.between(lastCreatePositionTimeStamp, Instant.now());
                waitingForConfirm = durationSinceLastPositionCreation.compareTo(confirmTimeOut) < 0;
            }else{
                waitingForConfirm=false;
                reloadPositionListFromBroker();
            }
        }

        return waitingForConfirm;
    }

    private void setWaitForConfirmMechnism(String dealRef) {
        if(dealRef!=null) {
            confirmUpdateConsumers.add(dealRef);
            lastCreatePositionTimeStamp = Instant.now();
        }
    }


    private boolean thereIsNoOtherPositionOnTheSameEPIC(String epic) {
        List<PositionSnapshot> positionsOnTheSameMarket = findPositionsOnTheSameEpic(epic);
        LOG.info("Positions opened on {}:{}",epic,positionsOnTheSameMarket);
        boolean thereIsNoOtherPositionOnThisEPIC = positionsOnTheSameMarket.isEmpty();
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


    public void initialize() {
        subscribeToConfirms();
    }
}
