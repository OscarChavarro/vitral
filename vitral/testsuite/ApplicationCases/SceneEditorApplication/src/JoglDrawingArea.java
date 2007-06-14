//===========================================================================
//===========================================================================

// JDK Basic classes
import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayList;

// AWT/Swing classes
import java.awt.Cursor;
import java.awt.Dimension;
//import java.awt.Image; // Do not define! conflicts with VSDK's Image
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglArrowRenderer;
import vsdk.toolkit.render.jogl.JoglTranslateGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglRotateGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglScaleGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglZBufferRenderer;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.gui.RotateGizmo;
import vsdk.toolkit.gui.ScaleGizmo;
import vsdk.toolkit.io.image.ImagePersistence;

public class JoglDrawingArea implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
    KeyListener
{
    public static final int CAMERA_INTERACTION_MODE = 1;
    public static final int SELECT_INTERACTION_MODE = 2;
    public static final int TRANSLATE_INTERACTION_MODE = 3;
    public static final int ROTATE_INTERACTION_MODE = 4;
    public static final int SCALE_INTERACTION_MODE = 5;

    public GLCanvas canvas;

    private RendererConfiguration qualitySelection;
    private RendererConfiguration qualitySelectionVisualDebug;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    private TranslateGizmo translationGizmo;
    private RotateGizmo rotateGizmo;
    private ScaleGizmo scaleGizmo;
    private Arrow visualDebugRayGizmo;

    private Scene theScene;
    private JLabel statusMessage;

    public int interactionMode;

    public boolean wantToGetColor;
    public boolean wantToGetDepth;
    public boolean wantToGetContourns;

    private Cursor camrotateCursor;
    private Cursor camtranslateCursor;
    private Cursor camadvanceCursor;
    private Cursor selectCursor;

    //
    private static final int RENDER_MODE_ZBUFFER = 1;
    private static final int RENDER_MODE_RAYTRACING = 2;
    private int renderMode;
    private boolean viewportFullSize;
    private boolean viewportResizeNeeded;
    private int viewportXpos;
    private int viewportYpos;
    private int viewportXsize;
    private int viewportYsize;
    private int viewportXframe;
    private int viewportYframe;

    SceneEditorApplication parent;

    public JoglDrawingArea(Scene theScene, JLabel statusMessage, SceneEditorApplication parent)
    {
        this.parent = parent;
        this.theScene = theScene;
        this.statusMessage = statusMessage;
        this.viewportResizeNeeded = false;
        this.viewportFullSize = true;
        this.viewportXpos = 0;
        this.viewportYpos = 0;
        this.viewportXsize = 0;
        this.viewportYsize = 0;
        this.viewportXframe = 0;
        this.viewportYframe = 0;
        this.renderMode = RENDER_MODE_ZBUFFER;

        interactionMode = CAMERA_INTERACTION_MODE;

        createCursors();

        //cameraController = new CameraControllerBlender(theScene.camera);
        cameraController = new CameraControllerAquynza(theScene.camera);
        translationGizmo = new TranslateGizmo(theScene.camera);

        qualitySelection = parent.theScene.qualityTemplate;
        qualityController = new RendererConfigurationController(qualitySelection);
        qualitySelectionVisualDebug = new RendererConfiguration();
        qualitySelectionVisualDebug.setShadingType(
            qualitySelectionVisualDebug.SHADING_TYPE_GOURAUD);
        rotateGizmo = new RotateGizmo();
        scaleGizmo = new ScaleGizmo();

        visualDebugRayGizmo = new Arrow(1, 0.4, 0.05, 0.1);

        canvas = new GLCanvas();

        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        wantToGetColor = false;
        wantToGetDepth = false;
        wantToGetContourns = false;
    }

    private void createCursors()
    {
      Toolkit awtToolkit = Toolkit.getDefaultToolkit();
      java.awt.Image i;

      i = awtToolkit.getImage("./etc/cursors/cursor_camrotate.gif");
      camrotateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraRotation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camtranslate.gif");
      camtranslateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraTranslation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camadvance.gif");
      camadvanceCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraAdvance");

      selectCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    }

    public void rotateBackground()
    {
        theScene.selectedBackground++;
        if ( theScene.selectedBackground > 2 ) {
            theScene.selectedBackground = 0;
        }
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    private void drawGizmos(GL gl)
    {
        // Pending: Turn off scene light and turn on gizmo specific lighting

        translationGizmo.setCamera(theScene.activeCamera);

        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        if ( interactionMode == TRANSLATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);

              Matrix4x4 composed;

              position = gi.getPosition();
              composed = new Matrix4x4(gi.getRotation());
              composed.M[0][3] = position.x;
              composed.M[1][3] = position.y;
              composed.M[2][3] = position.z;
              translationGizmo.setTransformationMatrix(composed);

              JoglTranslateGizmoRenderer.draw(gl, translationGizmo);
            }
        }
        else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);

              position = gi.getPosition();
              rotateGizmo.setTransformationMatrix(gi.getRotation());
              JoglRotateGizmoRenderer.draw(gl, rotateGizmo, position);
            }
        }
        else if ( interactionMode == SCALE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);

              position = gi.getPosition();
              scaleGizmo.setTransformationMatrix(gi.getRotation());
              JoglScaleGizmoRenderer.draw(gl, scaleGizmo, position);
            }
        }
        gl.glEnable(gl.GL_DEPTH_TEST);
    }

    private void copyColorBufferIfNeeded(GL gl)
    {
        if ( wantToGetColor ) {
            parent.zbufferImage = JoglRGBImageRenderer.getImageJOGL(gl);
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.zbufferImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.zbufferImage);
            }
            parent.imageControlWindow.redrawImage();
            parent.statusMessage.setText("ZBuffer Color Image obtained!");
            wantToGetColor = false;
        }
    }
    private void copyZBufferIfNeeded(GL gl)
    {
        if ( wantToGetDepth ) {
        if ( wantToGetContourns ) {
        IndexedColorImage zbuffer;
        NormalMap nm;
        zbuffer = JoglZBufferRenderer.importJOGLZBuffer(gl).exportIndexedColorImage();
        nm = new NormalMap();
        nm.importBumpMap(zbuffer, new Vector3D(1, 1, 0.1));
                parent.zbufferImage = nm.exportToRgbImageGradient();
          }
          else {
                parent.zbufferImage =
                    JoglZBufferRenderer.importJOGLZBuffer(gl).exportRGBImage(
                        parent.palette);
        }

            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.zbufferImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.zbufferImage);
            }
            parent.imageControlWindow.redrawImage();
            parent.statusMessage.setText("ZBuffer depth map obtained!");
            wantToGetDepth = false;
            wantToGetContourns = false;
        }
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        //-----------------------------------------------------------------
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        if ( viewportResizeNeeded ) {
            if ( viewportFullSize ) {
                gl.glViewport(viewportXpos, viewportYpos,
                              viewportXsize, viewportYsize); 
                theScene.activeCamera.updateViewportResize(
                    viewportXsize, viewportYsize);
                viewportResizeNeeded = false;
              }
              else {
                int w, h;

                if ( viewportXframe < viewportXsize ) {
                    w = viewportXframe;
                }
                else {
                    w = viewportXsize;
                }
                if ( viewportYframe < viewportYsize ) {
                    h = viewportYframe;
                }
                else {
                    h = viewportYsize;
                }
                viewportXpos = (viewportXsize - w) / 2;
                viewportYpos = (viewportYsize - h) / 2;
                gl.glViewport(viewportXpos, viewportYpos, w, h);
                theScene.activeCamera.updateViewportResize(w, h);
                viewportResizeNeeded = false;
            }
        }

        //-----------------------------------------------------------------
        if ( renderMode == RENDER_MODE_ZBUFFER ) {
            JoglSceneRenderer.draw(gl, theScene);
        }
        else {
            JoglSceneRenderer.drawBackground(gl, theScene);
            if ( viewportFullSize ) {
                parent.raytracedImageWidth = viewportXsize;
                parent.raytracedImageHeight = viewportYsize;
            }
            else {
                parent.raytracedImageWidth = viewportXframe;
                parent.raytracedImageHeight = viewportYframe;
            }

        parent.doRaytracedImage();
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            JoglImageRenderer.draw(gl, parent.raytracedImage);
            gl.glPopMatrix();
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(gl.GL_MODELVIEW);
        }

        //-----------------------------------------------------------------
        drawVisualRayDebug(gl);

        //-----------------------------------------------------------------
        // Note that gizmo information will not be reported, as they damage
        // the zbuffer...
        copyZBufferIfNeeded(gl);

        // Must be the last to draw
        drawGizmos(gl);

        copyColorBufferIfNeeded(gl);
    }

    private void drawVisualRayDebug(GL gl)
    {
        if ( !parent.withVisualDebugRay ) {
            return;
        }
        gl.glEnable(gl.GL_LIGHTING);
        gl.glPushMatrix();
        gl.glTranslated(parent.visualDebugRay.origin.x, parent.visualDebugRay.origin.y, parent.visualDebugRay.origin.z);
        Matrix4x4 R = new Matrix4x4();
        double yaw, pitch;
        yaw = parent.visualDebugRay.direction.obtainSphericalThetaAngle();
        pitch = parent.visualDebugRay.direction.obtainSphericalPhiAngle();
        R.eulerAnglesRotation(Math.toRadians(180)+yaw, pitch, 0);
        JoglMatrixRenderer.activate(gl, R);
        JoglArrowRenderer.draw(gl, visualDebugRayGizmo, theScene.camera, qualitySelectionVisualDebug);
        gl.glPopMatrix();
    }


    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        this.viewportXpos = 0;
        this.viewportYpos = 0;
        this.viewportXsize = width;
        this.viewportYsize = height;
        this.viewportResizeNeeded = true;
    }   

  public void mouseEntered(MouseEvent e) {
      canvas.requestFocusInWindow();

      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }
  }

  public void mouseExited(MouseEvent e) {
      //System.out.println("Mouse exited");
  }

  public void mousePressed(MouseEvent e) {
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      int m = e.getModifiersEx();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           (m & e.BUTTON1_DOWN_MASK) != 0 ) {
          canvas.setCursor(camrotateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON2_DOWN_MASK) != 0 ) {
          canvas.setCursor(camtranslateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON3_DOWN_MASK) != 0 ) {
          canvas.setCursor(camadvanceCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMousePressedEventAwt(e) ) {
          canvas.repaint();
        }
        else if ( interactionMode == SELECT_INTERACTION_MODE ||
                  interactionMode == TRANSLATE_INTERACTION_MODE || 
                  interactionMode == ROTATE_INTERACTION_MODE || 
                  interactionMode == SCALE_INTERACTION_MODE 
                 ) {
          boolean composite = false;
          if ( ((e.getModifiersEx()) & e.CTRL_DOWN_MASK) != 0x0 ) {
              composite = true;
          }
          int f = theScene.selectedThings.firstSelected();

          if ( viewportFullSize ) {
              theScene.selectObjectWithMouse(e.getX(), e.getY(), composite);
          }
          else {
              theScene.selectObjectWithMouse(e.getX()-viewportXpos,
                                             e.getY()-viewportXpos, composite);
          }

          if ( f >= 0 && theScene.selectedThings.firstSelected() < 0 &&
               interactionMode == TRANSLATE_INTERACTION_MODE &&
               translationGizmo.isActive() ) {
              theScene.selectedThings.select(f);
          }
          reportObjectSelection();
          canvas.repaint();
      }
  }

  public void mouseReleased(MouseEvent e) {
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded

      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseReleasedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.things.get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseReleasedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }

  }

  public void mouseClicked(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseClickedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.things.get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseClickedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  public void mouseMoved(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseMovedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.things.get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseMovedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  public void mouseDragged(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseDraggedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.things.get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseDraggedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  /**
  WARNING: It is not working... check pending
  */
  public void mouseWheelMoved(MouseWheelEvent e) {
      System.out.println(".");
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseWheelEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void keyPressed(KeyEvent e) {
      char unicode_id;
      int keycode;
      boolean skipKey = false;

      unicode_id = e.getKeyChar();
      keycode = e.getKeyCode();

      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processKeyPressedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == SELECT_INTERACTION_MODE ) {
          if ( unicode_id == e.CHAR_UNDEFINED ) {
            switch ( keycode ) {
              case KeyEvent.VK_LEFT:
        if ( theScene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
                    theScene.selectedThings.selectPrevious();
        }
        if ( theScene.selectedThings.numberOfSelections() < 1 ) {
            theScene.selectedDebugThingGroups.selectPrevious();
        }
                reportObjectSelection();
                break;
              case KeyEvent.VK_RIGHT:
        if ( theScene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
                    theScene.selectedThings.selectNext();
        }
        if ( theScene.selectedThings.numberOfSelections() < 1 ) {
            theScene.selectedDebugThingGroups.selectNext();
        }
                reportObjectSelection();
                break;
            }
            canvas.repaint();
          }
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE ) {
          if ( firstThingSelected >= 0 ) {
              Matrix4x4 composed;
              Vector3D position;
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);

              position = gi.getPosition();
              composed = new Matrix4x4(gi.getRotation());
              composed.M[0][3] = position.x;
              composed.M[1][3] = position.y;
              composed.M[2][3] = position.z;

              translationGizmo.setTransformationMatrix(composed);
              if ( translationGizmo.processKeyPressedEventAwt(e) ) {
                  composed = translationGizmo.getTransformationMatrix();
                  position.x = composed.M[0][3];
                  position.y = composed.M[1][3];
                  position.z = composed.M[2][3];
                  composed.M[0][3] = 0;
                  composed.M[1][3] = 0;
                  composed.M[2][3] = 0;
                  applyTransformToSelectedObjects(position, composed);
                  canvas.repaint();
              }
          }
      }
      else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
          if ( firstThingSelected >= 0 ) {
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);
              Matrix4x4 R = gi.getRotation();

              rotateGizmo.setTransformationMatrix(R);

              if ( rotateGizmo.processKeyPressedEventAwt(e) ) {
                  R = rotateGizmo.getTransformationMatrix();
                  gi.setRotation(R);
                  Matrix4x4 Ri = new Matrix4x4(R);
                  Ri.invert();
                  gi.setRotationInverse(Ri);
                  canvas.repaint();
              }
          }
      }
      else if ( interactionMode == SCALE_INTERACTION_MODE ) {
          if ( firstThingSelected >= 0 ) {
              SimpleBody gi;

              gi = theScene.things.get(firstThingSelected);
              Vector3D s = gi.getScale();
              Matrix4x4 S = new Matrix4x4();
              S.M[0][0] = s.x;
              S.M[1][1] = s.y;
              S.M[2][2] = s.z;

              scaleGizmo.setTransformationMatrix(S);

              if ( scaleGizmo.processKeyPressedEventAwt(e) ) {
                  S = scaleGizmo.getTransformationMatrix();
                  s = new Vector3D(S.M[0][0], S.M[1][1], S.M[2][2]);
                  gi.setScale(s);
                  canvas.repaint();
              }
          }
      }

      // Global commands
      if ( keycode == KeyEvent.VK_ESCAPE ) System.exit(0);

      if ( qualityController.processKeyPressedEventAwt(e) ) {
          System.out.println(qualitySelection);
          canvas.repaint();
      }

      if ( keycode == KeyEvent.VK_DELETE ) {
          int  i;

      //-----------------------------------------------------------------
          for ( i = theScene.things.size()-1; i >= 0; i-- ) {
              if ( theScene.selectedThings.isSelected(i) ) {
                  theScene.things.remove(i);
              }
          }
          theScene.selectedThings.sync();
      //-----------------------------------------------------------------
          for ( i = theScene.debugThingGroups.size()-1; i >= 0; i-- ) {
              if ( theScene.selectedDebugThingGroups.isSelected(i) ) {
                  theScene.debugThingGroups.remove(i);
              }
          }
          theScene.selectedThings.sync();
      //-----------------------------------------------------------------
          canvas.repaint();
      }

      if ( keycode == KeyEvent.VK_F10 ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_COMPUTING_RAYTRACING"));
        parent.doRaytracedImage();

            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.raytracedImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.raytracedImage);
            }
            parent.imageControlWindow.redrawImage();
      }

      if ( keycode == KeyEvent.VK_0 ) {
          // Alphanumeric 0
          skipKey = true;
          switch ( viewportXframe ) {
            case 0:
              viewportXframe = 320;
              viewportYframe = 240;
              viewportFullSize = false;
              break;
            case 320:
              viewportXframe = 640;
              viewportYframe = 480;
              viewportFullSize = false;
              break;
            case 640:
              viewportXframe = 800;
              viewportYframe = 600;
              viewportFullSize = false;
              break;
            case 800:
              viewportXframe = 0;
              viewportYframe = 0;
              viewportFullSize = true;
              break;
          }
          if ( viewportFullSize ) {
              viewportXpos = 0;
              viewportYpos = 0;
          }
          viewportResizeNeeded = true;
          canvas.repaint();
      }

      if ( keycode == KeyEvent.VK_9 ) {
          // Alphanumeric 0
          skipKey = true;
          switch ( renderMode ) {
            case RENDER_MODE_ZBUFFER:
              renderMode = RENDER_MODE_RAYTRACING;
              break;
            default:
              renderMode = RENDER_MODE_ZBUFFER;
              break;
          }
          canvas.repaint();
      }

      double theta = 0;
      double phi = Math.PI/2;

      if ( unicode_id != e.CHAR_UNDEFINED && !skipKey ) {
          switch ( unicode_id ) {
            //- Visual debug ray control ---------------------------------
            case '4': // Numpad 4
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.x -= 0.1;
                  canvas.repaint();
              }
              break;
            case '6': // Numpad 6
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.x += 0.1;
                  canvas.repaint();
              }
              break;
            case '8': // Numpad 8
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.y += 0.1;
                  canvas.repaint();
              }
              break;
            case '2': // Numpad 2
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.y -= 0.1;
                  canvas.repaint();
              }
              break;
            case '1': // Numpad 1
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.z -= 0.1;
                  canvas.repaint();
              }
              break;
            case '7': // Numpad 7
              if ( parent.withVisualDebugRay ) {
                  parent.visualDebugRay.origin.z += 0.1;
                  canvas.repaint();
              }
              break;
            case '5': // Numpad 5
              if ( parent.withVisualDebugRay ) {
                  parent.withVisualDebugRay = false;
              }
              else {
                  parent.withVisualDebugRay = true;
              }
              canvas.repaint();
              break;
            case '*': // Numpad *
              if ( parent.withVisualDebugRay ) {
                  theta =
                   parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                  phi =
                   parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                  theta -= Math.toRadians(15);
                  parent.visualDebugRay.direction.setSphericalCoordinates(
                   1, theta, phi);
          System.out.printf("Tetha: %.2f, Phi: %.2f\n", 
                    Math.toDegrees(theta), Math.toDegrees(phi));
                  canvas.repaint();
              }
              break;
            case '/': // Numpad /
              if ( parent.withVisualDebugRay ) {
                  theta =
                   parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                  phi =
                   parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                  theta += Math.toRadians(15);
                  parent.visualDebugRay.direction.setSphericalCoordinates(
                   1, theta, phi);
          System.out.printf("Tetha: %.2f, Phi: %.2f\n", 
                    Math.toDegrees(theta), Math.toDegrees(phi));
                  canvas.repaint();
              }
              break;
            case '+': // Numpad +
              if ( parent.withVisualDebugRay ) {
                  theta =
                   parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                  phi =
                   parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                  phi += Math.toRadians(15);
                  if ( phi > Math.PI ) phi = Math.PI;
                  parent.visualDebugRay.direction.setSphericalCoordinates(
                   1, theta, phi);
                  canvas.repaint();
              }
              break;
            case '-': // Numpad -
              if ( parent.withVisualDebugRay ) {
                  theta =
                   parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                  phi =
                   parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                  phi -= Math.toRadians(15);
                  if ( phi < 0 ) phi = 0;
                  parent.visualDebugRay.direction.setSphericalCoordinates(
                   1, theta, phi);
                  canvas.repaint();
              }
              break;
            //------------------------------------------------------------

            case 't':
              if ( firstThingSelected >= 0 ) {
                  SimpleBody gi;
                  Image texture;
                  gi = theScene.things.get(firstThingSelected);
                  texture = gi.getTexture();
                  if ( texture == null ) {
                      String imageFilename = "../../../etc/textures/miniearth.png";
                      try {
                          texture = 
                           ImagePersistence.importRGB(new File(imageFilename));
                      }
                      catch ( Exception ee ) {}
                      gi.setTexture(texture);
                  }
                  else {
                      gi.setTexture(null);
                  }
              }
              canvas.repaint();
              break;
            case 'b':
              if ( firstThingSelected >= 0 ) {
                  SimpleBody gi;
                  IndexedColorImage source = null;
                  NormalMap normalMap;
                  RGBImage exported;
                  gi = theScene.things.get(firstThingSelected);

                  normalMap = gi.getNormalMap();
                  if ( normalMap == null ) {
                      try {
                          normalMap = new NormalMap();
                          //String imageFilename = "../../../etc/bumpmaps/blinn2.bw";
                          String imageFilename = "../../../etc/bumpmaps/earth.bw";
                          source = ImagePersistence.importIndexedColor(new File(imageFilename));
                          normalMap.importBumpMap(source, new Vector3D(1, 1, 0.2));

                          exported = normalMap.exportToRgbImage();
                          //ImagePersistence.exportPPM(new File("./outputmap.ppm"), exported);
                      }
                      catch ( Exception ee ) {
                          System.err.println(ee);
                          ee.printStackTrace();
                      }
                      gi.setNormalMap(normalMap);
                    }
                    else {
                      gi.setNormalMap(null);
                  }
              }
              canvas.repaint();
              break;
            case 'h':
              //-------------------------------------------------------------
              if ( parent.selectorDialog == null ) {
                  parent.selectorDialog = new SwingSelectorDialog();
              }
              parent.selectorDialog.setVisible(true);
              parent.selectorDialog.repaint();
              //-------------------------------------------------------------


              SimpleBody o;
              int i;
              ArrayList generic = parent.theScene.things;
              String msg = "";

              for ( i = 0; i < generic.size(); i++ ) {
                  System.out.println("Consultando cosa " + i + ":");
                  o = (SimpleBody)generic.get(i);
                  try {
                      Method m = o.getClass().getMethod("getName", (Class[])null);
                      if ( !(m.getReturnType().isInstance(msg)) ) {
                          throw new Exception("Wrong method signature");
                      }
                      msg = (String)m.invoke(o);
                  }
                  catch ( Exception ee ) {
                      msg = null;
                  }
                  if ( msg == null || msg.equals("") ) {
                      msg = "Not named object";
                  }
                  System.out.println("Object: " + msg);
              }
              break;

            case 'c':
              statusMessage.setText("Camera mode interaction - drag mouse with different buttons over the scene to change current camera.");
              interactionMode = CAMERA_INTERACTION_MODE;
              canvas.repaint();
              break;

            case 'q':
              statusMessage.setText("Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.");
              interactionMode = SELECT_INTERACTION_MODE;
              canvas.repaint();
              break;

            case 'w':
              statusMessage.setText("Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.");
              interactionMode = TRANSLATE_INTERACTION_MODE;
              canvas.repaint();
              break;

            case 'e':
              statusMessage.setText("Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.");
              interactionMode = ROTATE_INTERACTION_MODE;
              canvas.repaint();
              break;

            case 'r':
              statusMessage.setText("Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.");
              interactionMode = SCALE_INTERACTION_MODE;
              canvas.repaint();
              break;

            case 'g':
              if ( theScene.showGrid == true ) {
                  theScene.showGrid = false;
              }
              else {
                  theScene.showGrid = true;
              }
              canvas.repaint();
              break;
          }
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }
  }

  private void applyTransformToSelectedObjects(Vector3D position,
                                               Matrix4x4 rotation)
  {
      SimpleBody gi;
      int firstThingSelected = theScene.selectedThings.firstSelected();
      int i;

      for ( i = 0; i < theScene.selectedThings.size(); i++ ) {
          if ( !theScene.selectedThings.isSelected(i) ) continue;
          gi = theScene.things.get(i);

          gi.setPosition(position);
          gi.setRotation(rotation);
          rotation = new Matrix4x4(rotation);
          rotation.invert();
          gi.setRotationInverse(rotation);
      }
  }

  private void reportObjectSelection()
  {
      String msg = "";
      int n;

      //-----------------------------------------------------------------
      theScene.selectedThings.sync();
      n = theScene.selectedThings.numberOfSelections();
      if ( n == 0 ) {
      msg += "All things are UNSELECTED";
      }
      else if ( n == 1 ) {
          int f = theScene.selectedThings.firstSelected();
          msg = "Thing [" + f + "] selected, which is a [" + 
     ((SimpleBody)(theScene.things.get(f))).getGeometry().getClass().getName() 
          + "]";
      }
      else {
          msg += "" + n + " things selected";
      }

      //-----------------------------------------------------------------
      theScene.selectedDebugThingGroups.sync();
      n = theScene.selectedDebugThingGroups.numberOfSelections();
      if ( n == 0 ) {
      msg += "; All visual debug groups are UNSELECTED";
      }
      else if ( n == 1 ) {
          int f = theScene.selectedDebugThingGroups.firstSelected();
          msg += "; Debug group [" + f + "] selected.";
      }
      else {
          msg += "; " + n + " debug groups selected";
      }

      //-----------------------------------------------------------------
      statusMessage.setText(msg);
  }

  public void keyReleased(KeyEvent e) {
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processKeyReleasedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  /**
  Do NOT call your controller from the `keyTyped` method, or the controller
  will be invoked twice for each key. Call it only from the `keyPressed` and
  `keyReleased` method
  */
  public void keyTyped(KeyEvent e) {
      ;
  }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
