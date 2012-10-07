//===========================================================================

package vsdk.framework.shapeMatching.plugins;

// Java classes
import java.util.HashMap;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.framework.shapeMatching.ShapeDescriptor2DGenerator;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.io.image.ImagePersistence;

public class ShapeDescriptor2DGeneratorFourier extends ShapeDescriptor2DGenerator
{
    @Override
    public ShapeDescriptor calculateShapeDescriptor(
        InputStream sceneSource,
        String sceneSourceUrl,
        HashMap<String, InputStream> subObjects) {
        //System.err.println("ShapeDescriptor2DGeneratorFourier::calculateShapeDescriptor expecting an Image with a distanceField to process FourierShapeDescriptor");

        try {
            // 1. Export in-memory info to a temporary file... should not
            //    be done, a import image from input stream is needed in
            //    ImagePersistence class.
            File fd = new File("temp.png");
            FileOutputStream fos = new FileOutputStream(fd);
            while ( sceneSource.available() > 0 ) {
                byte arr[] = new byte [sceneSource.available()];
                sceneSource.read(arr);
                fos.write(arr);
            }

            // 2. Reimport the exported data from temporary file
            IndexedColorImage distanceField;
            distanceField = ImagePersistence.importIndexedColor(new File("temp.png"));

            // 3. Do current work
            if ( distanceField.getXSize() != 64 || distanceField.getYSize() != 64 ) {
                System.err.println("Error: current processor can only compare 64x64 sized images!");
                return null;
            }
            calculateCircularHarmonicsShapeDescriptor(distanceField, "DETACHED_DATA");

        }
        catch ( Exception e ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "calculateShapeDescriptor", "" + e);
            return null;
        }


        return null;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
