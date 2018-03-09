package com.dario.agenttrader.marketStrategies;

import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.tradingservices.TradingAPI;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.PricesItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarketUpdateProducerTradingAPIMock implements TradingAPI {

    @Override
    public void connect() throws Exception {

    }

    @Override
    public void disconnect() throws Exception {

    }

    @Override
    public void unsubscribeAllLightstreamerListeners() throws Exception {

    }

    @Override
    public void subscribeToLighstreamerAccountUpdates() throws Exception {

    }

    @Override
    public List<PositionSnapshot> listOpenPositions() throws Exception {
        return null;
    }

    @Override
    public PositionSnapshot getPositionSnapshot(String positionId) throws Exception {
        return null;
    }

    @Override
    public void editPosition(String dealId, BigDecimal newStop, BigDecimal newLimit) throws Exception {

    }

    @Override
    public void subscribeToLighstreamerHeartbeat(HandyTableListenerAdapter listener) throws Exception {

    }

    @Override
    public PositionSnapshot createPositionSnapshot(PositionsItem position) {
        return null;
    }

    @Override
    public MarketInfo getMarketInfo(String epic) throws Exception {
        MarketInfo marketInfo = new MarketInfo();
        marketInfo.setMinDealSize(new BigDecimal(1));
        marketInfo.setMarketName(epic);
        marketInfo.setMinNormalStopLimitDistance(new BigDecimal(40));
        return marketInfo;
    }

    @Override
    public List<PricesItem> getHistoricPrices(String epic) throws Exception {
        return null;
    }

    @Override
    public void listWatchlists() throws Exception {

    }

    List<HandyTableListenerAdapter> priceSubscribers = new ArrayList<>();
    List<HandyTableListenerAdapter> chartSubscribers = new ArrayList<>();
    public void generateChartCandleUpdate(){
        UpdateInfo chartsUpdateInfo = new UpdateInfo() {
            String candleUpdate = "[ , null, 1519682340000, 31477.5, 357.9, 1.14, 31868.4, 31421, 31847.9, 31847.9, 31842.8, 31845.4, 31827.9, 31827.9, 31822.8, 31825.4, , , , , 1, 28 ]";

            @Override
            public String toString(){
                return candleUpdate;
            }
            @Override
            public int getItemPos() {
                return 0;
            }

            @Override
            public String getItemName() {
                return null;
            }

            @Override
            public boolean isValueChanged(int i) {
                return false;
            }

            @Override
            public boolean isValueChanged(String s) {
                return false;
            }

            @Override
            public String getNewValue(int i) {
                return null;
            }

            @Override
            public String getNewValue(String s) {
                return null;
            }

            @Override
            public String getOldValue(int i) {
                return null;
            }

            @Override
            public String getOldValue(String s) {
                return null;
            }

            @Override
            public int getNumFields() {
                return 0;
            }

            @Override
            public boolean isSnapshot() {
                return false;
            }
        };
        UpdateInfo priceUpdateInfo = new UpdateInfo() {

            String updateStr = "[ 1016.73, 1028.73, null, 1, 1516227922478, 1051.25, -2.71, 1076.1, 1005.6 ]";
            @Override
            public String toString(){
                return updateStr;
            }
            @Override
            public int getItemPos() {
                return 0;
            }

            @Override
            public String getItemName() {
                return null;
            }

            @Override
            public boolean isValueChanged(int i) {
                return false;
            }

            @Override
            public boolean isValueChanged(String s) {
                return false;
            }

            @Override
            public String getNewValue(int i) {
                return updateStr;
            }

            @Override
            public String getNewValue(String s) {
                return null;
            }

            @Override
            public String getOldValue(int i) {
                return null;
            }

            @Override
            public String getOldValue(String s) {
                return null;
            }

            @Override
            public int getNumFields() {
                return 0;
            }

            @Override
            public boolean isSnapshot() {
                return false;
            }
        };
        chartSubscribers.stream().forEach(l->l.onUpdate(1,"s",chartsUpdateInfo));
    }
    @Override
    public void subscribeToLighstreamerPriceUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception {
        priceSubscribers.add(listener);
    }

    @Override
    public void subscribeToLighstreamerChartUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception {
        chartSubscribers.add(listener);
    }

    @Override
    public void subscribeToLighstreamerChartCandleUpdates(String tradeableEpic, String scale, HandyTableListenerAdapter listener) throws Exception {
        chartSubscribers.add(listener);
    }

    @Override
    public void subscribeToOpenPositionUpdates(HandyTableListenerAdapter listener) throws Exception {

    }

    @Override
    public void unsubscribeLightstreamerForListner(HandyTableListenerAdapter listener) throws Exception {

    }

    @Override
    public AccountsItem accountPreferences() {
        return null;
    }

    @Override
    public void loadAccountPreferences(String accID) throws Exception {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}

