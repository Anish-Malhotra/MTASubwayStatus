package com.anish.mta;

import com.anish.mta.config.MtaRealtimeDataConfig;
import com.anish.mta.data.MtaLineInfo;
import com.anish.mta.web.MtaInfoWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anish.mta.data.MtaRealtimeDataPoller;
import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtimeNYCT;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MtaRealtimeDataDriver {

    private static Logger LOGGER = LoggerFactory.getLogger(MtaRealtimeDataDriver.class);

    /*
     * Main method
     * Reads configuration file (in resources folder)
     * Forks a thread to read MTA GTFS-RT API data at a scheduled interval (default: 15 seconds)
     * Forks a separate thread to start a webservice with the specified endpoints
     */

    public static void main(String[] args) throws Exception {

        MtaRealtimeDataConfig config = new MtaRealtimeDataConfig("app.config");

        ExtensionRegistry registry = buildExtensionRegistry();
        Map<String, MtaLineInfo> tripStatusMap = initializeTripStatusMap(config.getMtaTrainLines());

        MtaRealtimeDataPoller mtaAlertsPoller = new MtaRealtimeDataPoller(registry, config, tripStatusMap);
        MtaInfoWebService webService = new MtaInfoWebService(config, tripStatusMap);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService webServiceExecutor = Executors.newSingleThreadExecutor();

        scheduler.scheduleAtFixedRate(mtaAlertsPoller, 0, config.getPollingFrequency(), TimeUnit.SECONDS);
        webServiceExecutor.execute(webService);
    }

    /*
     * Initializes a ConcurrentHashMap (thread safe) with for specified train lines (config-driven), with a default NORMAL status
     * This map serves as the primary data structure that both the API poller and Web Service interact with
     * The ConcurrentHashMap will block on get calls from the web service during any updates from the API poller
     */
    private static Map<String, MtaLineInfo> initializeTripStatusMap(List<String> trainLines) {
        Map<String, MtaLineInfo> tripStatuses = new ConcurrentHashMap<>();
        long startTimeMillis = System.currentTimeMillis();

        for(String trainLine : trainLines) {

            MtaLineInfo lineInfo = new MtaLineInfo(trainLine, startTimeMillis);
            tripStatuses.put(trainLine, lineInfo);
        }

        return tripStatuses;
    }

    /*
     * The MTA has custom protobuf extensions for the GTFS-RT proto structure as defined by Google that we need to register
     */
    private static ExtensionRegistry buildExtensionRegistry() {
        LOGGER.info("Initializing NYCT GTFS extensions");

        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        registry.add(GtfsRealtimeNYCT.nyctFeedHeader);
        registry.add(GtfsRealtimeNYCT.nyctStopTimeUpdate);
        registry.add(GtfsRealtimeNYCT.nyctTripDescriptor);

        return registry;
    }
}
