package com.willpine.aps6;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

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

// Atividade principal - Iniciada assim que o App é iniciado
// Implementa Interface que nos permite acessar e manipular a câmera física através do OpenCV
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Inicializador estático dos Módulos do OpenCV
    static {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    private CameraBridgeViewBase mOpenCvCameraView; // Representa a câmera física
    FaceRecognizer recognizer; // Reconhecedor de Faces
    private double w, h; // Tamanho do frame que será capturado
    CascadeClassifier cascade; // Detector de faces no frame - o Classifier
    int[] labels,links; // Labels(id) e links de cada candidato
    double[] confidence; // Valor que será retornado a cada reconhecimento feito. Quanto menor, mais precisa foi a detecção
    MatOfRect faces ; // Faces detectadas pelo Classifier
    Rect[] facesArray; // Array de faces na forma de retângulos
    Mat mRgba; // Representa uma Matriz da imagem colorida capturada
    String[] candidatos; // Lista com nomes de candidatos
    TextView txtCandidato,linkCandidato; // Texto que aparece na tela do app
    int label; // id de um candidato

    // Inicializa de maneira Assíncrona a câmera e as variáveis utilizadas no reconhecimento
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
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

    // Chamado assim que o app é iniciado
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        txtCandidato = findViewById(R.id.txtCandidato);// Instância do nome do candidato na tela

        linkCandidato = findViewById(R.id.linkCandidato);//Instância do link da página do candidato na tela
        linkCandidato.setClickable(true);
        linkCandidato.setMovementMethod(LinkMovementMethod.getInstance());

        // Torna a câmera visível
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
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


    // Inicializa variáveis de maneira assícrona - Chamada no CallBack
    private void initializeOpenCVDependencies() throws IOException {
        labels = new int[]{0};
        confidence = new double[]{0};
        candidatos = new String[]{"Arthur", "Bruno Covas", "Celso"};
        links= new int[]{R.string.arthur_link,R.string.bruno_link,R.string.celso_link};
        mOpenCvCameraView.enableView();
        // Inicialização do Recognizer
        recognizer = LBPHFaceRecognizer.create(1,8,8,8,1.7976931348623157e+308);

        // Leitura e instanciamento do Recognizer e do Classifier
        InputStream stream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
        InputStream streamTrei = getResources().openRawResource(R.raw.trainner2);
        File cascDir = getDir("cascade", Context.MODE_PRIVATE);
        File haarFile = new File(cascDir, "haarcascade_frontalface_alt2.xml");
        File treiFile = new File(cascDir, "trainner2.yml");

        FileOutputStream outStreamHaar = new FileOutputStream(haarFile);
        FileOutputStream outStreamTrei = new FileOutputStream(treiFile);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while((bytesRead = stream.read(buffer)) != -1)
        {
            outStreamHaar.write(buffer,0,bytesRead);
        }

        while((bytesRead = streamTrei.read(buffer)) != -1)
        {
            outStreamTrei.write(buffer,0,bytesRead);
        }
        stream.close();
        outStreamHaar.close();
        outStreamTrei.close();

        cascade = new CascadeClassifier(haarFile.getAbsolutePath());
        recognizer.read(treiFile.getAbsolutePath()); // Lê o treinamento da parte 1
        MatOfRect faces = new MatOfRect();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return recognize(inputFrame);
        //return inputFrame.rgba();
    }

    // Reconhece faces, atualiza o nome e links e devolve frame retângulo nas faces detectadas
    public Mat recognize(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        faces = new MatOfRect();
        mRgba = inputFrame.gray(); // Tornamos o frame analisado em GrayScale
        cascade.detectMultiScale(mRgba,faces,1.2,5);

        // Para cada face, desenha-se um retângulo ao redor da região detectada pelo Classifier
        for(Rect rect : faces.toArray()){
            Imgproc.rectangle(mRgba,new Point(rect.x,rect.y),new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,0,0));
            // Momento do reconhecimento
            // Ao ser chamado, este método atualiza as variáveis de labels e confiança
            // Se as chamarmos, elas estarão com os valores do candidato reconhecido
            recognizer.predict(mRgba.submat(rect),labels,confidence);

            label = labels[0];

            if(confidence[0]<=50) {
                Log.d("conf", "CONF1: " + confidence[0] + " LABEL1: " + candidatos[labels[0]]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtCandidato.setText(candidatos[label]);
                        linkCandidato.setText(links[label]);
                    }
                });
            }
        }

        return mRgba;
    }
}