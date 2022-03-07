package com.anish.mta.data;

import com.anish.mta.config.MtaRealtimeDataConfig;
import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MtaRealtimeDataPoller implements Runnable {

    private static Logger LOGGER = LoggerFactory.getLogger(MtaRealtimeDataPoller.class);

    private static String API_SECURITY_HEADER = "x-api-key";

    private final ExtensionRegistry registry;
    private final String apiKey;
    private final String urlString;

    private final Predicate<GtfsRealtime.FeedEntity> isDelayedPredicate;

    private HttpsURLConnection httpsConnection;

    private Map<String, MtaLineInfo> tripStatuses;

    public MtaRealtimeDataPoller(ExtensionRegistry registry, MtaRealtimeDataConfig config, Map<String, MtaLineInfo> tripStatuses) {
        this.registry = registry;

        this.urlString = config.getApiUrl();
        this.apiKey = config.getApiKey();

        this.isDelayedPredicate = buildPredicate(config.getDelayMessagePatterns(), config.getAlertMessage());

        this.tripStatuses = tripStatuses;
    }

    /*
     * This method will be called after a specified duration
     * that is configured in the caller executor service
     * Filters response to delayed lines only, then processes to see if any updates
     */

    @Override
    public void run() {
        InputStream inputStream = connect();

        if (inputStream != null) {
            GtfsRealtime.FeedMessage feedMessage = null;
            try {
                feedMessage = GtfsRealtime.FeedMessage.parseFrom(inputStream, registry);
            } catch (IOException e) {
                LOGGER.error("Error encountered in parsing feed: ", e);
            }

            List<GtfsRealtime.FeedEntity> delayedLinesList = filterResponse(feedMessage);

            updateTripStatus(delayedLinesList);

            disconnect(inputStream);
        }
    }

    /*
     * Processes filtered response from GTFS-RT
     */
    private synchronized void updateTripStatus(List<GtfsRealtime.FeedEntity> delayedTrips) {

        Set<String> delayedLines = getLineNames(delayedTrips);

        for (String trainLine : tripStatuses.keySet()) {
            MtaLineInfo lineInfo = tripStatuses.get(trainLine);

            // If there are any new delays, register them. If existing delays are done with, deregister them

            if (delayedLines.contains(trainLine)) {
                if (!lineInfo.isDelayed()) {
                    LOGGER.info("Line {} is experiencing delays", trainLine);

                    lineInfo.setDelayed();
                }
            } else {
                if (lineInfo.isDelayed()) {
                    LOGGER.info("Line {} is now recovered", trainLine);

                    lineInfo.setNormal();
                }
            }
        }
    }

    /*
     * Filters response to extract proto messages indicating delayed lines
     */
    private List<GtfsRealtime.FeedEntity> filterResponse(GtfsRealtime.FeedMessage feedMessage) {
        List<GtfsRealtime.FeedEntity> delays = new ArrayList<>();

        if (feedMessage != null) {
            delays.addAll(feedMessage.getEntityList()
                    .stream()
                    .filter(isDelayedPredicate)
                    .collect(Collectors.toList()));
        }

        return delays;
    }


    /*
     * Streams actual line names from filtered API response
     */
    private Set<String> getLineNames(List<GtfsRealtime.FeedEntity> delayedTrips) {
        // Set here because there are some duplicates possible in the feed, and for constant lookup
        Set<String> delayedLines = new HashSet<>();

        delayedTrips.forEach(d -> {
            delayedLines.addAll(
                    d.getAlert().getInformedEntityList()
                            .stream()
                            .filter(e -> e.hasRouteId())
                            .map(e -> e.getRouteId())
                            .collect(Collectors.toList())
            );
        });

        return delayedLines;
    }

    /*
     * GTFS-RT feed for the MTA is not really uniform... status proto fields are not consistently filled out,
     * so here we build out an (overly-verbose) predicate to see if there is a delay
     * This predicate also captures "Some Delays" status
     */
    private Predicate<GtfsRealtime.FeedEntity> buildPredicate(List<String> delayMessagePatterns, String alertMessage) {

        Predicate<GtfsRealtime.FeedEntity> isAlertPredicate = e -> e.getId().contains(alertMessage);
        Predicate<GtfsRealtime.FeedEntity> isDelayPredicate = e -> delayMessagePatterns.stream().filter
                (s -> e.getAlert()
                        .getHeaderText()
                        .getTranslation(0)
                        .getText()
                        .contains(s)
                )
                .count() > 0;

        return isAlertPredicate.and(isDelayPredicate);
    }

    /*
     * Ensure a connection to the MTA's GTFS-RT alerts feed
     * We are closing/reopening the connection with the assumption that the websocket remains open (caching enabled)
     * As such, this is not a costly operation
     */
    private InputStream connect() {

        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            this.httpsConnection = (HttpsURLConnection) url.openConnection();
            this.httpsConnection.setRequestProperty(API_SECURITY_HEADER, apiKey);

            inputStream = httpsConnection.getInputStream();
        } catch (IOException e) {
            LOGGER.error("Error connecting to API: ", e);
        }

        return inputStream;
    }

    /*
     * Disconnect from the feed to free up resources
     */
    private void disconnect(InputStream inputStream) {
        try {
            inputStream.close();
            httpsConnection.disconnect();
        } catch (IOException e) {
            LOGGER.error("Error disconnecting from API: ", e);
        }
    }
}
