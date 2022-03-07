package com.anish.mta.web.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

    private Gson json;

    /*
     * A lightweight response serializer that makes use of Google's GSON library
     */
    public JsonTransformer() {
        GsonBuilder builder = new GsonBuilder().serializeNulls();
        json = builder.create();
    }

    @Override
    public String render(Object model) throws Exception {
        try {
            String jsonString = json.toJson(model);
            return jsonString;
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
