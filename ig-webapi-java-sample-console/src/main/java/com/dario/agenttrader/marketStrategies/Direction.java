package com.dario.agenttrader.marketStrategies;

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
}
