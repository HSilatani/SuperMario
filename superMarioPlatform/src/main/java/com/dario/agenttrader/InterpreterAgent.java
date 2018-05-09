package com.dario.agenttrader;

import com.dario.agenttrader.bots.SlackBot;
import com.dario.agenttrader.domain.PositionInfo;
import com.dario.agenttrader.domain.PositionSnapshot;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;


import com.dario.agenttrader.marketStrategies.MarketStrategySystem;
import com.dario.agenttrader.actors.Position;
import com.dario.agenttrader.actors.PositionManager;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpreterAgent {


    private TradingAPI tradingAPI = null;
    private MarketStrategySystem marketStrategySystem = null;

    private static InterpreterAgent OneAndOnlyInstance = new InterpreterAgent();
    private SlackBot slackBot = null;

    public static InterpreterAgent getInstance(){
        return OneAndOnlyInstance;
    }


    private static final Logger LOG = LoggerFactory.getLogger(InterpreterAgent.class);

    public void setIGClient(TradingAPI ptradingAPI) {
        this.tradingAPI = ptradingAPI;
    }

    public void setMarketStrategySystem(MarketStrategySystem marketStrategySystem) {
        this.marketStrategySystem = marketStrategySystem;
    }

    public void startAgent(){
        marketStrategySystem.getPositionManagerActor().tell(new PositionManager.LoadPositionsRequest(),
                marketStrategySystem.getPositionManagerActor());
    }

    public String respond(String queryCommand){
        String reply = "Oops 0_0";

        try {
            List<PositionSnapshot> positionSnapshots = tradingAPI.listOpenPositionsWithProfitAndLoss();
            reply = formatPositionList(positionSnapshots);

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
                tradingAPI.getLocale()).format(money);
        return strPandL;
    }


    public void sendMessage(Position.PositionUpdatedDelta positionupdated) {
        String message = ":)";
        message = positionupdated.getDelta().keySet().stream()
                .filter(k -> PositionInfo.LEVEL_KEY.equals(k)||PositionInfo.STOP_LEVEL_KEY.equals(k)||PositionInfo.LIMIT_LEVEL_KEY.equals(k)||PositionInfo.STATUS_KEY.equals(k))
                .map(k -> k + ":" + positionupdated.getDelta().get(k)[0] + "->" + positionupdated.getDelta().get(k)[1])
                .collect(Collectors.joining(","));
        message = positionupdated.getEpic() + "/" + positionupdated.getPositionId() +" : "+ message;
        slackBot.sendMessage(message);
    }

    public void setSlackBot(SlackBot pslackBot) {
        this.slackBot = pslackBot;
    }
}
