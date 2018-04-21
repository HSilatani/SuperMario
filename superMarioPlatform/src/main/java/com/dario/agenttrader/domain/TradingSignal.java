package com.dario.agenttrader.domain;

import java.math.BigDecimal;
import java.util.StringJoiner;

public final class TradingSignal {
    public static final String ENTER_MARKET_INSTRUCTION="ENTER MARKET";
    public static final String EDIT_POSITION_INSTRUCTION="EDIT POSITION";
    String instruction ="";
    BigDecimal newStopLevel;
    BigDecimal newLimitLevel;
    String dealId;
    String epic;
    Direction direction;
    BigDecimal size;
    BigDecimal stopDistance;
    public static TradingSignal createEnterMarketSignal(
            String pEPIC
            ,Direction pDirection
            ,BigDecimal pSize
            ,BigDecimal pStopDistance){

        TradingSignal newSingal =  new TradingSignal();
        newSingal.setInstruction(ENTER_MARKET_INSTRUCTION);
        newSingal.setDirection(pDirection);
        newSingal.setEpic(pEPIC);
        newSingal.setStopDistance(pStopDistance);
        newSingal.setSize(pSize);

        return newSingal;
    }

    public static TradingSignal createEditPositionSignal(
            String pdealId
            , BigDecimal pnewStopLevel
            , BigDecimal pnewLimitLevel
    ){
        TradingSignal newSingal =  new TradingSignal();
        newSingal.setInstruction(EDIT_POSITION_INSTRUCTION);
        newSingal.setDealId(pdealId);
        newSingal.setNewStopLevel(pnewStopLevel);
        newSingal.setNewLimitLevel(pnewLimitLevel);


        return newSingal;
    }

    private TradingSignal(){

    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public void setNewStopLevel(BigDecimal newStopLevel) {
        this.newStopLevel = newStopLevel;
    }

    public void setNewLimitLevel(BigDecimal newLimitLevel) {
        this.newLimitLevel = newLimitLevel;
    }

    public BigDecimal getStopDistance() {
        return stopDistance;
    }

    public void setStopDistance(BigDecimal stopDistance) {
        this.stopDistance = stopDistance;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getEpic() {
        return epic;
    }

    public void setEpic(String epic) {
        this.epic = epic;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public BigDecimal getSize() {
        return size;
    }

    public void setSize(BigDecimal size) {
        this.size = size;
    }

    public String getInstruction() {
        return instruction;
    }

    public BigDecimal getNewStopLevel(){
        return newStopLevel;
    }

    public BigDecimal getNewLimitLevel() {
        return newLimitLevel;
    }

    public String getDealId() {
        return dealId;
    }

    @Override
    public String toString() {
        StringJoiner instruction = new StringJoiner(",");
        if(EDIT_POSITION_INSTRUCTION.equalsIgnoreCase(this.getInstruction())){
            instruction.add(this.getInstruction());
            instruction.add(this.getEpic());
            instruction.add("DealID="+this.getDealId());
            instruction.add("STOP="+getNewStopLevel());
            instruction.add("LIMIT="+getNewLimitLevel());

        }else if(ENTER_MARKET_INSTRUCTION.equalsIgnoreCase(this.getInstruction())){
            instruction.add(this.getInstruction());
            instruction.add(this.getEpic());
            instruction.add("Direction="+this.getDirection().toString());
            instruction.add("Size="+this.getSize());
            instruction.add("STOP="+getStopDistance());
            instruction.add("LIMIT="+getNewLimitLevel());
        }
        return instruction.toString();
    }
}
