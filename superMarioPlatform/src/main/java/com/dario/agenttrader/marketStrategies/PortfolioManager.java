package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.domain.Position;
import com.dario.agenttrader.domain.PositionSnapshot;
import com.dario.agenttrader.domain.TradingSignal;
import com.dario.agenttrader.tradingservices.TradingAPI;
import static com.dario.agenttrader.domain.TradingSignal.EDIT_POSITION_INSTRUCTION;
import static com.dario.agenttrader.domain.TradingSignal.ENTER_MARKET_INSTRUCTION;


import com.dario.agenttrader.tradingservices.TradingDataStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class PortfolioManager {

    private final static Logger LOG = LoggerFactory.getLogger(PortfolioManager.class);

    private final TradingAPI tradingAPI;
    private final TradingDataStreamingService tradingDataStreamingService =TradingDataStreamingService.getInstance();
    private volatile boolean isSubscribedtoconfirms = false;

    private long confirmExpiryTimeOutMillis = Duration.ofSeconds(4).toMillis();
    private long positionReconciliationTimoutMillis = Duration.ofSeconds(10).toMillis();

    private PortfolioPositionTracker positionTracker = new PortfolioPositionTracker();

    public PortfolioManager(TradingAPI ptradingAPI){
        tradingAPI=ptradingAPI;
        positionTracker.setDealConfirmationFunction(dealRef-> tradingAPI.confirmPosition(dealRef));
        positionTracker.setPositionReloadSupplier(()->tradingAPI.listOpenPositions());
        positionTracker.setConfirmExpiryTimeOutMili(confirmExpiryTimeOutMillis);
        positionTracker.setPositionReconciliationTimeOutMili(positionReconciliationTimoutMillis);
    }

    public void setEpicCoolingOffPeriodTimeOut(long minutes){
        positionTracker.setEpiccoolingOffPeriod(Duration.ofMinutes(minutes));
    }

    public void processTradingSignal(TradingSignal signal){
        subscribeToConfirms();
        try {
            if (signal.isEditMarketSignal()) {
                executeEditPositionSignal(signal);
            }else if(signal.isExitMarketSignal()){
                executeExitPositionSignal(signal);
            }else if (signal.isEnterMarketSignal()) {
                executeEnterMarketSignal(signal);
            }
        }catch (Exception ex){
            LOG.warn("Unable to execute signal",ex);
        }
    }

    private void executeExitPositionSignal(TradingSignal signal) throws Exception{
        LOG.info("Processing trading signal:{}" , signal.toString());

        boolean noPositionsOnEpic = positionTracker.epicHasNoPosition(signal.getEpic());
        LOG.debug("There is no position to close:{}",noPositionsOnEpic);

        if (noPositionsOnEpic) {
            return;
        } else {
            Position openPositionToClose = positionTracker.getPositionsOnEpic(signal.getEpic(),signal.getDirection());

            if (openPositionToClose!=null){
                LOG.info("Closing position:{} on EPIC:{}", openPositionToClose.getDealId(),openPositionToClose.getEpic());
                tradingAPI.closeOpenPosition(openPositionToClose);
                positionTracker.removePosition(openPositionToClose.getDealRef());
                LOG.info("CLOSED position:{} on EPIC:{}", openPositionToClose.getDealId(),openPositionToClose.getEpic());
            } else {
                LOG.info("IGNORING Exit market signal {}", signal);
            }
        }

    }

    private void subscribeToConfirms() {
        if(!isSubscribedtoconfirms){
            try {
                tradingDataStreamingService.subscribeToDealConfirmStream(
                        this.toString()
                        ,dealConf->positionTracker.confirmPosition(dealConf)
                );
                isSubscribedtoconfirms=true;
            } catch (Exception e) {
                LOG.warn("Unable to subscribe to CONFIRMS",e);
            }
        }
    }

    private void executeEnterMarketSignal(TradingSignal signal) throws Exception{
            LOG.info("Processing trading signal:{}" , signal.toString());
            boolean isEpicInCoolingOffPeriod = positionTracker.isEpicInCoolingOffPeriod(signal.getEpic());

            if(isEpicInCoolingOffPeriod){
                LOG.info("IGNORING Enter market signal for {}", signal.getEpic());
                return;
            }

            boolean noOtherPositionsOnEpic = positionTracker.epicHasNoPosition(signal.getEpic());

            if (noOtherPositionsOnEpic) {
                Position position = tradingAPI.createPosition(signal);
                positionTracker.trackNewPosition(position);
                LOG.info("Position created:{}", signal);

            } else {
                Position oppositePosition = positionTracker.getOppositePosition(signal);

                if (oppositePosition!=null){
                    LOG.info("Closing position:{} on EPIC:{}", oppositePosition.getDealId(),oppositePosition.getEpic());
                    //tradingAPI.closeOpenPosition(oppositePosition);
                    //positionTracker.removePosition(oppositePosition.getDealRef());
                    LOG.info("CLOSED position:{} on EPIC:{}", oppositePosition.getDealId(),oppositePosition.getEpic());
                } else {
                    LOG.info("IGNORING Enter market signal for {}", signal.getEpic());
                }
            }
    }

    private void executeEditPositionSignal(TradingSignal signal){
        try {
            tradingAPI.editPosition(signal.getDealId()
                    ,signal.getNewStopLevel()
                    ,signal.getNewLimitLevel());
        } catch (Exception e) {
            LOG.warn("Unable to execute EDIT Position",e);
            e.printStackTrace();
        }
    }

    public void updatePositionList(List<PositionSnapshot> psnapsots){
        LOG.debug("Updating position list on position add/remove signal",psnapsots);
        positionTracker.reconcilePositions(psnapsots);
    }

    public void initialize() {
        subscribeToConfirms();
    }


}
