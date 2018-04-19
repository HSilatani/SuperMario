package com.dario.agenttrader.domain;

public class Direction {
    final int direction;

    public static final int SELL_INT=-1;
    public static final int BUY_INT=1;

    public static Direction BUY(){
        return new Direction(BUY_INT);
    }

    public static  Direction SELL(){
        return  new Direction(SELL_INT);
    }

    private Direction(int pDirection){
        direction=pDirection;
    }

    public int getDirection() {
        return direction;
    }

    public boolean isSell() {
        return direction==SELL_INT;
    }

    public boolean isBuy() {
        return direction==BUY_INT;
    }
    //TODO:test
    public boolean isInOppositDirection(Direction targetDirection){
        return (this.getDirection()+targetDirection.getDirection())==0;
    }

    @Override
    public String toString() {
        String strDirection="";
        if(SELL_INT==this.getDirection()){
            strDirection="SELL";
        }else if(BUY_INT==this.getDirection()){
            strDirection="BUY";
        }
        return strDirection;
    }

    public Direction opposite() {

        if(this.isBuy()) {
            return Direction.SELL();
        }else{
            return Direction.BUY();
        }
    }
}
