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
package com.d3x.morpheus.frame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.d3x.morpheus.db.DbSource;
import com.d3x.morpheus.index.Index;
import com.d3x.morpheus.matrix.D3xMatrix;
import com.d3x.morpheus.matrix.D3xMatrixView;
import com.d3x.morpheus.range.Range;
import com.d3x.morpheus.stats.Stats;
import com.d3x.morpheus.util.Resource;
import com.d3x.morpheus.util.functions.ToBooleanFunction;
import com.d3x.morpheus.vector.D3xVector;
import com.d3x.morpheus.vector.DataVectorView;

/**
 * The central interface of the Morpheus Library that defines a 2-dimensional data structure called <code>DataFrame</code>
 *
 * <p>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></p>
 *
 * @author  Xavier Witdouck
 */
public interface DataFrame<R,C> extends DataFrameAccess<R,C>, DataFrameOperations<R,C,DataFrame<R,C>>, DataFrameIterators<R,C>, DataFrameAlgebra<R,C>, D3xMatrixView {

    /**
     * Returns true if this frame operates in parallel mode
     * @return  true if parallel mode is enabled
     */
    boolean isParallel();

    /**
     * Returns a parallel implementation of the DataFrame
     * @return  a parallel implementation of the DataFrame
     */
    DataFrame<R,C> parallel();

    /**
     * Returns a sequential implementation of the DataFrame
     * @return  a sequential implementation of the DataFrame
     */
    DataFrame<R,C> sequential();

    /**
     * Returns a deep copy of this <code>DataFrame</code>
     * @return  deep copy of this <code>DataFrame</code>
     */
    DataFrame<R,C> copy();

    /**
     * Returns a reference to the output interface for this <code>DataFrame</code>
     * @return  the output interface for this <code>DataFrame</code>
     */
    DataFrameOutput<R,C> out();

    /**
     * Returns the row operator for this DataFrame
     * @return  the row operator for this DataFrame
     */
    DataFrameRows<R,C> rows();

    /**
     * Returns the column operator for this DataFrame
     * @return  the column operator for this DataFrame
     */
    DataFrameColumns<R,C> cols();

    /**
     * Returns a newly created cursor for random access to elements of this frame
     * @return  the newly created cursor for element random access
     */
    DataFrameCursor<R,C> cursor();

    /**
     * Returns the value for the row and column key specified
     * @param rowKey    the row key
     * @param colKey    the column key
     * @return          the DataFrameValue
     */
    DataFrameValue<R,C> get(R rowKey, C colKey);

    /**
     * Returns the value for the row and column ordinal specified
     * @param rowOrdinal    the row ordinal
     * @param colOrdinal    the column ordinal
     * @return              the DataFrameValue
     */
    DataFrameValue<R,C> at(int rowOrdinal, int colOrdinal);

    /**
     * Returns a reference to the row for the key specified
     * @param rowKey    the row key to match
     * @return          the matching row
     */
    DataFrameRow<R,C> row(R rowKey);

    /**
     * Returns a reference to the column for the key specified
     * @param colKey    the column key to match
     * @return          the matching column
     */
    DataFrameColumn<R,C> col(C colKey);

    /**
     * Returns a reference to a row for the ordinal specified
     * @param rowOrdinal    the row ordinal
     * @return              the matching row
     */
    DataFrameRow<R,C> rowAt(int rowOrdinal);

    /**
     * Returns a reference to a column for the ordinal specified
     * @param colIOrdinal   the column ordinal
     * @return              the matching column
     */
    DataFrameColumn<R,C> colAt(int colIOrdinal);

    /**
     * Returns a stream of values over this DataFrame
     * @return  the stream of values over this DataFrame
     */
    Stream<DataFrameValue<R,C>> values();

    /**
     * Returns a reference to the fill interface for copy values
     * @return  the fill interface for this <code>DataFrame</code>
     */
    DataFrameFill fill();

    /**
     * Returns the sign DataFrame of -1, 0, 1 for negative, zero and positive elements
     * @return  a DataFrame of -1, 0, 1 for negative, zero and positive elements
     * @see <a href="http://en.wikipedia.org/wiki/Signum_function">Wikiepdia Reference</a>
     */
    DataFrame<R,C> sign();

    /**
     * Returns the stats for this <code>DataFrame</code>
     * @return  the stats for frame
     */
    Stats<Double> stats();

    /**
     * Returns the transpose of this DataFrame
     * @return  the transpose of this frame
     */
    DataFrame<C,R> transpose();

    /**
     * Returns the rank interface for this <code>DataFrame</code>
     * @return  the rank interface for the <code>DataFrame</code>
     */
    DataFrameRank<R,C> rank();

    /**
     * Returns the event notification interface for this DataFrame
     * @return  the event notification interface
     */
    DataFrameEvents events();

    /**
     * Returns the write interface which provides a mechanism to write frames to a data store
     * @return      the DataFrame write interface
     */
    DataFrameWrite<R,C> write();

    /**
     * Returns an interface that enables this frame to be exported as other types
     * @return      the <code>DataFrame</code> export interface
     */
    DataFrameExport export();

    /**
     * Returns an interface that can be used to efficiently cap values in the frame
     * @param inPlace   true if capping should be applied in place, otherwise cap & copy.
     * @return          the DataFrame capping interface
     */
    DataFrameCap<R,C> cap(boolean inPlace);

    /**
     * Returns a DataFrame containing the first N rows where N=min(count, frame.rowCount())
     * @param count     the max number of rows to capture
     * @return          the DataFrame containing first N rows where N=min(count, frame.rowCount())
     */
    DataFrame<R,C> head(int count);

    /**
     * Returns a DataFrame containing the last N rows where N=min(count, frame.rowCount())
     * @param count     the max number of rows to capture
     * @return          the DataFrame containing last N rows where N=min(count, frame.rowCount())
     */
    DataFrame<R,C> tail(int count);

    /**
     * Returns a DataFrame containing the first N columns where N=min(count, frame.colCount())
     * @param count     the max number of left most columns to include
     * @return          the DataFrame containing the first B columns where N=min(count, frame.colCount())
     */
    DataFrame<R,C> left(int count);

    /**
     * Returns a DataFrame containing the last N columns where N=min(count, frame.colCount())
     * @param count the max number of right most columns to include
     * @return      the DataFrame containing the last N columns where N=min(count frame.colCount())
     */
    DataFrame<R,C> right(int count);

    /**
     * Returns the Principal Component Analysis interface for this DataFrame
     * @return  the PCA interface for this DataFrame
     */
    DataFramePCA<R,C> pca();

    /**
     * Returns the calculation interface for this <code>DataFrame</code>
     * @return  the calculation interface for the <code>DataFrame</code>
     */
    DataFrameCalculate<R,C> calc();

    /**
     * Returns the DataFrame smoothing interface to apply SMA or an EWMA filter to the data
     * @param inPlace   if true, smoothing will be applied to this frame, otherwise copy & smooth.
     * @return      the DataFrame smoothing data smoothing interface
     */
    DataFrameSmooth<R,C> smooth(boolean inPlace);

    /**
     * Returns a reference to the regression interface for this DataFrame
     * @return      the regression interface for this DataFrame
     */
    DataFrameRegression<R,C> regress();

    /**
     * Adds all rows & columns from the argument that do not exist in this frame, and applies data for added coordinates
     * @param other     the other frame from which to add rows, columns & data that do not exist in this frame
     * @return          the resulting frame with additional rows and columns
     */
    DataFrame<R,C> addAll(DataFrame<R,C> other);

    /**
     * Updates data in this frame based on update frame provided
     * @param update        the DataFrame with updates to apply to this frame
     * @param addRows       if true, add any missing row keys from the update
     * @param addColumns    if true, add any missing column keys from the update
     * @return              the updated DataFrame
     */
    DataFrame<R,C> update(DataFrame<R,C> update, boolean addRows, boolean addColumns);

    /**
     * Updates data in this frame based on update frames provided
     * @param updates       the DataFrames with updates to apply to this frame
     * @param addRows       if true, add any missing row keys from the update
     * @param addColumns    if true, add any missing column keys from the update
     * @return              the updated DataFrame
     */
    default DataFrame<R,C> update(Iterable<DataFrame<R,C>> updates, boolean addRows, boolean addColumns) {
        DataFrame<R,C> updated = this;

        for (var update : updates)
            updated = updated.update(update, addRows, addColumns);

        return updated;
    }

    /**
     * Returns a <code>DataFrame</code> filter that includes a subset of rows and columns
     * @param rowKeys   the row key selection
     * @param colKeys   the column key selection
     * @return          the <code>DataFrame</code> filter containing selected rows & columns
     */
    DataFrame<R,C> select(Iterable<R> rowKeys, Iterable<C> colKeys);

    /**
     * Returns a {@code DataFrame} that contains a subset of the rows in this frame.
     *
     * @param rowKeys the keys of the rows to retain.
     *
     * @return a new {@code DataFrame} containing all columns in this frame but only
     * the specified rows.
     */
    default DataFrame<R,C> selectRows(Iterable<R> rowKeys) {
        return select(rowKeys, listColumnKeys());
    }

    /**
     * Returns a {@code DataFrame} that contains a subset of the columns in this frame.
     *
     * @param colKeys the keys of the columns to retain.
     *
     * @return a new {@code DataFrame} containing all rows in this frame but only the
     * specified columns.
     */
    default DataFrame<R,C> selectColumns(Iterable<C> colKeys) {
        return select(listRowKeys(), colKeys);
    }

    /**
     * Returns a <code>DataFrame</code> selection that includes a subset of rows and columns
     * @param rowPredicate  the predicate to select rows
     * @param colPredicate  the predicate to select columns
     * @return              the <code>DataFrame</code> containing selected rows & columns
     */
    DataFrame<R,C> select(Predicate<DataFrameRow<R,C>> rowPredicate, Predicate<DataFrameColumn<R,C>> colPredicate);

    /**
     * Returns a newly created DataFrame with all elements of this frame mapped to booleans
     * @param mapper    the mapper function to apply
     * @return          the newly created frame
     */
    DataFrame<R,C> mapToBooleans(ToBooleanFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a newly created DataFrame with all elements of this frame mapped to ints
     * @param mapper    the mapper function to apply
     * @return          the newly created frame
     */
    DataFrame<R,C> mapToInts(ToIntFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a newly created DataFrame with all elements of this frame mapped to longs
     * @param mapper    the mapper function to apply
     * @return          the newly created frame
     */
    DataFrame<R,C> mapToLongs(ToLongFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a newly created DataFrame with all elements of this frame mapped to doubles
     * @param mapper    the mapper function to apply
     * @return          the newly created frame
     */
    DataFrame<R,C> mapToDoubles(ToDoubleFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a newly created DataFrame with all elements of this frame mapped to objects
     * @param type      the type for mapper function
     * @param mapper    the mapper function to apply
     * @return          the newly created frame
     */
    <T> DataFrame<R,C> mapToObjects(Class<T> type, Function<DataFrameValue<R,C>,T> mapper);

    /**
     * Returns a shallow copy of the DataFrame with the specified column mapped to booleans
     * @param colKey    the column key to apply mapping function
     * @param mapper    the mapper function to apply
     * @return          the new frame
     * @throws DataFrameException   if frame is transposed, or column does not exist
     */
    DataFrame<R,C> mapToBooleans(C colKey, ToBooleanFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a shallow copy of the DataFrame with the specified column mapped to ints
     * @param colKey    the column key to apply mapping function
     * @param mapper    the mapper function to apply
     * @return          the new frame
     * @throws DataFrameException   if frame is transposed, or column does not exist
     */
    DataFrame<R,C> mapToInts(C colKey, ToIntFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a shallow copy of the DataFrame with the specified column mapped to longs
     * @param colKey    the column key to apply mapping function
     * @param mapper    the mapper function to apply
     * @return          the new frame
     * @throws DataFrameException   if frame is transposed, or column does not exist
     */
    DataFrame<R,C> mapToLongs(C colKey, ToLongFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a shallow copy of the DataFrame with the specified column mapped to doubles
     * @param colKey    the column key to apply mapping function
     * @param mapper    the mapper function to apply
     * @return          the new frame
     * @throws DataFrameException   if frame is transposed, or column does not exist
     */
    DataFrame<R,C> mapToDoubles(C colKey, ToDoubleFunction<DataFrameValue<R,C>> mapper);

    /**
     * Returns a shallow copy of the DataFrame with the specified column mapped to objects
     * @param colKey    the column key to apply mapping function
     * @param type      the data type for mapped column
     * @param mapper    the mapper function to apply
     * @return          the new frame
     * @throws DataFrameException   if frame is transposed, or column does not exist
     */
    <T> DataFrame<R,C> mapToObjects(C colKey, Class<T> type, Function<DataFrameValue<R,C>,T> mapper);

    /**
     * Returns a copy of this DataFrame with row and column keys mapped to
     * other classes.
     *
     * @param rowMapper a function to map row keys
     * @param colMapper a function to map column keys
     *
     * @return a new data frame with the same element values as this but with
     * row and column keys mapped by the mapping functions.
     */
    default <R2, C2> DataFrame<R2, C2> remapKeys(Function<R, R2> rowMapper,
                                                 Function<C, C2> colMapper) {
        return sequential()
                .rows().mapKeys(row -> rowMapper.apply(row.key()))
                .cols().mapKeys(col -> colMapper.apply(col.key()));
    }

    /**
     * Returns a newly created builder initialized with the contents of this frame
     * @return      the newly created builder with contents of this frame
     */
    default DataFrameBuilder<R,C> toBuilder() {
        return new DataFrameBuilder<>(this);
    }

    /**
     * Determines whether this DataFrame contains a particular column.
     *
     * @param colKey the key of the column in question.
     *
     * @return {@code true} iff this DataFrame contains a column with the specified key.
     */
    default boolean containsColumn(C colKey) {
        return cols().contains(colKey);
    }

    /**
     * Determines whether this DataFrame contains a particular column.
     *
     * @param colKey the key of the column in question.
     *
     * @return {@code true} unless this DataFrame contains a column with the specified key.
     */
    default boolean missingColumn(C colKey) {
        return !containsColumn(colKey);
    }

    /**
     * Determines whether this DataFrame contains a particular row.
     *
     * @param rowKey the key of the row in question.
     *
     * @return {@code true} iff this DataFrame contains a row with the specified key.
     */
    default boolean containsRow(R rowKey) {
        return rows().contains(rowKey);
    }

    /**
     * Determines whether this DataFrame contains a particular row.
     *
     * @param rowKey the key of the row in question.
     *
     * @return {@code true} unless this DataFrame contains a row with the specified key.
     */
    default boolean missingRow(R rowKey) {
        return !containsRow(rowKey);
    }

    /**
     * Determines whether this numeric data frame has the same contents as
     * another, within the tolerance of the default DoubleComparator.
     *
     * @param that the DataFrame to compare to this.
     *
     * @return {@code true} iff this frame and the input frame both contain
     * numeric data, have exactly the same row and column keys in the same
     * order, and all data elements are equal within the tolerance of the
     * default DoubleComparator.
     */
    default boolean equalsNumeric(DataFrame<R,C> that) {
        if (that == null)
            return false;

        if (!this.listRowKeys().equals(that.listRowKeys()))
            return false;

        if (!this.listColumnKeys().equals(that.listColumnKeys()))
            return false;

        for (C colKey : listColumnKeys()) {
            DataFrameColumn<R, C> thisCol = this.col(colKey);
            DataFrameColumn<R, C> thatCol = that.col(colKey);

            if (!thisCol.isNumeric())
                return false;

            if (!thatCol.isNumeric())
                return false;

            if (!thisCol.equalsView(thatCol))
                return false;
        }

        return true;
    }

    /**
     * Returns the value of an element at a given location.
     *
     * @param row the row index of the element to return.
     * @param col the column index of the element to return.
     *
     * @return the value of the element at the specified location.
     *
     * @throws RuntimeException if either index is out of bounds.
     */
    default double get(int row, int col) {
        return getDoubleAt(row, col);
    }

    /**
     * Returns a DataVectorView of a numeric column.
     *
     * @param colKey the key of the desired column.
     *
     * @return a DataVectorView of the specified column.
     *
     * @throws RuntimeException unless the column exists.
     */
    default DataVectorView<R> getDoubleColumn(C colKey) {
        return col(colKey);
    }

    /**
     * Returns a DataVectorView of a numeric column.
     *
     * @param colOrdinal the index of the desired column.
     *
     * @return a DataVectorView of the specified column.
     *
     * @throws RuntimeException unless the column index is in bounds.
     */
    default DataVectorView<R> getDoubleColumnAt(int colOrdinal) {
        return colAt(colOrdinal);
    }

    /**
     * Returns a list of the row keys for this DataFrame.
     *
     * @return a list of the row keys for this DataFrame.
     */
    default List<R> listRowKeys() {
        return rows().keyList();
    }

    /**
     * Returns a list of the column keys for this DataFrame.
     *
     * @return a list of the column keys for this DataFrame.
     */
    default List<C> listColumnKeys() {
        return cols().keyList();
    }

    /**
     * Returns the number of rows in this DataFrame.
     * @return the number of rows in this DataFrame.
     */
    default int nrow() {
        return rowCount();
    }

    /**
     * Returns the number of columns in this DataFrame.
     * @return the number of columns in this DataFrame.
     */
    default int ncol() {
        return colCount();
    }

    /**
     * Ensures that this DataFrame contains a particular column and that column
     * contains data of a particular type.
     *
     * @param colKey the required column key.
     * @param class_ the required data type.
     *
     * @throws DataFrameException unless this DataFrame contains a column with the
     * specified key and that column contains double data of the specified type.
     */
    default void requireColumnClass(C colKey, Class<?> class_) {
        requireColumn(colKey);

        Class<?> actual = cols().type(colKey);
        Class<?> expected = class_;

        if (!actual.equals(expected))
            throw new DataFrameException(
                    "Column [%s] contains data of type [%s], not [%s] as required.",
                    colKey, actual.getSimpleName(), expected.getSimpleName());
    }

    /**
     * Ensures that this DataFrame contains a particular column and that column
     * contains double precision data.
     *
     * @param colKey the required double precision column key.
     *
     * @throws DataFrameException unless this DataFrame contains a column with the
     * specified key and that column contains double precision data.
     */
    default void requireDoubleColumn(C colKey) {
        requireColumnClass(colKey, Double.class);
    }

    /**
     * Ensures that this DataFrame contains particular columns and those columns
     * contain double precision data.
     *
     * @param colKeys the required double precision column keys.
     *
     * @throws DataFrameException unless this DataFrame contains a column for each
     * specified key and each column contains double precision data.
     */
    default void requireDoubleColumns(Iterable<C> colKeys) {
        for (C colKey : colKeys)
            requireDoubleColumn(colKey);
    }

    /**
     * Ensures that this DataFrame contains a particular column and that column contains numeric data.
     *
     * @param colKey the required numeric column key.
     *
     * @throws DataFrameException unless this DataFrame contains a column with the specified key and
     * that column contains numeric data.
     */
    default void requireNumericColumn(C colKey) {
        requireColumn(colKey);

        if (!col(colKey).isNumeric())
            throw new DataFrameException("Column [%s] contains non-numeric data.", colKey);
    }

    /**
     * Ensures that this DataFrame contains particular columns and those columns contain numeric data.
     *
     * @param colKeys the required column keys.
     *
     * @throws DataFrameException unless this DataFrame contains a column for each specified key and
     * each column contains numeric data.
     */
    default void requireNumericColumns(Iterable<C> colKeys) {
        for (C colKey : colKeys)
            requireNumericColumn(colKey);
    }

    /**
     * Returns the double precision data in this frame in a two-dimensional array.
     *
     * @return the double precision data in this frame in a two-dimensional array.
     */
    default double[][] getDoubleMatrix() {
        double[][] matrixData = new double[rowCount()][];

        for (int irow = 0; irow < rowCount(); irow++)
            matrixData[irow] = rowAt(irow).getDoubleArray();

        return matrixData;
    }

    /**
     * Returns particular double precision data from this frame in a two-dimensional array.
     *
     * @param rowKeys the keys of the rows to extract.
     * @param colKeys the keys of the columns to extract.
     *
     * @return the double precision elements from the specified rows and columns in
     * a two-dimensional array.
     */
    default double[][] getDoubleMatrix(List<R> rowKeys, List<C> colKeys) {
        double[][] matrixData = new double[rowKeys.size()][];

        for (int irow = 0; irow < rowKeys.size(); irow++)
            matrixData[irow] = row(rowKeys.get(irow)).getDoubleArray(colKeys);

        return matrixData;
    }

    /**
     * Writes the contents of this DataFrame to an output stream.
     * @param stream the stream where this frame will appear.
     */
    default void display(PrintStream stream) {
        values().forEach(v -> stream.printf("(%s, %s) => %s%n",
                v.rowKey().toString(),
                v.colKey().toString(),
                v.getValue() != null ? v.getValue().toString() : "null"));
    }

    /**
     * Replaces missing double values.
     *
     * @param value the replacement value.
     */
    default void replaceNaN(double value) {
        values().forEach(v -> v.replaceNaN(value));
    }

    /**
     * Returns a new DataFrame builder for row and column key types
     * @param rowType   the row key type for builder
     * @param colType   the column key type for builder
     * @param <R>       the row type
     * @param <C>       the column type
     * @return          the newly created builder
     */
    static <R,C> DataFrameBuilder<R,C> builder(Class<R> rowType, Class<C> colType) {
        return new DataFrameBuilder<>(rowType, colType);
    }


    /**
     * Returns a new DataFrame builder for row and column key types
     * @param rowType   the row key type for builder
     * @param colType   the column key type for builder
     * @param <R>       the row type
     * @param <C>       the column type
     * @return          the newly created builder
     */
    static <R,C> DataFrameBuilder<R,C> builder(Class<R> rowType, Class<C> colType, Consumer<DataFrameBuilder<R,C>> consumer) {
        var builder = new DataFrameBuilder<>(rowType, colType);
        consumer.accept(builder);
        return builder;
    }


    /**
     * Returns a reference to the factory that creates new DataFrames
     * @return      the DataFrame factory
     */
    static DataFrameFactory factory() {
        return DataFrameFactory.getInstance();
    }


    /**
     * Returns a DataFrame DB source to extract data from SQL ResultSet
     * @param resultSet     the SQL result set to extract data from
     * @return              the DbSource to configure result set extraction
     */
    static DbSource read(java.sql.ResultSet resultSet) {
        return new DbSource(resultSet);
    }


    /**
     * Returns a reference to a DataFrame reader for resource
     * @param file  the file to read from
     * @return      the DataFrame read interface
     */
    static DataFrameRead read(File file) {
        return DataFrameFactory.getInstance().read(Resource.of(file));
    }


    /**
     * Returns a reference to a DataFrame reader for resource
     * @param url   the url to read from
     * @return      the DataFrame read interface
     */
    static DataFrameRead read(URL url) {
        return DataFrameFactory.getInstance().read(Resource.of(url));
    }


    /**
     * Returns a reference to a DataFrame reader for resource
     * @param path  the file path or classpath resource to read from
     * @return      the DataFrame read interface
     */
    static DataFrameRead read(String path) {
        return DataFrameFactory.getInstance().read(Resource.of(path));
    }


    /**
     * Returns a reference to a DataFrame reader for resource
     * @param is    the input stream to read from
     * @return      the DataFrame read interface
     */
    static DataFrameRead read(InputStream is) {
        return DataFrameFactory.getInstance().read(Resource.of(is));
    }


    /**
     * Returns a source of the type specified
     * @param type  the source type to initialize
     * @param <T>   the source data type
     * @return      the source instance
     */
    static <R,C,O,T extends DataFrameSource<R,C,O>> T source(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new DataFrameException("Failed to initialize DataFrame source for type: " + type, ex);
        }
    }

    /**
     * Returns an empty DataFrame with zero length rows and columns
     * @param rowAxisType   the row axis key type
     * @param colAxisType   the column axis key type
     * @param <R>           the row key type
     * @param <C>           the column key type
     * @return              the empty DataFrame
     */
    static <R,C> DataFrame<R,C> empty(Class<R> rowAxisType, Class<C> colAxisType) {
        return DataFrame.factory().empty(rowAxisType, colAxisType);
    }

    /**
     * Returns a DataFrame result by concatenating a selection of frames
     * @param frames    the iterable of frames to concatenate
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the concatenated DataFrame
     */
    @SafeVarargs
    static <R,C> DataFrame<R,C> combineFirst(DataFrame<R,C>... frames) {
        return DataFrame.factory().combineFirst(Arrays.asList(frames).iterator());
    }

    /**
     * Returns a DataFrame result by concatenating a selection of frames
     * @param frames    the iterable of frames to concatenate
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the concatenated DataFrame
     */
    static <R,C> DataFrame<R,C> combineFirst(Iterable<DataFrame<R,C>> frames) {
        return DataFrame.factory().combineFirst(frames.iterator());
    }

    /**
     * Returns a DataFrame result by combining multiple frames into one while applying only the first non-null element value
     * If there are intersecting coordinates across the frames, that first non-null value will apply in the resulting frame
     * @param frames    the stream of frames to apply
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the concatenated DataFrame
     */
    static <R,C> DataFrame<R,C> combineFirst(Stream<DataFrame<R,C>> frames) {
        return DataFrame.factory().combineFirst(frames.iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating rows from the input frames
     * If there are overlapping row keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate rows
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    @SafeVarargs
    static <R,C> DataFrame<R,C> concatRows(DataFrame<R,C>... frames) {
        return DataFrame.factory().concatRows(Arrays.asList(frames).iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating rows from the input frames
     * If there are overlapping row keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate rows
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    static <R,C> DataFrame<R,C> concatRows(Iterable<DataFrame<R,C>> frames) {
        return DataFrame.factory().concatRows(frames.iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating rows from the input frames
     * If there are overlapping row keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate rows
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    static <R,C> DataFrame<R,C> concatRows(Stream<DataFrame<R,C>> frames) {
        return DataFrame.factory().concatRows(frames.iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating columns from the input frames
     * If there are overlapping column keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate columns
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    @SafeVarargs
    static <R,C> DataFrame<R,C> concatColumns(DataFrame<R,C>... frames) {
        return DataFrame.factory().concatColumns(Arrays.asList(frames).iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating columns from the input frames
     * If there are overlapping column keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate columns
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    static <R,C> DataFrame<R,C> concatColumns(Iterable<DataFrame<R,C>> frames) {
        return DataFrame.factory().concatColumns(frames.iterator());
    }

    /**
     * Returns a newly created DataFrame by concatenating columns from the input frames
     * If there are overlapping column keys, the row values from the first frame will apply
     * @param frames        the iterable of frames from which to concatenate columns
     * @param <R>           the row key type for frames
     * @param <C>           the column key type for frames
     * @return              the resulting DataFrame
     */
    static <R,C> DataFrame<R,C> concatColumns(Stream<DataFrame<R,C>> frames) {
        return DataFrame.factory().concatColumns(frames.iterator());
    }


    /**
     * Returns a DataFrame initialized from a consumers actions on a builder
     * @param rowType   the row key type
     * @param colType   the column key type
     * @param consumer  the consumer to affect builder
     * @param <R>       the row type
     * @param <C>       the column type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> of(
        @lombok.NonNull Class<R> rowType,
        @lombok.NonNull Class<C> colType,
        @lombok.NonNull Consumer<DataFrameBuilder<R,C>> consumer) {
        var builder = DataFrame.builder(rowType, colType);
        consumer.accept(builder);
        return builder.build();
    }


    /**
     * Returns an empty DataFrame with the row and column types specified
     * @param rowType   the row key type
     * @param colType   the column key type
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> of(Class<R> rowType, Class<C> colType) {
        return DataFrame.factory().from(Index.of(rowType, 1000), Index.of(colType, 20), Object.class);
    }

    /**
     * Returns a newly created DataFrame optimized for columns with the type specified
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param type      the data type for columns
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> of(Iterable<R> rowKeys, Iterable<C> colKeys, Class<?> type) {
        return DataFrame.factory().from(rowKeys, colKeys, type);
    }

    /**
     * Returns a newly created DataFrame with 1 row and N columns all with the data type specified
     * @param rowKey    the row key for frame
     * @param colKeys   the column keys for frame
     * @param dataType  the data type for columns
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> of(R rowKey, Iterable<C> colKeys, Class<?> dataType) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, dataType);
    }

    /**
     * Returns a newly created DataFrame with N rows and 1 column with the data type specified
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param type      the data type for columns
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> of(Iterable<R> rowKeys, C colKey, Class<?> type) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), type);
    }

    /**
     * Returns a newly created DataFrame initialized with rows and any state added by the consumer
     * @param rowKeys   the row keys for frame
     * @param colType   the column key type
     * @param columns   the consumer which can be used to add columns
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created <code>DataFrame</code>
     */
    static <R,C> DataFrame<R,C> of(Iterable<R> rowKeys, Class<C> colType, Consumer<DataFrameColumns<R,C>> columns) {
        return DataFrame.factory().from(rowKeys, colType, columns);
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive booleans
     * @param rowKey    the row key for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofBooleans(R rowKey, Iterable<C> colKeys) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, Boolean.class);
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive integers
     * @param rowKey    the row key for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofInts(R rowKey, Iterable<C> colKeys) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, Integer.class);
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive longs
     * @param rowKey    the row key for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofLongs(R rowKey, Iterable<C> colKeys) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, Long.class);
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive doubles
     * @param rowKey    the row key for frame
     * @param colKeys   the column index for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofDoubles(R rowKey, Iterable<C> colKeys) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, Double.class);
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive doubles
     * @param rowKey    the row key for frame
     * @param colKeys   the column index for frame
     * @param values    a vector of values to assign
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     * @throws          DataFrameException if the number of keys and values disagrees
     */
    static <R,C> DataFrame<R,C> ofDoubles(R rowKey, Iterable<C> colKeys, double[] values) {
        DataFrame<R,C> frame = ofDoubles(rowKey, colKeys);

        if (frame.colCount() != values.length)
            throw new DataFrameException("Key/value length mismatch.");

        frame.applyDoubles(cursor -> values[cursor.colOrdinal()]);
        return frame;
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold primitive doubles
     * @param rowKey    the row key for frame
     * @param colKeys   the column index for frame
     * @param values    a vector of values to assign
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     * @throws          DataFrameException if the number of keys and values disagrees
     */
    static <R,C> DataFrame<R,C> ofDoubles(R rowKey, Iterable<C> colKeys, D3xVector values) {
        DataFrame<R,C> frame = ofDoubles(rowKey, colKeys);

        if (frame.colCount() != values.length())
            throw new DataFrameException("Key/value length mismatch.");

        frame.applyDoubles(cursor -> values.get(cursor.colOrdinal()));
        return frame;
    }

    /**
     * Returns a newly created DataFrame with 1 row optimized to hold any object
     * @param rowKey    the row key for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofObjects(R rowKey, Iterable<C> colKeys) {
        return DataFrame.factory().from(Index.singleton(rowKey), colKeys, Object.class);
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive booleans
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofBooleans(Iterable<R> rowKeys, C colKey) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), Boolean.class);
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive integers
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofInts(Iterable<R> rowKeys, C colKey) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), Integer.class);
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive longs
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofLongs(Iterable<R> rowKeys, C colKey) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), Long.class);
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive doubles
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, C colKey) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), Double.class);
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive doubles
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param values    the vector values to assign
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     * @throws          DataFrameException if the number of keys and values disagrees
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, C colKey, double[] values) {
        DataFrame<R,C> frame = ofDoubles(rowKeys, colKey);

        if (frame.rowCount() != values.length)
            throw new DataFrameException("Key/value length mismatch.");

        frame.applyDoubles(cursor -> values[cursor.rowOrdinal()]);
        return frame;
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold primitive doubles
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param values    the vector values to assign
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     * @throws          DataFrameException if the number of keys and values disagrees
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, C colKey, D3xVector values) {
        DataFrame<R,C> frame = ofDoubles(rowKeys, colKey);

        if (frame.rowCount() != values.length())
            throw new DataFrameException("Key/value length mismatch.");

        frame.applyDoubles(cursor -> values.get(cursor.rowOrdinal()));
        return frame;
    }

    /**
     * Returns a newly created DataFrame with 1 column optimized to hold any object
     * @param rowKeys   the row keys for frame
     * @param colKey    the column key for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofObjects(Iterable<R> rowKeys, C colKey) {
        return DataFrame.factory().from(rowKeys, Index.singleton(colKey), Object.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive booleans
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofBooleans(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, Boolean.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive integers
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofInts(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, Integer.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive longs
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofLongs(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, Long.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive doubles
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, Double.class);
    }

    /**
     * Returns a newly created DataFrame with values assigned from a matrix.
     *
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param values    the matrix of values to assign
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     *
     * @throws DataFrameException unless the matrix dimensions match those of the row and column keys.
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, Iterable<C> colKeys, D3xMatrix values) {
        DataFrame<R,C> frame = ofDoubles(rowKeys, colKeys);

        if (frame.rowCount() != values.nrow())
            throw new DataFrameException("Row dimension mismatch.");

        if (frame.colCount() != values.ncol())
            throw new DataFrameException("Column dimension mismatch.");

        frame.applyDoubles(cursor -> values.get(cursor.rowOrdinal(), cursor.colOrdinal()));
        return frame;
    }

    /**
     * Returns a newly created DataFrame optimized to hold Strings
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofStrings(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, String.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold any objects
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofObjects(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.factory().from(rowKeys, colKeys, Object.class);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive booleans
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param initials  a function to provide initial values
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofBooleans(Iterable<R> rowKeys, Iterable<C> colKeys, ToBooleanFunction<DataFrameValue<R,C>> initials) {
        return DataFrame.factory().from(rowKeys, colKeys, Boolean.class).applyBooleans(initials);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive integers
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param initials  a function to provide initial values
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofInts(Iterable<R> rowKeys, Iterable<C> colKeys, ToIntFunction<DataFrameValue<R,C>> initials) {
        return DataFrame.factory().from(rowKeys, colKeys, Integer.class).applyInts(initials);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive longs
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param initials  a function to provide initial values
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofLongs(Iterable<R> rowKeys, Iterable<C> colKeys, ToLongFunction<DataFrameValue<R,C>> initials) {
        return DataFrame.factory().from(rowKeys, colKeys, Long.class).applyLongs(initials);
    }

    /**
     * Returns a newly created DataFrame optimized to hold primitive doubles
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param initials  a function to provide initial values
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofDoubles(Iterable<R> rowKeys, Iterable<C> colKeys, ToDoubleFunction<DataFrameValue<R,C>> initials) {
        return DataFrame.factory().from(rowKeys, colKeys, Double.class).applyDoubles(initials);
    }

    /**
     * Returns a newly created DataFrame optimized to hold any objects
     * @param rowKeys   the row keys for frame
     * @param colKeys   the column keys for frame
     * @param initials  a function to provide initial values
     * @param <R>       the row key type
     * @param <C>       the column key type
     * @return          the newly created DataFrame
     */
    static <R,C> DataFrame<R,C> ofObjects(Iterable<R> rowKeys, Iterable<C> colKeys, Function<DataFrameValue<R, C>, ?> initials) {
        return DataFrame.factory().from(rowKeys, colKeys, Object.class).applyValues(initials);
    }

    /**
     * Returns a DataFrame of doubles initialized with ARGB values for each pixel in the image
     * @param file      the file to load the image from
     * @return          the DataFrame of ARGB values extracted from java.awt.image.BufferedImage
     * @link java.awt.image.BufferedImage#getRGB
     */
    static DataFrame<Integer,Integer> ofImage(File file) {
        try {
            final BufferedImage image = ImageIO.read(file);
            final Range<Integer> rowKeys = Range.of(0, image.getHeight());
            final Range<Integer> colKeys = Range.of(0, image.getWidth());
            return DataFrame.ofInts(rowKeys, colKeys, v -> image.getRGB(v.colOrdinal(), v.rowOrdinal()));
        } catch (Exception ex) {
            throw new DataFrameException("Failed to initialize DataFrame from image file: " + file.getAbsolutePath(), ex);
        }
    }

    /**
     * Returns a DataFrame of doubles initialized with ARGB values for each pixel in the image
     * @param url       the url to load the image from
     * @return          the DataFrame of ARGB values extracted from java.awt.image.BufferedImage
     * @see java.awt.image.BufferedImage#getRGB
     */
    static DataFrame<Integer,Integer> ofImage(URL url) {
        try {
            final BufferedImage image = ImageIO.read(url);
            final Range<Integer> rowKeys = Range.of(0, image.getHeight());
            final Range<Integer> colKeys = Range.of(0, image.getWidth());
            return DataFrame.ofInts(rowKeys, colKeys, v -> image.getRGB(v.colOrdinal(), v.rowOrdinal()));
        } catch (Exception ex) {
            throw new DataFrameException("Failed to initialize DataFrame from image url: " + url, ex);
        }
    }


    /**
     * Returns a DataFrame of doubles initialized with ARGB values for each pixel in the image
     * @param inputStream   the input stream to load the image from
     * @return          the DataFrame of ARGB values extracted from java.awt.image.BufferedImage
     * @see java.awt.image.BufferedImage#getRGB
     */
    static DataFrame<Integer,Integer> ofImage(InputStream inputStream) {
        try {
            final BufferedImage image = ImageIO.read(inputStream);
            final Range<Integer> rowKeys = Range.of(0, image.getHeight());
            final Range<Integer> colKeys = Range.of(0, image.getWidth());
            return DataFrame.ofInts(rowKeys, colKeys, v -> image.getRGB(v.colOrdinal(), v.rowOrdinal()));
        } catch (Exception ex) {
            throw new DataFrameException("Failed to initialize DataFrame from image input stream", ex);
        }
    }

    /**
     * Returns a single-column DataFrame with every row set to 1.0.
     *
     * @param <R>     the row key type.
     * @param <C>     the column key type.
     * @param rowKeys the row keys for the frame.
     * @param colKey  the column key for the frame.
     *
     * @return a single-column DataFrame with every row set to 1.0.
     */
    static <R,C> DataFrame<R,C> onesColumn(Iterable<R> rowKeys, C colKey) {
        return DataFrame.ofDoubles(rowKeys, colKey).applyDoubles(x -> 1.0);
    }

    /**
     * Returns a single-row DataFrame with every column set to 1.0.
     *
     * @param <R>     the row key type.
     * @param <C>     the column key type.
     * @param rowKey  the row key for the frame.
     * @param colKeys the column keys for the frame.
     *
     * @return a single-column DataFrame with every column set to 1.0.
     */
    static <R,C> DataFrame<R,C> onesRow(R rowKey, Iterable<C> colKeys) {
        return DataFrame.ofDoubles(rowKey, colKeys).applyDoubles(x -> 1.0);
    }

    /**
     * Returns a single-column DataFrame with every row set to 0.0.
     *
     * @param <R>     the row key type.
     * @param <C>     the column key type.
     * @param rowKeys the row keys for the frame.
     * @param colKey  the column key for the frame.
     *
     * @return a single-column DataFrame with every row set to 0.0.
     */
    static <R,C> DataFrame<R,C> zerosColumn(Iterable<R> rowKeys, C colKey) {
        return DataFrame.ofDoubles(rowKeys, colKey).applyDoubles(x -> 0.0);
    }

    /**
     * Returns a single-column DataFrame with every column set to 0.0.
     *
     * @param <R>     the row key type.
     * @param <C>     the column key type.
     * @param rowKey  the row key for the frame.
     * @param colKeys the column keys for the frame.
     *
     * @return a single-column DataFrame with every column set to 0.0.
     */
    static <R,C> DataFrame<R,C> zerosRow(R rowKey, Iterable<C> colKeys) {
        return DataFrame.ofDoubles(rowKey, colKeys).applyDoubles(x -> 0.0);
    }

    /**
     * Returns a DataFrame with every element set to 0.0.
     *
     * @param <R>     the row key type.
     * @param <C>     the column key type.
     * @param rowKeys the row key for the frame.
     * @param colKeys the column keys for the frame.
     *
     * @return a DataFrame with every element set to 0.0.
     */
    static <R,C> DataFrame<R,C> zeros(Iterable<R> rowKeys, Iterable<C> colKeys) {
        return DataFrame.ofDoubles(rowKeys, colKeys, x -> 0.0);
    }
}
