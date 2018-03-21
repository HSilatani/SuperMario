package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.tradingservices.IGClient;

public class IGClientShutDownHook extends Thread {
             @Override
         public void run() {
             try {
                 IGClient.getInstance().disconnect();
                 TradingDataStreamingService.getInstance().stopStreamingService();
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }
}
