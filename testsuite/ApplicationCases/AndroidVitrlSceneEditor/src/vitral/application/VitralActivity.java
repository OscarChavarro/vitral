//===========================================================================

package vitral.application;

// Android packages
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;

// Android packages: GPS API
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.content.Context;

// Vsdk classes
import vsdk.toolkit.gui.AndroidSystem;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.CameraControllerAquynza;

@TargetApi(Build.VERSION_CODES.CUPCAKE) 
public class VitralActivity extends Activity implements LocationListener, GpsStatus.Listener {
    private BasicGLSurfaceView canvas;

    private CameraControllerAquynza cameraController;

    private LocationManager locationManager;
    private String provider;
    private int numberOfLocations;

    //= Basic Activity methods ==================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        canvas = new BasicGLSurfaceView(getApplication());
        setContentView(canvas);

        cameraController = new CameraControllerAquynza(canvas.glExecutor.camera);

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
/*
        switch ( e.getAction() ) {
          case MotionEvent.ACTION_DOWN:            // one touch: drag
            System.out.println("one down");
            break;
          case MotionEvent.ACTION_POINTER_DOWN:    // two touches: zoom
            System.out.println("two down");
            break;
          case MotionEvent.ACTION_UP:              // no mode
            System.out.println("up");
            break;
          case MotionEvent.ACTION_POINTER_UP:      // no mode
            System.out.println("upup");
            break;
          case MotionEvent.ACTION_MOVE:            // rotation
            System.out.println("move");
            break;
        }
*/
        MouseEvent evsdk = AndroidSystem.android2vsdkEvent(e);

        return cameraController.processMouseDraggedEvent(evsdk); // 
    }

    //= Extended methods for GPS ================================================
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
