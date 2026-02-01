import java.util.*;

/**
 * Product Shipment Solution
 * 
 * Problem: Given a catalog of products with shipping dependencies (products that must be
 * shipped together), determine which products must be shipped together using an
 * undirected graph approach.
 * 
 * This is essentially finding connected components in an undirected graph.
 */
public class ProductShipment {

    /**
     * Finds groups of products that must be shipped together.
     * 
     * @param n            the total number of products (0 to n-1)
     * @param dependencies array where each pair [i, i+1] represents products that must ship together
     * @return list of product groups, where each group contains products that must be shipped together
     */
    public List<List<Integer>> getProductShipmentList(int n, int[][] dependencies) {
        List<List<Integer>> graph = createGraph(n, dependencies);
        boolean[] visited = new boolean[n];
        List<List<Integer>> result = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            if (!visited[i]) {
                List<Integer> component = new ArrayList<>();
                // dfs(i, graph, visited, component);
                bfs(i, graph, visited, component);
                result.add(component);
            }
        }
        return result;
    }

    private void dfs(int node, List<List<Integer>> graph, boolean[] visited, List<Integer> component) {
        visited[node] = true;
        component.add(node);
        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfs(neighbor, graph, visited, component);
            }
        }
    }

    private void bfs(int start, List<List<Integer>> graph, boolean[] visited, List<Integer> component) {
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        visited[start] = true;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            component.add(node);
            for (int neighbor : graph.get(node)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.add(neighbor);
                }
            }
        }
    }

    private List<List<Integer>> createGraph(int n, int[][] dependencies) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int[] dependency: dependencies) {
            int u = dependency[0];
            int v = dependency[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }
        return graph;
    }
}
