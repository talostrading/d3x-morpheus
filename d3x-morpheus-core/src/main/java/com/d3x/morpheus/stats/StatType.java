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
package com.d3x.morpheus.stats;

import java.util.List;

import com.d3x.morpheus.frame.DataFrameException;

/**
 * An enum that defines various statistic types
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public enum StatType {

    SUM,
    MIN,
    MAX,
    MAD,
    SEM,
    MEAN,
    COUNT,
    MEDIAN,
    MEDIAN_ABS_DEV,
    PRODUCT,
    PERCENTILE,
    SUM_ABS,
    SUM_LOGS,
    SUM_SQUARES,
    STD_DEV,
    VARIANCE,
    KURTOSIS,
    SKEWNESS,
    MEAN_ABS,
    GEO_MEAN,
    COVARIANCE,
    CORRELATION,
    AUTO_CORREL;


    private static final List<StatType> univariate = List.of(
            SUM, MIN, MAX, MAD, SEM, MEAN, COUNT, MEDIAN, MEDIAN_ABS_DEV, PERCENTILE, SUM_ABS, SUM_LOGS,
            SUM_SQUARES, STD_DEV, VARIANCE, KURTOSIS, SKEWNESS, MEAN_ABS, GEO_MEAN, AUTO_CORREL);


    /**
     * Returns the universe of univariate stat types supported
     * @return  the universe of univariate stat types
     */
    public static List<StatType> univariate() {
        return univariate;
    }


    /**
     * Returns the stat value for this type
     * @param stats     the stats entity
     * @return          the stat value
     */
    public double apply(Stats<Double> stats) {
        switch (this) {
            case MIN:               return stats.min();
            case MAX:               return stats.max();
            case MEAN:              return stats.mean();
            case MEDIAN:            return stats.median();
            case MEDIAN_ABS_DEV:    return stats.medianAbsDev();
            case MAD:               return stats.mad();
            case SUM:               return stats.sum();
            case SUM_ABS:           return stats.sumAbs();
            case SUM_LOGS:          return stats.sumLogs();
            case SEM:               return stats.sem();
            case STD_DEV:           return stats.stdDev();
            case VARIANCE:          return stats.variance();
            case KURTOSIS:          return stats.kurtosis();
            case SKEWNESS:          return stats.skew();
            case MEAN_ABS:          return stats.meanAbs();
            case GEO_MEAN:          return stats.geoMean();
            case COUNT:             return stats.count();
            case PRODUCT:           return stats.product();
            case SUM_SQUARES:       return stats.sumSquares();
            case PERCENTILE:        return stats.percentile(0.5d);
            case AUTO_CORREL:       return stats.autocorr(1);
            default:    throw new DataFrameException("Unsupported stat type: " + this.name());
        }
    }
}
