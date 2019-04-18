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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.d3x.morpheus.frame.DataFrame;

/**
 * A class that captures meta-data for a specific dataset in a Qunadl database
 *
 * https://www.quandl.com/api/v3/datasets/WIKI/AAPL/metadata.json?api_key=DrFK1MBShGiB32kCHZXx
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
@lombok.Builder()
@lombok.ToString()
@lombok.EqualsAndHashCode(of={"id"})
public class TigerBrokerDatasetInfo {

    /** The unique identifier for dataset */
    @lombok.Getter private int id;
    /** The database code*/
    @lombok.NonNull @lombok.Getter private String databaseCode;
    /** The dataset code */
    @lombok.NonNull @lombok.Getter private String datasetCode;
    /** The dataset friendly name*/
    @lombok.NonNull @lombok.Getter private String name;
    /** The dataset description */
    @lombok.Getter private String description;
    /** The time at which the dataset was last refreshed */
    @lombok.NonNull @lombok.Getter private ZonedDateTime refreshedAt;
    /** The latest available effective date for data */
    @lombok.Getter private LocalDate newestAvailableDate;
    /** The earliest available effective date for data */
    @lombok.Getter private LocalDate oldestAvailableDate;
    /** The list of column names for dataset */
    @lombok.Getter private List<String> columnNames;
    /** The sampling frequency for dataset */
    @lombok.NonNull @lombok.Getter private String frequency;
    /** The dataset type */
    @lombok.NonNull @lombok.Getter private String type;
    /** True if this is a premium dataset */
    @lombok.Getter private boolean premium;
    /** The database identifier */
    @lombok.Getter private int databaseId;


    /**
     * Returns a newly created empty DataFrame for dataset meta-data
     * @param initialSize   the initial row capacity for frame
     * @return              the newly created frame
     */
    static DataFrame<Integer,TigerBrokerField> frame(int initialSize) {
        return TigerBrokerField.frame(Integer.class, initialSize,
                TigerBrokerField.DATABASE_CODE,
                TigerBrokerField.DATASET_CODE,
                TigerBrokerField.NAME,
                TigerBrokerField.DESCRIPTION,
                TigerBrokerField.LAST_REFRESH_TIME,
                TigerBrokerField.START_DATE,
                TigerBrokerField.END_DATE,
                TigerBrokerField.COLUMN_NAMES,
                TigerBrokerField.FREQUENCY,
                TigerBrokerField.DATASET_TYPE,
                TigerBrokerField.PREMIUM,
                TigerBrokerField.DATABASE_ID
        );
    }


    public static class Deserializer implements JsonDeserializer<TigerBrokerDatasetInfo> {
        @Override
        public TigerBrokerDatasetInfo deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.equals(JsonNull.INSTANCE)) {
                return null;
            } else {
                final JsonObject dataset = (JsonObject)json;
                return TigerBrokerDatasetInfo.builder()
                        .id(dataset.get("id").getAsInt())
                        .databaseCode(dataset.get("database_code").getAsString())
                        .datasetCode(dataset.get("dataset_code").getAsString())
                        .name(dataset.get("name").getAsString())
                        .refreshedAt(context.deserialize(dataset.get("refreshed_at"), ZonedDateTime.class))
                        .newestAvailableDate(context.deserialize(dataset.get("newest_available_date"), LocalDate.class))
                        .oldestAvailableDate(context.deserialize(dataset.get("oldest_available_date"), LocalDate.class))
                        .columnNames(Arrays.asList(context.deserialize(dataset.get("column_names"), String[].class)))
                        .description(dataset.get("description").getAsString())
                        .frequency(dataset.get("frequency").getAsString())
                        .type(dataset.get("type").getAsString())
                        .premium(dataset.get("premium").getAsBoolean())
                        .databaseId(dataset.get("database_id").getAsInt())
                        .build();
            }
        }
    }
}
