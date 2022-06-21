package com.hossein.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;


public class Compass implements SensorEventListener {

    public interface CompassListener {
        void onNewAzimuth(float azimuth);
    }

    private CompassListener listener;

    private SensorManager sensorManager;
    private Sensor gsensor;
    private Sensor msensor;
    private Sensor osensor;
    private Sensor grsensor;

    private float[] mOrintation = new float[3];
    private float[] mGameRotate = new float[3];
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] R = new float[9];
    private float[] I = new float[9];

    private float azimuth;
    private float mPitchDegrees;
    private float mRollDegrees;

    private float azimuthFix;

    boolean combineGravityMagnetic = false;

    public Compass(Context context) {
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        osensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        grsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    public void start() {
        boolean accler = sensorManager.registerListener(this, gsensor,
                SensorManager.SENSOR_DELAY_GAME);
        boolean magnetic = sensorManager.registerListener(this, msensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, osensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, grsensor,
                SensorManager.SENSOR_DELAY_GAME);
        if (accler && magnetic) {
            combineGravityMagnetic = true;
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setAzimuthFix(float fix) {
        azimuthFix = fix;
    }

    public void resetAzimuthFix() {
        setAzimuthFix(0);
    }

    public void setListener(CompassListener l) {
        listener = l;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (combineGravityMagnetic) {
                Log.i("TypeeeSensor", "onSensorChanged: TYPE_ACCELEROMETER or TYPE_MAGNETIC_FIELD");
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                            * event.values[0];
                    mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                            * event.values[1];
                    mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                            * event.values[2];

                    // mGravity = event.values;

                    // Log.e(TAG, Float.toString(mGravity[0]));
                }
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // mGeomagnetic = event.values;

                    mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                            * event.values[0];
                    mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                            * event.values[1];
                    mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                            * event.values[2];
                    // Log.e(TAG, Float.toString(event.values[0]));

                }

                boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                        mGeomagnetic);


                if (success) {

                    SensorManager.getOrientation(R, mOrintation);
                    // Log.d(TAG, "azimuth (rad): " + azimuth);
                    azimuth = (float) Math.toDegrees(mOrintation[0]); // orientation

                    // Orientation Handling
                    manageOrientation(mOrintation);


                    azimuth = (azimuth + azimuthFix + 360) % 360;
                    // Log.d(TAG, "azimuth (deg): " + azimuth);
                    if (listener != null) {
                        listener.onNewAzimuth(azimuth);
                    }
                }

            } else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                Log.i("TypeeeSensor", "onSensorChanged: TYPE_ORIENTATION");

                mOrintation[0] = alpha * mOrintation[0] + (1 - alpha)
                        * event.values[0];
                mOrintation[1] = alpha * mOrintation[1] + (1 - alpha)
                        * event.values[1];
                mOrintation[2] = alpha * mOrintation[2] + (1 - alpha)
                        * event.values[2];


                azimuth = (float) Math.toDegrees(mOrintation[0]);

                // Orientation Handling
                manageOrientation(mOrintation);
                azimuth = (azimuth + azimuthFix + 360) % 360;
                if (listener != null) {
                    listener.onNewAzimuth(azimuth);
                }


            } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                Log.i("TypeeeSensor", "onSensorChanged: TYPE_GAME_ROTATION_VECTOR");

                // SetUp Smooth state
                mGameRotate[0] = alpha * mGameRotate[0] + (1 - alpha)
                        * event.values[0];
                mGameRotate[1] = alpha * mGameRotate[1] + (1 - alpha)
                        * event.values[1];
                mGameRotate[2] = alpha * mGameRotate[2] + (1 - alpha)
                        * event.values[2];


//                // Convert the rotation-vector to a 4x4 matrix.
                SensorManager.getRotationMatrixFromVector(R, event.values);
                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Y, R);
                SensorManager.getOrientation(R, mOrintation);

                azimuth = (float) Math.toDegrees(mOrintation[0]); // orientation

                azimuth = azimuth - 45;


                // Orientation Handling
                manageOrientation(mOrintation);
                azimuth = (azimuth + azimuthFix + 360) % 360;

                if (listener != null) {
                    listener.onNewAzimuth(azimuth);
                }

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void manageOrientation(float[] orientation) {
        final int screenRotation = (((WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()).getRotation();

        if (screenRotation == Surface.ROTATION_0) {
            mPitchDegrees = (float) Math.toDegrees(orientation[1]);
            mRollDegrees = (float) Math.toDegrees(orientation[2]);
            if (mRollDegrees >= 90 || mRollDegrees <= -90) {
                azimuth += 180;
                mPitchDegrees = mPitchDegrees > 0 ? 180 - mPitchDegrees : -180 - mPitchDegrees;
                mRollDegrees = mRollDegrees > 0 ? 180 - mRollDegrees : -180 - mRollDegrees;
            }
        } else if (screenRotation == Surface.ROTATION_90) {
            azimuth += 90;
            mPitchDegrees = (float) Math.toDegrees(orientation[2]);
            mRollDegrees = (float) -Math.toDegrees(orientation[1]);
        } else if (screenRotation == Surface.ROTATION_180) {
            azimuth += 180;
            mPitchDegrees = (float) -Math.toDegrees(orientation[1]);
            mRollDegrees = (float) -Math.toDegrees(orientation[2]);
            if (mRollDegrees >= 90 || mRollDegrees <= -90) {
                azimuth += 180;
                mPitchDegrees = mPitchDegrees > 0 ? 180 - mPitchDegrees : -180 - mPitchDegrees;
                mRollDegrees = mRollDegrees > 0 ? 180 - mRollDegrees : -180 - mRollDegrees;
            }
        } else if (screenRotation == Surface.ROTATION_270) {
            azimuth += 270;
            mPitchDegrees = (float) -Math.toDegrees(orientation[2]);
            mRollDegrees = (float) Math.toDegrees(orientation[1]);
        }
    }
}
