package se.vti.roundtrips.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import se.vti.roundtrips.single.RoundTrip;

class TestRoundTripUtils {

    private RoundTrip<Node> createRoundTrip(Node... nodes) {
        List<Node> nodeList = Arrays.asList(nodes);
        List<Integer> departures = Arrays.asList(new Integer[nodeList.size()]);
        return new RoundTrip<>(0, nodeList, departures);
    }

    @Test
    void testFindIndices_singleOccurrence() {
        Node a = new Node("A");
        RoundTrip<Node> rt = createRoundTrip(a, new Node("B"), new Node("C"));
        int[] indices = RoundTripUtils.findIndices(a, rt);
        assertArrayEquals(new int[]{0}, indices);
    }

    @Test
    void testFindIndices_multipleOccurrences() {
        Node a = new Node("A");
        RoundTrip<Node> rt = createRoundTrip(a, new Node("B"), a, new Node("C"));
        int[] indices = RoundTripUtils.findIndices(a, rt);
        assertArrayEquals(new int[]{0, 2}, indices);
    }

    @Test
    void testShortestPaths_emptyRoundTrip() {
        RoundTrip<Node> rt = createRoundTrip();
        List<int[]> paths = RoundTripUtils.shortestPaths(new Node("A"), new Node("B"), rt);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testShortestPaths_nodesNotFound() {
        RoundTrip<Node> rt = createRoundTrip(new Node("X"), new Node("Y"));
        List<int[]> paths = RoundTripUtils.shortestPaths(new Node("A"), new Node("B"), rt);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testShortestPaths_sameNode() {
        Node a = new Node("A");
        RoundTrip<Node> rt = createRoundTrip(a, new Node("B"), a);
        List<int[]> paths = RoundTripUtils.shortestPaths(a, a, rt);
        assertEquals(2, paths.size());
        assertArrayEquals(new int[]{0}, paths.get(0));
        assertArrayEquals(new int[]{2}, paths.get(1));
    }

    @Test
    void testShortestPaths_simpleCase() {
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        RoundTrip<Node> rt = createRoundTrip(a, b, c);
        List<int[]> paths = RoundTripUtils.shortestPaths(a, c, rt);
        assertEquals(1, paths.size());
        assertArrayEquals(new int[]{0, 1, 2}, paths.get(0));
    }

    @Test
    void testShortestPaths_wrapAround() {
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        RoundTrip<Node> rt = createRoundTrip(b, c, a);
        List<int[]> paths = RoundTripUtils.shortestPaths(a, b, rt);
        assertEquals(1, paths.size());
        assertArrayEquals(new int[]{2, 0}, paths.get(0));
    }

    @Test
    void testShortestPaths_multipleShortestPaths() {
        Node a = new Node("A");
        Node b = new Node("B");
        RoundTrip<Node> rt = createRoundTrip(a, b, a, b);
        List<int[]> paths = RoundTripUtils.shortestPaths(a, b, rt);
        assertEquals(2, paths.size());
        assertArrayEquals(new int[]{0, 1}, paths.get(0));
        assertArrayEquals(new int[]{2, 3}, paths.get(1));
    }
}