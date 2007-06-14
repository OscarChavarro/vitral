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
import vsdk.toolkit.environment.scene.SimpleBody;
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
    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    private TranslateGizmo translationGizmo;
    private RotateGizmo rotateGizmo;
    private ScaleGizmo scaleGizmo;

    private Scene theScene;
    private JLabel statusMessage;

    public int interactionMode;

    public boolean wantToGetColor;
    public boolean wantToGetDepth;

    private Cursor camrotateCursor;
    private Cursor camtranslateCursor;
    private Cursor camadvanceCursor;
    private Cursor selectCursor;

    SceneEditorApplication parent;

    public JoglDrawingArea(Scene theScene, JLabel statusMessage, SceneEditorApplication parent)
    {
        this.parent = parent;
        this.theScene = theScene;
        this.statusMessage = statusMessage;

        interactionMode = CAMERA_INTERACTION_MODE;

        createCursors();

        //cameraController = new CameraControllerBlender(theScene.camera);
        cameraController = new CameraControllerAquynza(theScene.camera);
        translationGizmo = new TranslateGizmo(theScene.camera);

        qualitySelection = parent.theScene.qualityTemplate;
        qualityController = new RendererConfigurationController(qualitySelection);

        rotateGizmo = new RotateGizmo();
        scaleGizmo = new ScaleGizmo();

        canvas = new GLCanvas();

        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        wantToGetColor = false;
        wantToGetDepth = false;
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
        if ( theScene.selectedBackground > 1 ) {
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
            parent.zbufferImage = JoglZBufferRenderer.importJOGLZBuffer(gl).exportRGBImage(parent.palette);
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.zbufferImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.zbufferImage);
            }
            parent.imageControlWindow.redrawImage();
            parent.statusMessage.setText("ZBuffer depth map obtained!");
            wantToGetDepth = false;
        }
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        JoglSceneRenderer.draw(gl, theScene);

        // Note that gizmo information will not be reported, as they damage
        // the zbuffer...
        copyZBufferIfNeeded(gl);

        // Must be the last to draw
        drawGizmos(gl);

        copyColorBufferIfNeeded(gl);
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
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height); 

        theScene.activeCamera.updateViewportResize(width, height);
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
          theScene.selectObjectWithMouse(e.getX(), e.getY(), composite);
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
                theScene.selectedThings.selectPrevious();
                reportObjectSelection();
                break;
              case KeyEvent.VK_RIGHT:
                theScene.selectedThings.selectNext();
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
          for ( i = theScene.things.size()-1; i >= 0; i-- ) {
              if ( theScene.selectedThings.isSelected(i) ) {
                  theScene.things.remove(i);
              }
          }
          theScene.selectedThings.sync();
          canvas.repaint();
      }

      if ( keycode == KeyEvent.VK_F10 ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_COMPUTING_RAYTRACING"));
            parent.raytracedImage.init(parent.raytracedImageWidth, parent.raytracedImageHeight);
            parent.theScene.raytrace(parent.raytracedImage);
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.raytracedImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.raytracedImage);
            }
            parent.imageControlWindow.redrawImage();
      }

      if ( unicode_id != e.CHAR_UNDEFINED ) {
          switch ( unicode_id ) {
            case 't':
              if ( firstThingSelected >= 0 ) {
                  SimpleBody gi;
                  Image texture;
                  gi = theScene.things.get(firstThingSelected);
                  texture = gi.getTexture();
                  if ( texture == null ) {
                      System.out.println("PONGO TEXTURA");
                      String imageFilename = "../../../etc/images/render.jpg";
                      try {
                          texture = 
                           ImagePersistence.importRGB(new File(imageFilename));
                      }
                      catch ( Exception ee ) {}
                      gi.setTexture(texture);
                  }
                  else {
                      System.out.println("QUITO TEXTURA");
                      gi.setTexture(null);
                  }
              }
              canvas.repaint();
              break;
            case 'b':
              if ( firstThingSelected >= 0 ) {
                  SimpleBody gi;
                  IndexedColorImage source = null;
                  NormalMap normalMap = new NormalMap();
                  RGBImage exported;
                  gi = theScene.things.get(firstThingSelected);

                  try {
                      String imageFilename = "../../../etc/bumpmaps/blinn1.bw";
                      source = ImagePersistence.importIndexedColor(new File(imageFilename));
                      normalMap.importBumpMap(source, new Vector3D(1, 1, 0));
                      exported = normalMap.exportToRgbImage();
                      ImagePersistence.exportJPG(new File("./output.jpg"), exported);
                  }
                  catch ( Exception ee ) {}

                  /*
                  texture = gi.getTexture();
                  if ( texture == null ) {
                      System.out.println("PONGO TEXTURA");
                      String imageFilename = "../../../etc/images/render.jpg";
                      try {
                          texture = 
                           ImagePersistence.importRGB(new File(imageFilename));
                      }
                      catch ( Exception ee ) {}
                      gi.setTexture(texture);
                  }
                  else {
                      System.out.println("QUITO TEXTURA");
                      gi.setTexture(null);
                  }
                  */
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
      theScene.selectedThings.sync();
      int n = theScene.selectedThings.numberOfSelections();
      if ( n == 0 ) {
          statusMessage.setText("All things are UNSELECTED");
      }
      else if ( n == 1 ) {
          int f = theScene.selectedThings.firstSelected();
          statusMessage.setText("Thing [" + f + "] selected, which is a [" + 
     ((SimpleBody)(theScene.things.get(f))).getGeometry().getClass().getName() 
                                + "]");
      }
      else {
          statusMessage.setText(n + " things selected");
      }
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
