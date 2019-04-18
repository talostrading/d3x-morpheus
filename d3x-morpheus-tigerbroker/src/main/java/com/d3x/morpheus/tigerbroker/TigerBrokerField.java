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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.index.Index;

/**
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public enum TigerBrokerField {

    NAME(String.class),
    DESCRIPTION(String.class),
    DATABASE_CODE(String.class),
    DATASET_CODE(String.class),
    DATASET_COUNT(String.class),
    DOWNLOADS(int.class),
    PREMIUM(boolean.class),
    IMAGE_URL(String.class),
    LAST_REFRESH_TIME(ZonedDateTime.class),
    START_DATE(LocalDate.class),
    END_DATE(LocalDate.class),
    DATASET_TYPE(String.class),
    FREQUENCY(String.class),
    DATABASE_ID(int.class),
    COLUMN_NAMES(List.class),
    FAVOURITE(boolean.class),
    URL_NAME(String.class);

    public static final Map<String,TigerBrokerField> fieldMap = new HashMap<>();

    /*
     * Static initializer
     */
    static {
        fieldMap.put("name", TigerBrokerField.NAME);
        fieldMap.put("description", TigerBrokerField.DESCRIPTION);
        fieldMap.put("databasecode", TigerBrokerField.DATABASE_CODE);
        fieldMap.put("database_code", TigerBrokerField.DATABASE_CODE);
        fieldMap.put("datasetcode", TigerBrokerField.DATASET_CODE);
        fieldMap.put("dataset_code", TigerBrokerField.DATASET_CODE);
        fieldMap.put("datasetcount", TigerBrokerField.DATASET_COUNT);
        fieldMap.put("datasets_count", TigerBrokerField.DATASET_COUNT);
        fieldMap.put("downloads", TigerBrokerField.DOWNLOADS);
        fieldMap.put("premium", TigerBrokerField.PREMIUM);
        fieldMap.put("imageurl", TigerBrokerField.IMAGE_URL);
        fieldMap.put("image", TigerBrokerField.IMAGE_URL);
        fieldMap.put("favorite", TigerBrokerField.FAVOURITE);
        fieldMap.put("url_name", TigerBrokerField.URL_NAME);
    }


    private Class<?> type;

    /**
     * Constructor
     * @param type  the data type for this field
     */
    TigerBrokerField(Class<?> type) {
        this.type = type;
    }


    /**
     * Returns the data type for this field
     * @return  the data type for field
     */
    public Class<?> getType() {
        return type;
    }


    /**
     * Returns a newly created empty DataFrame for dataset meta-data
     * @param rowType       the row type for frame
     * @param initialSize   the initial row capacity for frame
     * @return              the newly created frame
     */
    static <T> DataFrame<T,TigerBrokerField> frame(Class<T> rowType, int initialSize, TigerBrokerField... fields) {
        return DataFrame.of(Index.of(rowType, initialSize), TigerBrokerField.class, columns -> {
            for (TigerBrokerField field : fields) {
                columns.add(field, field.getType());
            }
        });
    }

    /**
     * Returns a newly created empty DataFrame for dataset meta-data
     * @param rowType       the row type for frame
     * @param initialSize   the initial row capacity for frame
     * @return              the newly created frame
     */
    static <T> DataFrame<T,TigerBrokerField> frame(Class<T> rowType, int initialSize, List<TigerBrokerField> fields) {
        return DataFrame.of(Index.of(rowType, initialSize), TigerBrokerField.class, columns -> {
            for (TigerBrokerField field : fields) {
                columns.add(field, field.getType());
            }
        });
    }


    /**
     * Returns the QuandlField representation for the name specified
     * @param name  the name for a field
     * @return      the matched field
     * @throws TigerBrokerException  if there is no match for a field
     */
    public static TigerBrokerField of(String name) {
        final TigerBrokerField field = fieldMap.get(name.toLowerCase());
        if (field == null) {
            throw new TigerBrokerException("No match for field named: " + name);
        } else {
            return field;
        }
    }

}
