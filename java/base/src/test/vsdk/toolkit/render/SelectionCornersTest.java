package vsdk.toolkit.render;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the geometry of the selection corners, independently of any
rendering technology.
 */
class SelectionCornersTest
{
    private static final double EPS = 1.0e-9;
    private static final double[] BOX = {-1, -2, -3, 3, 2, 1};

    @Test
    void hasThreeLinesOnEachOfTheEightCorners()
    {
        Vector3Dd[] segments = SelectionCorners.buildSegments(BOX);

        assertThat(segments).hasSize(SelectionCorners.NUMBER_OF_CORNERS
            * SelectionCorners.LINES_PER_CORNER * 2);
    }

    @Test
    void missingOrIncompleteBoxGivesNoSegments()
    {
        assertThat(SelectionCorners.buildSegments(null)).isEmpty();
        assertThat(SelectionCorners.buildSegments(new double[] {0, 0, 0})).isEmpty();
    }

    @Test
    void firstCornerIsAtTheEnlargedMinimumAndGoesInside()
    {
        double border = SelectionCorners.DEFAULT_BORDER_FRACTION;
        double line = SelectionCorners.DEFAULT_LINE_FRACTION;
        Vector3Dd[] segments = SelectionCorners.buildSegments(BOX);
        // Size of the box: 4 x 4 x 4
        Vector3Dd corner = new Vector3Dd(-1 - 4 * border, -2 - 4 * border, -3 - 4 * border);

        assertThat(segments[0].x()).isCloseTo(corner.x(), offset(EPS));
        assertThat(segments[0].y()).isCloseTo(corner.y(), offset(EPS));
        assertThat(segments[0].z()).isCloseTo(corner.z(), offset(EPS));
        assertThat(segments[1].x()).isCloseTo(corner.x() + 4 * line, offset(EPS));
        assertThat(segments[3].y()).isCloseTo(corner.y() + 4 * line, offset(EPS));
        assertThat(segments[5].z()).isCloseTo(corner.z() + 4 * line, offset(EPS));
    }

    @Test
    void lastCornerIsAtTheEnlargedMaximumAndGoesInside()
    {
        double border = SelectionCorners.DEFAULT_BORDER_FRACTION;
        double line = SelectionCorners.DEFAULT_LINE_FRACTION;
        Vector3Dd[] segments = SelectionCorners.buildSegments(BOX);
        int last = segments.length - 6;

        assertThat(segments[last].x()).isCloseTo(3 + 4 * border, offset(EPS));
        assertThat(segments[last].y()).isCloseTo(2 + 4 * border, offset(EPS));
        assertThat(segments[last].z()).isCloseTo(1 + 4 * border, offset(EPS));
        assertThat(segments[last + 1].x()).isCloseTo(3 + 4 * border - 4 * line, offset(EPS));
        assertThat(segments[last + 3].y()).isCloseTo(2 + 4 * border - 4 * line, offset(EPS));
        assertThat(segments[last + 5].z()).isCloseTo(1 + 4 * border - 4 * line, offset(EPS));
    }

    @Test
    void everySegmentIsAxisAlignedAndHasTheLineLength()
    {
        double line = SelectionCorners.DEFAULT_LINE_FRACTION;
        Vector3Dd[] segments = SelectionCorners.buildSegments(BOX);

        for ( int i = 0; i < segments.length; i += 2 ) {
            Vector3Dd d = segments[i + 1].subtract(segments[i]);
            int nonZero = 0;

            nonZero += Math.abs(d.x()) > EPS ? 1 : 0;
            nonZero += Math.abs(d.y()) > EPS ? 1 : 0;
            nonZero += Math.abs(d.z()) > EPS ? 1 : 0;
            assertThat(nonZero).isEqualTo(1);
            assertThat(d.length()).isCloseTo(4 * line, offset(EPS));
        }
    }

    @Test
    void defaultColorIsWhiteAndIsACopy()
    {
        assertThat(SelectionCorners.getDefaultColor().r()).isEqualTo(1.0);
        assertThat(SelectionCorners.getDefaultColor()).isNotSameAs(SelectionCorners.getDefaultColor());
    }
}
