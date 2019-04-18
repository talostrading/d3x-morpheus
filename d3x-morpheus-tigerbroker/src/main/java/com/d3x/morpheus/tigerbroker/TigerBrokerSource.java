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

import static com.d3x.morpheus.tigerbroker.TigerBrokerField.END_DATE;
import static com.d3x.morpheus.tigerbroker.TigerBrokerField.LAST_REFRESH_TIME;
import static com.d3x.morpheus.tigerbroker.TigerBrokerField.NAME;
import static com.d3x.morpheus.tigerbroker.TigerBrokerField.START_DATE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.util.IO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;


public class TigerBrokerSource {

    private static final ThreadLocal<String> apiKeyThreadLocal = new ThreadLocal<>();

    private Gson gson;
    /** The default API key to use */
    @lombok.Getter @lombok.Setter private String apiKey;
    /** The Quandl server base url */
    @lombok.Getter @lombok.Setter private String baseUrl;
    /** The http client to interact with Quandl */
    private CloseableHttpClient httpClient;


    /**
     * Constructor
     * @param apiKey    the Quandl API token
     */
    public TigerBrokerSource(String apiKey) {
        this("https://www.quandl.com", apiKey);
    }

    /**
     * Constructor
     * @param baseUrl   the Quandl base url
     * @param apiKey    the Quandl API token
     */
    public TigerBrokerSource(String baseUrl, String apiKey) {
        Objects.requireNonNull(baseUrl, "The Quandl baseUrl cannot be null");
        Objects.requireNonNull(apiKey, "The Quandl apiKey cannot be null");
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClientBuilder.create().build();
        this.gson = new GsonBuilder()
            .registerTypeAdapter(TigerBrokerDatasetInfo.class, new TigerBrokerDatasetInfo.Deserializer())
            .registerTypeAdapter(TigerBrokerDatabaseInfo.class, new TigerBrokerDatabaseInfo.Deserializer())
            .registerTypeAdapter(LocalTime.class, new TemporalDeserializer(DateTimeFormatter.ISO_LOCAL_TIME))
            .registerTypeAdapter(LocalDate.class, new TemporalDeserializer(DateTimeFormatter.ISO_LOCAL_DATE))
            .registerTypeAdapter(LocalDateTime.class, new TemporalDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .registerTypeAdapter(ZonedDateTime.class, new TemporalDeserializer(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .create();
    }

    /**
     * Returns the fully qualified Quandl URL string
     * @param path      the path to append to base url
     * @return          the Quandl request URL
     */
    private URL createUrl(String path) {
        return createUrl(path, null);
    }


    /**
     * Returns the fully qualified Quandl URL string
     * @param path      the path to append to base url
     * @param query     the query string if any, null permitted
     * @return          the Quandl request URL
     */
    private URL createUrl(String path, String query) {
        try {
            final String key = apiKeyThreadLocal.get();
            final String apiKey = key != null ? key : this.apiKey;
            if (apiKey == null) {
                throw new TigerBrokerException("No API key configured for QuandlSource");
            } else {
                final String url = baseUrl + path + "?api_key=" + apiKey;
                return query != null ? new URL(url + "&" + query) : new URL(url);
            }
        } catch (MalformedURLException ex) {
            throw new TigerBrokerException("Failed to create Quandl REST url", ex);
        }
    }


    /**
     * Returns a DataFrame with a full listing of all databases available on Quandl
     * https://www.quandl.com/api/v3/databases.csv?api_key=DrFK1MBShGiB32kCHZXx&per_page=10000
     * @return  the DataFrame with a full listing of Quandl databases
     * @throws TigerBrokerException  if this operation fails
     */
    public DataFrame<Integer,TigerBrokerField> getDatabases() throws TigerBrokerException {
        try {
            final int maxPages = 1000;
            final int pageSize = 100;
            final List<DataFrame<Integer,String>> frameList = new ArrayList<>();
            for (int i=0; i<maxPages; ++i) {
                final URL url = createUrl("/api/v3/databases.csv", "page=" + i + "&per_page=" + pageSize);
                System.out.println("Calling: " + url);
                final DataFrame<Integer,String> frame = DataFrame.read().csv(options -> {
                    options.setURL(url);
                    options.setExcludeColumns("id");
                    options.setColumnType("datasets_count", Long.class);
                    options.setColumnType("downloads", Long.class);
                    options.setRowKeyParser(Integer.class, v -> Integer.parseInt(v[0]));
                });
                if (frame.rowCount() == 0) break;
                frameList.add(frame);
            }
            final DataFrame<Integer,String> combined = DataFrame.combineFirst(frameList);
            return combined.cols().mapKeys(column -> TigerBrokerField.of(column.key()));
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load database list from Quandl: " + ex.getMessage(), ex);
        }
    }


    /**
     * Returns a DataFrame with all the datasets in the database with code specified
     * @param database  the database code to select all dataset codes
     * @return              the DataFrame of all dataset codes
     */
    public DataFrame<String,TigerBrokerField> getDatasets(String database) {
        InputStream stream = null;
        Objects.requireNonNull(database, "The database code cannot be null");
        try {
            final URL url = createUrl("/api/v3/databases/" + database  + "/metadata");
            final File localFile = downloadZipFile(url);
            final ZipFile zipfile = new ZipFile(localFile);
            final ZipEntry entry = zipfile.stream().iterator().next();
            final DatabaseCodesProcessor processor = new DatabaseCodesProcessor();
            final CsvParser parser = createParser(processor);
            parser.parse(stream = zipfile.getInputStream(entry));
            return processor.frame;
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load dataset codes for Quandl database: " + database, ex);
        } finally {
            IO.close(stream);
        }
    }


    /**
     * Executes a Quandl search operation for datasets that match the expression specified
     * @param expression    the search expression
     * @return              the frame with dataset meta-data
     */
    public DataFrame<Integer,TigerBrokerField> search(String expression) {
        JsonReader reader = null;
        CloseableHttpResponse response = null;
        try {
            final String search = URLEncoder.encode(expression, "UTF-8");
            final URL url = createUrl("/api/v3/datasets.json", "query=" + search + "&per_page=2000");
            final DataFrame<Integer,TigerBrokerField> frame = TigerBrokerDatasetInfo.frame(2000);
            response = doGet(url.toString(), null);
            reader = new JsonReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
            reader.beginObject();
            reader.nextName();
            reader.beginArray();
            while (reader.hasNext()) {
                final TigerBrokerDatasetInfo info = gson.fromJson(reader, TigerBrokerDatasetInfo.class);
                final int row = frame.rows().add(info.getId());
                frame.rows().setValueAt(row, TigerBrokerField.DATABASE_CODE, info.getDatabaseCode());
                frame.rows().setValueAt(row, TigerBrokerField.DATASET_CODE, info.getDatasetCode());
                frame.rows().setValueAt(row, TigerBrokerField.NAME, info.getName());
                frame.rows().setValueAt(row, TigerBrokerField.DESCRIPTION, info.getDescription());
                frame.rows().setValueAt(row, TigerBrokerField.LAST_REFRESH_TIME, info.getRefreshedAt());
                frame.rows().setValueAt(row, TigerBrokerField.START_DATE, info.getOldestAvailableDate());
                frame.rows().setValueAt(row, TigerBrokerField.END_DATE, info.getNewestAvailableDate());
                frame.rows().setValueAt(row, TigerBrokerField.COLUMN_NAMES, info.getColumnNames());
                frame.rows().setValueAt(row, TigerBrokerField.FREQUENCY, info.getFrequency());
                frame.rows().setValueAt(row, TigerBrokerField.DATASET_TYPE, info.getType());
                frame.rows().setValueAt(row, TigerBrokerField.PREMIUM, info.isPremium());
                frame.rows().setValueAt(row, TigerBrokerField.DATABASE_ID, info.getDatabaseId());
            }
            return frame;
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to execute search request for " + expression, ex);
        } finally {
            IO.close(reader);
            IO.close(response);
        }
    }


    /**
     * Returns meta-data for a database identified by the code provided
     * https://www.quandl.com/api/v3/databases/WIKI.json?api_key=DrFK1MBShGiB32kCHZXx
     * @param database  the Quandl database code, for example "WIKI"
     * @return          the database meta-data definition
     */
    public TigerBrokerDatabaseInfo getMetaData(String database) throws TigerBrokerException {
        JsonReader reader = null;
        CloseableHttpResponse response = null;
        try {
            final URL url = createUrl("/api/v3/databases/" + database + ".json");
            response = doGet(url.toString(), null);
            reader = new JsonReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
            reader.beginObject();
            reader.nextName();
            return gson.fromJson(reader, TigerBrokerDatabaseInfo.class);
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load database meta-data for " + database, ex);
        } finally {
            IO.close(reader);
            IO.close(response);
        }
    }


    /**
     * Returns meta-data for a dataset in the specified database
     * @param database  the Quandl database code, for example "WIKI"
     * @param dataset   the Quandl dataset code in database, for example "AAPL"
     * @return          the dataset meta-data definition
     */
    public TigerBrokerDatasetInfo getMetaData(String database, String dataset) throws TigerBrokerException {
        JsonReader reader = null;
        CloseableHttpResponse response = null;
        try {
            final URL url = createUrl("/api/v3/datasets/" + database + "/" + dataset + "/metadata.json");
            response = doGet(url.toString(), null);
            reader = new JsonReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
            reader.beginObject();
            reader.nextName();
            return gson.fromJson(reader, TigerBrokerDatasetInfo.class);
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load dataset meta-data for " + database + "/" + dataset, ex);
        } finally {
            IO.close(reader);
            IO.close(response);
        }
    }


    /**
     * Returns a DataFrame result from a Quandl time series query
     * @link https://docs.quandl.com/docs/time-series
     * @link https://www.quandl.com/api/v3/datasets/WIKI/AAPL.csv?api_key=NSXspMMxn41-Y-9w_hw_&start_date=2014-01-06&end_date=2014-02-04&order=asc
     * @param consumer  the consumer to initialize the options
     * @return          the resulting DataFrame
     */
    public DataFrame<LocalDate,String> getTimeSeries(Consumer<TimeSeriesOptions> consumer) {
        final TimeSeriesOptions options = initOptions(TimeSeriesOptions.class, consumer);
        try {
            final String database = options.getDatabase();
            final String dataset = options.getDataset();
            final String queryString = options.toQueryString();
            final URL url = createUrl("/api/v3/datasets/" + database + "/" + dataset + ".csv", queryString);
            IO.println(url);
            return DataFrame.read().csv(csvOptions -> {
                csvOptions.setURL(url);
                csvOptions.setColIndexPredicate(index -> index != 0);
                csvOptions.setRowKeyParser(LocalDate.class, v -> LocalDate.parse(v[0]));
            });
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load time-series from Quandl: " + options, ex);
        }
    }


    /**
     * Returns a DataFrame result from a Quandl DataTable query
     * @link https://docs.quandl.com/docs/tables-1
     * @link https://www.quandl.com/api/v3/datatables/FXCM/H1.csv?api_key=NSXspMMxn41-Y-9w_hw_
     * @param consumer  the consumer to initialize the options
     * @return          the resulting DataFrame
     */
    public DataFrame<Integer,String> getDataTable(Consumer<DataTableOptions> consumer) {
        CloseableHttpResponse response = null;
        final DataTableOptions options = initOptions(DataTableOptions.class, consumer);
        try {
            final String database = options.getDatabase();
            final String dataset = options.getDataset();
            final String queryString = options.toQueryString();
            final String urlPath = "/api/v3/datatables/" + database + "/" + dataset + ".csv";
            final URL url = createUrl(urlPath, queryString);
            response = doGet(url.toString(), null);
            final DataFrame<Integer,String> frame = DataFrame.read().csv(response.getEntity().getContent());
            Header cursorId = response.getFirstHeader("Cursor_ID");
            while (cursorId != null) {
                final String nextQuery = queryString + "&qopts.cursor_id=" + cursorId.getValue();
                final URL nextUrl = createUrl(urlPath, nextQuery);
                IO.close(response);
                response = doGet(nextUrl.toString(), null);
                final DataFrame<Integer,String> nextPage = DataFrame.read().csv(response.getEntity().getContent());
                final DataFrame<Integer,String> nextFrame = nextPage.rows().mapKeys(row -> frame.rowCount() + row.ordinal());
                cursorId = response.getFirstHeader("Cursor_ID");
                frame.rows().addAll(nextFrame);
            }
            return frame;
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to load data-table from Quandl for: " + options, ex);
        } finally {
            IO.close(response);
        }
    }


    /**
     * Returns initialized quandl options based on consumer
     * @param configurator  the options configurator
     * @return              the newly initialized options
     */
    private <O extends BaseOptions> O initOptions(Class<O> type, Consumer<O> configurator) {
        try {
            final O options = type.getDeclaredConstructor().newInstance();
            configurator.accept(options);
            if (options.getDatabase() == null) {
                throw new TigerBrokerException("The database code cannot be null");
            } else if (options.getDataset() == null) {
                throw new TigerBrokerException("The dataset code cannot be null");
            } else {
                return options;
            }
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to initialize quandl query options for type: " + type, ex);
        }
    }


    /**
     * Downloads a zip file to a temp file and returns that temp file handle
     * @param url   the URL of zip file resource to download
     * @return      the temp zip file
     */
    private File downloadZipFile(URL url) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            final String tmpDir = System.getProperty("java.io.tmpdir");
            final File file = new File(tmpDir, UUID.randomUUID().toString() + ".zip");
            file.deleteOnExit();
            bis = new BufferedInputStream(url.openStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));
            final byte[] buffer = new byte[1024 * 100];
            while (true) {
                final int read = bis.read(buffer);
                if (read < 0) break;
                bos.write(buffer, 0, read);
            }
            return file;
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to download zip file from " + url, ex);
        } finally {
            IO.close(bis);
            IO.close(bos);
        }
    }


    /**
     * Returns a newly created CSV parser with the row processor provided
     * @param processor the row processor
     * @return          the CSV parser
     */
    private CsvParser createParser(RowProcessor processor) {
        final CsvParserSettings settings = new CsvParserSettings();
        settings.getFormat().setDelimiter(',');
        settings.setHeaderExtractionEnabled(true);
        settings.setLineSeparatorDetectionEnabled(true);
        settings.setProcessor(processor);
        settings.setMaxCharsPerColumn(10000);
        settings.setIgnoreTrailingWhitespaces(true);
        settings.setIgnoreLeadingWhitespaces(true);
        settings.setSkipEmptyLines(true);
        settings.setMaxColumns(10000);
        settings.setReadInputOnSeparateThread(true);
        return new CsvParser(settings);
    }


    /**
     * Performs an HTTP GET and returns the Apache response object
     * @param url       the request url
     * @param handler   the handler to configure GET request
     * @return          the Apache response
     */
    public CloseableHttpResponse doGet(String url, Consumer<HttpGet> handler) {
        try {
            HttpGet request = new HttpGet(url);
            if (handler != null) handler.accept(request);
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new TigerBrokerException("Quandl response code of " + statusCode + " to " + url);
            } else {
                return response;
            }
        } catch (TigerBrokerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TigerBrokerException("Failed to execute HTTP GET for " + url, ex);
        }
    }



    /**
     * A CSV row processor to parse dataset codes for a Quandl database.
     */
    private class DatabaseCodesProcessor implements RowProcessor {

        private DataFrame<String,TigerBrokerField> frame;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private List<TigerBrokerField> fields = Arrays.asList(NAME, LAST_REFRESH_TIME, START_DATE, END_DATE);

        @Override
        public void processStarted(ParsingContext context) {
            this.frame = TigerBrokerField.frame(String.class, 10000, fields);
        }
        @Override
        public void processEnded(ParsingContext context) {

        }
        @Override
        public void rowProcessed(String[] row, ParsingContext context) {
            try {
                if (row.length < 2) {
                    System.err.println("Ignoring line: " + String.join(",", row));
                } else {
                    final String ticker = row[0].trim();
                    final String name = row[1];
                    final int rowIndex = frame.rows().add(ticker);
                    frame.setValueAt(rowIndex, 0, name);
                    if (row.length == 6) {
                        final LocalDateTime timestamp = LocalDateTime.parse(row[3], formatter);
                        final ZonedDateTime zonedDateTime = ZonedDateTime.of(timestamp, ZoneId.of("America/New_York"));
                        frame.setValueAt(rowIndex, 0, row[1]);
                        frame.setValueAt(rowIndex, 1, zonedDateTime);
                        frame.setValueAt(rowIndex, 2, LocalDate.parse(row[4]));
                        frame.setValueAt(rowIndex, 3, LocalDate.parse(row[5]));
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to process line: " +  String.join(",", row), ex);
            }
        }
    }


    /**
     * A base class for building query options for Quandl
     */
    static class BaseOptions {
        /** The optional override API key */
        @lombok.Getter @lombok.Setter private String apiKey;
        /** The Quandl database code to query */
        @lombok.Getter @lombok.Setter private String database;
        /** The Quandl dataset code to query */
        @lombok.Getter @lombok.Setter private String dataset;
    }


    /**
     * A request descriptor used to load time-series from Quandl
     */
    @lombok.ToString()
    public static class TimeSeriesOptions extends BaseOptions {

        /** The start date for query range, inclusive */
        @lombok.Getter @lombok.Setter private LocalDate startDate;
        /** The end date for query range, inclusive */
        @lombok.Getter @lombok.Setter private LocalDate endDate;
        /** Optional row count limit */
        @lombok.Getter @lombok.Setter private Integer limit;
        /** True to return time series data in ascending order */
        @lombok.Getter @lombok.Setter private Boolean ascending = true;


        /**
         * Sets the start date for these options
         * @param start date string in YYYY-mm-dd
         */
        public void startDate(String start) {
            this.startDate = LocalDate.parse(start);
        }

        /**
         * Sets the end date for these options
         * @param end date string in YYYY-mm-dd
         */
        public void endDate(String end) {
            this.endDate = LocalDate.parse(end);
        }

        /**
         * Returns a URL query string for these options
         * @return      the URL query string
         */
        String toQueryString() {
            final StringBuilder query = new StringBuilder();
            if (getApiKey() != null) {
                query.append(query.length() > 0 ? "&" : "");
                query.append("api_key=").append(getApiKey());
            }
            if (startDate != null) {
                query.append(query.length() > 0 ? "&" : "");
                query.append("start_date=").append(startDate);
            }
            if (endDate != null) {
                query.append(query.length() > 0 ? "&" : "");
                query.append("end_date=").append(endDate);
            }
            if (ascending != null) {
                query.append(query.length() > 0 ? "&" : "");
                query.append(ascending ? "order=asc" : "order=desc");
            }
            if (limit != null) {
                query.append(query.length() > 0 ? "&" : "");
                query.append("limit=").append(limit);
            }
            return query.toString();
        }
    }


    /**
     * A request descriptor used to load data-table data from Quandl
     */
    @lombok.ToString()
    public static class DataTableOptions extends BaseOptions {

        /** The page size for response */
        @lombok.Getter @lombok.Setter private int pageSize = 10000;
        /** The optional list of columns to select */
        @lombok.Getter private List<String> columns = new ArrayList<>();
        /** The map of filter key / value pairs to include */
        @lombok.Getter private Map<String,String> filter = new HashMap<>();


        /**
         * Adds column names to the filer
         * @param columns   the column names
         */
        public void addColumns(String... columns) {
            this.columns.addAll(Arrays.asList(columns));
        }

        /**
         * Returns a URL query string for these options
         * @return      the URL query string
         */
        String toQueryString() {
            try {
                final StringBuilder query = new StringBuilder();
                if (getApiKey() != null) {
                    query.append(query.length() > 0 ? "&" : "");
                    query.append("api_key=").append(getApiKey());
                }
                if (pageSize > 0) {
                    query.append(query.length() > 0 ? "&" : "");
                    query.append("qopts.per_page=").append(pageSize);
                }
                if (columns.size() > 0) {
                    query.append(query.length() > 0 ? "&" : "");
                    query.append("qopts.columns=").append(String.join(",", columns));
                }
                for (String key : filter.keySet()) {
                    final String raw = filter.get(key);
                    if (raw != null) {
                        final String value = URLEncoder.encode(raw, "UTF-8");
                        query.append(query.length() > 0 ? "&" : "");
                        query.append(key).append(value);
                    }
                }
                return query.toString();
            } catch (Exception ex) {
                throw new TigerBrokerException("Failed to generate query URL string from options: " + this, ex);
            }
        }
    }



    @lombok.AllArgsConstructor()
    private static class TemporalDeserializer<T extends Temporal> implements JsonDeserializer<T> {
        @lombok.NonNull()
        private DateTimeFormatter formatter;
        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.equals(JsonNull.INSTANCE)) {
                return null;
            } else if (type.equals(LocalTime.class)) {
                return (T)LocalTime.parse(json.getAsString(), formatter);
            } else if (type.equals(LocalDate.class)) {
                return (T)LocalDate.parse(json.getAsString(), formatter);
            } else if (type.equals(LocalDateTime.class)) {
                return (T)LocalDateTime.parse(json.getAsString(), formatter);
            } else if (type.equals(ZonedDateTime.class)) {
                return (T)ZonedDateTime.parse(json.getAsString(), formatter);
            } else {
                throw new IllegalArgumentException("Unsupported temporal type: " + type);
            }
        }
    }

}
