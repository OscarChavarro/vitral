//===========================================================================
package vsdk.toolkit.render.androidgles20.gui;

// Java basic classes
import java.util.HashMap;

// Android OpenGL ES2.0 classes
import android.opengl.GLES20;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.render.androidgles20.AndroidGLES20HudIconRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;
import vsdk.toolkit.gui.AndroidSystem;

// Transition classes
import vsdk.toolkit.gui.dialog.InformationDialog;

/**
*/
public class AndroidGLES20InformationDialogRenderer 
    extends AndroidGLES20Renderer {
    
    private static RGBAImage backgroundTexture = null;
    private static HashMap<String, RGBAImage> characterSprites;
    private static int fontSize;

    static {
        fontSize = 24;
        characterSprites = new HashMap<String, RGBAImage>();
    }
    
    /**
    This method is auto-animating.
    @param dialog
    @param camera
    */
    public static void draw(InformationDialog dialog, Camera camera)
    {
        if ( backgroundTexture == null ) {
            backgroundTexture = new RGBAImage();
            backgroundTexture.init(16, 16);
            
            //backgroundTexture.createTestPattern();
            
            int x;
            int y;
            RGBAPixel p = new RGBAPixel();
            p.r = VSDK.unsigned8BitInteger2signedByte(200);
            p.g = VSDK.unsigned8BitInteger2signedByte(200);
            p.b = VSDK.unsigned8BitInteger2signedByte(200);
            p.a = VSDK.unsigned8BitInteger2signedByte(200);
            for ( y = 0; y < backgroundTexture.getYSize(); y++ ) {
                for ( x = 0; x < backgroundTexture.getXSize(); x++ ) {
                    backgroundTexture.putPixel(x, y, p);
                }
            }
            
        }

        // Animation control
        if ( dialog.getAnimationParameter() < 1.0 + VSDK.EPSILON && 
            dialog.getDialogState() == InformationDialog.OPENING ) {
            dialog.setAnimationParameter(
                dialog.getAnimationParameter() + dialog.getAnimationDelta());
        }
        
        if ( dialog.getAnimationParameter() > 0.0 - VSDK.EPSILON && 
            dialog.getDialogState() == InformationDialog.CLOSING ) {
            dialog.setAnimationParameter(
                dialog.getAnimationParameter() - dialog.getAnimationDelta());
        }

        // Elements drawing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Background
        drawBackgroundPolygon(dialog);

        // Text
        drawTextInUnitSquare("Hola. Este es un mensaje retelargo para probar que el visor de imágenes funcione bien.\n\nXXXXXXXXXXX\nlllllllllll", camera, dialog.getAnimationParameter(), 0.05, 0.95);

        // Icons
        if ( dialog.getDialogState() == InformationDialog.NORMAL ) {
            AndroidGLES20HudIconRenderer.draw(dialog.getDialogIcons(), camera);
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
    */
    private static void
    drawTextInUnitSquare(String msg, Camera c, double t, double x0, double y0)
    {
        double x = x0;
        double y = y0;
        int i;
        String key;
        RGBAImage img;

        double unitsPerPixelX = 2.0 / c.getViewportXSize();
        double unitsPerPixelY = 2.0 / c.getViewportYSize();
        
        double fx;
        double fy;

        RendererConfiguration q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        setRendererConfiguration(q);

        for ( i = 0; i < msg.length(); i++ ) {
            key = "" + msg.charAt(i);
            
            if ( key.equals("\n") || x > 0.95 ) {
                x = x0;
                y -= unitsPerPixelY*25;
                if ( key.equals("\n") ) {
                    continue;
                }
            }

            if ( !characterSprites.containsKey(key) ) {
                img = AndroidSystem.calculateLabelImage(
                    key, new ColorRgb(1.0, 1.0, 1.0), fontSize);
                characterSprites.put(key, img);
            }
            if ( characterSprites.containsKey(key) ) {
                img = characterSprites.get(key);
                
                fx = (((double) img.getXSize())*2.0)
                    / ((double) c.getViewportXSize());

                fy = (((double) img.getYSize())*2.0)
                    / ((double) c.getViewportYSize());

                activateEffectTransformation(t);
                glTranslated(x, y, 0);
                glScaled(fx, fy, 1.0);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                activateDefaultTextureParameters();
                AndroidGLES20ImageRenderer.activate(img);
                drawUnitSquare();
                
                x += ((double)img.getXSize()) * unitsPerPixelX;
            }
        }
    }

    
    private static void drawBackgroundPolygon(InformationDialog dialog) {
        RendererConfiguration q;
        
        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(false);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);

        Material backgroundMaterial;
        
        backgroundMaterial = new Material();
        backgroundMaterial.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        backgroundMaterial.setDiffuse(new ColorRgb(1.0, 1.0, 1.0));
        backgroundMaterial.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        backgroundMaterial.setPhongExponent(10.0);
        AndroidGLES20MaterialRenderer.activate(backgroundMaterial);
        
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);

        activateEffectTransformation(dialog.getAnimationParameter());
        
        //
        setRendererConfiguration(q);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        activateDefaultTextureParameters();
        AndroidGLES20ImageRenderer.activate(backgroundTexture);
        glTranslated(0.5, 0.5, 0.0);
        drawUnitSquare();
    }

    private static void activateEffectTransformation(double t) {
        glLoadIdentity();

        // For GL_MODELVIEW at identity, moves coordinates from [0, 0] - [1, 1]
        // to [-1, -1] - [1, 1] interval
        glTranslated(-1.0, -1.0, 0.0);
        glScaled(2.0, 2.0, 2.0);
        
        // Transformation locates elements in the interval [0, 0] - [1, 1]
        effectSlideFromLeft2RightTransform(t);
    }

    private static void effectSlideFromLeft2RightTransform(double t) {
        
        double coverPercent = 0.8;

        glTranslated(-coverPercent*(1.0 - t), 0.0, 0.0);
        glScaled(t*coverPercent, 1.0, 1.0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
