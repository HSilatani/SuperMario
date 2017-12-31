package com.dario.agenttrader;

import com.dario.agenttrader.dto.PositionSnapshot;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.StringJoiner;


import com.dario.agenttrader.dto.PositionUpdate;
import com.dario.agenttrader.marketStrategies.MarketStrategySystem;
import com.dario.agenttrader.marketStrategies.PositionManager;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpreterAgent {


    private IGClient igClient = null;
    private MarketStrategySystem marketStrategySystem = null;

    private static InterpreterAgent OneAndOnlyInstance = new InterpreterAgent();

    public static InterpreterAgent getInstance(){
        return OneAndOnlyInstance;
    }


    private static final Logger LOG = LoggerFactory.getLogger(InterpreterAgent.class);

    public void setIGClient(IGClient igClient) {
        this.igClient = igClient;
    }

    public void setMarketStrategySystem(MarketStrategySystem marketStrategySystem) {
        this.marketStrategySystem = marketStrategySystem;
    }

    public String respond(String queryCommand){
        String reply = "Oops 0_0";

        try {
            List<PositionSnapshot> positionSnapshots = igClient.listOpenPositions();
            reply = formatPositionList(positionSnapshots);

            igClient.subscribeToOpenPositionUpdates(new HandyTableListenerAdapter() {

                    @Override
                    public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                        PositionUpdate positionUpdate = new PositionUpdate(
                                IGClientUtility.flatJSontoMap(updateInfo.getNewValue(1)),s,i);

                        if (updateInfo.getNewValue("OPU") != null) {
                            LOG.info("Position update i {} s {} data {}", i, s, updateInfo);
                            marketStrategySystem.getPositionManagerActor().tell(
                                    new PositionManager.OPU(positionUpdate),
                                    marketStrategySystem.getPositionManagerActor());
                        }

                    }
                }
            );
//            igClient.subscribeToLighstreamerPriceUpdates(
//                    positionSnapshots.get(2).getPositionsItem().getMarket().getEpic()
//            );
            igClient.subscribeToLighstreamerChartUpdates(
                    positionSnapshots.get(2).getPositionsItem().getMarket().getEpic()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return reply;
    }

    public String formatPositionList(List<PositionSnapshot> positions){

        StringJoiner positionsStr = new StringJoiner("\n\n");
        positionsStr.add("Ammo amir");


        positions.stream().forEach(p -> positionsStr.add(formatPosition(p)));

        LOG.info(positionsStr.toString());
        return positionsStr.toString();


    }

    public String formatPosition(PositionSnapshot positionSnap){

        StringJoiner positionStr = new StringJoiner("|","","");

        PositionsItem position = positionSnap.getPositionsItem();

        String name = position.getMarket().getInstrumentName();
        positionStr.add(name.substring(0, Math.min(name.length(), 3)));
        positionStr.add(""+position.getPosition().getDirection());
        positionStr.add(""+position.getPosition().getSize());
        String pl = formatMoney(positionSnap.getProfitLoss());
        int idx = pl.indexOf(45);
        pl= ((idx>=0)?":cry:"+pl:":smile:"+pl);
        positionStr.add(""+ pl);
        int intMoveDirection = positionSnap.getMarketMoveToday().compareTo(new BigDecimal(0));
        String strMoveDirection = new String(":left_right_arrow:");
        strMoveDirection = (intMoveDirection>0)?":arrow_up:":":arrow_down:";
        positionStr.add(strMoveDirection);

        return  positionStr.toString();
    }


    public String formatMoney(BigDecimal money){
        String strPandL = "NA";
        strPandL = NumberFormat.getCurrencyInstance(
                igClient.getLocale()).format(money);
        return strPandL;
    }


}
