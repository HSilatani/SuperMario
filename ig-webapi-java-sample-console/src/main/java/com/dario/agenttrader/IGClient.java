package com.dario.agenttrader;

import com.dario.agenttrader.dto.PositionSnapshot;
import com.iggroup.webapi.samples.PropertiesUtil;
import com.iggroup.webapi.samples.client.RestAPI;
import com.iggroup.webapi.samples.client.StreamingAPI;
import com.iggroup.webapi.samples.client.rest.AuthenticationResponseAndConversationContext;
import com.iggroup.webapi.samples.client.rest.ConversationContext;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.GetAccountsV1Response;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV2.CurrenciesItem;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV2.GetMarketDetailsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV2.MarketOrderPreference;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.GetPositionsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.TimeInForce;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.CreateOTCPositionV1Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.OrderType;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesByDateRangeV2.GetPricesByDateRangeV2Response;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesByNumberOfPointsV2.GetPricesByNumberOfPointsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.session.createSessionV2.CreateSessionV2Request;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.GetWatchlistByWatchlistIdV1Response;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.MarketStatus;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.MarketsItem;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistsV1.GetWatchlistsV1Response;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistsV1.WatchlistsItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;


import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IGClient {
    private static IGClient OneAndOnlyIGClient = new IGClient();

    public static IGClient getInstance(){
        return OneAndOnlyIGClient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(IGClient.class);
    public static final String IDENTIFIER = "identifier";
    public static final String PASSWORD = "password";
    public static final String API_KEY = "apiKey";
    public static final String ACCOUNT_ID="ig.accountID";

    private Calculator cal = new Calculator();

    private final ApplicationBootStrapper applicationBootStrapper;
    AuthenticationResponseAndConversationContext authenticationContext = null;
    ArrayList<HandyTableListenerAdapter> listeners = new ArrayList<HandyTableListenerAdapter>();
    String tradeableEpic = null;
    AtomicBoolean receivedConfirm = new AtomicBoolean(false);
    AtomicBoolean receivedOPU = new AtomicBoolean(false);

    private StreamingAPI streamingAPI;
    private RestAPI restAPI;


    private AccountsItem accountSetting;

    private Locale locale;



    private IGClient(){
        String[] args = new String[3];
        args[0] = PropertiesUtil.getProperty(IDENTIFIER);
        args[1] = PropertiesUtil.getProperty(PASSWORD);
        args[2] = PropertiesUtil.getProperty(API_KEY);

        this.applicationBootStrapper = new ApplicationBootStrapper(args);
        init();
    }

    private void init() {
        streamingAPI = applicationBootStrapper.streamingAPI();
        restAPI = applicationBootStrapper.restAPI();


    }

    public void connect() throws Exception {
       String identifier = applicationBootStrapper.identifier();
       String password = applicationBootStrapper.password();
       String apiKey = applicationBootStrapper.apiKey();

        LOG.info("Connecting as {}", identifier);

        boolean encrypt = Boolean.TRUE;

        CreateSessionV2Request authRequest = new CreateSessionV2Request();
        authRequest.setIdentifier(identifier);
        authRequest.setPassword(password);
        authRequest.setEncryptedPassword(encrypt);
        authenticationContext =
                restAPI.createSession(authRequest, apiKey, encrypt);
        streamingAPI.connect(
                authenticationContext.getAccountId(),
                authenticationContext.getConversationContext(),
                authenticationContext.getLightstreamerEndpoint());

        loadAccountPreferences(authenticationContext.getAccountId());
    }

    public void disconnect() throws Exception {
        unsubscribeAllLightstreamerListeners();
        streamingAPI.disconnect();
    }

    public void unsubscribeAllLightstreamerListeners() throws Exception {

        for (HandyTableListenerAdapter listener : listeners) {
            streamingAPI.unsubscribe(listener.getSubscribedTableKey());
        }
    }

    public void subscribeToLighstreamerAccountUpdates() throws Exception {

        LOG.info("Subscribing to Lightstreamer account updates");
        listeners.add(streamingAPI
                .subscribeForAccountBalanceInfo(
                        authenticationContext.getAccountId(),
                        new HandyTableListenerAdapter() {
                            @Override
                            public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                                LOG.info("Account balance info = " + updateInfo);
                            }
                        }));

    }
    
    public List<PositionSnapshot> listOpenPositions() throws Exception {

        ConversationContext conversationContext = authenticationContext.getConversationContext();
        GetPositionsV2Response positionsResponse = restAPI.getPositionsV2(conversationContext);
      LOG.info("Open positions Amoo Amir: {}", positionsResponse.getPositions().size());

      List<PositionSnapshot> positionSnapshotList = positionsResponse.getPositions().stream()
              .map( positionsItem -> createPositionSnapshot(positionsItem))
              .collect(Collectors.toList());

      return positionSnapshotList;
   }


   public PositionSnapshot createPositionSnapshot(PositionsItem position){
        PositionSnapshot psnap = new PositionSnapshot(position);

        try {
            ConversationContext conversationContext = authenticationContext.getConversationContext();
            GetPricesByNumberOfPointsV2Response prices = restAPI.getPricesByNumberOfPointsV2(conversationContext
                    , "1", position.getMarket().getEpic(), "DAY");

            BigDecimal pl = cal.calPandL(position, prices);
            BigDecimal marketMoveToday = cal.calPriceMove(prices);

            psnap.setMarketMoveToday(marketMoveToday);
            psnap.setProfitLoss(pl);
        }catch (Exception ex){
            LOG.warn("Unable to calculate p&l for: " + position.getPosition().getDealId() ,ex);
        }

        return  psnap;

   }

   public void listWatchlists() throws Exception {

      GetWatchlistsV1Response watchlistsResponse = restAPI.getWatchlistsV1(authenticationContext.getConversationContext());
      LOG.info("Watchlists: {}", watchlistsResponse.getWatchlists().size());
      for (WatchlistsItem watchlist : watchlistsResponse.getWatchlists()) {
         LOG.info(watchlist.getName() + " : ");
         GetWatchlistByWatchlistIdV1Response watchlistInstrumentsResponse = restAPI.getWatchlistByWatchlistIdV1(authenticationContext.getConversationContext(), watchlist.getId());
         for (MarketsItem market : watchlistInstrumentsResponse.getMarkets()) {
            LOG.info(market.getEpic());
            if (market.getStreamingPricesAvailable() && market.getMarketStatus() == MarketStatus.TRADEABLE) {
               tradeableEpic = market.getEpic();
            }
         }
      }
   }

   public void subscribeToLighstreamerPriceUpdates(String tradeableEpic) throws Exception {

        if (tradeableEpic != null) {
            LOG.info("Subscribing to Lightstreamer price updates for market: {} ", tradeableEpic);
            listeners.add(streamingAPI.subscribeForMarket(tradeableEpic,
                    new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                    LOG.info("Market i {} s {} data {}", i, s, updateInfo);
                }
            }));
        }
    }

    public void subscribeToLighstreamerChartUpdates(String tradeableEpic) throws Exception {

        if (tradeableEpic != null) {
            LOG.info("Subscribing to Lightstreamer chart updates for market: {} ", tradeableEpic);
            listeners.add(streamingAPI.subscribeForChartTicks(tradeableEpic, new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                    LOG.info("Chart i {} s {} data {}", i, s, updateInfo);
                }
            }));
        }
    }

   public AccountsItem accountPreferences(){
       return accountSetting;
   }
   public void loadAccountPreferences(String accID) throws Exception {
        GetAccountsV1Response getAccountsV1Response =
                restAPI.getAccountsV1(authenticationContext.getConversationContext());
        Optional<AccountsItem> optional = getAccountsV1Response.getAccounts()
                .stream().filter(x -> accID.equalsIgnoreCase(x.getAccountId()))
                .findFirst();

        if(optional.isPresent()){
            this.accountSetting = optional.get();
            this.locale = IGClientUtility.findLocalForCurrency(
                    accountSetting.getCurrency());
        }else{
            throw new Exception("Account setting not found!");
        }
   }

    public Locale getLocale() {
        return locale;
    }
}