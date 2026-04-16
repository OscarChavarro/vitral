package vsdk.toolkit.render.androidgles20;

// Java classes
import java.util.ArrayList;

// Android classes: OpenGL ES 2.0
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.media.RGBImage;

public class AndroidGLES20RGBImageRenderer extends AndroidGLES20Renderer
{
    private static ArrayList<_AndroidGLES20RGBImageRendererAssociation> compiledImages;

    static {
        compiledImages = new ArrayList<_AndroidGLES20RGBImageRendererAssociation>();
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
        _AndroidGLES20RGBImageRendererAssociation item = null;

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
            GLES20.glGenTextures(1, lists, 0);
            item = new _AndroidGLES20RGBImageRendererAssociation();
            item.image = img;
            item.glList = lists[0];
            compiledImages.add(item);

            //----
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, item.glList);

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, x_tam, y_tam, 0, 
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, 
                item.image.getRawImageDirectBuffer());

            checkGlError("glTexImage2D");
        }

        //- 4. Use the image's glList -------------------------------------
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, item.glList);

        return item.glList;
    }
}
