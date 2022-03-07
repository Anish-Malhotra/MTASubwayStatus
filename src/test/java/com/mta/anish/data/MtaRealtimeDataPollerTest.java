package com.mta.anish.data;

import com.anish.mta.config.MtaRealtimeDataConfig;
import com.anish.mta.data.MtaRealtimeDataPoller;
import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtimeNYCT;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MtaRealtimeDataPollerTest {

    MtaRealtimeDataConfig config;
    MtaRealtimeDataPoller dataPoller;

    ExtensionRegistry registry;
    File protoDump;

    @Before
    public void setup() {
        config = Mockito.mock(MtaRealtimeDataConfig.class);
        Mockito.when(config.getApiKey()).thenReturn("");
        Mockito.when(config.getApiUrl()).thenReturn("");
        Mockito.when(config.getAlertMessage()).thenReturn("alert");
        Mockito.when(config.getDelayMessagePatterns()).thenReturn(Arrays.asList("You may wait longer","are delayed","delays"));

        registry = ExtensionRegistry.newInstance();
        registry.add(GtfsRealtimeNYCT.nyctFeedHeader);
        registry.add(GtfsRealtimeNYCT.nyctStopTimeUpdate);
        registry.add(GtfsRealtimeNYCT.nyctTripDescriptor);

        dataPoller = new MtaRealtimeDataPoller(registry, config, new HashMap<>());

        protoDump = new File("src/test/resources/mta_api_proto_output_dump_20220306");
    }

    @After
    public void tearDown() {
        dataPoller = null;
        config = null;
        registry = null;
        protoDump = null;
    }

    @Test
    public void testFilterResponse() throws IOException {
        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.parseFrom(new FileInputStream(protoDump), registry);

        ReflectionTestUtils.invokeMethod(dataPoller, "buildPredicate", config.getDelayMessagePatterns(), config.getAlertMessage());
        List<GtfsRealtime.FeedEntity> delayedLines = ReflectionTestUtils.invokeMethod(dataPoller, "filterResponse", feedMessage);

        Assert.assertEquals(2, delayedLines.size());

        feedMessage = null;
        delayedLines = ReflectionTestUtils.invokeMethod(dataPoller, "filterResponse", feedMessage);

        Assert.assertEquals(0, delayedLines.size());
    }

    @Test
    public void testGetLineNames() throws IOException {
        GtfsRealtime.FeedMessage feedMessage =
                GtfsRealtime.FeedMessage.parseFrom(new FileInputStream(protoDump), registry);

        ReflectionTestUtils.invokeMethod(dataPoller, "buildPredicate", config.getDelayMessagePatterns(), config.getAlertMessage());
        List<GtfsRealtime.FeedEntity> delayedLines = ReflectionTestUtils.invokeMethod(dataPoller, "filterResponse", feedMessage);

        Set<String> lineNames = ReflectionTestUtils.invokeMethod(dataPoller, "getLineNames", delayedLines);

        Assert.assertTrue(lineNames.contains("6"));
        Assert.assertTrue(lineNames.contains("D"));
    }
}
