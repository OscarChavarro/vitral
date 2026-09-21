package vsdk.toolkit.gui.viewport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
Exercises the translation of pixel coordinates between a `ViewportSet` and
its viewports.
 */
class ViewportSetCoordinatesTest
{
    @Test
    void given_everyViewportOfTheStandardSet_when_convertingBackAndForth_then_coordinatesAreKept()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");

        set.resize(1600, 900);

        for ( Viewport viewport : set.getViewports() ) {
            for ( int x : new int[] {-20, 0, 37, 400} ) {
                for ( int y : new int[] {-20, 0, 55, 300} ) {
                    // Act
                    int setX = set.toSetX(viewport, x);
                    int setY = set.toSetY(viewport, y);

                    // Assert
                    assertThat(set.toViewportX(viewport, setX)).isEqualTo(x);
                    assertThat(set.toViewportY(viewport, setY)).isEqualTo(y);
                }
            }
        }
    }

    @Test
    void given_aViewport_when_convertingItsUpperLeftCorner_then_itIsInsideTheViewportArea()
    {
        // Arrange
        ViewportSet set = ViewportSet.createStandardSet("test");
        set.resize(1600, 900);
        Viewport viewport = set.getViewport(1);

        // Act
        Viewport found = set.findViewportAt(
            set.toSetX(viewport, 1), set.toSetY(viewport, 1));

        // Assert
        assertThat(found).isSameAs(viewport);
    }
}
