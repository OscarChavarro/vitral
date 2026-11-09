package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.*;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;

class RayCrossingTest {
    @Test void sphereCrossingsAndUnsupportedGeometry() {
        Sphere sphere = new Sphere(1);
        RayCrossingQuery query = new RayCrossingQuery(0, 10);
        assertThat(Sphere.intersectionRoots(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0), 1, query)).containsExactly(1, 2);
        ArrayList<RayCrossing> events = new ArrayList<>();
        Ray ray = new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
        assertThat(sphere.emitRayCrossings(ray, query, events::add)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).kind).isEqualTo(RayCrossingKind.Entering);
        assertThat(events.get(1).kind).isEqualTo(RayCrossingKind.Leaving);
        assertThat(events.get(0).geometry).isSameAs(sphere);
        events.clear();
        sphere.emitRayCrossings(new Ray(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0)), query, events::add);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).kind).isEqualTo(RayCrossingKind.Tangent);
        events.clear();
        assertThat(new Box(1, 1, 1).emitRayCrossings(ray, query, events::add)).isEqualTo(RayCrossingSupport.Unsupported);
        assertThat(events).isEmpty();
        assertThat(Sphere.intersectionRoots(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0), 1, new RayCrossingQuery(2.5, 3.5))).isEmpty();
    }
}
