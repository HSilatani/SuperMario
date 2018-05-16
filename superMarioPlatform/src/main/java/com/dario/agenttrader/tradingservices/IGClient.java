package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.ApplicationBootStrapper;
import com.dario.agenttrader.domain.*;
import com.dario.agenttrader.utility.Calculator;
import com.dario.agenttrader.utility.IGClientUtility;
import com.iggroup.webapi.samples.PropertiesUtil;
import com.iggroup.webapi.samples.client.RestAPI;
import com.iggroup.webapi.samples.client.StreamingAPI;
import com.iggroup.webapi.samples.client.rest.AuthenticationResponseAndConversationContext;
import com.iggroup.webapi.samples.client.rest.ConversationContext;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.AccountsItem;
import com.iggroup.webapi.samples.client.rest.dto.getAccountsV1.GetAccountsV1Response;
import com.iggroup.webapi.samples.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV2.GetMarketDetailsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV2.MarketOrderPreference;
import com.iggroup.webapi.samples.client.rest.dto.markets.getMarketDetailsV3.GetMarketDetailsV3Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.GetPositionsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.TimeInForce;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.CreateOTCPositionV1Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.CreateOTCPositionV1Response;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.OrderType;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import com.iggroup.webapi.samples.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Response;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesByNumberOfPointsV2.GetPricesByNumberOfPointsV2Response;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.GetPricesV3Response;
import com.iggroup.webapi.samples.client.rest.dto.prices.getPricesV3.PricesItem;
import com.iggroup.webapi.samples.client.rest.dto.session.createSessionV2.CreateSessionV2Request;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.GetWatchlistByWatchlistIdV1Response;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.MarketStatus;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistByWatchlistIdV1.MarketsItem;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistsV1.GetWatchlistsV1Response;
import com.iggroup.webapi.samples.client.rest.dto.watchlists.getWatchlistsV1.WatchlistsItem;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;


import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IGClient implements TradingAPI {
    private static TradingAPI OneAndOnlyIGClient = (TradingAPI) Proxy.newProxyInstance(
            IGClient.class.getClassLoader(),new Class[]{TradingAPI.class},new TradingAPIAuditor(new IGClient()));

    public static TradingAPI getInstance(){
        return OneAndOnlyIGClient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(IGClient.class);
    private final Logger TRADE_LOGGER = LoggerFactory.getLogger("TRADE_LOGGER");

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
    public List<PositionSnapshot> listOpenPositionsWithProfitAndLoss() throws RuntimeException {

        ConversationContext conversationContext = authenticationContext.getConversationContext();
        GetPositionsV2Response positionsResponse = null;
        try {
            positionsResponse = restAPI.getPositionsV2(conversationContext);
        } catch (Exception e) {
            LOG.warn("Unable to load list of positions",e);
            throw new RuntimeException(e);
        }
        LOG.info("Open positions Amoo Amir: {}", positionsResponse.getPositions().size());

      List<PositionSnapshot> positionSnapshotList = positionsResponse.getPositions().stream()
              .map( positionsItem -> createPositionSnapshot(positionsItem))
              .collect(Collectors.toList());

      return positionSnapshotList;
   }
    @Override
    public List<PositionSnapshot> listOpenPositions() throws RuntimeException {

        ConversationContext conversationContext = authenticationContext.getConversationContext();
        GetPositionsV2Response positionsResponse = null;
        try {
            positionsResponse = restAPI.getPositionsV2(conversationContext);
        } catch (Exception e) {
            LOG.warn("Unable to load list of positions",e);
            throw new RuntimeException(e);
        }
        LOG.info("Open positions Amoo Amir: {}", positionsResponse.getPositions().size());

        List<PositionSnapshot> positionSnapshotList = positionsResponse.getPositions().stream()
                .map( positionsItem -> new PositionSnapshot(positionsItem))
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
            marketInfo.setExpiry(marketDetails.getInstrument().getExpiry());
        }catch (Exception ex){
            LOG.warn("Unable to get market details  for: " + epic ,ex);
            throw ex;
        }
        return marketInfo;
   }

   @Override
   public PositionSnapshot getPositionSnapshot(String positionId) throws Exception{


      List<PositionSnapshot> positionSnapshotList = listOpenPositionsWithProfitAndLoss();

      Optional<PositionSnapshot> positionSnapShot= positionSnapshotList.stream()
              .filter(psnap -> positionId.equalsIgnoreCase(psnap.getPositionId()))
              .findFirst();

      return positionSnapShot.get();
   }

   @Override
   public List<PricesItem> getHistoricPrices(String epic,CandleResolution candleResolution) throws Exception{
        List<PricesItem> listOfPrices = new ArrayList<>();
       try {
           ConversationContext conversationContext = authenticationContext.getConversationContext();

           GetPricesV3Response prices = restAPI.getPricesV3(
                   conversationContext
                   , null
                   , "26"
                   ,"0"
                   ,epic
                   ,null
                   ,null
                   ,convertIntervaltoIGClientHistoricPriceInterval(candleResolution)
           );
           listOfPrices.addAll(prices.getPrices());

       }catch (Exception ex){
           LOG.warn("Unable to get historic prices for: " + epic ,ex);
           throw ex;
       }

       return listOfPrices;
   }
   private static final Map<String,String> igIntervalMapping = new HashMap<>();
    static {
        igIntervalMapping.put(CandleResolution.oneMinuteResolution().getCandleInterval(),"MINUTE");
        igIntervalMapping.put(CandleResolution.twoMinuteResolution().getCandleInterval(),"MINUTE_2");
        igIntervalMapping.put(CandleResolution.threeMinuteResolution().getCandleInterval(),"MINUTE_3");
        igIntervalMapping.put(CandleResolution.fiveMinuteResolution().getCandleInterval(),"MINUTE_5");
        //igIntervalMapping.put("","MINUTE_10");
        //igIntervalMapping.put("","MINUTE_15");
        //igIntervalMapping.put("","MINUTE_30");
        igIntervalMapping.put(CandleResolution.oneHourResolution().getCandleInterval(),"HOUR");
        //igIntervalMapping.put("","HOUR_2");
        //igIntervalMapping.put("","HOUR_3");
        //igIntervalMapping.put("","HOUR_4");
        //igIntervalMapping.put("","DAY");
        //igIntervalMapping.put("","WEEK");
        //igIntervalMapping.put("","MONTH");
    }
   private String convertIntervaltoIGClientHistoricPriceInterval(CandleResolution candleResolution){
        String igInterva = igIntervalMapping.get(candleResolution.getCandleInterval());

        return igInterva;
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
    public void subscribeToLighstreamerChartCandleUpdates(String tradeableEpic,String scale, HandyTableListenerAdapter listener) throws Exception {
        if (tradeableEpic != null) {
            LOG.info("Subscribing to Lightstreamer chart candle updates for market: {}:{} ", tradeableEpic,scale);
            listeners.add(streamingAPI.subscribeForChartCandles(tradeableEpic,scale,listener));
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
    public void subscribeToPositionConfirms(HandyTableListenerAdapter listener) throws Exception {
        listeners.add(streamingAPI.subscribeForConfirms(authenticationContext.getAccountId(),listener));
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
   @Override
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
   public Position createPosition(TradingSignal signal) throws Exception{

           com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction igDirection
                   = convertToIGDirection(signal.getDirection());
           MarketInfo marketInfo = this.getMarketInfo(signal.getEpic());

           CreateOTCPositionV1Request createPositionRequest = new CreateOTCPositionV1Request();
           createPositionRequest.setEpic(signal.getEpic());
           createPositionRequest.setExpiry(marketInfo.getExpiry());
           createPositionRequest.setDirection(igDirection);

           createPositionRequest.setOrderType(OrderType.MARKET);

           createPositionRequest.setCurrencyCode("GBP");
           createPositionRequest.setSize(signal.getSize());
           createPositionRequest.setStopDistance(signal.getStopDistance());
           createPositionRequest.setGuaranteedStop(false);
           createPositionRequest.setForceOpen(true);

           LOG.info(">>> Creating {} position epic={}, expiry={} size={} orderType={} level={} currency={}"
                   ,createPositionRequest.getDirection()
                   ,createPositionRequest.getEpic()
                   , createPositionRequest.getExpiry()
                   , createPositionRequest.getSize()
                   , createPositionRequest.getOrderType()
                   , createPositionRequest.getLevel()
                   , createPositionRequest.getCurrencyCode()
           );
           CreateOTCPositionV1Response response = restAPI.createOTCPositionV1(authenticationContext.getConversationContext(), createPositionRequest);
           Position position = new Position(
                   createPositionRequest.getEpic()
                   ,response.getDealReference()
                   ,createPositionRequest.getSize().doubleValue()
                   ,signal.getDirection()

           );
           TRADE_LOGGER.info("TRADE: {}",position);
           return position;
    }

   private com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction convertToIGDirection(Direction pDirection){
       com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction  direction = null;
       if(pDirection.isBuy()) {
           direction = com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction.BUY;
       } else if(pDirection.isSell()){
           direction = com.iggroup.webapi.samples.client.rest.dto.positions.otc.createOTCPositionV1.Direction.SELL;
       }

       return direction;

   }

   @Override
   public String closeOpenPosition(Position position) throws Exception {
       GetMarketDetailsV2Response marketDetails = restAPI.getMarketDetailsV2(
               authenticationContext.getConversationContext(), position.getEpic());

       Direction closeDirection = position.getDirection().opposite();
       CloseOTCPositionV1Request closePositionRequest = new CloseOTCPositionV1Request();
       closePositionRequest.setDealId(position.getDealId());
       closePositionRequest.setDirection(
               com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.Direction.valueOf(closeDirection.toString()));
       closePositionRequest.setSize(new BigDecimal(position.getSize()));
       closePositionRequest.setTimeInForce(TimeInForce.FILL_OR_KILL);

       if(marketDetails.getDealingRules().getMarketOrderPreference() != MarketOrderPreference.NOT_AVAILABLE) {
           closePositionRequest.setOrderType(com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.OrderType.MARKET);
       } else {
           closePositionRequest.setOrderType(com.iggroup.webapi.samples.client.rest.dto.positions.otc.closeOTCPositionV1.OrderType.LIMIT);
           closePositionRequest.setLevel(marketDetails.getSnapshot().getBid());
       }
       closePositionRequest.setTimeInForce(TimeInForce.FILL_OR_KILL);

       LOG.info("<<< Closing position: dealId={} direction={} size={} orderType={} level={}", position.getDealId(), position.getDirection(), position.getSize(),
               closePositionRequest.getOrderType(), closePositionRequest.getLevel());
       CloseOTCPositionV1Response closeResp = restAPI.closeOTCPositionV1(authenticationContext.getConversationContext(), closePositionRequest);

       TRADE_LOGGER.info("TRADE: {}",position);

       return closeResp.getDealReference();
    }

   @Override
   public DealConfirmation confirmPosition(String dealRef){
       GetDealConfirmationV1Response igDealConf =
               null;
       DealConfirmation dealConf = null;
       try {
           igDealConf = restAPI.getDealConfirmationV1(authenticationContext.getConversationContext(),dealRef);
           dealConf =new  DealConfirmation(
               igDealConf.getDealReference()
               ,igDealConf.getDealId()
               ,igDealConf.getDealStatus().name());

       } catch (Exception e) {
           if(e.getMessage()!=null && "404 Not Found".contains(e.getMessage())){
               LOG.info("Unable to find confirmation: {}",e.getMessage());
           }else {
               LOG.warn("Unable to retrieve DealConfirmation", e);
           }
       }

       return dealConf;
    }


    @Override
    public Locale getLocale() {
        return locale;
    }
}