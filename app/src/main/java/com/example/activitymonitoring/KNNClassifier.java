package com.example.activitymonitoring;


import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class KNNClassifier {
    List<double[]> neighbors;
    private int k;
    private int features;



    public void setFeatures(int features) {
        this.features = features;
    }

    public KNNClassifier(int k, int features, List<double[]> neighbors ) {

        this.k = k;
        this.features = features;
        this.neighbors = neighbors;

    }

    public void setK(int k) {
        this.k = k;
    }

//    private void createNeighborMatrix() {
//
//        List<double[]> rowList = new ArrayList<double[]>();
//
//
//        try (
//
//
//                BufferedReader br = new BufferedReader(new InputStreamReader(inpustream))) {
//
//            String line;
//            while ((line = br.readLine()) != null) {
//                String[] lineItemsStrings = line.split(",");
//                double[] lineItems = new double[lineItemsStrings.length];
//                for (int i = 0; i < lineItemsStrings.length; i++) {
//                    lineItems[i] = Double.parseDouble(lineItemsStrings[i]);
//                }
//                rowList.add(lineItems);
//            }
//        } catch (Exception e) {
//            System.out.println("Could not create neighbors:    "+e.toString());
//        }
//
//        neighbors = new double[rowList.size()][features];
//
//        for (int i = 0; i < rowList.size(); i++) {
//            double[] row = rowList.get(i);
//            neighbors[i] = row;
//        }
//    }

    private double[][] createDistanceMatrix(double[] vector) {

        double[][] distanceMatrix = new double[neighbors.size()][features];

        for (int i = 0; i < neighbors.size(); i++) {
            if(neighbors.get(i).length < 10) {
                continue;
            }
            distanceMatrix[i] = calcManhattan(vector, neighbors.get(i));
        }

        return distanceMatrix;

    }

    private double[] calcManhattan(double[] vector, double[] neighbor) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += Math.abs(vector[i] - neighbor[i]);
        }
        double[] result = {sum, neighbor[neighbor.length - 1]};

        return result;
    }

    private double[] calcEuclid(double[] vector, double[] neighbor) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += Math.abs(vector[i] - neighbor[i]);
        }

        double[] result = {sqrt(sum), neighbor[neighbor.length - 1]};

        return result;
    }



    public int classify(double[] vector) {
        double[][] distances = createDistanceMatrix(vector);

        Arrays.sort(distances, Comparator.comparingDouble(a -> a[0]));

        int[] kNeighbors = new int[k];

        for (int i = 0; i < kNeighbors.length; i++) {
            kNeighbors[i] = (int) distances[i][1];
        }

        return mostFrequent(kNeighbors);
    }

    private int mostFrequent(int arr[]) {

        Arrays.sort(arr);

        // find the max frequency using linear traversal
        int max_count = 1, res = arr[0];
        int curr_count = 1;

        for (int i = 1; i < arr.length; i++) {
            if (arr[i] == arr[i - 1])
                curr_count++;
            else {
                if (curr_count > max_count) {
                    max_count = curr_count;
                    res = arr[i - 1];
                }
                curr_count = 1;
            }
        }

        // If last element is most frequent
        if (curr_count > max_count) {
            max_count = curr_count;
            res = arr[arr.length - 1];
        }

        return res;
    }
}
