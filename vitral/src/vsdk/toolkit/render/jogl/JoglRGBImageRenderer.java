//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 25 2005 - Oscar Chavarro: Original base version              =
//= - December 7 2005 - Fabio Aroca / Eduardo Mendoza: importJOGLimage and  =
//=   getImageJOGL methods added                                            =
//= - March 14 2006 - Oscar Chavarro: quality check                         =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.io.FileInputStream;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import javax.media.opengl.GL;

// import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;
import com.sun.opengl.util.texture.TextureData;

import vsdk.toolkit.media.RGBImage;

class _JoglRGBImageRendererImageAssociation extends JoglRenderer
{
    public int glList;
    public Texture renderer;
    public RGBImage image;
}

public class JoglRGBImageRenderer extends JoglRenderer 
{
    private static ArrayList<_JoglRGBImageRendererImageAssociation> compiledImages = new ArrayList<_JoglRGBImageRendererImageAssociation>();
    //private static GLU glu = null;

    /**
    This method generates an OpenGL/JOGL MipMap structure, assoiates it with
    the given image reference and activates.

    The method keeps track of all images activated, and take that history into
    account to pass the image data to the graphics hardware only once. Note that
    this method creates and use an OpenGL/JOGL compilation list for each image,
    to ensure optimal performance.
    @return The OpenGL display list associated with this visualization
    \todo
    In applications with changing images, the memory list of compiled lists
    and the list themselves should be cleared, or not used. This will lead to
    the creation of new methods.
    */
    public static int activate(GL gl, RGBImage img)
    {
        //- 1. Initialization of texture parameters -----------------------
        int x_tam = img.getXSize();
        int y_tam = img.getYSize();
        int lists[] = new int[1];

        /*
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
        */

        //- 2. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        _JoglRGBImageRendererImageAssociation item = null;

        Iterator it = compiledImages.iterator();
        while ( it.hasNext() ) {
            item = (_JoglRGBImageRendererImageAssociation)it.next();
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        if ( glListIsCompiled == false ) {
            //----
            item = new _JoglRGBImageRendererImageAssociation();
            item.image = img;
            item.glList = 1;
            compiledImages.add(item);

            //----
            //gl.glGenTextures(1, lists, 0);
            //item.glList=lists[0];
            //gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);

            try {
                TextureData textureData;
                textureData = new TextureData(
                   3, // int internalFormat (number of components)
                   x_tam, // int width
                   y_tam, // int height
                   0, // int border
                   gl.GL_RGB, // int pixelFormat
                   gl.GL_UNSIGNED_BYTE, // int pixelType
                   true, // boolean mipmap
                   false, // boolean dataIsCompressed
                   false, // boolean mustFlipVertically
                   ByteBuffer.wrap(img.getRawImage()), // Buffer buffer
                   null // TextureData.Flusher flusher
                );
                item.renderer = TextureIO.newTexture(textureData);
                item.glList = item.renderer.getTextureObject();
            }
            catch ( Exception e ) {
                System.err.println(e);
            }
            /*
            //glu.gluBuild2DMipmaps(gl.GL_TEXTURE_2D, 3, x_tam, y_tam, gl.GL_RGB, 
            //                  gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(img.getRawImage()));
            gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, 3, x_tam, y_tam, 0, gl.GL_RGB, 
                            gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(img.getRawImage()));
            */
        }

        //- 4. Use the image's glList -------------------------------------
        //if ( glListIsCompiled == false ) {
            item.renderer.bind();
            item.renderer.enable();
/*
          }
          else {
            gl.glCallList(item.glList);
        }
*/
        /*
        if ( item != null ) {
            gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);
        }
        */
        return item.glList;
    }

    public static void draw(GL gl, RGBImage img)
    {
        gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);
        gl.glRasterPos2f(-1, -1);
        gl.glDrawPixels(img.getXSize(), img.getYSize(), 
                        gl.GL_RGB, gl.GL_UNSIGNED_BYTE, 
                        ByteBuffer.wrap(img.getRawImage()));
    }

    public static ByteBuffer importJOGLimage(GL gl) {
        int[] view= new int[4];
        //IntBuffer vpBuffer = BufferUtils.newIntBuffer(16);
        gl.glGetIntegerv(GL.GL_VIEWPORT, view,0);
        int width = view[2], height = view[3];

        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);
        gl.glReadBuffer(GL.GL_FRONT_LEFT);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels( -1, -1, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE,
                        bb);
        gl.glFlush();
        return bb;
    }

    public static RGBImage getImageJOGL(GL gl) {
        RGBImage image = new RGBImage();
        int[] view= new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, view,0);
        int width = view[2], height = view[3];

        image.init(width, height);

        // TODO: Check if this can be done without duplication!
        ByteBuffer bb = importJOGLimage(gl).duplicate();

        int pos = 0;

        for (int y =image.getYSize()-1; y >=0; y--) {
            for (int x = 0; x < image.getXSize(); x++) {
                image.putPixel(x,y, bb.get(pos), bb.get(pos + 1),
                               bb.get(pos + 2));
                pos += 3;
            }
        }
        return image;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
