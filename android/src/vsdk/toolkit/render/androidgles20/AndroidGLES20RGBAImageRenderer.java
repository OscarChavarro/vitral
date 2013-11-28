//===========================================================================

package vsdk.toolkit.render.androidgles20;

// Java classes
import java.util.ArrayList;

// Android classes: OpenGL ES 2.0
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.media.RGBAImage;

public class AndroidGLES20RGBAImageRenderer extends AndroidGLES20Renderer
{
    private static ArrayList<_AndroidGLES20RGBAImageRendererAssociation> compiledImages;

    static {
        compiledImages = new ArrayList<_AndroidGLES20RGBAImageRendererAssociation>();
    }

    public static int activate(RGBAImage img, int machete)
    {
        int list = activateBase(img, machete);

        return list;
    }

    private static int activateBase(RGBAImage img, int machete)
    {
        //- 1. Initialization of texture parameters -----------------------
        int x_tam = img.getXSize();
        int y_tam = img.getYSize();
        int lists[] = new int[1];

        if ( (x_tam % 4) == 0 ) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
          }
          else if ( (x_tam % 2) == 0 ) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 2);
          }
          else {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        }

        //- 2. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        _AndroidGLES20RGBAImageRendererAssociation item = null;

        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            item = compiledImages.get(i);
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        if ( glListIsCompiled == false ) {
            //----
            item = new _AndroidGLES20RGBAImageRendererAssociation();
            item.image = img;
            item.glList = 1;
            compiledImages.add(item);

            //----
            GLES20.glGenTextures(1, lists, 0);
            item.glList = lists[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, item.glList);

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, x_tam, y_tam, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, item.image.getRawImageDirectBuffer());
            checkGlError("glTexImage2D");
        }

        //- 4. Use the image's glList -------------------------------------
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, item.glList);

        return item.glList;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
