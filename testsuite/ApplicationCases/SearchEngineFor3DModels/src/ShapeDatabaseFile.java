//===========================================================================
//= A 3D Search Engine based on Vitral SDK toolkit platform                 =
//=-------------------------------------------------------------------------=
//= Oscar Chavarro, May 23 2007                                             =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003] Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,   =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

// Java basic classes
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

// VSDK Classes
import vsdk.toolkit.io.metadata.ShapeDescriptorPersistence;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;

public class ShapeDatabaseFile implements ShapeDatabase
{
    private ArrayList<GeometryMetadata> descriptorsArray;
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

    /**
    Check superclass method ShapeDatabase.searchMatches
    */
    public void searchMatches(ArrayList <Result> results,
                              ShapeDescriptor referenceDescriptor,
        int resultId, int sketchId, double tolerance)
    {
        int i;
        double Ls;
        Result result;
        GeometryMetadata n;
        GeometryMetadata descriptor;

        GeometryMetadata metadata;
        metadata = new GeometryMetadata();
        metadata.getDescriptors().add(referenceDescriptor);

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptor = descriptorsArray.get(i);
            // Takes into consideration the euclidean distance as indicated
            // in [FUNK2003].4 (L2 Minskowski distance).
            Ls = metadata.doMinskowskiDistance(descriptor, 2.0, referenceDescriptor.getLabel());
            if ( Ls < tolerance ) {
                n = descriptor;
                result = Result.searchResult(results, n.getId());
                if ( result == null ) {
                    result = new Result(n.getFilename(), n.getId(),
                                        new ResultSource(resultId, Ls, sketchId));
                    result.setDescriptor(n);
                    results.add(result);
                }
                else {
                    result.addSource(
                        new ResultSource(resultId, Ls, sketchId));
                }
            }
        }
    }

    public void addEntry(GeometryMetadata newEntry)
    {
        if ( newEntry != null ) {
            descriptorsArray.add(newEntry);
        }
    }

    public GeometryMetadata searchEntryByFilename(String filename)
    {
        int i;
        GeometryMetadata entry;

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            entry = descriptorsArray.get(i);
            if ( entry.getFilename().equals(filename) ) {
                return entry;
            }
        }
        return null;
    }

    public GeometryMetadata searchEntryById(long id)
    {
        int i;
        GeometryMetadata entry;

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            entry = descriptorsArray.get(i);
            if ( entry.getId() == id ) {
                return entry;
            }
        }
        return null;
    }

    public void removeEntryById(long id)
    {
        int i;
        GeometryMetadata entry;

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            entry = descriptorsArray.get(i);
            if ( entry.getId() == id ) {
                descriptorsArray.remove(i);
                return;
            }
        }
    }

    public void sync()
    {
        GeometryMetadata m;
        int i;

        File fd = new File(databaseFilename);
        FileOutputStream fos;
        BufferedOutputStream writer;

        try {
            fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);
            for ( i = 0; i < descriptorsArray.size(); i++ ) {
                m = descriptorsArray.get(i);
                ShapeDescriptorPersistence.exportGeometryMetadata(writer, m);
            }
            writer.flush();
            writer.close();
            fos.close();
        }
        catch ( Exception e ) {
            System.err.println("ERROR syncing database!");
        }
    }

    public long getNumEntries()
    {
        return descriptorsArray.size();
    }

    public long getMaxEntryId()
    {
        long id = -1;
        int i;
        GeometryMetadata entry;

        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            entry = descriptorsArray.get(i);
            if ( entry.getId() > id ) {
                id = entry.getId();
            }
        }
        return id;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
