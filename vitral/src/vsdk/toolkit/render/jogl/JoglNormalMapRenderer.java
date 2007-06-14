//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 30 2006 - Oscar Chavarro: Original base version              =
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

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.NormalMap;

class _JoglNormalMapRendererImageAssociation extends JoglRenderer
{
    public int glList;
    public Texture renderer;
    public NormalMap image;
}

public class JoglNormalMapRenderer extends JoglRenderer 
{
    private static ArrayList<_JoglNormalMapRendererImageAssociation> compiledImages = new ArrayList<_JoglNormalMapRendererImageAssociation>();
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
    public static int activate(GL gl, NormalMap map)
    {
        //- 1. Initialization of texture parameters -----------------------
        int xSize = map.getXSize();
        int ySize = map.getYSize();
        int lists[] = new int[1];

        /*
        if ( (xSize % 4) == 0 ) {
            gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 4);
          }
          else if ( (xSize % 2) == 0 ) {
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
        _JoglNormalMapRendererImageAssociation item = null;

        Iterator it = compiledImages.iterator();
        while ( it.hasNext() ) {
            item = (_JoglNormalMapRendererImageAssociation)it.next();
            if ( item.image == map ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        if ( glListIsCompiled == false ) {
            //----
            item = new _JoglNormalMapRendererImageAssociation();
            item.image = map;
            item.glList = 1;
            compiledImages.add(item);

            //----
            //gl.glGenTextures(1, lists, 0);
            //item.glList=lists[0];
            //gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);

            try {
                //
                byte byteArr[] = new byte[4*xSize*ySize];
                int i;
                long high, low;
                int u;
                int v;
                Vector3D sample;

                for ( u = 0, i = 0; u < xSize; u++ ) {
                    for ( v = 0; v < ySize; v++, i += 4 ) {
                        sample = map.getNormal(((double)u)/((double)xSize-1),
                                               ((double)v)/((double)ySize-1));
                        high = (int)(32767.0*sample.x);
                        low = (int)(32767.0*sample.y);
                        byteArr[i] = VSDK.unsigned8BitInteger2signedByte((int)(high & 0xFF00) >> 8);
                        byteArr[i+1] = VSDK.unsigned8BitInteger2signedByte((int)(high & 0x00FF));
                        byteArr[i+2] = VSDK.unsigned8BitInteger2signedByte((int)(low & 0xFF00) >> 8);
                        byteArr[i+3] = VSDK.unsigned8BitInteger2signedByte((int)(low & 0x00FF));
                    }
                }

                //
                TextureData textureData;
                textureData = new TextureData(
                   gl.GL_SIGNED_HILO_NV, // int internalFormat (number of components)
                   xSize, // int width
                   ySize, // int height
                   0, // int border
                   gl.GL_HILO_NV, // int pixelFormat
                   gl.GL_SHORT, // int pixelType
                   false, // boolean mipmap
                   false, // boolean dataIsCompressed
                   false, // boolean mustFlipVertically
                   ByteBuffer.wrap(byteArr), // Buffer buffer
                   null // TextureData.Flusher flusher
                );
                item.renderer = TextureIO.newTexture(textureData);
                item.glList = item.renderer.getTextureObject();
            }
            catch ( Exception e ) {
                System.err.println(e);
            }
            /*
            //glu.gluBuild2DMipmaps(gl.GL_TEXTURE_2D, 3, xSize, ySize, gl.GL_RGB, 
            //                  gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(map.getRawImage()));
            gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, 3, xSize, ySize, 0, gl.GL_RGB, 
                            gl.GL_UNSIGNED_BYTE, ByteBuffer.wrap(map.getRawImage()));
            */
        }

        //- 4. Use the image's glList -------------------------------------
        if ( glListIsCompiled == false ) {
            item.renderer.bind();
            item.renderer.enable();
        }
        else {
            gl.glCallList(item.glList);
        }
        /*
        if ( item != null ) {
            gl.glBindTexture(gl.GL_TEXTURE_2D, item.glList);
        }
        */
        return item.glList;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
