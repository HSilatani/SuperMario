package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.ApplicationBootStrapper;
import com.dario.agenttrader.dto.MarketInfo;
import com.dario.agenttrader.dto.PositionSnapshot;
import com.dario.agenttrader.utility.Calculator;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.PropertiesUtil;
import com.iggroup.webapi.samples.client.RestAPI;
import com.iggroup.webapi.samples.client.StreamingAPI;
import com.iggroup.webapi.samples.client.rest.AuthenticationResponseAndConversationContext;
import com.iggroup.webapi.samples.client.rest.ConversationContext;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.GetAccountsV1Response;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV3.GetMarketDetailsV3Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.GetPositionsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Response;
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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;

public class IGClient implements TradingAPI {
    private static IGClient OneAndOnlyIGClient = new IGClient();

    public static IGClient getInstance(){
        return OneAndOnlyIGClient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(IGClient.class);

    private Calculator cal = new Calculator();

    private final ApplicationBootStrapper applicationBootStrapper;
    AuthenticationResponseAndConversationContext authenticationContext = null;
    ArrayList<HandyTableListenerAdapter> listeners = new ArrayList<HandyTableListenerAdapter>();
    String tradeableEpic = null;

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

    @Override
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

    @Override
    public void disconnect() throws Exception {
        unsubscribeAllLightstreamerListeners();
        streamingAPI.disconnect();
    }

    @Override
    public void unsubscribeAllLightstreamerListeners() throws Exception {

        for (HandyTableListenerAdapter listener : listeners) {
            streamingAPI.unsubscribe(listener.getSubscribedTableKey());
        }
    }

    @Override
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
    
    @Override
    public List<PositionSnapshot> listOpenPositions() throws Exception {

        ConversationContext conversationContext = authenticationContext.getConversationContext();
        GetPositionsV2Response positionsResponse = restAPI.getPositionsV2(conversationContext);
      LOG.info("Open positions Amoo Amir: {}", positionsResponse.getPositions().size());

      List<PositionSnapshot> positionSnapshotList = positionsResponse.getPositions().stream()
              .map( positionsItem -> createPositionSnapshot(positionsItem))
              .collect(Collectors.toList());

      return positionSnapshotList;
   }

   @Override
   public MarketInfo getMarketInfo(String epic) throws Exception{
        GetMarketDetailsV3Response marketDetails = null;
        MarketInfo marketInfo = new MarketInfo();
         try {
            ConversationContext conversationContext = authenticationContext.getConversationContext();

            marketDetails = restAPI.getMarketDetailsV3(conversationContext,epic);
            BigDecimal minDealSize = BigDecimal.valueOf(marketDetails.getDealingRules().getMinDealSize().getValue());
            BigDecimal minStopLimitDistance = BigDecimal.valueOf(marketDetails.getDealingRules().getMinNormalStopOrLimitDistance().getValue());
            marketInfo.setMinDealSize(minDealSize);
            marketInfo.setMinNormalStopLimitDistance(minStopLimitDistance);
            marketInfo.setMarketName(marketDetails.getInstrument().getName());
        }catch (Exception ex){
            LOG.warn("Unable to get market details  for: " + epic ,ex);
            throw ex;
        }
        return marketInfo;
   }

   @Override
   public PositionSnapshot getPositionSnapshot(String positionId) throws Exception{


      List<PositionSnapshot> positionSnapshotList = listOpenPositions();

      Optional<PositionSnapshot> positionSnapShot= positionSnapshotList.stream()
              .filter(psnap -> positionId.equalsIgnoreCase(psnap.getPositionId()))
              .findFirst();

      return positionSnapShot.get();
   }


   @Override
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

   @Override
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

   @Override
   public void subscribeToLighstreamerPriceUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception {
       if (tradeableEpic != null) {
           LOG.info("Subscribing to Lightstreamer chart updates for market: {} ", tradeableEpic);
           listeners.add(streamingAPI.subscribeForMarket(tradeableEpic, listener));
       }

    }

    @Override
    public void subscribeToLighstreamerChartUpdates(String tradeableEpic, HandyTableListenerAdapter listener) throws Exception {
        if (tradeableEpic != null) {
            LOG.info("Subscribing to Lightstreamer chart updates for market: {} ", tradeableEpic);
            listeners.add(streamingAPI.subscribeForChartTicks(tradeableEpic, listener));
        }
    }

    @Override
    public void subscribeToOpenPositionUpdates(HandyTableListenerAdapter listener) throws Exception{

        listeners.add(streamingAPI.subscribeForOPUs(authenticationContext.getAccountId(),listener));

    }

    @Override
    public void subscribeToLighstreamerHeartbeat(HandyTableListenerAdapter listener) throws Exception {
      listeners.add(streamingAPI.subscribe(listener,
              new String[]{"TRADE:HB.U.HEARTBEAT.IP"}, "MERGE", new String[]{"HEARTBEAT"}));
    }

    @Override
    public void unsubscribeLightstreamerForListner(HandyTableListenerAdapter listener) throws Exception {
            streamingAPI.unsubscribe(listener.getSubscribedTableKey());
    }

   @Override
   public AccountsItem accountPreferences(){
       return accountSetting;
   }
   @Override
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

     public void editPosition(String dealId,BigDecimal newStop,BigDecimal newLimit) throws Exception {
        UpdateOTCPositionV2Request updatePositionRequest = new UpdateOTCPositionV2Request();
        Optional<BigDecimal> optNewStop = Optional.ofNullable(newStop);
        optNewStop.ifPresent(nstop -> updatePositionRequest.setStopLevel(nstop));

        Optional<BigDecimal> optNewLimit = Optional.ofNullable(newLimit);
        optNewLimit.ifPresent(nLimit -> updatePositionRequest.setLimitLevel(nLimit));
        updatePositionRequest.setTrailingStop(Boolean.FALSE);
        UpdateOTCPositionV2Response updateResponse =
                restAPI.updateOTCPositionV2(
                        authenticationContext.getConversationContext()
                        ,dealId
                        ,updatePositionRequest);

   }

    @Override
    public Locale getLocale() {
        return locale;
    }
}