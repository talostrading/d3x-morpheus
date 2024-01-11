/*
 * Copyright (C) 2018-2019 D3X Systems - All Rights Reserved
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
package com.d3x.morpheus.series;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.d3x.morpheus.vector.D3xVector;

import com.d3x.morpheus.vector.DataVectorView;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for data series
 *
 * @author Xavier Witdouck
 */
public class DoubleSeriesTests {
    private static final double TOLERANCE = 1.0E-12;

    private IntFunction ofIntFunction(IntFunction function) {
        return function;
    }


    @DataProvider(name="types")
    public Object[][] types() {
        var now = LocalDate.now();
        return new Object[][] {
            { Integer.class, ofIntFunction(i -> i) },
            { LocalDate.class, ofIntFunction(now::plusDays)},
            { String.class, ofIntFunction(i -> "X" + i) }
        };
    }


    @Test()
    public void csvRead() {
        var path = "/csv/aapl.csv";
        var series = DoubleSeries.<LocalDate>read(path).csv("Date", "Adj Close");
        Assert.assertEquals(series.valueClass(), Double.class);
        Assert.assertEquals(series.keyClass(), LocalDate.class);
        Assert.assertEquals(series.size(), 8503);
        Assert.assertEquals(series.firstKey().orElse(null), LocalDate.parse("1980-12-12"));
        Assert.assertEquals(series.lastKey().orElse(null), LocalDate.parse("2014-08-29"));
        Assert.assertEquals(series.getDouble(LocalDate.parse("1980-12-12")), 0.44203d, 0.000001d);
        Assert.assertEquals(series.getDouble(LocalDate.parse("2014-08-29")), 101.65627d, 0.000001d);
        Assert.assertEquals(series.stats().min(), 0.16913d, 0.000001d);
        Assert.assertEquals(series.stats().max(), 101.65627d, 0.000001d);
    }



    @Test()
    public void mapKeys() {
        var path = "/csv/aapl.csv";
        var series = DoubleSeries.<LocalDate>read(path).csv("Date", "Adj Close");
        var result = series.mapKeys(LocalDate.class, v -> v.minusDays(1));
        Assert.assertEquals(result.size(), series.size());
        Assert.assertEquals(result.valueClass(), series.valueClass());
        Assert.assertEquals(result.keyClass(), series.keyClass());
        for (int i=0; i<series.size(); ++i) {
            var mapped = result.getKey(i);
            var expected = series.getKey(i).minusDays(1);
            Assert.assertEquals(mapped, expected);
            Assert.assertEquals(result.getDoubleAt(i), series.getDoubleAt(i), 0.000001d);
        }
    }


    @Test()
    public void filterKeys() {
        var path = "/csv/aapl.csv";
        var series = DoubleSeries.<LocalDate>read(path).csv("Date", "Adj Close");
        var result1 = series.filter(v -> v.getDayOfWeek() == DayOfWeek.MONDAY);
        Assert.assertTrue(result1.size() > 0);
        Assert.assertTrue(result1.size() < series.size());
        Assert.assertEquals(result1.valueClass(), series.valueClass());
        Assert.assertEquals(result1.keyClass(), series.keyClass());
        result1.keys().forEach(key -> {
            Assert.assertEquals(key.getDayOfWeek(), DayOfWeek.MONDAY);
            Assert.assertEquals(result1.getDouble(key), series.getDouble(key));
        });
    }

    @Test
    public void filterValues() {
        var series1 = DoubleSeries.build(String.class, List.of("A", "B", "C"), List.of(1.0, Double.NaN, 88.8));
        var expected1 = DoubleSeries.build(String.class, List.of("A", "C"), List.of(1.0, 88.8));
        var expected2 = DoubleSeries.build(String.class, List.of("B", "C"), List.of(Double.NaN, 88.8));

        Assert.assertTrue(series1.filterValues(Double::isFinite).equalsSeries(expected1));
        Assert.assertTrue(series1.filterValues(x -> x > 10.0).equalsSeries(expected2));
    }

    @Test
    public void nonZeros() {
        var original = DoubleSeries.build(String.class, List.of("A", "B", "C", "D"), List.of(0.0, 1.0, Double.NaN, 88.8));
        var expected = DoubleSeries.build(String.class, List.of("B", "D"), List.of(1.0, 88.8));
        Assert.assertTrue(original.nonZeros().equalsSeries(expected));
    }

    @Test()
    public void sorting1() {
        var path = "/csv/aapl.csv";
        var series = DoubleSeries.<LocalDate>read(path).csv("Date", "Adj Close");
        var sorted = series.copy();
        sorted.sort((i1, i2) -> {
            var v1 = sorted.getDoubleAt(i1);
            var v2 = sorted.getDoubleAt(i2);
            return Double.compare(v1, v2);
        });
        Assert.assertTrue(sorted.size() > 0);
        Assert.assertEquals(sorted.size(),  series.size());
        Assert.assertNotEquals(sorted.firstKey(), series.firstKey());
        Assert.assertEquals(sorted.lastKey(), series.lastKey());
        Assert.assertEquals(series.firstKey().orElse(null), LocalDate.parse("1980-12-12"));
        Assert.assertEquals(series.lastKey().orElse(null), LocalDate.parse("2014-08-29"));
        Assert.assertEquals(sorted.firstKey().orElse(null), LocalDate.parse("1982-07-08"));
        Assert.assertEquals(sorted.lastKey().orElse(null), LocalDate.parse("2014-08-29"));
        DoubleSeries.assertAscending(sorted);
    }


    @Test(dataProvider="types")
    public <K> void sorting(Class<K> keyType, IntFunction<K> keyGen) {
        var builder = DoubleSeries.builder(keyType).capacity(100);
        IntStream.range(0, 1000).forEach(i -> builder.putDouble(keyGen.apply(i), Math.random() * 100d));
        var series = builder.build();
        var sorted = series.copy();
        sorted.sort((i1, i2) -> {
            var v1 = sorted.getDoubleAt(i1);
            var v2 = sorted.getDoubleAt(i2);
            return Double.compare(v1, v2);
        });
        Assert.assertTrue(sorted.size() > 0);
        Assert.assertEquals(sorted.size(), series.size());
        Assert.assertNotEquals(sorted.firstKey(), series.firstKey());
        Assert.assertNotEquals(sorted.lastKey(), series.lastKey());
        DoubleSeries.assertAscending(sorted);
    }

    @Test
    public void testBuildFromLists() {
        List<String> keys = List.of("A", "B", "C", "D");
        List<Double> values = List.of(1.0, 2.0, Double.NaN, 4.0);
        DoubleSeries<String> series = DoubleSeries.build(String.class, keys, values);

        Assert.assertEquals(series.size(), 3);
        Assert.assertEquals(series.getDouble("A"), 1.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(0), 1.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("B"), 2.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(1), 2.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("D"), 4.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(2), 4.0, TOLERANCE);
    }

    @Test
    public void testBuildFromMap() {
        DoubleSeries<String> series = DoubleSeries.build(String.class, Map.of("A", 1.0, "B", 2.0, "C", 3.0));

        Assert.assertEquals(series.size(), 3);
        Assert.assertEquals(series.getDouble("A"), 1.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("B"), 2.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("C"), 3.0, TOLERANCE);
    }

    @Test
    public void testBuildFromVector() {
        List<String> keys = List.of("A", "B", "C", "D");
        D3xVector values = D3xVector.wrap(1.0, 2.0, Double.NaN, 4.0);
        DoubleSeries<String> series = DoubleSeries.build(String.class, keys, values);

        Assert.assertEquals(series.size(), 3);
        Assert.assertEquals(series.getDouble("A"), 1.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(0), 1.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("B"), 2.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(1), 2.0, TOLERANCE);
        Assert.assertEquals(series.getDouble("D"), 4.0, TOLERANCE);
        Assert.assertEquals(series.getDoubleAt(2), 4.0, TOLERANCE);
    }

    @Test
    public void testCopyOf() {
        var map = new HashMap<String, Double>();
        map.put("A", 1.0);
        map.put("B", 2.0);
        map.put("C", 3.0);

        var view = DataVectorView.of(map);
        var actual = DoubleSeries.copyOf(String.class, view);
        var expected = DoubleSeries.build(String.class, Map.of("A", 1.0, "B", 2.0, "C", 3.0));

        Assert.assertTrue(actual.equalsSeries(expected));

        map.put("B", 22.0);
        map.put("D", 44.0);

        Assert.assertTrue(actual.equalsSeries(expected));
    }

    @Test
    public void testPutSeries() {
        DoubleSeries<String> series1 =
                DoubleSeries.build(String.class, List.of("A", "B", "C"), List.of(1.0, 2.0, 3.0));

        DoubleSeries<String> series2 =
                DoubleSeries.build(String.class, List.of("D", "E"), List.of(4.0, 5.0));

        DoubleSeries<String> series3 =
                DoubleSeries.build(String.class, List.of("A", "B", "C", "D", "E"), List.of(1.0, 2.0, 3.0, 4.0, 5.0));

        DoubleSeries<String> series4 =
                DoubleSeries.builder(String.class)
                .putSeries(series1)
                .putSeries(series2)
                .build();

        Assert.assertTrue(series3.equalsSeries(series4));
    }

    @Test()
    public void mapWithCollisions() {
        var keyMap = Map.of(1, "A", 2, "B", 3, "C", 4, "A");
        var original = DoubleSeries.of(Integer.class, keyMap.keySet().stream(), v -> Math.random());
        var mapped = original.mapKeys(String.class, keyMap::get);
        Assert.assertEquals(original.length(), 4);
        Assert.assertEquals(mapped.length(), 3);
        Assert.assertEquals(mapped.getDouble("A"), original.getDouble(1) + original.getDouble(4));
        Assert.assertEquals(mapped.getDouble("B"), original.getDouble(2));
        Assert.assertEquals(mapped.getDouble("C"), original.getDouble(3));
    }


}
