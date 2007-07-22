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
            ReaderObj.importEnvironment(inSceneFileFd,
                inoutSimpleBodiesArray, inoutLightsArray,
                inoutBackgroundsArray, inoutCamerasArray);
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
