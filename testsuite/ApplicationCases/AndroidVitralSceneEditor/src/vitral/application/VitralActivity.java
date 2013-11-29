//===========================================================================

package vitral.application;

// Java classes
import java.util.Random;

// Android packages
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;

// Android packages: GUI
import android.view.View;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Gallery.LayoutParams;
import android.view.Gravity;
import android.widget.ImageView.ScaleType;
import android.widget.Button;
import android.view.View.OnClickListener;

// Android packages: GPS API
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.content.Context;

// Vsdk classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.gui.AndroidSystem;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.geometry.Sphere;

@TargetApi(Build.VERSION_CODES.CUPCAKE) 
public class VitralActivity extends Activity 
implements LocationListener, 
GpsStatus.Listener, OnClickListener {
    private BasicGLSurfaceView canvas;

    private CameraControllerAquynza cameraController;

    private LocationManager locationManager;
    private String provider;
    private int numberOfLocations;
    private int numberOfLights;
    private Random randomNumberGenerator;

    private int interaction;
    private int mouseMovementsFromLastDown;

    //= Basic Activity methods =================================================
    private void createGUI()
    {
/*
        TextView label = new TextView(this);
        label.setText("VITRAL Android Application Template");
        label.setTextSize(10);
        label.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView pic = new ImageView(this);
        pic.setImageResource(R.raw.render);  
        pic.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));  
        pic.setAdjustViewBounds(true);  
        pic.setScaleType(ScaleType.FIT_XY);  
        pic.setMaxHeight(250);  
        pic.setMaxWidth(250);  
*/

        canvas = new BasicGLSurfaceView(getApplication());
        //canvas.setLayoutParams(new LayoutParams(
        //    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

/*
        Button b1 = new Button(this);
        b1.setId(1);
        b1.setText("Click me");
        b1.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        b1.setOnClickListener(this);

        Button b2 = new Button(this);
        b2.setId(2);
        b2.setText("Tickle me");
        b2.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        b2.setOnClickListener(this);

        LinearLayout buttonsPanel = new LinearLayout(this);  
        buttonsPanel.setOrientation(LinearLayout.HORIZONTAL);  
        buttonsPanel.setLayoutParams(new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));  
        //buttonsPanel.setGravity(Gravity.CENTER);  
        buttonsPanel.addView(b1);
        buttonsPanel.addView(b2);

        // Organize elements
        LinearLayout mainPanel = new LinearLayout(this);  
        mainPanel.setOrientation(LinearLayout.VERTICAL);  
        mainPanel.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));  
        mainPanel.setGravity(Gravity.CENTER);  
        mainPanel.addView(label);  
        //mainPanel.addView(pic);
        mainPanel.addView(buttonsPanel);
        mainPanel.addView(canvas);  
        setContentView(mainPanel);
*/
        setContentView(canvas);
    }

    @Override
    public void onClick(View v)
    {
        System.out.println("Huy! " + v.getId());
        Toast.makeText(this, "Huy!", Toast.LENGTH_SHORT).show();  
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        randomNumberGenerator = new Random(System.currentTimeMillis());
        interaction = 1;
        mouseMovementsFromLastDown = 0;

        createGUI();

        cameraController = 
            new CameraControllerAquynza(canvas.glExecutor.getCamera());

        // Scene related
        numberOfLights = 1;

        // GPS related
        numberOfLocations = 1;
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getProvider(LocationManager.GPS_PROVIDER).getName();
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if ( location != null ) {
            System.out.println("OnCreate: provider " + provider + " has been selected.");
            onLocationChanged(location);
          }
          else {
            System.out.println("Location not available");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        canvas.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        canvas.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    //= Extended methods for user interface =====================================
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch ( e.getAction() ) {
          case MotionEvent.ACTION_DOWN:            // one touch: drag
            //System.out.println("one down");
	    mouseMovementsFromLastDown = 0;
            break;
          case MotionEvent.ACTION_POINTER_DOWN:    // two touches: zoom
            //System.out.println("two down");
            break;
          case MotionEvent.ACTION_UP:              // no mode
	    mouseMovementsFromLastDown++;
            //System.out.println("up");
            break;
          case MotionEvent.ACTION_POINTER_UP:      // no mode
            //System.out.println("upup");
            break;
          case MotionEvent.ACTION_MOVE:            // rotation
            //System.out.println("move");
	    mouseMovementsFromLastDown += 10;
            break;
        }

        MouseEvent evsdk = AndroidSystem.android2vsdkEvent(e);

        switch ( interaction ) {
          case 1:
	    if ( mouseMovementsFromLastDown > 1 ) { // Drag
                cameraController.processMouseDraggedEvent(evsdk);
	    }
	    else if ( mouseMovementsFromLastDown == 1 ) { // Click
	        canvas.glExecutor.selectObjectWithMouse(evsdk.getX(), evsdk.getY());
	    }
            break;

          case 2:
	    if ( mouseMovementsFromLastDown > 1 ) { // Drag
                cameraController.processMouseDraggedEvent(evsdk);
	    }
	    else if ( mouseMovementsFromLastDown == 1 ) { // Click
	        canvas.glExecutor.insertSphereWithMouse(evsdk.getX(), evsdk.getY());
	    }
            break;

          default:
            return false;
        }
        return true;
    }

    //= Extended methods for GPS ===============================================
    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        System.out.println("[" + numberOfLocations + "] LAT: " + lat + " LON: " + lon);
        numberOfLocations++;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        ;
    }

    @Override
    public void onProviderEnabled(String provider) {
        System.out.println("Enabled new location provider: " + provider);
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if ( location != null ) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
          }
          else {
            System.out.println("(Again) Location not available");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        System.out.println("Enabled new location provider: " + provider);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch ( event ) {
          case GpsStatus.GPS_EVENT_STARTED:
            System.out.println("GPS_EVENT_STARTED");
            break;
          case GpsStatus.GPS_EVENT_STOPPED:
            System.out.println("GPS_EVENT_STOPPED");
            break;
          case GpsStatus.GPS_EVENT_FIRST_FIX:
            System.out.println("GPS_EVENT_FIRST_FIX");
            break;
          case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            System.out.println("GPS_EVENT_SATELLITE_STATUS");
            break;
          default:
            System.out.println("Unknown GPS event");
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu q = menu.addSubMenu(0, 100, 0, "Rendering configuration");
        q.add(0, 0, 0, "Constant shading");
        q.add(0, 1, 0, "Flat shading");
        q.add(0, 2, 0, "Gouraud shading");
        q.add(0, 3, 0, "Phong shading");
        q.add(0, 4, 0, "Toggle texture");
        q.add(0, 5, 0, "Toggle points");
        q.add(0, 6, 0, "Toggle wires");
        q.add(0, 7, 0, "Toggle surfaces");
        q.add(0, 8, 0, "Toggle bounding boxes");
        q.add(0, 9, 0, "Toggle selection corners");
        q.add(0, 10, 0, "Toggle normals");

        SubMenu o = menu.addSubMenu(1, 101, 1, "Object selection");
        o.add(0, 11, 0, "Sphere (low res)");
        o.add(0, 12, 0, "Sphere (high res)");
        o.add(0, 13, 0, "Box");
        o.add(0, 14, 0, "Cone");
        o.add(0, 15, 0, "Mesh Mug");
        o.add(0, 16, 0, "Toggle reference square");

        SubMenu a = menu.addSubMenu(1, 101, 1, "Animation options");
        a.add(0, 17, 0, "Toggle object rotation");
        a.add(0, 18, 0, "Toggle first light rotation");

        SubMenu x = menu.addSubMenu(1, 102, 1, "Scene");
        x.add(0, 19, 0, "Add light");
        x.add(0, 20, 0, "Add sphere");
        x.add(0, 21, 0, "Add 10 spheres");

        SubMenu i = menu.addSubMenu(1, 103, 1, "Interaction");
        i.add(0, 22, 0, "Select objects");
        i.add(0, 23, 0, "Insert spheres");

        return true;
    }

    private Vector3D randomPosition()
    {
        Vector3D p;

        p = new Vector3D();
/*
        p.x = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
        p.y = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
        p.z = (randomNumberGenerator.nextDouble() * 2.0) - 1.0;
*/
        double phi;
        double theta;
        double r;

        phi = (randomNumberGenerator.nextDouble()) * Math.PI;
        theta = (randomNumberGenerator.nextDouble()*2.0) * Math.PI;
        r = (randomNumberGenerator.nextDouble()*0.5) + 1.2;

	p.setSphericalCoordinates(r, theta, phi);

        return p;
    }

    private ColorRgb randomColor()
    {
        ColorRgb c;
        c = new ColorRgb();
        c.r = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        c.g = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        c.b = (randomNumberGenerator.nextDouble() / 2.0) + 0.5;
        return c;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RendererConfiguration q;
        q = canvas.glExecutor.getRendererConfiguration();
        Scene s = canvas.glExecutor.getScene();
        SimpleBody b;

        switch ( item.getItemId() ) {
          case 0:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            break;
          case 1:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);
            break;
          case 2:
            q.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);
            break;
          case 3:
            break;
          case 4:
            q.changeTexture();
            break;
          case 5:
            q.changePoints();
            break;
          case 6:
            q.changeWires();
            break;
          case 7:
            q.changeSurfaces();
            break;
          case 8:
            q.changeBoundingVolume();
            break;
          case 9:
            q.changeSelectionCorners();
            break;
          case 10:
            q.changeNormals();
            break;
          case 11:
            canvas.glExecutor.selectObject(1);
            break;
          case 12:
            canvas.glExecutor.selectObject(2);
            break;
          case 13:
            canvas.glExecutor.selectObject(3);
            break;
          case 14:
            canvas.glExecutor.selectObject(4);
            break;
          case 15:
            canvas.glExecutor.selectObject(5);
            break;
          case 16:
            canvas.glExecutor.toggleReferenceSquare();
            break;
          case 17:
            canvas.glExecutor.toggleObjectRotation();
            break;
          case 18:
            canvas.glExecutor.toggleLightRotation();
            break;
          case 19:
            numberOfLights++;
            canvas.glExecutor.prepareLights(numberOfLights);
            break;
          case 20:
	    b = s.addThing(new Sphere(0.4), randomColor());
            b.setPosition(randomPosition());
            break;
          case 21:
	    for ( int i = 0; i < 10; i++ ) {
   	        b = s.addThing(new Sphere(0.4), randomColor());
                b.setPosition(randomPosition());
	    }
            break;
          case 22:
            interaction = 1;
            break;
          case 23:
            interaction = 2;
            break;
        }
        return true;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
