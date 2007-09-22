//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - September 3 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package scivis;

import java.io.File;

import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.image.ImagePersistence;

/**
*/
public class CachedImage extends CachedInformation {
    private Image image;
    String source;

    public CachedImage(String source)
    {
        this.source = source;
        image = null;
    }

    //public ? lastTimeAccessed
/*
    public ? timeFromLastAccess()
    {

    }
*/

    public int getSizeInBytes()
    {
        if ( image == null) {
            return 0;
        }
        return image.getSizeInBytes();
    }

    public boolean load()
    {
        // Pending: extend the source String format management...
        RGBImage img = null;
        try {
            img = ImagePersistence.importRGB(new File(source));
        }
        catch ( Exception e ) {
            System.err.println(e);
            System.exit(0);
        }

        image = img;

        return true;
    }

    public void unload()
    {
        ;
    }

    public boolean isLoaded()
    {
        return ( image != null );
    }

    public Image getImage()
    {
        if ( !isLoaded() ) {
            System.out.println("[CachedImage] Loading " + source);
            load();
        }
        System.out.println("[CachedImage] Image of " + image.getSizeInBytes() + " bytes");
        return image;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
