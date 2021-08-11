/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.randomcutforest.ERCF;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;

import org.junit.Test;

import com.amazon.randomcutforest.RandomCutForest;
import com.amazon.randomcutforest.config.Precision;

public class ErcfExperimentTests {

    @Test
    public void run_experiment() throws IOException {
        assertEquals(1, Integer.parseInt("1"));
        String inputFilename = "/home/ec2-user/datasets/input.csv";
        String outputFilename = "/home/ec2-user/datasets/output.csv";

        // configuration and data source
        int shingleSize = 8;
        int numberOfTrees = 200;
        int sampleSize = 256;
        Precision precision = Precision.FLOAT_32;

        double[][] data = ErcfExperimentTests.getData(inputFilename);
        int baseDimensions = data[0].length;
        int dimensions = baseDimensions * shingleSize;

        // forest creation
        ExtendedRandomCutForest forest = new ExtendedRandomCutForest(
            RandomCutForest
                .builder()
                .compact(true)
                .dimensions(dimensions)
                .randomSeed(0)
                .numberOfTrees(numberOfTrees)
                .shingleSize(shingleSize)
                .sampleSize(sampleSize)
                .precision(precision),
            0.005
        );

        // run the experiment
        ArrayList<Double> scores = new ArrayList<Double>(data.length);
        ArrayList<Double> grades = new ArrayList<Double>(data.length);
        ArrayList<Integer> predicted = new ArrayList<Integer>(data.length);
        ArrayDeque<Double> current_shingle = new ArrayDeque<Double>(dimensions);

        for (double[] point : data) {

            // (1) build the current shingle from the data stream. if we don't
            // have enough data for a shingle then record zero scores
            for (double datum : point)
                current_shingle.addLast(datum);

            if (current_shingle.size() < dimensions) {
                scores.add(0.0);
                grades.add(0.0);
                predicted.add(0);
                continue;
            }

            if (current_shingle.size() > dimensions) {
                for (int i = 0; i < point.length; ++i)
                    current_shingle.removeFirst();
            }

            double[] shingle = current_shingle.stream().mapToDouble(d -> d).toArray();
            assertEquals(shingle.length, dimensions);

            // (2) score and update with the model
            AnomalyDescriptor result = forest.process(shingle);

            // (3) record results
            scores.add(result.getRcfScore());
            grades.add(result.getAnomalyGrade());
            if (result.getAnomalyGrade() > 0) {
                predicted.add(1);
            } else {
                predicted.add(0);
            }
        }

        ErcfExperimentTests.writeResults(outputFilename, data, scores, grades, predicted);
    }

    /**
     *  Return an array of points given a filename.
     */
    public static double[][] getData(String filename) throws IOException {
        System.out.println("getData()");
        ArrayList<double[]> data = new ArrayList<double[]>();
        int dimension = 0;

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            boolean ignoreHeader = true;

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                if (ignoreHeader) {
                    ignoreHeader = false;
                    continue;
                }
                dimension = columns.length - 1;

                double[] datum = new double[dimension];
                for (int i = 0; i < dimension; i++) {
                    datum[i] = Double.parseDouble(columns[i + 1]);
                }
                data.add(datum);
            }
        }

        double[][] data_array = new double[data.size()][dimension];
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < dimension; j++) {
                data_array[i][j] = data.get(i)[j];
            }
        }
        return data_array;
    }

    /**
     *  Return an array of points given a filename.
     */
    public static void writeResults(
        String filename,
        double[][] data,
        ArrayList<Double> scores,
        ArrayList<Double> grades,
        ArrayList<Integer> predicted
    ) throws IOException {
        File outputFile = new File(filename);
        try (PrintWriter pw = new PrintWriter(outputFile)) {
            // print the header
            pw.print("timestamp,");
            for (int i = 0; i < data[0].length; i++) {
                pw.print("data" + i + ",");
            }
            pw.print("score,grade,predicted");
            pw.println();

            // print the data for each data point
            for (int i = 0; i < data.length; i++) {
                pw.print(i + ",");
                for (int j = 0; j < data[i].length; j++) {
                    pw.print(data[i][j] + ",");
                }
                pw.print(scores.get(i) + ",");
                pw.print(grades.get(i) + ",");
                pw.print(predicted.get(i));
                pw.println();
            }
        }
    }
}
