//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 8 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.OutputStream;

// VSDK Classes
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.PersistenceElement;

public class EnvironmentPersistence extends PersistenceElement {

    public static void
    importEnvironment(File inSceneFileFd, SimpleScene inoutScene)
        throws Exception
    {
        String type = extractExtensionFromFile(inSceneFileFd);

        if ( type.equals("obj") ) {
            ReaderObj.importEnvironment(inSceneFileFd, inoutScene);
        }
        else if ( type.equals("3ds") ) {
            Reader3ds.importEnvironment(inSceneFileFd, inoutScene);
        }
        else if ( type.equals("gts") ) {
            ReaderGts.importEnvironment(inSceneFileFd, inoutScene);
        }
        else if ( type.equals("ase") ) {
            ReaderAse.importEnvironment(inSceneFileFd, inoutScene);
        }
        else if ( type.equals("vtk") ) {
            ReaderVtk.importEnvironment(inSceneFileFd, inoutScene);
        }
    }

    public static void
    exportEnvironmentObj(OutputStream inOutputStream, SimpleScene inScene)
        throws Exception
    {
        WriterObj.exportEnvironment(inOutputStream, inScene);
    }

    public static void
    exportEnvironmentGts(OutputStream inOutputStream, SimpleScene inScene)
        throws Exception
    {
        WriterGts.exportEnvironment(inOutputStream, inScene);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
