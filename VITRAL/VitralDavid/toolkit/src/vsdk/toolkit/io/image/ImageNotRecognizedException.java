/*
 * Created on 2 de septiembre de 2005, 10:38 AM
 */

package vsdk.toolkit.io.image;
import java.io.File;

public class ImageNotRecognizedException extends Exception
{
    File imagen;
    public ImageNotRecognizedException(String message, File imagen) 
    {
        super(message);
        this.imagen=imagen;
    }
}
