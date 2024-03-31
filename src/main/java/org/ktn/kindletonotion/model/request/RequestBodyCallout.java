package org.ktn.kindletonotion.model.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ktn.kindletonotion.model.notion.Callout;

/**
 * @author 贺佳
 */
public record RequestBodyCallout(Callout callout) {

    @Override
    public String toString() {
        Gson gson = new GsonBuilder()
                .setFieldNamingStrategy(f -> f.getName().replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase()).create();
        return gson.toJson(this);
    }

}
