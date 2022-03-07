package com.anish.mta.web.controller;

import com.anish.mta.data.MtaLineInfo;
import com.anish.mta.web.renderer.JsonTransformer;

import java.util.Map;

import static spark.Spark.get;

public class MtaLineStatusController {

    public static void register(JsonTransformer transformer, Map<String, MtaLineInfo> mtaLineInfo) {

        /*
         * Returns the current status of an MTA train line
         * USAGE: localhost:8080/status?F
         */
        get("/status", "application/json", (request, response) -> {
            String lineName = request.queryString();

            if (!mtaLineInfo.containsKey(lineName)) {
                return "Line " + lineName + " does not exist.";
            }

            return mtaLineInfo.get(lineName).getStatus();
        }, transformer);


        /*
         * Returns the uptime relative to delays of an MTA train line
         * USAGE: localhost:8080/uptime?A
         */
        get("/uptime", "application/json", (request, response) -> {
            String lineName = request.queryString();

            if (!mtaLineInfo.containsKey(lineName)) {
                return "Line " + lineName + " does not exist.";
            }

            return mtaLineInfo.get(lineName).getRelativeUptime();
        }, transformer);
    }
}
