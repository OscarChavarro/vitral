//===========================================================================
package vsdk.toolkit.render.androidgles10;

// Java classes
import java.util.ArrayList;

// Android classes: OpenGL ES 2.0
import android.opengl.GLES10;

// VSDK classes
import vsdk.toolkit.media.RGBImage;

public class AndroidGLES10RGBImageRenderer extends AndroidGLES10Renderer
{
    private static ArrayList<_AndroidGLES10RGBImageRendererAssociation> compiledImages;

    static {
        compiledImages = new ArrayList<_AndroidGLES10RGBImageRendererAssociation>();
    }

    public static int activate(RGBImage img)
    {
        int list = activateBase(img);

        return list;
    }

    private static int activateBase(RGBImage img)
    {
        //- 1. Initialization of texture parameters -----------------------
        int x_tam = img.getXSize();
        int y_tam = img.getYSize();
        int lists[] = new int[1];

        if ( (x_tam % 4) == 0 ) {
            GLES10.glPixelStorei(GLES10.GL_UNPACK_ALIGNMENT, 4);
          }
          else if ( (x_tam % 2) == 0 ) {
            GLES10.glPixelStorei(GLES10.GL_UNPACK_ALIGNMENT, 2);
          }
          else {
            GLES10.glPixelStorei(GLES10.GL_UNPACK_ALIGNMENT, 1);
        }

        //- 2. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        _AndroidGLES10RGBImageRendererAssociation item = null;

        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            item = compiledImages.get(i);
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
        GLES10.glEnable(GLES10.GL_BLEND);
        if ( glListIsCompiled == false ) {
            //----
            GLES10.glGenTextures(1, lists, 0);
            item = new _AndroidGLES10RGBImageRendererAssociation();
            item.image = img;
            item.glList = lists[0];
            compiledImages.add(item);

            //----
            GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, item.glList);

            GLES10.glTexImage2D(
                GLES10.GL_TEXTURE_2D, 0, GLES10.GL_RGB, x_tam, y_tam, 0, 
                GLES10.GL_RGB, GLES10.GL_UNSIGNED_BYTE, 
                item.image.getRawImageDirectBuffer());

            checkGlError("glTexImage2D");
        }

        //- 4. Use the image's glList -------------------------------------
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, item.glList);

        return item.glList;
    }
    
    public static void disable(RGBImage img)
    {
        //- 1. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        _AndroidGLES10RGBImageRendererAssociation item = null;

        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            item = compiledImages.get(i);
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 2. If there is a glList, delete it ----------------------------
        if ( glListIsCompiled != false && item != null ) {
            int arr[] = {item.glList};
            GLES10.glDeleteTextures(1, arr, 0);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
