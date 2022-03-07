package com.anish.mta.web;

import com.anish.mta.config.MtaRealtimeDataConfig;
import com.anish.mta.data.MtaLineInfo;
import com.anish.mta.web.controller.MtaLineStatusController;
import com.anish.mta.web.renderer.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static spark.Spark.*;

public class MtaInfoWebService implements Runnable {

    private static Logger LOGGER = LoggerFactory.getLogger(MtaInfoWebService.class);

    private static volatile boolean isStopped;

    public MtaInfoWebService(MtaRealtimeDataConfig config, Map<String, MtaLineInfo> mtaLineInfo) {

        // Create a simple response serializer
        JsonTransformer transformer = new JsonTransformer();

        // Starts a simple web server and registers API hooks
        initWebServer(config);

        // Wires the controller that serves data
        MtaLineStatusController.register(transformer, mtaLineInfo);
    }

    private void initWebServer(MtaRealtimeDataConfig config) {
        port(config.getWebServicePort());

        before("/*", (request, response) -> LOGGER.info("ip=" + request.ip() + " path=" + request.pathInfo()));

        after("/*", (request, response) -> {
            response.type("application/json");
        });
    }

    @Override
    public void run() {
        isStopped = false;
        keepAlive();
    }

    /*
     * Standard boiler plate to keep thread alive
     */
    private void keepAlive() {
        synchronized (MtaInfoWebService.class) {
            while (!isStopped) {
                try {
                    MtaInfoWebService.class.wait(20000);
                } catch (InterruptedException e) {
                    LOGGER.error("Error encountered in keeping web service alive: ", e);
                }
            }
        }
    }
}
