package com.dario.agenttrader.tradingservices;

import com.dario.agenttrader.domain.CandleResolution;
import com.dario.agenttrader.dto.PriceCandle;
import com.iggroup.webapi.samples.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TradingDataStreamingService {

    private static final Logger LOG = LoggerFactory.getLogger(TradingDataStreamingService.class);
    public static final String CHART_TICK = "CHART_TICK";
    public static final String CHART_CANDLE = "CHART_CANDLE";
    private static final String OPU = "OPU" ;

    private static TradingDataStreamingService oneAndOnlyStreamingServiceInstance = new TradingDataStreamingService();
    public static final long HEARTBEAT_CHECK_INTERVAL_MILIS = 1500l;

    public static TradingDataStreamingService getInstance(){
        return oneAndOnlyStreamingServiceInstance;
    }


    private TradingDataStreamingService(){

    }

    private boolean initialized = false;
    private TradingAPI tradingAPI = null;
    private Instant lastHeartBeat=null;
    private Duration maxDelayFromLastHeartBeat = Duration.ofMillis(10000);
    private Timer heartbeatTimer = new Timer("HeartBeatTimer");

    public void initializeStreamingService(TradingAPI ptradingAPI){
        if(ptradingAPI!=null){
            try {
                tradingAPI = ptradingAPI;
                startStreamingConnectionQuality();
                initialized = true;
            }catch (Exception e){
                LOG.error("Unable to initialise StreamingService",e);
            }

        }
    }

    public void stopStreamingService(){
        heartbeatTimer.cancel();
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void startStreamingConnectionQuality() throws Exception{
        subscribeToLighstreamerHeartbeat();
        TimerTask hearbeatTimerTask = new TimerTask() {
            @Override
            public void run() {
                onHeartbeatTimer();
            }
        };

        long delay = HEARTBEAT_CHECK_INTERVAL_MILIS;
        long interval = HEARTBEAT_CHECK_INTERVAL_MILIS;
        heartbeatTimer.scheduleAtFixedRate(hearbeatTimerTask,delay,interval);
    }

    private void onHeartbeatTimer() {
        Optional<Instant> lastHB = Optional.ofNullable(lastHeartBeat);
        lastHeartBeat = lastHB.orElse(Instant.now());
        Duration durationSinceLastHB = Duration.between(lastHeartBeat,Instant.now());
        int intIsMaxDelayBreached = durationSinceLastHB.compareTo(maxDelayFromLastHeartBeat);
        if(intIsMaxDelayBreached>-1){
            LOG.warn("HEARBEAT Abscence - {} since last HEARTBEAT",durationSinceLastHB);
            recoverConnection();
        }
    }

    private void subscribeToLighstreamerHeartbeat() throws Exception {
        LOG.info("Subscribing to Lightstreamer heartbeat");
        tradingAPI.subscribeToLighstreamerHeartbeat(new HandyTableListenerAdapter() {
            @Override
            public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                lastHeartBeat = Instant.now();
                LOG.debug("Heartbeat = " + updateInfo);
            }
        });
    }

    private void recoverConnection() {
        LOG.info("Recovering Streaming connection");
        try {
            restartTradingAPIConnection();
            subscribeToLighstreamerHeartbeat();
            refreshSubscriptions();
        }catch(Exception e){
            LOG.error("Unable to recover connection to TradingAPI provider.",e);
        }
    }

    private void refreshSubscriptions() {
        streamingUpdateSubscriptionRegistry.keySet().forEach(k->{
            streamingUpdateSubscriptionRegistry.get(k).forEach((subscriptionKey,consumer)->{
                if(subscriptionKey.contains(CHART_TICK)){
                    try {
                        subscribeToChartTickUpdates(k,consumer);
                    } catch (Exception e) {
                        LOG.warn("Subscription for {}:{} failed",k,subscriptionKey,e);
                    }
                }else if(subscriptionKey.contains(CHART_CANDLE)){
                    try {
                        String resolution = extractResultionFromKey(subscriptionKey);
                        subscribeToChartCandleUpdates(k,resolution,consumer);
                    } catch (Exception e) {
                        LOG.warn("Subscription for {}:{} failed",k,subscriptionKey,e);
                    }
                }else if(subscriptionKey.contains(OPU)){
                    try {
                        subscribeToPositionUpdate(consumer);
                    } catch (Exception e) {
                        LOG.warn("Subscription for PositionUpdate failed",e);
                    }
                }
            });
        });
    }

    private String extractResultionFromKey(String subscriptionKey) {
        CandleResolution candleResolution =
                CandleResolution.findMatchingResolutionOrDefaultToFiveMin(subscriptionKey);

        return candleResolution.getCandleInterval();
    }


    private void restartTradingAPIConnection() throws Exception {
        try {
            tradingAPI.disconnect();
        }catch(Exception ex){
            LOG.warn("Subscription failure",ex);
        }
        tradingAPI.connect();
    }

    Map<String,Map<String,Consumer>> streamingUpdateSubscriptionRegistry = new ConcurrentHashMap<>();
    private String generateChartTickSubscriptionKey(String subscriberUniqRef){
        String key= CHART_TICK + ":" +subscriberUniqRef;
        return key;
    }
    private String generateChartCandleSubscriptionKey(String subscriberUniqRef,String resolution) {
        String key= CHART_CANDLE+":"+ resolution +":" +subscriberUniqRef;
        return key;
    }
    private String generateOpenPositionUpdateSubscriptionKey(String subscriberUniqRef) {
        String key= OPU+":"+subscriberUniqRef;
        return key;
    }
    public void subscribeToOpenPositionUpdates(
            String subscriberUniqRef
            , Consumer<UpdateInfo> consumer) throws  Exception{

        String subscriptionKey =  generateOpenPositionUpdateSubscriptionKey(subscriberUniqRef);
        String streamingKey = OPU;
        Consumer exitingSubscriber = findSubscriber(streamingKey,subscriptionKey);
        if(exitingSubscriber == null && consumer != null){
            subscribeToPositionUpdate(consumer);
            registerSubscription(streamingKey,subscriptionKey,consumer);
        }
    }

    private void subscribeToPositionUpdate(Consumer<UpdateInfo> consumer) throws Exception{
        HandyTableListenerAdapter lightStreamerOPUListener = createLSListener(consumer);
        tradingAPI.subscribeToOpenPositionUpdates(lightStreamerOPUListener);
        LOG.info("Subscribed to position update");
    }

    public void subscribeToLighstreamerChartTickUpdates(
            String epic
            ,String subscriberUniqRef
            ,Consumer newSubscriberConsumer)throws Exception{
        String streamingKey = epic;
        String subscriptionKey = generateChartTickSubscriptionKey(subscriberUniqRef);
        Consumer exitingSubscriber = findSubscriber(streamingKey,subscriptionKey);
        if(exitingSubscriber == null && newSubscriberConsumer!=null){
            subscribeToChartTickUpdates(epic, newSubscriberConsumer);
            registerSubscription(streamingKey,subscriptionKey,newSubscriberConsumer);
        }
    }

    private void subscribeToChartTickUpdates(String epic, Consumer newSubscriberConsumer) throws Exception {
        HandyTableListenerAdapter lightStreamerChartTickListner = createLSListener(newSubscriberConsumer);
        tradingAPI.subscribeToLighstreamerChartUpdates(epic, lightStreamerChartTickListner);
        LOG.info("Subscribed to Chart Tick update");
    }

    public void subscribeToLighstreamerChartCandleUpdates(
            String epic
            ,CandleResolution candleResolution
            ,String subscriberUniqRef
            ,Consumer<UpdateInfo> newSubscriberConsumer) throws Exception{
        String resolution = candleResolution.getCandleInterval();
        String streamingKey = epic;
        String subscriptionKey = generateChartCandleSubscriptionKey(subscriberUniqRef,resolution);
        Consumer exitingSubscriber = findSubscriber(streamingKey,subscriptionKey);
        if(exitingSubscriber == null && newSubscriberConsumer!=null){
            subscribeToChartCandleUpdates(epic,resolution, newSubscriberConsumer);
            registerSubscription(streamingKey,subscriptionKey,newSubscriberConsumer);
        }
    }
    private void subscribeToChartCandleUpdates(String epic,String resolution, Consumer newSubscriberConsumer) throws Exception {
        HandyTableListenerAdapter lightStreamerChartCandleListner = createLSListener(newSubscriberConsumer);
        tradingAPI.subscribeToLighstreamerChartCandleUpdates(epic,resolution, lightStreamerChartCandleListner);
        LOG.info("Subscribed to Chart Candle update");
    }
    private  HandyTableListenerAdapter createLSListener(Consumer consumer){
        HandyTableListenerAdapter subscriptionListner = new HandyTableListenerAdapter() {
            @Override
            public void onUpdate(int i, String s, UpdateInfo updateInfo) {
               consumer.accept(updateInfo);
            }
        };
        return  subscriptionListner;
    }

    private synchronized void  registerSubscription(String streamingKey, String subscriptionKey, Consumer newSubscriberConsumer) {

        Map<String,Consumer> subscriptions = streamingUpdateSubscriptionRegistry.get(streamingKey);
        if(subscriptions == null){
            subscriptions = new ConcurrentHashMap<>();
            streamingUpdateSubscriptionRegistry.put(streamingKey,subscriptions);
        }
        subscriptions.put(subscriptionKey,newSubscriberConsumer);
    }

    private Consumer findSubscriber(String streamingKey,String subscriptionKey){
        Consumer subscriber = null;
        Map<String,Consumer> subscriptions = streamingUpdateSubscriptionRegistry.get(streamingKey);
        if(subscriptions!=null){
            subscriber = subscriptions.get(subscriptionKey);
        }
        return  subscriber;
    }


    public void removeSubscriber(String subscriberUniqRef) {
        streamingUpdateSubscriptionRegistry.forEach((streamingKey,subscriptions)->{
            subscriptions.keySet()
                    .stream()
                    .filter(k->k.contains(subscriberUniqRef))
                    .forEach(subscriptionKey->subscriptions.remove(subscriptionKey));
        });

    }


}
