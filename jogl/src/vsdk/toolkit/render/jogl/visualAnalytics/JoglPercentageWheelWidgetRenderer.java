//===========================================================================
//= References:                                                             =
//= [.wGOOD2014] "Good Infographics", example available at                  =
//= http://awesome.good.is/transparency/web/1010/political-climate-chart/   =
//=     interactive.html                                                    =
//===========================================================================
package vsdk.toolkit.render.jogl.visualAnalytics;

// Java Swing/Awt classes
import java.awt.Font;

// JOGL classes
import com.jogamp.opengl.util.awt.TextRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.media.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.visualAnalytics.PercentageWheelWidget;
import vsdk.toolkit.gui.visualAnalytics.VisualDoubleVariable;
//import vsdk.toolkit.render.jogl.JoglImageRenderer;

/**
This class generates OpenGL / JOGL primitives needed to show a 
PercentageWheelWidget. Current version should be refined in
three ways:
  - When changing data without changing the size of the dataset, animation
    should be supported.
  - Better text rendering techniques should be used. Current version is
    based upon bitmaps which tend to suffer a lot from aliasing when drawn
    on orientation different to 0 - 90 - 270 degrees. Compare current
    text rendering performance with original 2D example at [.wGOOD2014]
    from which this code is inspired.
  - Check why when using parallel projection z depth dimension should be
    inverted (currently a bug is hidden here).
  - Text proportion calculation is inefficient since creates a set of
    never used textures.
*/
public class JoglPercentageWheelWidgetRenderer {
    private static final HashMap<String, RGBAImage>characterSpritesSmall;
    private static final TextRenderer joglTextRendererSmall;
    private static final TextRenderer joglTextRendererBig;
    private static final int fontSize = 20;

    static {
        characterSpritesSmall = new HashMap<String, RGBAImage>();
        joglTextRendererSmall = new TextRenderer(new Font("Arial", Font.PLAIN, fontSize), true, true);
        joglTextRendererBig = new TextRenderer(new Font("Arial", Font.PLAIN, fontSize*3), true, true);
    }
    
    private static void
    drawCharJOGL(GL2 gl, String key, TextRenderer r, ColorRgb c, Vector3D pos, double scale)
    {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        
        r.begin3DRendering();
        r.setColor((float)c.r, (float)c.g, (float)c.b, 1.0f); // Recuerda RGB son los tres primeros
        r.draw3D(key, 0.0f, 0.0f, 0.0f, 0.05f); // La cadena y la posicion
        r.endRendering();
        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }
    
    private static double
    calculateTextWidth(String key)
    {
        RGBAImage img;
        double dx = 0.0;

        if ( !characterSpritesSmall.containsKey(key) ) {
            img = AwtSystem.calculateLabelImage(
                key, new ColorRgb(1.0, 1.0, 1.0), fontSize);
            characterSpritesSmall.put(key, img);
        }

        if ( characterSpritesSmall.containsKey(key) ) {
            img = characterSpritesSmall.get(key);

            dx = (double)img.getXSize()/(double)img.getYSize();
        }
        return dx;
    }
    
    /*
    private static double
    drawChar(GL2 gl, String key)
    {
        RGBAImage img;
        double dx = 0.0;

        if ( !characterSpritesSmall.containsKey(key) ) {
            img = AwtSystem.calculateLabelImage(
                key, new ColorRgb(1.0, 1.0, 1.0), fontSize);
            characterSpritesSmall.put(key, img);
        }

        if ( characterSpritesSmall.containsKey(key) ) {
            img = characterSpritesSmall.get(key);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            JoglImageRenderer.activate(gl, img);

            dx = (double)img.getXSize()/(double)img.getYSize();

            gl.glBegin(GL2.GL_QUADS);
                gl.glNormal3d(0, 0, 1);
                gl.glColor3d(1, 1, 1);

                gl.glTexCoord2d(0, 0);
                gl.glVertex3d(0, 0, 0);

                gl.glTexCoord2d(1, 0);
                gl.glVertex3d(dx, 0, 0);

                gl.glTexCoord2d(1, 1);
                gl.glVertex3d(dx, 1, 0);

                gl.glTexCoord2d(0, 1);
                gl.glVertex3d(0, 1, 0);
            gl.glEnd();
        }
        return dx;
    }
    */

    /**
    Draws widget on current OpenGL context.
    @param gl OpenGL/JOGL context
    @param widget Widget to be drawn
    */
    public static void draw(GL2 gl, PercentageWheelWidget widget)
    {
        // OpenGL state setup
        gl.glPushMatrix();

        Vector3D pos = widget.getPosition();
        double s = widget.getScale();
        
        gl.glPushAttrib(GL2.GL_DEPTH_BITS | GL2.GL_TEXTURE_2D );
        gl.glLoadIdentity();
        gl.glTranslated(pos.x, pos.y, pos.z);
        gl.glScaled(s, s, 1);
        
        drawInternal(gl, widget);
        
        // End
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPopAttrib();
        
        gl.glPopMatrix();
    }

    private static void drawInternal(GL2 gl, PercentageWheelWidget widget) {
        Vector3D pos = widget.getPosition();
        double s = widget.getScale();
                
        gl.glDisable(GL2.GL_TEXTURE_2D);

        gl.glLineWidth((float)1.0);
        //gl.glDisable(GL2.GL_DEPTH_TEST);
        //gl.glDepthFunc(GL2.GL_LEQUAL);
        //
        int i;
        double angle;
        double da;
        double delta;
        ArrayList<VisualDoubleVariable> dataset;
        delta = 360.0 / (widget.getAproximationSteps());
        dataset = widget.getDataset().getDoubles();
        da = 360.0 / ((double)dataset.size());
        // Layer 1: Draw border
        for ( i = 0, angle = 0.0; i < dataset.size(); i++, angle += da ) {
            ColorRgb c = widget.getBorderBackgroundColor();
            if ( i == widget.getSelectedSector() ) {
                c = widget.getBorderSelectedColor();
            }
            else if ( i == widget.getHighligtedSector() ) {
                c = widget.getBorderHighlightedColor();
            }
            drawSector(gl, angle, angle + da,
                    widget.getOuterRadius(),
                    widget.getOuterRadius() + widget.getBorderWidth(),
                    1.0, delta, c);    
        }
        
        double deltaZ = 0.001;
        
        // Layer 1: Draw background
        for ( i = 0, angle = 0.0; i < dataset.size(); i++, angle += da ) {
            drawSector(gl, angle, angle + da, 0.0,
                    widget.getOuterRadius(), 1.0, delta,
                    widget.getWheelBackgroundColor());    
        }
        // Layer 2: Draw data
        gl.glTranslated(0, 0, deltaZ);
        for ( i = 0, angle = 0.0; i < dataset.size(); i++, angle += da ) {
            ColorRgb c = widget.getSectorForegroundColor();
            if ( i == widget.getSelectedSector() ) {
                c = widget.getSelectedSectorColor();
            }
            else if ( i == widget.getHighligtedSector() ) {
                c = widget.getHighlightedSectorColor();
            }
            drawSector(gl, angle, angle + da,
                    widget.getInnerRadius(),
                    widget.getOuterRadius(),
                    dataset.get(i).getCurrentValue(), delta, c);    
        }
        // Layer 3: Draw scale lines
        gl.glTranslated(0, 0, deltaZ);
        double p;
        double r;
        for ( p = 0; p < 1.0 - VSDK.EPSILON; p += 0.1 ) {
            r = widget.getInnerRadius() +
                    p * (widget.getOuterRadius()-widget.getInnerRadius());
            drawCircle(gl, delta, r, widget.getTrackLineColor());             
        }
        // Layer 4: Draw external border
        gl.glTranslated(0, 0, deltaZ);
        drawTrack(gl,
                delta,
                widget.getOuterRadius(),
                widget.getOuterRadius() + widget.getBorderWidth() / 5.0,
                widget.getSectorLineColor());
        double x, y, r1, r2;
        r1 = widget.getInnerRadius();
        r2 = widget.getOuterRadius() + widget.getBorderWidth();
        ColorRgb c = widget.getSectorLineColor();
        gl.glColor3d(c.r, c.g, c.b);
        gl.glBegin(GL2.GL_LINES);
        for ( i = 0, angle = 0.0;
                i < dataset.size();
                i++, angle += da ) {
            x = r1*Math.cos(Math.toRadians(angle));
            y = r1*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);

            x = r2*Math.cos(Math.toRadians(angle));
            y = r2*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
        }
        gl.glEnd();
        // Layer 4: Draw texts

        for ( i = 0, angle = 0.0; i < dataset.size(); i++, angle += da ) {
            gl.glLoadIdentity();
            gl.glTranslated(pos.x, pos.y, pos.z);
            gl.glScaled(s, s, s);

            drawSectorText(
                    gl,
                    dataset.get(i).getName(),
                    angle,
                    angle + da,
                    widget.getOuterRadius(),
                    widget.getBorderWidth(),
                    deltaZ,
                    pos, s);
        }
        // Draw title in center
        
        double scale = 0.1 * widget.getInnerRadius();
        double xx;
        double yy;
        gl.glPushMatrix();
        
        gl.glLoadIdentity();
        gl.glTranslated(pos.x, pos.y, pos.z);
        gl.glScaled(s, s, s);

        
        if ( widget.getSelectedSector() == -1 ) {
            xx = -widget.getInnerRadius() + 0.05;
            yy = -0.025;
            gl.glTranslated(xx, yy, 4 * deltaZ);
            gl.glScaled(scale, scale, 1);
            drawCharJOGL(gl, widget.getDefaultTitle(), joglTextRendererBig,
                    new ColorRgb(1, 1, 1), pos, s);
        }
        else {
            double val;
            xx = -widget.getInnerRadius() + 0.13;
            yy = 0.065;

            int index = widget.getSelectedSector();
            val = widget.getDataset().getDoubles().get(index).getCurrentValue()*100.0;
            String msg = "" + VSDK.formatDouble(val, 1) + "%";
            
            gl.glTranslated(xx, yy, 4 * deltaZ);
            gl.glScaled(scale, scale, 1);
            drawCharJOGL(gl, msg, joglTextRendererBig, new ColorRgb(1, 1, 1), pos, s);
            
            xx = -widget.getInnerRadius() + 0.05;
            yy = 0.02;
            scale = 0.1 * widget.getInnerRadius() * 2;

            msg = widget.getDataset().getDoubles().get(index).getDescription();
            StringTokenizer parser = new StringTokenizer(msg, "\n");
            while ( parser.hasMoreElements() ) {
                String token = parser.nextToken();
                
                gl.glPushMatrix();
                gl.glTranslated(xx, yy, 4 * deltaZ);
                gl.glScaled(scale, scale, 1);
                drawCharJOGL(gl, token, joglTextRendererSmall,
                        new ColorRgb(1, 1, 1), pos, s);
                gl.glPopMatrix();
                yy -= 0.05;
            }  
        }
        gl.glPopMatrix();
    }

    private static void drawCircle(GL2 gl, double delta, double r, 
        ColorRgb lineColor) {
        double angle;
        double x;
        double y;
        gl.glColor3d(lineColor.r, lineColor.g, lineColor.b);
        gl.glBegin(GL2.GL_LINE_LOOP);
        for ( angle = 0.0; angle < 360.0 - VSDK.EPSILON; angle += delta ) {
            x = r*Math.cos(Math.toRadians(angle));
            y = r*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
        }
        gl.glEnd();
    }

    private static void drawTrack(
        GL2 gl, 
        double delta, 
        double innerRadius, 
        double outerRadius, 
        ColorRgb sectorBackgroundColor) {
        double angle;
        double x;
        double y;
        
        gl.glColor3d(sectorBackgroundColor.r, sectorBackgroundColor.g, sectorBackgroundColor.b);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for ( angle = 0.0; angle < 360.0 + VSDK.EPSILON; angle += delta ) {
            x = innerRadius*Math.cos(Math.toRadians(angle));
            y = innerRadius*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
            
            x = outerRadius*Math.cos(Math.toRadians(angle));
            y = outerRadius*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
        }
        gl.glEnd();
    }

    private static void drawSector(GL2 gl, double startAngle, double endAngle, 
        double innerRadius, double outerRadius, double currentValue,
        double delta, ColorRgb backgroundColor) {
        double x;
        double y;
        double angle;
        double dataRadius;
        
        if ( currentValue < 0.0 ) {
            currentValue = 0.0;
        }
        if ( currentValue > 1.0 ) {
            currentValue = 1.0;
        }
        
        dataRadius = innerRadius + currentValue*(outerRadius-innerRadius);
        
        gl.glColor3d(backgroundColor.r, backgroundColor.g, backgroundColor.b);
        gl.glBegin(GL2.GL_QUAD_STRIP);
        for ( angle = startAngle; angle < endAngle + VSDK.EPSILON; angle += delta ) {
            x = innerRadius*Math.cos(Math.toRadians(angle));
            y = innerRadius*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
            
            x = dataRadius*Math.cos(Math.toRadians(angle));
            y = dataRadius*Math.sin(Math.toRadians(angle));
            gl.glVertex3d(x, y, 0.0);
        }
        gl.glEnd();

    }

    private static void drawSectorText(
        GL2 gl, 
        String msg,
        double startAngle,
        double endAngle,
        double outerRadius,
        double borderWidth,
        double deltaZ,
        Vector3D pos,
        double ss) {
        
        String key;
        int i;
        double x;
        double y;
        double scale;
        double angle;
        double da;
        double r;
        double widthProportions[];
        double totalWidth;

        scale = borderWidth * 0.8;
        r = outerRadius + borderWidth/3;
        da = (endAngle-startAngle);
        widthProportions = new double[msg.length()];
        totalWidth = 0;
        
        // Calculate relative glyph width proportions
        for ( i = 0; i < msg.length(); i++ ) {
            widthProportions[i] = calculateTextWidth("" + msg.charAt(i));
            totalWidth = totalWidth + widthProportions[i];
        }
        for ( i = 0; i < msg.length(); i++ ) {
            widthProportions[i] /= totalWidth;
        }

        // Draw bitmmaped glyphs
        for ( i = 0, angle = endAngle; i < msg.length(); i++ ) {
            x = r*Math.cos(Math.toRadians(angle));
            y = r*Math.sin(Math.toRadians(angle));

            
            gl.glPushMatrix();
            gl.glTranslated(x, y, 4*deltaZ);
            gl.glRotated(90 - angle, 0, 0, -1);
            gl.glScaled(scale, scale, 1);

            key = "" + msg.charAt(i);
            //drawChar(gl, key);
            drawCharJOGL(gl, key, joglTextRendererSmall, new ColorRgb(0, 0, 0), pos, ss);

            gl.glPopMatrix();
            angle -= da*widthProportions[i];
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
