import { _Construct } from "./_Construct.js";
export class _SegmentTableBuilder {
    private constructor() {}
    public static prepareSegments(
        vertices: ArrayLike<number>,
        count: number,
        sizes: ArrayLike<number>,
        contours: number,
    ): number {
        _Construct.prepareStorage(count);
        let index = 1;
        for (let contour = 0; contour < contours; contour++) {
            const first = index,
                last = first + sizes[contour]! - 1;
            for (; index <= last; index++) {
                const segment = _Construct.segmentAt(index);
                segment.startPoint.x = vertices[2 * (index - 1)]!;
                segment.startPoint.y = vertices[2 * (index - 1) + 1]!;
                segment.nextSegmentIndex = index === last ? first : index + 1;
                segment.previousSegmentIndex = index === first ? last : index - 1;
                _Construct.segmentAt(index === first ? last : index - 1).endPoint.set(segment.startPoint);
                segment.hasBeenInserted = false;
            }
        }
        return index - 1;
    }
}
