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
package com.d3x.morpheus.excel;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.d3x.morpheus.array.Array;
import com.d3x.morpheus.array.ArrayBuilder;
import com.d3x.morpheus.frame.DataFrame;
import com.d3x.morpheus.frame.DataFrameException;
import com.d3x.morpheus.frame.DataFrameSource;
import com.d3x.morpheus.index.Index;
import com.d3x.morpheus.range.Range;
import com.d3x.morpheus.util.IO;
import com.d3x.morpheus.util.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * A component used to initialize a DataFrame from the contents of an Excel sheet
 *
 * @author Xavier Witdouck
 */
public class ExcelSource implements DataFrameSource<Integer,String, ExcelSource.Options> {


    public static DataFrame<Integer,String> load(Consumer<Options> consumer) {
        return new ExcelSource().read(consumer);
    }

    /**
     * Returns a DataFrames loaded from an Excel sheet according to configured options
     * @param consumer      the consumer to configure options
     * @return              the resulting DataFrame
     */
    public DataFrame<Integer,String> read(Consumer<Options> consumer) {
        Workbook workbook = null;
        try {
            final Options options = new Options();
            consumer.accept(options);
            options.validate();
            workbook = WorkbookFactory.create(options.resource.toInputStream());
            final Sheet sheet = options.getSheet(workbook);
            final int rowStart = options.getDataStartRowIndex();
            final int rowEnd = options.getDataEndRowIndex(sheet);
            final int rowCount = rowEnd - rowStart + 1;
            final Array<String> header = getHeader(sheet, options);
            final int colCount = header.length();
            final int colStart = options.topLeft != null ? options.topLeft.colIndex : 0;
            final Index<Integer> rowKeys = Index.of(Integer.class, rowCount);
            final List<ArrayBuilder<Object>> valueList = createValues(rowCount, colCount);
            final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int i = rowStart; i<=rowEnd; ++i) {
                final Row row = sheet.getRow(i);
                if (row != null) {
                    if (options.rowPredicate == null || options.rowPredicate.test(row)) {
                        final int rowNum = row.getRowNum();
                        rowKeys.add(rowNum);
                        for (int j=0; j<header.length(); ++j) {
                            final int colIndex = colStart + j;
                            final ArrayBuilder<Object> values = valueList.get(j);
                            final Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                            final Object value = getCellValue(cell, evaluator);
                            values.append(value);
                        }
                    }
                }
            }
            return DataFrame.of(rowKeys, String.class, columns -> {
                for (int j=0; j<colCount; ++j) {
                    final String name = header.getValue(j);
                    final Array<Object> values = valueList.get(j).toArray();
                    columns.add(name, values);
                }

            });
        } catch (Exception ex) {
            throw new DataFrameException("Failed to initialize DataFrame from Excel resource", ex);
        } finally {
            IO.close(workbook);
        }
    }


    /**
     * Returns the header array based extracted from the sheet
     * @param sheet     the sheet reference
     * @param options   the options
     * @return          the array of header values
     */
    private Array<String> getHeader(Sheet sheet, Options options) {
        final Coordinate topLeft = options.topLeft;
        final Coordinate bottomRight = options.bottomRight;
        final Row row = topLeft != null ? sheet.getRow(topLeft.rowIndex) : sheet.getRow(0);
        final int colStart = topLeft != null ? topLeft.colIndex : 0;
        final int colEnd = bottomRight != null ? bottomRight.colIndex : row.getLastCellNum() - 1 ;
        final int colCount = colEnd - colStart + 1;
        if (!options.header) {
            return Range.of(0, colCount).map(i -> "Column-" + i).toArray();
        } else {
            final Array<String> header = Array.of(String.class, colCount);
            final DataFormatter formatter = new DataFormatter();
            for (int j=0; j<colCount; ++j) {
                final int colIndex = colStart + j;
                final Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                final String value = cell == null ? "Column-" + j : formatter.formatCellValue(cell);
                header.setValue(j, value);
            }
            return header;
        }
    }


    /**
     * Returns a list of column array builders to capture data from sheet
     * @param rowCount      the initial row count for builder
     * @param colCount      the column count for DataFrame
     * @return              the list of array builders
     */
    private List<ArrayBuilder<Object>> createValues(int rowCount, int colCount) {
        return IntStream.range(0, colCount).mapToObj(i -> ArrayBuilder.of(rowCount)).collect(Collectors.toList());
    }


    /**
     * Returns a typed value from the cell specified
     * @param cell      the cell reference
     * @param evaluator the formula evaluator
     * @return          the cell value
     */
    private Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            return cell.getRichStringCellValue().getString();
        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            return getCellValue(evaluator.evaluateInCell(cell), evaluator);
        } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            return null;
        } else {
            return null;
        }
    }


    /**
     * Defines a zero-based row and column coordinate in an Excel sheet
     */
    @lombok.AllArgsConstructor()
    public static class Coordinate {
        private final int rowIndex;
        private final int colIndex;
    }


    /**
     * The options for this source
     */
    public static class Options {

        /** The resource to load from */
        @lombok.Getter
        private Resource resource;
        /** True indicates that the first row serves as a header for frame */
        @lombok.Getter @lombok.Setter
        private boolean header;
        /** The optional worksheet name, otherwise the first sheet is extracted */
        @lombok.Getter @lombok.Setter
        private String sheetName;
        /** The optional top right coordinate (zero-based) that defines top left corner cell for table to parse  */
        @lombok.Getter @lombok.Setter
        private Coordinate topLeft;
        /** The optional top right coordinate (zero-based) that defines bottom right corner cell for table to parse  */
        @lombok.Getter @lombok.Setter
        private Coordinate bottomRight;
        /** The optional row predicate to includes rows */
        @lombok.Getter @lombok.Setter
        private Predicate<Row> rowPredicate;


        /**
         * Validates that these options are complete
         */
        public void validate() {
            Objects.requireNonNull(resource, "The resource cannot be null");
        }

        /**
         * Returns the worksheet to parse
         * @param workbook  the workbook reference
         * @return          the worksheet
         */
        private Sheet getSheet(Workbook workbook) {
            if (sheetName == null) {
                return workbook.getSheetAt(0);
            } else {
                final Sheet sheet = workbook.getSheet(sheetName);
                Objects.requireNonNull(sheet, "No worksheet found with name: " + sheetName);
                return sheet;
            }
        }

        /**
         * Sets the file to load from
         * @param file  the file reference
         */
        public void setFile(File file) {
            this.resource = Resource.of(file);
        }

        /**
         * Sets the URL to load from
         * @param url   the url reference
         */
        public void setURL(URL url) {
            this.resource = Resource.of(url);
        }

        /**
         * Sets the input stream to load from
         * @param stream    the input stream
         */
        public void setInputStream(InputStream stream) {
            this.resource = Resource.of(stream);
        }

        /**
         * Sets the resource string to load from
         * @param resource  the resource string
         */
        public void setResource(String resource) {
            this.resource = Resource.of(resource);
        }

        /**
         * Returns the row index of the first row containing data
         * @return  the row index of first row with data
         */
        private int getDataStartRowIndex() {
            return header ? topLeft != null ? topLeft.rowIndex + 1 : 1 : topLeft != null ? topLeft.rowIndex : 0;
        }

        /**
         * Returns the row index of the last row containing data
         * @param sheet     the sheet reference
         * @return          the row index of last row with data
         */
        private int getDataEndRowIndex(Sheet sheet) {
            return bottomRight != null ? bottomRight.rowIndex : sheet.getLastRowNum();
        }
    }
}
