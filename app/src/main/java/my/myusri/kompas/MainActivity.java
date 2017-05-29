package my.myusri.kompas;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity
  extends AppCompatActivity
  implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

  // let save some battery, shall we
  private static final int    SAMPLING_PERIOD =  500000;
  private static final double MAX_ALPHA = 0.95;

  private double alpha;
  private double minProjRatio;

  private Display display;
  private SeekBar alphaBar;
  private Toast toast;
  private ImageView compass;
  private SensorManager sensorMgr;
  private Sensor magSensor;
  private SharedPreferences prefs;

  private float lastDeg;
  private float lastVals[] = { Float.NaN, Float.NaN, Float.NaN };

  private void showToast(String msg) {
    if (toast == null) toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
    else toast.setText(msg);
    toast.show();
  }

  private void setAlphaBar(double alpha) {
    int max = alphaBar.getMax();
    int progress = (int)Math.round(alpha/MAX_ALPHA * max);
    if (progress > max) progress = max;
    alphaBar.setProgress(progress);
  }

  private double getAlphaBar() {
    double progress = alphaBar.getProgress();
    double max = alphaBar.getMax();
    return MAX_ALPHA * progress/max;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    alpha = prefs.getFloat("alpha", 0.5f);
    minProjRatio = prefs.getFloat("min_proj_ratio", 0.1f);

    alphaBar = (SeekBar) findViewById(R.id.alphaBar);
    alphaBar.setOnSeekBarChangeListener(this);
    setAlphaBar(alpha);

    compass = (ImageView) findViewById(R.id.compassView);
    display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
      .getDefaultDisplay();
    sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    magSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    if (magSensor == null)
      showToast("Your device doesn't seem to have magnetic sensor");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (magSensor != null) {
      // use NaN to signal to restart compass computation/filtering
      lastDeg = lastVals[0] = lastVals[1] = lastVals[2] = Float.NaN;
      sensorMgr.registerListener(this, magSensor, SAMPLING_PERIOD);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    sensorMgr.unregisterListener(this);

  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    alpha = getAlphaBar();
    prefs.edit().putFloat("alpha", (float)alpha).apply();
    showToast(String.format(Locale.US, "Filter coefficient: %5.3f", alpha));
  }
  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  private void animateNeedleAngle(float startDeg, float endDeg) {
    RotateAnimation rot = new RotateAnimation(startDeg, endDeg,
      RotateAnimation.RELATIVE_TO_SELF, 0.5f,
      RotateAnimation.RELATIVE_TO_SELF, 0.5f);
    rot.setDuration(250);
    rot.setInterpolator(this,
      android.R.anim.accelerate_decelerate_interpolator);
    rot.setFillAfter(true);
    compass.startAnimation(rot);
  }
  private void rotateCompass(float startDeg, float endDeg) {
    // need to correct for device rotation
    switch (display.getRotation()) {
      case Surface.ROTATION_0: break;
      case Surface.ROTATION_90:
        startDeg -=  90; endDeg -=  90;
        break;
      case Surface.ROTATION_180:
        startDeg += 180; endDeg += 180;
        break;
      case Surface.ROTATION_270:
        startDeg +=  90; endDeg +=  90;
        break;
    }
    animateNeedleAngle(startDeg, endDeg);
  }

  /** symmetric modulo */
  private static float smod(float x, float m)
  {
    return x-(float)((Math.floor(x/m + 0.5))*m);
  }

  // first order IIR filter:
  //   y0 = (1-a) * x0 + a * y1
  // where,
  //   y0 - filtered output
  //   x0 - raw current value
  //   y1 - last filtered value

  private double filter(double a, double x0, double y1) {
    return (1-a)*x0 + a*y1;
  }

  @Override
  public void onSensorChanged(SensorEvent ev) {

    if (ev.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD)
      return;
    if (ev.accuracy < SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
      showToast("Unreliable reading detected");
    }
    if (Float.isNaN(lastVals[0]))
      System.arraycopy(ev.values, 0, lastVals, 0, 3);

    final double mx = filter(alpha, ev.values[0], lastVals[0]);
    final double my = filter(alpha, ev.values[1], lastVals[1]);
    final double mz = filter(alpha, ev.values[2], lastVals[2]);
    final double mx2 = mx*mx;
    final double my2 = my*my;
    final double mz2 = mz*mz;
    final double mag = Math.sqrt(mx2+my2+mz2);
    final double projMag = Math.sqrt(mx2+my2);

    // we will do simple projection to xy plane. But if the direction is
    // too much into the z direction, we will prompt the user to make
    // the device less vertical.

    if (projMag/mag < minProjRatio) {
      showToast("Please keep your device as horizontal as possible");
      return;
    }
    lastVals[0] = (float)mx;
    lastVals[1] = (float)my;
    lastVals[2] = (float)mz;

    float deg = (float)(180.0*Math.atan2(mx, my)/Math.PI);
    deg = (deg + 360.0f) % 360.0f;
    if (Float.isNaN(lastDeg)) lastDeg = deg;
    else {
      // as we go beyond 360 degrees, we don't reset back to 0 but
      // continue on increasing the degrees.
      float delta = smod(deg - lastDeg, 360);
      deg = lastDeg + delta;
    }
    rotateCompass(lastDeg, deg);
    lastDeg = deg;
  }

  @Override
  public void onAccuracyChanged(Sensor s, int acc) {
  }
}
