//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 18 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.io.metadata;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;

import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;

public class ShapeDescriptorPersistence extends PersistenceElement
{
    public static final byte TYPE_ENDING             = 0x01;
    public static final byte TYPE_STRING             = 0x02;
    public static final byte TYPE_SPHERICAL_HARMONIC = 0x03;

    public static void
    importDescriptorMetadata(InputStream reader,
                   ArrayList<ShapeDescriptor> inoutShapeDescriptorsArray)
                   throws Exception
    {
        System.exit(0);
    }

    public static void
    exportByte(OutputStream writer, byte var)
        throws Exception
    {
        byte data[] = new byte[1];
        data[0] = var;
        writer.write(data, 0, data.length);
    }

    public static byte
    importByte(InputStream reader)
        throws Exception
    {
        byte data[] = new byte[1];
        reader.read(data, 0, data.length);
    return data[0];
    }

    public static void
    exportEnding(OutputStream writer)
        throws Exception
    {
        exportByte(writer, TYPE_ENDING);
    }

    public static void
    exportDescriptorMetadata(OutputStream writer,
                   ArrayList<ShapeDescriptor> inShapeDescriptorsArray)
                   throws Exception
    {
        int i, j;
        ShapeDescriptor s;
    double featureVector[];

        for ( i = 0; i < inShapeDescriptorsArray.size(); i++ ) {
            s = inShapeDescriptorsArray.get(i);
            if ( s instanceof SphericalHarmonicShapeDescriptor ) {
                exportByte(writer, TYPE_SPHERICAL_HARMONIC);
          }
          else {
        System.out.println("Non registered class. Dumping skipped.");
        return;
        }

            featureVector = s.getFeatureVector();
        writeIntBE(writer, featureVector.length*4); // Bytes to skip
        for ( j = 0; j < featureVector.length; j++ ) {
        writeFloatBE(writer, (float)featureVector[j]);
        }
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
