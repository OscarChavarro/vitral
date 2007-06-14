/*
 * ImageNotRecognizedException.java
 *
 * Created on 2 de septiembre de 2005, 10:38 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package vitral.toolkits.media;
import java.io.File;
/**
 *
 * @author usuario
 */
public class ImageNotRecognizedException extends Exception
{
    File imagen;
    public ImageNotRecognizedException(String message, File imagen) 
    {
        super(message);
        this.imagen=imagen;
    }
}
