package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.dto.PositionSnapshot;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;

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

    PositionSnapshot createPositionSnapshot(PositionsItem position);

    void listWatchlists() throws Exception;

    void subscribeToLighstreamerPriceUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception;

    void subscribeToLighstreamerChartUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception;

    void subscribeToOpenPositionUpdates(HandyTableListenerAdapter listener) throws Exception;

    AccountsItem accountPreferences();

    void loadAccountPreferences(String accID) throws Exception;

    Locale getLocale();
}
