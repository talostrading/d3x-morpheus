/*
 * Copyright (C) 2014-2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.morpheus.tigerbroker;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * A class to capture meta-data about a Quandl database.
 *
 * @author Xavier Witdouck
 */
@lombok.Builder()
@lombok.ToString()
@lombok.EqualsAndHashCode(of={"id"})
public class TigerBrokerDatabaseInfo {

    /** The numeric unique id for database */
    @lombok.Getter private int id;
    /** The database code identifier */
    @lombok.Getter private String code;
    /** The name for the database */
    @lombok.Getter private String name;
    /** The number of datasets in the database */
    @lombok.Getter private int datasetCount;
    /** The description for database */
    @lombok.Getter private String description;
    /** The number of downloads for this database */
    @lombok.Getter private int downloads;
    /** True if this is a premium database */
    @lombok.Getter private boolean premium;

    public static class Deserializer implements JsonDeserializer<TigerBrokerDatabaseInfo> {
        @Override
        public TigerBrokerDatabaseInfo deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.equals(JsonNull.INSTANCE)) return null;
            else {
                final JsonObject database = (JsonObject)json;
                return TigerBrokerDatabaseInfo.builder()
                        .id(database.get("id").getAsInt())
                        .code(database.get("database_code").getAsString())
                        .name(database.get("name").getAsString())
                        .description(database.get("description").getAsString())
                        .datasetCount(database.get("datasets_count").getAsInt())
                        .downloads(database.get("downloads").getAsInt())
                        .premium(database.get("premium").getAsBoolean())
                        .build();
            }
        }
    }

}
