package net.myusri.kompas;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity
  extends AppCompatActivity
  implements SensorEventListener, SeekBar.OnSeekBarChangeListener {

  private static final String TAG = "MainActivity";
  private float alpha = 0.4f;
  private Display display;
  private SeekBar alphaBar;
  private TextView alphaPopup;
  private TextView logOutput;
  private ImageView compass;
  private SensorManager sensorMgr;
  private Sensor magSensor;

  // let save some battery, shall we
  private final int SENSOR_DELAY
    = Build.VERSION.SDK_INT < 9 ? SensorManager.SENSOR_DELAY_NORMAL : 500000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(TAG, "onCreate");
    setContentView(R.layout.activity_main);
    alphaBar = (SeekBar) findViewById(R.id.alphaBar);
    alphaPopup = (TextView) findViewById(R.id.alphaPopup);
    logOutput = (TextView) findViewById(R.id.logOutput);
    compass = (ImageView) findViewById(R.id.compassView);
    alphaBar.setOnSeekBarChangeListener(this);
    alphaBar.setProgress((int)(alpha * alphaBar.getMax()));

    display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
      .getDefaultDisplay();
    sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    magSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "onCreate");

  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.i(TAG, "onResume");
    sensorMgr.registerListener(this, magSensor, SENSOR_DELAY);
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.i(TAG, "onStop");
    sensorMgr.unregisterListener(this);

  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    alpha = (float) alphaBar.getProgress() / alphaBar.getMax();
    alphaPopup.setText(String.format(Locale.US, "Alpha: %f", alpha));
  }
  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    alphaPopup.setVisibility(View.VISIBLE);
  }
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    alphaPopup.setVisibility(View.GONE);
  }

  private float lastDeg = 0;
  private void rotateCompass(double deg) {
    // Compass direction runs from 0 to +/-180 degrees. When the direction
    // changes around south direction it may change from positive to negative
    // vice versa. The animation therefore may go the wrong way if not
    // corrected. Correction is made if the change is more than 180 degrees.

    float startDeg = lastDeg;
    float endDeg = (float) deg;
    float diff = endDeg - startDeg;
    if (diff < -180) endDeg = 360 + endDeg; // clockwise
    else if (diff > 180) endDeg = endDeg - 360; // counterclockwise

    // need to correct for device rotation too
    switch (display.getRotation()) {
      case Surface.ROTATION_0: break;
      case Surface.ROTATION_90:  startDeg -=  90; endDeg -=  90; break;
      case Surface.ROTATION_180: startDeg += 180; endDeg += 180; break;
      case Surface.ROTATION_270: startDeg +=  90; endDeg +=  90; break;
    }

    RotateAnimation rot = new RotateAnimation(startDeg, endDeg,
      RotateAnimation.RELATIVE_TO_SELF, 0.5f,
      RotateAnimation.RELATIVE_TO_SELF, 0.5f);
    rot.setDuration(250);
    rot.setInterpolator(this,
      android.R.anim.accelerate_decelerate_interpolator);
    rot.setFillAfter(true);
    compass.startAnimation(rot);
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
    final float mx = ev.values[0];
    final float my = ev.values[1], my2 = my*my;
    final float mz = ev.values[2], mz2 = mz*mz;

    // rotate around x axis into xy plane. After rotation:
    //   x' = x
    //   y' = sgn(y) * sqrt(z*z + y*y)
    //   z' = 0
    // magnitude of the vector stays the same (left as an exercise to the
    // reader). The sign of the y value of the rotated vector follows the
    // sign of the original y value.

    final double yp = Math.copySign(1.0f, my) * Math.sqrt(mz2 + my2);

    // apply filter to compass value
    double deg = 180.0*Math.atan2(mx, yp)/Math.PI;
    deg = filter(alpha, deg, lastDeg);
    rotateCompass(deg);
    lastDeg = (float)deg;
    final String out = String.format(Locale.US,
      "[%6.2f %6.2f %6.2f] deg %6.1f", mx, my, mz, deg);
    logOutput.setText(out);
  }

  @Override
  public void onAccuracyChanged(Sensor s, int acc) {
  }
}
