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
package com.zavtech.morpheus.frame;

import java.util.Optional;
import java.util.function.Function;

/**
 * An interface to a Principal Component Analysis model over column data in a DataFrame
 *
 * @param <R>   the row key type
 * @param <C>   the column key type
 *
 * <p>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></p>
 *
 * @author  Xavier Witdouck
 */
public interface DataFramePCA<R,C> {

    enum Solver {
        SVD,
        EVD_COV,
        EVD_COR
    }

    enum Field  {
        EIGENVALUE,
        VAR_PERCENT,
        VAR_PERCENT_CUM
    }


    /**
     * Performs Principal Component Analysis on a DataFrame assuming the column represent the number of measurements, and the rows the sample size
     * The default PCA Solver is to use Singular Value Decomposition on the nxm data matrix
     * @param demean        true if the columns should be demeaned before apply PCA
     * @param handler       the function to consume the resulting model
     * @param <T>           the type of the object returned by the handler
     * @return              the optional result generated by the handler
     */
    <T> Optional<T> apply(boolean demean, Function<Model<R,C>,Optional<T>> handler);

    /**
     * Performs Principal Component Analysis on a DataFrame assuming the column represent the number of measurements, and the rows the sample size
     * @param demean        true if the columns should be demeaned before apply PCA
     * @param solver        the solver type to use when performing PCA
     * @param handler       the function to consume the resulting model
     * @param <T>           the type of the object returned by the handler
     * @return              the optional result generated by the handler
     */
    <T> Optional<T> apply(boolean demean, Solver solver, Function<Model<R,C>,Optional<T>> handler);


    /**
     * An interface to a Principal Component Analysis model generated from a DataFrame dataset
     * @param <R>   the row key type
     * @param <C>   the column key type
     */
    interface Model<R,C> {

        /**
         * Returns an mx1 DataFrame of Eigen Values ordered from largest to smallest
         * @return  the mx1 DataFrame of sorted Eigen Values
         */
        DataFrame<Integer,Field> getEigenValues();

        /**
         * Returns a DataFrame of Eigenvectors where each column represents an Eigenvector
         * @return  the DataFrame of Eigenvectors arranged in columns and ordered as per the Eigenvalues.
         */
        DataFrame<Integer,Integer> getEigenVectors();

        /**
         * Returns the scores for this PCA model based on all components
         * @return   the resulting scores
         */
        DataFrame<R,Integer> getScores();

        /**
         * Returns the original data projected back to the original basis using the specified number of components
         * @param numComponents     the number of principal components to include in projection
         * @return                  the data with the same dimensions as the original, but projected on a subset of components
         */
        DataFrame<R,C> getProjection(int numComponents);

        /**
         * Returns the scores for this PCA model based on the number of components specified
         * @param numComponents     the number of principal components to include in score generation
         * @return                  the resulting scores
         */
        DataFrame<R,Integer> getScores(int numComponents);

    }

}
