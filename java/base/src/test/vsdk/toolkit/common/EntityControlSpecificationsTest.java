package vsdk.toolkit.common;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.gui.editor.ControlSpecification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
Guard for the naming rule of control specifications: each declared attribute
"type;name;interval" must have `getName()` returning the type and
`setName(type)`, so generic editors can read and write it.
*/
class EntityControlSpecificationsTest
{
    private static final List<Entity> SAMPLES = List.of(
        new Sphere(1.0),
        new Cone(1.0, 0.0, 2.0));

    @Test
    void everyControlSpecificationHasMatchingAccessors() throws Exception
    {
        for ( Entity entity : SAMPLES ) {
            for ( String text : entity.getControlSpecifications() ) {
                ControlSpecification specification = ControlSpecification.parse(text);
                assertNotNull(specification, "Malformed specification " + text);
                Class<?> type = primitive(specification.getType());
                Method getter = entity.getClass().getMethod("get" + specification.getLabel());
                assertEquals(type, getter.getReturnType(),
                    entity.getClass().getSimpleName() + "." + getter.getName());
                assertNotNull(entity.getClass().getMethod("set" + specification.getLabel(), type));
            }
        }
    }

    private static Class<?> primitive(String type)
    {
        switch ( type ) {
            case "double": return double.class;
            case "float": return float.class;
            case "int": return int.class;
            case "long": return long.class;
            default: throw new IllegalArgumentException("Unsupported type " + type);
        }
    }
}
