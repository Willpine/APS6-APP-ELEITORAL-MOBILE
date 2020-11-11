package com.willpine.aps6;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.osgi.OpenCVNativeLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    private CameraBridgeViewBase mOpenCvCameraView;
    FaceRecognizer recognizer;
    private double w, h;
    CascadeClassifier cascade;
    int[] labels = new int[]{1};
    Mat frame, frame_gray;
    MatOfRect faces ;
    Rect[] facesArray;
    Mat mRgba;
    //MatOfByte mem = new MatOfByte();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    try {
                        initializeOpenCVDependencies();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        w = width;
        h = height;
    }

    public void onCameraViewStopped() {
        mRgba.release();;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
    }



    private void initializeOpenCVDependencies() throws IOException {
        mOpenCvCameraView.enableView();
        recognizer = LBPHFaceRecognizer.create();
        InputStream stream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
        File cascDir = getDir("cascade", Context.MODE_PRIVATE);
        File haarFile = new File(cascDir, "haarcascade_frontalface_alt2.xml");

        FileOutputStream outStream = new FileOutputStream(haarFile);
        byte[] buffer = new byte[4096];
        int bytesRead;

        while((bytesRead = stream.read(buffer)) != -1)
        {
            outStream.write(buffer,0,bytesRead);
        }
        stream.close();
        outStream.close();

        cascade = new CascadeClassifier(haarFile.getAbsolutePath());

        MatOfRect faces = new MatOfRect();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return recognize(inputFrame);
        //return inputFrame.rgba();
    }

    public Mat recognize(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        faces = new MatOfRect();
        mRgba = inputFrame.gray();
        //Imgproc.cvtColor(inputFrame.rgba(), frame_gray, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.equalizeHist(frame_gray, frame_gray);
        //cascade.detectMultiScale(mRgba, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, new Size(30, 30), new Size(w, h) );
        cascade.detectMultiScale(mRgba,faces,1.5,2);
        //cascade.detectMultiScale(mRgba,faces);

        for(Rect rect : faces.toArray()){
            Imgproc.rectangle(mRgba,new Point(rect.x,rect.y),new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,0,0));
        }

        Log.d("OpenCV", "FACES: " + faces.toArray().length);

        return mRgba;
    }
}