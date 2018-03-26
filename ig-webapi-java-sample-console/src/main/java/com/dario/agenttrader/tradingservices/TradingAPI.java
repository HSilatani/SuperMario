package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.domain.CandleResolution;
import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.domain.Direction;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.PricesItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public interface TradingAPI {
    String IDENTIFIER = "identifier";
    String PASSWORD = "password";
    String API_KEY = "apiKey";
    String ACCOUNT_ID="ig.accountID";

    void connect() throws Exception;

    void disconnect() throws Exception;

    void unsubscribeAllLightstreamerListeners() throws Exception;

    void subscribeToLighstreamerAccountUpdates() throws Exception;

    List<PositionSnapshot> listOpenPositions() throws Exception;

    PositionSnapshot getPositionSnapshot(String positionId) throws Exception;


    public void editPosition(String dealId, BigDecimal newStop, BigDecimal newLimit) throws Exception;


    public String createPosition(String epic, Direction direction, BigDecimal size, BigDecimal stopDistance) throws Exception;

    public void subscribeToLighstreamerHeartbeat(HandyTableListenerAdapter listener) throws Exception;

    PositionSnapshot createPositionSnapshot(PositionsItem position);

    public void closeOpenPosition(String dealId,String epic,  Direction direction, BigDecimal size) throws Exception;

    public MarketInfo getMarketInfo(String epic) throws Exception;

    public List<PricesItem> getHistoricPrices(String epic, CandleResolution candleResolution) throws Exception;

    void listWatchlists() throws Exception;

    void subscribeToLighstreamerPriceUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception;

    void subscribeToLighstreamerChartUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception;

    public void subscribeToLighstreamerChartCandleUpdates(String tradeableEpic,String scale, HandyTableListenerAdapter listener) throws Exception;

    void subscribeToOpenPositionUpdates(HandyTableListenerAdapter listener) throws Exception;

    public void unsubscribeLightstreamerForListner(HandyTableListenerAdapter listener) throws Exception;

    AccountsItem accountPreferences();

    void loadAccountPreferences(String accID) throws Exception;

    Locale getLocale();
}
