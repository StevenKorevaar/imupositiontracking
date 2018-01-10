package xyan.projects.imu_tracking;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import android.view.View;
import android.widget.Button;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;
import android.app.Activity;
import java.lang.Math;
import java.util.Locale;

import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor mRotationSensor, mStepSensor;

    private float[] mGravity = {0, 0, 0}, mGeomagnetic = {0, 0, 0};

    private static final int SENSOR_DELAY = 1000;
    private static final double FROM_RADS_TO_DEGS = 180/Math.PI;
    private double norming;

    private int steps = 0;
    private int pmin = 0, pmax=0;
    private long actualTime = 0;

    float[] rotationMatrix = new float[9];

    //Yaw, Pitch, Roll
    float[] orientation = new float[4];
    float[] quaternion = new float[4];

    private double mStartingAngle;
    private double xdeg;

    private double accel[] = {0,0,0};
    private double accelWorld[] = {0,0,0};

    private int count = 0;
    private int accelCount[] = {0,0};


    private double velocity[] = {0,0,0};
    private double displacement[] = {0,0,0};

    private double posStep[] = {0, 0};

    TextView t;
    Button b;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        t=(TextView)findViewById(R.id.text);
        t.setText("Step One: blast egg");

        b = (Button)findViewById(R.id.reset);

        b.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Set Text on button click via this function.
                //t.setText(" Text Change successfully ");
                posStep[0] = 0;
                posStep[1] = 0;
                steps = 0;

                velocity[0] = 0;
                velocity[1] = 0;
                velocity[2] = 0;

                displacement[0] = 0;
                displacement[1] = 0;
                displacement[2] = 0;

            }
        });

        try {

            mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
            mSensorManager.registerListener(this,mStepSensor,SENSOR_DELAY);

        }
        catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (event.sensor == mRotationSensor) {

            updateRot(event.values);
        }

        if(event.sensor == mStepSensor) {

            updateAcc(event.values);
            norming = Math.sqrt((event.values[0]*event.values[0]) +
                    (event.values[1]*event.values[1])+(event.values[2]*event.values[2]));
            stepCount(norming);
        }
    }


    private void updateRot(float[] vectors) {

        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        SensorManager.getOrientation(rotationMatrix, orientation);

        SensorManager.getQuaternionFromVector(quaternion, orientation);

        xdeg =  orientation[0] * FROM_RADS_TO_DEGS;
    }

    String filename = "myfile";
    FileOutputStream outputStream;

    private static final int NO_MOVEMENT = 10000;
    double lowPassA = 0.0;

    private void updateAcc(float[] vectors) {

        accel[0] = lowPassA * accel[0] + (1 - lowPassA)
                * vectors[0];
        accel[1] = lowPassA * accel[1] + (1 - lowPassA)
                * vectors[1];
        accel[2] = lowPassA * accel[2] + (1 - lowPassA)
                * vectors[2];

        /*
        accel[0] = vectors[0];
        accel[1] = vectors[1];
        accel[2] = vectors[2];
        */
        multiplyRotation(rotationMatrix, accel, accelWorld);

        ++count;

        if(count == 10){
            velocity[0] += accelWorld[0];
            velocity[1] += accelWorld[1];
            //velocity[2] += accelWorld[2];

            //displacement[0] += velocity[0];
            //displacement[1] += velocity[1];
            //displacement[2] += velocity[2];

        }



        roundVector(accelWorld,100);

        if(accelWorld[0] <= 0.2) {
            ++accelCount[0];
        }
        else {
            accelCount[0] = 0;
        }

        if(accelWorld[1] <= 0.2) {
            ++accelCount[1];
        }
        else {
            accelCount[1] = 0;
        }

        if(accelCount[0] > NO_MOVEMENT) {
            velocity[0] = 0;
        }

        if(accelCount[1] > NO_MOVEMENT) {
            velocity[1] = 0;
        }

        if(count == 10){

            displacement[0] += velocity[0];
            displacement[1] += velocity[1];
            //displacement[2] += velocity[2];

            count = 0;
        }

        //roundVector(velocity, 100);
        //roundVector(displacement, 100);

        /*
        accel[0] = vectors[0];
        accel[1] = vectors[1];
        accel[2] = vectors[2];

        for(int i = 0; i < 3; ++i) {
            accelWorld[i] = 0;
            for(int j = 0; j < 3; ++j) {
                accelWorld[i] += accel[i]*quaternion[j];
            }
        }
        */
    }

    private void roundVector(double[] vec, int p) {
        for(int i = 0; i < vec.length; ++i) {
            vec[i] = (double) Math.round(vec[i] * p) / p;
        }
    }

    private void multiplyRotation(float[] rot, double[] vec, double[] ans) {
        ans[0] = rot[0]*vec[0] + rot[1]*vec[1] + rot[2]*vec[2];
        ans[1] = rot[3]*vec[0] + rot[4]*vec[1] + rot[5]*vec[2];
        ans[2] = rot[6]*vec[0] + rot[7]*vec[1] + rot[8]*vec[2];

    }

    public void stepCount (double norming){

        if (norming > 10.403 )
            pmax = 1;

        if (norming < 8.45)
            pmin = 1;

        if (pmax == 1 && pmin == 1) {

            if (steps == 0){
                steps++;
                actualTime = System.currentTimeMillis();
                if(mStartingAngle == 0)
                {
                    mStartingAngle = xdeg;
                }

                //TODO Correct Movement to be actual movements rather than estimated
                //
                posStep[0] = (float) ( posStep[0] - (1*Math.cos(Math.toRadians(xdeg-mStartingAngle))) );
                posStep[1] = (float) ( posStep[1] - (1*Math.sin(Math.toRadians(xdeg-mStartingAngle))) );
                //.newPointAdd((int) (myView.getLastX()-Math.round(93*Math.cos(Math.toRadians(mData.ObjectHandlergetAngle()-mStartingAngle))) ), (int) (myView.getLastY()-Math.round(93*Math.sin(Math.toRadians(mData.ObjectHandlergetAngle()-mStartingAngle)))));

            }

            else {
                if (System.currentTimeMillis() - actualTime > 400) {
                    steps++;
                    actualTime = System.currentTimeMillis();
                    posStep[0] = (float) ( posStep[0] - (1*Math.cos(Math.toRadians(xdeg-mStartingAngle))) );
                    posStep[1] = (float) ( posStep[1] - (1*Math.sin(Math.toRadians(xdeg-mStartingAngle))) );
                    //myView.newPointAdd(xnew,ynew);
                }
            }

            pmin = 0;
            pmax = 0;
        }


        String s;

        s = String.format(Locale.ENGLISH, "Compass Direction:    %f\n",
                xdeg);

        s = String.format(Locale.ENGLISH, "%s\nAcceleration (PHONE REFERENCE):\n" +
                        "       X:     %f\n" +
                        "       Y:     %f\n" +
                        "       Z:     %f" +
                        "\n",
                s, accel[0], accel[1], accel[2]);

        s = String.format(Locale.ENGLISH, "%s\nAcceleration (WORLD REFERENCE):\n" +
                        "       X:     %f       No Movement: %d\n" +
                        "       Y:     %f       No Movement: %d\n" +
                        "       Z:     %f" +
                        "\n",
                s, accelWorld[0], accelCount[0], accelWorld[1], accelCount[1], accelWorld[2]);

        s = String.format(Locale.ENGLISH, "%s\nVelocity (WORLD REFERENCE):\n" +
                        "       X:     %f\n" +
                        "       Y:     %f\n",
                s, velocity[0], velocity[1]);

        s = String.format(Locale.ENGLISH, "%s\nDisplacement (WORLD REFERENCE):\n" +
                        "       X:     %f\n" +
                        "       Y:     %f\n",
                s, displacement[0], displacement[1]);

        s = String.format(Locale.ENGLISH, "%s\nRotation Matrix: \n" +
                        "     [ %f , %f , %f \n" +
                        "       %f , %f , %f \n" +
                        "       %f , %f , %f ]\n",
                s ,rotationMatrix[0], rotationMatrix[1], rotationMatrix[2],
                rotationMatrix[3], rotationMatrix[4], rotationMatrix[5],
                rotationMatrix[6], rotationMatrix[7], rotationMatrix[8]);

        s = String.format(Locale.ENGLISH, "%s\nOrientation Matrix: \n" +
                        "       Azimuth:    %f\n" +
                        "       Pitch:      %f\n" +
                        "       Roll:       %f\n"
                ,s ,orientation[0], orientation[1], orientation[1]);


        s = String.format(Locale.ENGLISH, "%s\nStep Count: %d\n", s, steps);

        s = String.format(Locale.ENGLISH, "%s\nCurrent Position: \n" +
                        "       X:  %f\n" +
                        "       Y:  %f\n"
                ,s , posStep[1], posStep[0]);

        t.setText(s);

    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
