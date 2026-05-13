// Java basic classes
import java.util.ArrayList;

// VSDK Classes
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.media.GeometryMetadata;

public interface ShapeDatabase
{
    public abstract boolean connect();

    /**
    Given current shape database and a `referenceDescriptor`, this method
    search for similar descriptors to the reference descriptor inside the
    data warehouse.  For each euclidean distance under the `tolerance` range,
    this method adds a new match result to the `results` list. New results
    are marked with `resultId` number, and result parts are marked under
    a `sketchId`.
    */
    public abstract void searchMatches(
        ArrayList <Result> results, ShapeDescriptor referenceDescriptor,
        int resultId, int sketchId, double tolerance);

    public abstract void addEntry(GeometryMetadata newEntry);

    public abstract GeometryMetadata searchEntryById(long id);

    public abstract GeometryMetadata searchEntryByFilename(String filename);

    public abstract void removeEntryById(long id);

    public abstract void sync();

    public abstract long getNumEntries();

    public abstract long getMaxEntryId();
}
