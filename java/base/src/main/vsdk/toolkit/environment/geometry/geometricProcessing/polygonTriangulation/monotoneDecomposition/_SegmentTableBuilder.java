package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

public final class _SegmentTableBuilder {
    private _SegmentTableBuilder() {}

    public static int prepareSegments(double[] vertices, int numberOfVertices, int[] contourSizes, int numberOfContours) {
        _Construct.prepareStorage(numberOfVertices);

        int i = 1;
        for (int contourCount = 0; contourCount < numberOfContours; contourCount++) {
            int numPoints = contourSizes[contourCount];
            int first = i;
            int last = first + numPoints - 1;
            for (int j = 0; j < numPoints; j++, i++) {
                _Construct.segmentAt(i).startPoint.x = vertices[2 * (i - 1)];
                _Construct.segmentAt(i).startPoint.y = vertices[2 * (i - 1) + 1];
                if (i == last) {
                    _Construct.segmentAt(i).nextSegmentIndex = first;
                    _Construct.segmentAt(i).previousSegmentIndex = i - 1;
                    _Construct.segmentAt(i - 1).endPoint.set(_Construct.segmentAt(i).startPoint);
                }
                else if (i == first) {
                    _Construct.segmentAt(i).nextSegmentIndex = i + 1;
                    _Construct.segmentAt(i).previousSegmentIndex = last;
                    _Construct.segmentAt(last).endPoint.set(_Construct.segmentAt(i).startPoint);
                }
                else {
                    _Construct.segmentAt(i).previousSegmentIndex = i - 1;
                    _Construct.segmentAt(i).nextSegmentIndex = i + 1;
                    _Construct.segmentAt(i - 1).endPoint.set(_Construct.segmentAt(i).startPoint);
                }
                _Construct.segmentAt(i).hasBeenInserted = false;
            }
        }
        return i - 1;
    }
}
