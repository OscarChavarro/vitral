package vsdk.toolkit.gui.editor;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericEditorTest
{
    /** Records the presentation hooks instead of creating widgets. */
    private static class RecordingEditor extends GenericEditor
    {
        final List<String> controls = new ArrayList<String>();
        String message = null;
        String clearedMessage = null;

        @Override
        protected void beginBuild(String title)
        {
            controls.clear();
        }

        @Override
        protected void addControl(ControlSpecification specification,
            String value)
        {
            controls.add(specification.getLabel() + "=" + value);
        }

        @Override
        protected void endBuild()
        {
        }

        @Override
        protected void showValidationMessage(String message)
        {
            this.message = message;
        }

        @Override
        protected void setControlValue(ControlSpecification specification,
            String value)
        {
            for ( int i = 0; i < controls.size(); i++ ) {
                if ( controls.get(i).startsWith(specification.getLabel() + "=") ) {
                    controls.set(i, specification.getLabel() + "=" + value);
                }
            }
        }

        @Override
        protected void clearControls(String message)
        {
            controls.clear();
            clearedMessage = message;
        }

        ControlSpecification first()
        {
            return specifications.get(0);
        }
    }

    @Test
    void parsesOpenAndClosedIntervals()
    {
        ControlSpecification open = ControlSpecification.parse(
            "double;radius;(0, INFINITE)");
        assertNotNull(open);
        assertEquals("double", open.getType());
        assertEquals("radius", open.getName());
        assertEquals("Radius", open.getLabel());
        assertFalse(open.contains(0.0));
        assertFalse(open.contains(-1.0));
        assertTrue(open.contains(1e-9));
        assertTrue(open.contains(1e300));

        ControlSpecification closed = ControlSpecification.parse(
            "int;count;[1, 10]");
        assertNotNull(closed);
        assertTrue(closed.contains(1));
        assertTrue(closed.contains(10));
        assertFalse(closed.contains(0));
        assertFalse(closed.contains(11));

        ControlSpecification unbounded = ControlSpecification.parse(
            "double;offset");
        assertNotNull(unbounded);
        assertTrue(unbounded.contains(-1e300));
    }

    @Test
    void rejectsMalformedSpecifications()
    {
        assertNull(ControlSpecification.parse("double"));
        assertNull(ControlSpecification.parse("double;radius;0, 1"));
        assertNull(ControlSpecification.parse("double;radius;(1, 0)"));
        assertNull(ControlSpecification.parse("double;radius;(a, 1)"));
    }

    @Test
    void editsSphereRadiusThroughAccessors()
    {
        Sphere sphere = new Sphere(1.0);
        RecordingEditor editor = new RecordingEditor();
        List<Entity> changed = new ArrayList<Entity>();
        editor.setListener(changed::add);

        editor.build(sphere);
        assertEquals(List.of("Radius=1.0"), editor.controls);

        assertTrue(editor.updateValue(editor.first(), "2.5"));
        assertEquals(2.5, sphere.getRadius());
        assertEquals(6.25, sphere.getRadiusSquared());
        assertEquals(1, changed.size());
        assertNull(editor.message);

        assertFalse(editor.updateValue(editor.first(), "0"));
        assertFalse(editor.updateValue(editor.first(), "-3"));
        assertFalse(editor.updateValue(editor.first(), "abc"));
        assertEquals(2.5, sphere.getRadius());
        assertEquals(1, changed.size());
        assertNotNull(editor.message);
    }

    @Test
    void editsConeAttributesThroughAccessors()
    {
        Cone cone = new Cone(1.0, 0.0, 2.0);
        RecordingEditor editor = new RecordingEditor();

        editor.build(cone);
        assertEquals(List.of("BottomRadius=1.0", "TopRadius=0.0", "Height=2.0"),
            editor.controls);

        ControlSpecification bottom = editor.specifications.get(0);
        ControlSpecification top = editor.specifications.get(1);
        ControlSpecification height = editor.specifications.get(2);
        assertFalse(editor.updateValue(bottom, "0"));
        assertTrue(editor.updateValue(top, "0"));
        assertTrue(editor.updateValue(top, "0.5"));
        assertFalse(editor.updateValue(height, "0"));
        assertTrue(editor.updateValue(height, "3"));
        assertEquals(1.0, cone.getBottomRadius());
        assertEquals(0.5, cone.getTopRadius());
        assertEquals(3.0, cone.getHeight());
    }

    @Test
    void refreshesControlsWhenEntityIsUpdated()
    {
        Sphere sphere = new Sphere(1.0);
        RecordingEditor editor = new RecordingEditor();
        editor.build(sphere);

        // A change made by another tool, announced with update()
        sphere.setRadius(4.0);
        sphere.update();
        assertEquals(List.of("Radius=4.0"), editor.controls);
    }

    @Test
    void clearsControlsWhenEntityIsDisposed()
    {
        Sphere sphere = new Sphere(1.0);
        RecordingEditor editor = new RecordingEditor();
        editor.build(sphere);

        sphere.dispose();
        assertTrue(editor.isEntityDeleted());
        assertNull(editor.getEntity());
        assertTrue(editor.controls.isEmpty());
        assertEquals("Entity deleted (Sphere).", editor.clearedMessage);
        assertFalse(editor.updateValue(ControlSpecification.parse(
            "double;radius;(0, INFINITE)"), "2"));
    }

    @Test
    void stopsListeningToPreviousEntity()
    {
        Sphere first = new Sphere(1.0);
        Sphere second = new Sphere(2.0);
        RecordingEditor editor = new RecordingEditor();
        editor.build(first);
        editor.build(second);

        first.dispose();
        assertFalse(editor.isEntityDeleted());
        assertEquals(List.of("Radius=2.0"), editor.controls);
    }

    @Test
    void reportsEntitiesWithoutSpecifications()
    {
        RecordingEditor editor = new RecordingEditor();
        editor.build(new Entity());
        assertTrue(editor.controls.isEmpty());
        assertNotNull(editor.message);
    }
}
