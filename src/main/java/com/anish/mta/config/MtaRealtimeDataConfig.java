package com.anish.mta.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MtaRealtimeDataConfig {

    private final List<String> mtaTrainLines;
    private final List<String> delayMessagePatterns;
    private final String alertMessage;
    private final String apiUrl;
    private final String apiKey;
    private final long pollingFrequency;
    private final int webServicePort;

    public MtaRealtimeDataConfig(String propertiesUri) throws IOException {
        Properties props = new Properties();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesUri);

        props.load(inputStream);

        this.mtaTrainLines = Arrays.asList(props.getProperty("mta.train.lines").split(","));
        this.delayMessagePatterns = Arrays.asList(props.getProperty("mta.delay.msg.patterns").split(","));
        this.alertMessage = props.getProperty("mta.alert.msg");
        this.apiUrl = props.getProperty("mta.gtfs.api.url");
        this.apiKey = props.getProperty("mta.gtfs.api.key");
        this.pollingFrequency = Long.parseLong(props.getProperty("mta.gtfs.poll.freq"));
        this.webServicePort = Integer.parseInt(props.getProperty("mta.web.service.port"));

    }

    public List<String> getMtaTrainLines() {
        return mtaTrainLines;
    }

    public List<String> getDelayMessagePatterns() {
        return delayMessagePatterns;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public long getPollingFrequency() {
        return pollingFrequency;
    }

    public int getWebServicePort() {
        return webServicePort;
    }
}
