//=   getImageJOGL methods added                                            =

package vsdk.toolkit.render.jogl;

// Java base classes
import java.nio.ByteBuffer;
import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.common.RendererConfiguration;

public class Jogl2RGBAImageRenderer extends Jogl2Renderer
{
    private static ArrayList<_JoglRGBAImageRendererAssociation> compiledImages = new ArrayList<_JoglRGBAImageRendererAssociation>();
    //private static GLU glu = null;

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
    private static int activateBase(GL2 gl, RGBAImage img)
    {
        //- 1. Initialization of texture parameters -----------------------
        int x_tam = img.getXSize();
        int y_tam = img.getYSize();
        int lists[] = new int[1];

        if ( (x_tam % 4) == 0 ) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);
          }
          else if ( (x_tam % 2) == 0 ) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 2);
          }
          else {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        }

        /*
        if ( glu == null ) {
            glu = new GLU();
        }
        */

        //- 2. Seek if there is a precompiled glList for this image -------
        boolean glListIsCompiled = false;
        _JoglRGBAImageRendererAssociation item = null;

        int i;
        for ( i = 0; i < compiledImages.size(); i++ ) {
            item = compiledImages.get(i);
            if ( item.image == img ) {
                glListIsCompiled = true;
                break;
            }
        }

        //- 3. If there is no glList, create it ---------------------------
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        if ( glListIsCompiled == false ) {
            //----
            item = new _JoglRGBAImageRendererAssociation();
            item.image = img;
            item.glList = 1;
            compiledImages.add(item);

            //----
            gl.glGenTextures(1, lists, 0);
            item.glList=lists[0];
            //gl.glBindTexture(GL.GL_TEXTURE_2D, item.glList);

            //----
            try {
                GLProfile glprof;
                TextureData textureData;

                glprof = GLProfile.get(GLProfile.GL2);
                textureData = new TextureData(
                   glprof,
                   4,              // int internalFormat (number of components)
                   x_tam,          // int width
                   y_tam,          // int height
                   0,              // int border
                   GL.GL_RGBA,     // int pixelFormat
                   GL.GL_UNSIGNED_BYTE, // int pixelType
                   true,           // boolean mipmap
                   false,          // boolean dataIsCompressed
                   false,          // boolean mustFlipVertically
                   img.getRawImageDirectBuffer(), // Buffer buffer
                   null            // TextureData.Flusher flusher
                );

                item.renderer = TextureIO.newTexture(textureData);
            }
            catch ( Exception e ) {
                VSDK.reportMessage(null, VSDK.FATAL_ERROR, "activateBase", "" + e);
            }

            //----
            /*
            //glu.gluBuild2DMipmaps(GL.GL_TEXTURE_2D, 4, x_tam, y_tam, GL.GL_RGBA, 
            //                  GL.GL_UNSIGNED_BYTE, img.getRawImageDirectBuffer());
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 
                            0, 4, 
                            x_tam, y_tam, 
                            0, GL.GL_RGBA,
                            GL.GL_UNSIGNED_BYTE, 
                            img.getRawImageDirectBuffer());
            */    
        }

        //- 4. Use the image's glList -------------------------------------
//        if ( glListIsCompiled == false ) {
            item.renderer.bind(gl);
            item.renderer.enable(gl);
/*
          }
          else {
            gl.glCallList(item.glList);
        }
*/
        /*
        if ( item != null ) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, item.glList);
        }
        */
        //gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        return item.glList;
    }

    public static int activate(GL2 gl, RGBAImage img)
    {
        int list = activateBase(gl, img);

        return list;
    }

    public static int activateAsNormalMap(GL2 gl, RGBAImage img, RendererConfiguration quality)
    {
        int list = -1;

        return list;
    }

    public static void unload(GL2 gl, RGBAImage img)
    {
        _JoglRGBAImageRendererAssociation item;

        try { 
            int i;
            for ( i = 0; i < compiledImages.size(); i++ ) {
                item = compiledImages.get(i);
                if ( item.image == img ) {
                    item.renderer.disable(gl);
                    //item.renderer.dispose();
                    item.renderer = null;
                    compiledImages.remove(i);
                    return;
                }
            }
        }
        catch ( Exception e ) {
            VSDK.reportMessage(null, VSDK.WARNING, "Jogl2RGBAImageRenderer.unload", "Error unloading image.");

        }
    }

    public static void draw(GL2 gl, RGBAImage img)
    {
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glRasterPos2f(-1, -1);
        gl.glDrawPixels(
            img.getXSize(), img.getYSize(), 
            GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, 
            img.getRawImageDirectBuffer());
    }

    public static ByteBuffer importJOGLimage(GL2 gl) {
        int[] view= new int[4];
        //IntBuffer vpBuffer = BufferUtils.newIntBuffer(16);
        gl.glGetIntegerv(GL.GL_VIEWPORT, view,0);
        int width = view[2], height = view[3];

        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);
        gl.glReadBuffer(GL2GL3.GL_FRONT_LEFT);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels( -1, -1, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE,
                        bb);
        gl.glFlush();
        return bb;
    }

    public static void getImageJOGL(GL2 gl, RGBAImage image)
    {
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
    }

    public static RGBAImage getImageJOGL(GL2 gl) {
        RGBAImage image = new RGBAImage();

        getImageJOGL(gl, image);

        return image;
    }

}
