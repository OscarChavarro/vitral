//===========================================================================

package vitral.application;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.Material;

/**
This should evolve to the point of being equivalent to "Scene" class used at
"SceneEditorApplication" version of application for desktop computers.
*/
public class Scene
{
    public SimpleScene scene;
    public int selectedObjectIndex = -1;

    // Others
    private int acumObject = 1;

    public Scene()
    {
        scene = new SimpleScene();
    }

    public Material defaultMaterial()
    {
        Material m = new Material();

/*
        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100.0);
*/

        m.setAmbient(new ColorRgb(0, 0, 0));
        m.setDiffuse(new ColorRgb(1, 1, 1));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(40.0);


        return m;
    }

    public SimpleBody addThing(Geometry g, ColorRgb c)
    {
        SimpleBody thing;
        thing = addThing(g);
        thing.getMaterial().setDiffuse(c);
        return thing;
    }

    public SimpleBody addThing(Geometry g)
    {
        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3D());
        thing.setRotation(new Matrix4x4());
        thing.setRotationInverse(new Matrix4x4());
        thing.setMaterial(defaultMaterial());
        thing.setName("Geometric object " + acumObject);
        scene.getSimpleBodies().add(thing);

        acumObject++;
        //selectedThings.sync();
        return thing;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
