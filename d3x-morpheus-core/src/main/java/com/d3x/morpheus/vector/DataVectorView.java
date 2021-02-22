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
package com.d3x.morpheus.vector;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import com.d3x.morpheus.util.DoubleComparator;

/**
 * Provides a read-only view of {@code double} values that are accessed
 * by a fixed set of keys.
 *
 * @param <K> the runtime key type.
 *
 * @author Scott Shaffer
 */
public interface DataVectorView<K> {
    /**
     * Determines whether this view contains a particular element.
     *
     * @param key the key of interest.
     *
     * @return {@code true} iff this view contains an element indexed
     * by the specified key.
     */
    boolean containsElement(K key);

    /**
     * Extracts an element from this view.
     *
     * @param elementKey   the key of the desired element.
     * @param defaultValue a default value to return if this view
     *                     does not contain the specified element.
     *
     * @return the value of the element indexed by the specified
     * key, or the default value if the key is not present.
     */
    double getElement(K elementKey, double defaultValue);

    /**
     * Returns the number of elements in this view.
     *
     * @return the number of elements in this view.
     */
    int length();

    /**
     * Returns a stream containing the keys in this view.
     *
     * @return a stream containing the keys in this view.
     */
    Stream<K> streamKeys();

    /**
     * Returns a stream containing the keys in this view.
     *
     * @return a stream containing the keys in this view.
     */
    DoubleStream streamValues();

    /**
     * Returns a DataVectorView of a Double map; changes to the
     * underlying map will be reflected in the returned vector.
     *
     * @param map the underlying Double map.
     *
     * @return a DataVectorView of the specified Double map.
     */
    static <K> DataVectorView<K> of(Map<K, Double> map) {
        return new MapDataVector<>(map);
    }

    /**
     * Returns a read-only set view of the keys in this view.
     *
     * @return a read-only set view of the keys in this view.
     */
    default Set<K> collectKeys() {
        return streamKeys().collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns a read-only iterator over this vector view.
     * @return a read-only iterator over this vector view.
     */
    default PrimitiveIterator.OfDouble doubleIterator() {
        return new DataVectorIterator<>(this);
    }

    /**
     * Determines whether the keys and values in this view are equal to those in
     * another view <em>within the tolerance of the default DoubleComparator</em>.
     *
     * @param that the view to test for equality.
     *
     * @return {@code true} iff the input view has the same keys as this view and
     * each value matches the corresponding entry in this view within the tolerance
     * of the default DoubleComparator.
     */
    default boolean equalsView(DataVectorView<K> that) {
        return equalsView(that, DoubleComparator.DEFAULT);
    }

    /**
     * Determines whether the keys and values in this view are equal to those in
     * another view within the tolerance of a given DoubleComparator.
     *
     * @param that       the vector to test for equality.
     * @param comparator the element comparator.
     *
     * @return {@code true} iff the input view has the same keys as this view and
     * each value matches the corresponding entry in this view within the tolerance
     * of the specified comparator.
     */
    default boolean equalsView(DataVectorView<K> that, DoubleComparator comparator) {
        if (!this.collectKeys().equals(that.collectKeys()))
            return false;

        for (K key : collectKeys())
            if (!comparator.equals(this.getElement(key), that.getElement(key)))
                return false;

        return true;
    }

    /**
     * Extracts an element from this view.
     *
     * @param key the key of the desired element.
     *
     * @return the value of the element indexed by the specified
     * key, or {@code Double.NaN} if the key is not present.
     */
    default double getElement(K key) {
        return getElement(key, Double.NaN);
    }

    /**
     * Extracts elements from this view.
     *
     * @param elementKeys the keys of the desired elements.
     * @param defaultValue a default value to return if this view
     *                     does not contain the specified element.
     *
     * @return the value of the element indexed by the specified
     * key, or {@code Double.NaN} if the key is not present.
     */
    default D3xVector getElements(List<K> elementKeys, double defaultValue) {
        D3xVector vector = D3xVector.dense(length());

        for (int index = 0; index < elementKeys.size(); ++index)
            vector.set(index, getElement(elementKeys.get(index), defaultValue));

        return vector;
    }

    /**
     * Computes the inner (dot) product between this data vector and
     * another vector; missing values (elements not contained in both
     * vectors) are replaced with zero.
     *
     * @param that the other vector in the inner product.
     *
     * @return the inner (dot) product between this data vector and
     * the input vector.
     */
    default double innerProduct(DataVectorView<K> that) {
        return InnerProduct.compute(this, that);
    }

    /**
     * Computes the weighted inner product between this data vector and
     * another vector; missing values (elements not contained in both
     * vectors and the weight vector) are replaced with zero.
     *
     * @param operand the other vector operand in the inner product.
     * @param weights a vector of weights to apply to each term in the
     *                inner product.
     *
     * @return the weighted inner product between this data vector and
     * the input vector.
     */
    default double innerProduct(DataVectorView<K> operand, DataVectorView<K> weights) {
        return InnerProduct.compute(this, operand, weights);
    }
}