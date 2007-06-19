//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 8 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.util.ArrayList;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;

import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;

import vsdk.toolkit.io.PersistenceElement;

public class EnvironmentPersistence extends PersistenceElement {

    private static Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    private static void addThing(Geometry g,
        ArrayList<SimpleBody> inoutSimpleBodiesArray)
    {
        if ( inoutSimpleBodiesArray == null ) return;

        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3D());
        thing.setRotation(new Matrix4x4());
        thing.setRotationInverse(new Matrix4x4());
        thing.setMaterial(defaultMaterial());
        inoutSimpleBodiesArray.add(thing);
    }

    public static void
    importEnvironment(File inSceneFileFd,
                      ArrayList<SimpleBody> inoutSimpleBodiesArray,
                      ArrayList<Light> inoutLightsArray,
                      ArrayList<Background> inoutBackgroundsArray,
                      ArrayList<Camera> inoutCamerasArray
                      ) throws Exception
    {
        String type = extractExtensionFromFile(inSceneFileFd);

        if ( type.equals("obj") ) {
            TriangleMeshGroup mg = null;
            mg = ReaderObj.read(inSceneFileFd.getAbsolutePath());
            addThing(mg, inoutSimpleBodiesArray);
        }
        else if ( type.equals("3ds") ) {
            Reader3ds.importEnvironment(inSceneFileFd,
                inoutSimpleBodiesArray, inoutLightsArray,
                inoutBackgroundsArray, inoutCamerasArray);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
