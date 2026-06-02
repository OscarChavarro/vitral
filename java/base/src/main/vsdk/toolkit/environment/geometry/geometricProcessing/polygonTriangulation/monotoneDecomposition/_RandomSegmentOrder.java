package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import java.util.ArrayList;

public final class _RandomSegmentOrder {
    private static int nextPermutationIndex;
    private static ArrayList<Integer> segmentPermutation = new ArrayList<>();
    private static long seed = 1L;

    private _RandomSegmentOrder() {}

    private static double randomUnit() {
        // Park-Miller "minimal standard" rand() sequence, matching the C++ runtime here.
        seed = (16807L * seed) % 2147483647L;
        return (double) seed / (double) 2147483647L;
    }

    public static int generateRandomOrdering(int n) {
        nextPermutationIndex = 1;
        segmentPermutation.clear();
        for (int i = 0; i <= n; i++) segmentPermutation.add(0);

        ArrayList<Integer> localPermutation = new ArrayList<>();
        for (int i = 0; i <= n; i++) localPermutation.add(i);

        int pBase = 0;
        for (int i = 1; i <= n; i++, pBase++) {
            int m = ((int)Math.floor(randomUnit() * 32000.0)) % (n + 1 - i) + 1;
            segmentPermutation.set(i, localPermutation.get(pBase + m));
            if (m != 1) {
                localPermutation.set(pBase + m, localPermutation.get(pBase + 1));
            }
        }
        return 0;
    }

    public static int chooseSegment() {
        return segmentPermutation.get(nextPermutationIndex++);
    }
}
