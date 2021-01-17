/*
 * Copyright (C) 2014-2021 D3X Systems - All Rights Reserved
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

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SumTest {
    private static final double TOLERANCE = 1.0E-12;

    @Test
    public void testOf() {
        assertEquals(Sum.of(), 0.0, TOLERANCE);
        assertEquals(Sum.of(1.0), 1.0, TOLERANCE);
        assertEquals(Sum.of(1.0, 2.0, 3.0, 4.0), 10.0, TOLERANCE);

        assertEquals(Sum.of(new double[] { }), 0.0, TOLERANCE);
        assertEquals(Sum.of(new double[] { 1.0 }), 1.0, TOLERANCE);
        assertEquals(Sum.of(new double[] { 1.0, 2.0, 3.0, 4.0 }), 10.0, TOLERANCE);
    }
}