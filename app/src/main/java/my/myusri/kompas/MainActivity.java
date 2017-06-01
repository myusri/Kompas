package my.myusri.kompas;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity
  extends AppCompatActivity
  implements CompassStrategy.Listener, SeekBar.OnSeekBarChangeListener {

  private static final float MAX_ALPHA = 0.95f;
  private double minProjRatio;

  private float lastAlpha;
  private Display display;
  private SeekBar alphaBar;
  private Toast toast;
  private ImageView compass;
  private SharedPreferences prefs;
  private CompassStrategy strategy;
  private float lastDeg;

  private void showMessage(String format, Object ...args) {
    format = String.format(format, args);
    if (toast == null) {
      toast = Toast.makeText(this, format, Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.BOTTOM, 0, 20);
    }
    else toast.setText(format);
    toast.show();
  }

  private void setAlphaBar(float alpha) {
    int max = alphaBar.getMax();
    int progress = Math.round(alpha/MAX_ALPHA * max);
    if (progress > max) progress = max;
    alphaBar.setProgress(progress);
  }

  private float getAlphaBar() {
    float progress = alphaBar.getProgress();
    float max = alphaBar.getMax();
    return MAX_ALPHA * progress/max;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    float alpha = prefs.getFloat("alpha", 0.5f);
    minProjRatio = prefs.getFloat("min_proj_ratio", 0.1f);

    compass = (ImageView) findViewById(R.id.compassView);
    display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
      .getDefaultDisplay();
    SensorManager sensorMgr
      = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    strategy = new RotationCompassStrategy(sensorMgr, this);
    if (strategy.isValid())
      showMessage("Using rotation vector sensor");
    else {
      strategy = new SimpleCompassStrategy(sensorMgr, this);
      if (strategy.isValid())
        showMessage("Using magnetic sensor");
    }
    if (!strategy.isValid())
      showMessage("No sensor to use for compass");

    strategy.setAlpha(alpha);

    lastAlpha = Float.NaN;
    alphaBar = (SeekBar) findViewById(R.id.alphaBar);
    alphaBar.setOnSeekBarChangeListener(this);
    setAlphaBar(alpha);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (strategy.isValid()) {
      lastDeg = Float.NaN;
      strategy.reset();
      strategy.register();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (strategy.isValid())
      strategy.unregister();
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    float alpha = getAlphaBar();
    strategy.setAlpha(alpha);
    prefs.edit().putFloat("alpha", alpha).apply();
    if (!Float.isNaN(lastAlpha) && lastAlpha != alpha)
      showMessage("Filter coefficient: %5.3f", alpha);
    lastAlpha = alpha;
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

  private int nData;

  @Override
  public void newData(float deg, float mx, float my, float mz) {
    final double mx2 = mx*mx;
    final double my2 = my*my;
    final double mz2 = mz*mz;
    final double mv2 = mx2+my2+mz2;
    final double mp2 = mx2+my2;
    final double mag = Math.sqrt(mv2);
    final double projMag = Math.sqrt(mp2);
    final float incl = (float)(180.0*Math.acos(mp2/mag/projMag)/Math.PI);

    // we will do simple projection to xy plane. But if the direction is
    // too much into the z direction, we will prompt the user to make
    // the device less vertical.

    if (projMag/mag < minProjRatio) {
      showMessage("Please keep your device as horizontal as possible");
      return;
    }
    deg = (deg + 360.0f) % 360.0f;
    if (Float.isNaN(lastDeg)) lastDeg = deg;
    else {
      // as we go beyond 360 degrees, we don't reset back to 0 but
      // continue on increasing the degrees. Same when we go below
      // 0 and beyond -360, we will continue decreasing the degrees.
      float delta = smod(deg - lastDeg, 360);
      float contDeg = lastDeg + delta;
      rotateCompass(lastDeg, contDeg);
      if (++nData%4 == 0) {
        showMessage(
          "N %5.1f° INC %5.1f° [%5.2f %5.2f %5.2f]", deg, incl, mx, my, mz);
      }
      lastDeg = contDeg;
    }
  }
}
