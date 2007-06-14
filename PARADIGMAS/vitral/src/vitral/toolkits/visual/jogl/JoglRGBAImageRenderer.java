//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 25 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vitral.toolkits.visual.jogl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import vitral.toolkits.media.RGBAImage;

class JoglRGBAImageRendererImageAssociation
{
    public int glList;
    public RGBAImage image;
}

public class JoglRGBAImageRenderer
{
    private static ArrayList<JoglRGBAImageRendererImageAssociation> compiledImages = new ArrayList<JoglRGBAImageRendererImageAssociation>();
    private static GLU glu = null;

    /**
    This method generates an OpenGL/JOGL MipMap structure, assoiates it with
    the given image reference and activates.

    The method keeps track of all images activated, and take that history into
    account to pass the image data to the graphics hardware only once. Note that
    this method creates and use an OpenGL/JOGL compilation list for each image,
    to ensure optimal performance.

    \todo
    In applications with changing images, the memory list of compiled lists
    and the list themselves should be cleared, or not used. This will lead to
    the creation of new methods.
    */
    public static void activateGL(GL gl, RGBAImage img)
    {
        //- 1. Initialization of texture parameters -----------------------
        int x_tam = img.getXSize();
        int y_tam = img.getYSize();
        int lists[] = new int[1];

        if ( (x_tam % 4) == 0 ) {
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 4);
          }
          else if ( (x_tam % 2) == 0 ) {
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 2);
          }
          else {
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);
        }

        if ( glu == null ) {
            glu = new GLU();
        }

        //- 2. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        JoglRGBAImageRendererImageAssociation item = null;

        Iterator it = compiledImages.iterator();
        while ( it.hasNext() ) {
            item = (JoglRGBAImageRendererImageAssociation)it.next();
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        if ( glListIsCompiled == false ) {
            //----
            item = new JoglRGBAImageRendererImageAssociation();
            item.image = img;
            item.glList = 1;
            compiledImages.add(item);

            //----
            gl.glGenTextures(1, lists, 0);
            item.glList=lists[0];
            gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);
            //glu.gluBuild2DMipmaps(gl.GL_TEXTURE_2D, 4, x_tam, y_tam, gl.GL_RGBA, 
            //                  gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(img.getRawImage()));
            gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, 4, x_tam, y_tam, 0, gl.GL_RGBA, 
                            gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(img.getRawImage()));
        }

        //- 4. Use the image's glList -------------------------------------
        if ( item != null ) {
            gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);
        }
    }

    public static void draw(GL gl, RGBAImage img)
    {
        gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);
        gl.glRasterPos2f(-1, -1);
        gl.glDrawPixels(img.getXSize(), img.getYSize(), 
                        gl.GL_RGBA, gl.GL_UNSIGNED_BYTE, 
                        ByteBuffer.wrap(img.getRawImage()));
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
