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
package com.zavtech.morpheus.stats;

/**
 * A Statistic implementation that supports incremental calculation of a sample geometric mean
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public class GeoMean extends SumLogs {

    /**
     * Constructor
     */
    public GeoMean() {
        super();
    }

    @Override
    public double getValue() {
        final long count = super.getN();
        final double sumLogs = super.getValue();
        return Math.exp(sumLogs / count);
    }

    @Override
    public StatType getType() {
        return StatType.GEO_MEAN;
    }
}
