//===========================================================================

// Java basic classes
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;

// VSDK Classes
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.media.GeometryMetadata;

public class ShapeDatabaseFile implements ShapeDatabase
{
    public ArrayList<GeometryMetadata> descriptorsArray;
    private String databaseFilename;

    public ShapeDatabaseFile(String filename)
    {
        databaseFilename = new String(filename);
        descriptorsArray = new ArrayList<GeometryMetadata>();
    }

    public void finalize()
    {
        int i;
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.set(i, null);
        }
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.remove(0);
        }
        descriptorsArray = null;
    }

    public boolean connect()
    {
        System.out.print("Reading database ... ");

        File fd = new File(databaseFilename);
        FileInputStream fis;
        BufferedInputStream reader;
        GeometryMetadata m;

        try {
            fis = new FileInputStream(fd);
            reader = new BufferedInputStream(fis);

            while ( reader.available() > 0 ) {
                m = ShapeDescriptorPersistence.importGeometryMetadata(reader);
                if ( m != null ) {
                    descriptorsArray.add(m);
                }
            }
            reader.close();
            fis.close();
        }
        catch ( Exception e ) {
            if ( !(e instanceof FileNotFoundException) ) {
                System.err.println("ERROR importing database!" + e);
            }
            return false;
        }
        System.out.println(descriptorsArray.size() + " entries, Ok. ");

        return true;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
