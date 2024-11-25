package ups.diego.proyecto_camara;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;


import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

import com.longdo.mjpegviewer.MjpegView;

import java.net.HttpURLConnection;
import java.net.URL;

import ups.diego.proyecto_camara.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'proyecto_camara' library on application startup.
    static {
        System.loadLibrary("proyecto_camara");
    }

    private ActivityMainBinding binding;

    private android.widget.Button btnConectar;
    private android.widget.ImageView original;
    private Handler handler;

    private MjpegView mjpeg;
    private static final long CAPTURE_INTERVAL = 100; // Intervalo de captura en milisegundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        btnConectar = findViewById(R.id.button);
        mjpeg = findViewById(R.id.mjpegview);
        handler = new Handler(getMainLooper());
        original = findViewById(R.id.imageView);

        btnConectar.setOnClickListener(v -> iniciarStream());

        verificarConexion("http://192.168.20.62:81/stream");

        /*btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv.setText(stringButton());
                verificarConexion("http://192.168.221.62:81/stream");
                iniciarStream();
            }
        });*/

    }





    private void verificarConexion(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Conexión exitosa", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error de conexión: " + responseCode, Toast.LENGTH_LONG).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void iniciarStream() {
        mjpeg.setMode(MjpegView.MODE_FIT_WIDTH);
        mjpeg.setAdjustHeight(true);
        mjpeg.setSupportPinchZoomAndPan(true);
        mjpeg.setUrl("http://192.168.20.62:81/stream");
        mjpeg.setRecycleBitmap(true);
        mjpeg.startStream();

        iniciarCapturaDeFrames();
    }

    private void iniciarCapturaDeFrames() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap frame = capturarFrame();
                if (frame != null) {
                    procesarBitmap(frame);
                }
                handler.postDelayed(this, CAPTURE_INTERVAL);
            }
        });
    }

    //private Bitmap capturarFrame() {
        //if (mjpeg.isLaidOut()) {
            //return mjpeg.drawToBitmap();
        //} else {
            //return null; // Si la vista no está lista, retorna null
        //}
    //}

    private Bitmap capturarFrame() {
        mjpeg.setDrawingCacheEnabled(true);
        mjpeg.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(mjpeg.getDrawingCache());
        mjpeg.setDrawingCacheEnabled(false);  // Desactiva para evitar consumir memoria innecesaria
        return bitmap;
    }

    private void procesarBitmap(Bitmap bitmap) {
        Bitmap copia = bitmap.copy(bitmap.getConfig(), true);
        detectorBordes(bitmap,copia);
        original.setImageBitmap(copia);  // Mostrar la imagen capturada
    }

    @Override
    protected void onResume() {
        super.onResume();
        mjpeg.startStream();
        iniciarCapturaDeFrames();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mjpeg.stopStream();
        detenerCapturaDeFrames();
    }

    private void detenerCapturaDeFrames() {
        handler.removeCallbacksAndMessages(null);
    }


    /**
     * A native method that is implemented by the 'proyecto_camara' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native String stringButton();
    public native void detectorBordes(android.graphics.Bitmap in, android.graphics.Bitmap out);
}