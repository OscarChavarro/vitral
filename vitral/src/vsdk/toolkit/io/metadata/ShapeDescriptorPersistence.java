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
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.SphericalHarmonicShapeDescriptor;

public class ShapeDescriptorPersistence extends PersistenceElement
{
    private static final byte TYPE_ENDING             = 0x01;
    private static final byte TYPE_STRING             = 0x02;
    private static final byte TYPE_SPHERICAL_HARMONIC = 0x03;

    public static void
    exportGeometryMetadata(OutputStream writer, GeometryMetadata m)
        throws Exception
    {
        ArrayList<ShapeDescriptor> list;

        exportByte(writer, TYPE_STRING);
        writeAsciiString(writer, m.getFilename());
        list = m.getDescriptors();
        exportDescriptorMetadata(writer, list);
        exportEnding(writer);
    }

    /**
    Returns null if gets error... can also return Exception
    */
    public static GeometryMetadata
    importGeometryMetadata(InputStream reader) throws Exception {
        GeometryMetadata m;
        byte subChunkId;
        byte chunkId;
        int bytesToSkip;
        double vector[];
        int i;
        ShapeDescriptor shapeDescriptor;
    String label;

        chunkId = importByte(reader);
        switch ( chunkId ) {
          case TYPE_STRING:
            m = new GeometryMetadata();
            m.setFilename(readAsciiString(reader));
            do {
                subChunkId = importByte(reader);
                switch( subChunkId ) {
                  case TYPE_ENDING:
                    break;
                  case TYPE_SPHERICAL_HARMONIC:
            label = readAsciiString(reader);
                    bytesToSkip = readIntBE(reader);
                    vector = new double[bytesToSkip/4];
                    for ( i = 0; i < vector.length; i++ ) {
                        vector[i] = readFloatBE(reader);
                    }
                    shapeDescriptor = new SphericalHarmonicShapeDescriptor(label);
                    shapeDescriptor.setFeatureVector(vector);
                    m.getDescriptors().add(shapeDescriptor);
                    reader.skip(bytesToSkip - vector.length*4);
                    break;
                  default:
                    bytesToSkip = readIntBE(reader);
                    System.out.println("Skipping bytes: " + bytesToSkip);
                    reader.skip(bytesToSkip);
                    break;
                }
            } while ( subChunkId != TYPE_ENDING );
            break;
          default:
            System.err.println("ERROR importing database (wrong format " +
                chunkId + ")!");
            return null;
        }
        return m;
    }

    private static void
    exportByte(OutputStream writer, byte var)
        throws Exception
    {
        byte data[] = new byte[1];
        data[0] = var;
        writer.write(data, 0, data.length);
    }

    private static byte
    importByte(InputStream reader)
        throws Exception
    {
        byte data[] = new byte[1];
        reader.read(data, 0, data.length);
    return data[0];
    }

    private static void
    exportEnding(OutputStream writer)
        throws Exception
    {
        exportByte(writer, TYPE_ENDING);
    }

    private static void
    exportDescriptorMetadata(OutputStream writer,
                   ArrayList<ShapeDescriptor> inShapeDescriptorsArray)
                   throws Exception
    {
        int i, j;
        ShapeDescriptor s;
    double featureVector[];

        for ( i = 0; i < inShapeDescriptorsArray.size(); i++ ) {
            s = inShapeDescriptorsArray.get(i);
            //-----------------------------------------------------------------
            if ( s instanceof SphericalHarmonicShapeDescriptor ) {
                exportByte(writer, TYPE_SPHERICAL_HARMONIC);
          }
          else {
        System.out.println("Non registered class. Dumping skipped.");
        return;
        }

            //-----------------------------------------------------------------
            writeAsciiString(writer, s.getLabel());

            //-----------------------------------------------------------------
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
