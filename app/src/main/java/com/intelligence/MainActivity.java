package com.intelligence;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.graphics.NativeGraphics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

import intelligence.imageanalysis.CarSnapshot;
import intelligence.intelligence.Intelligence;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "ANPR";
    CameraControllerV2WithPreview ccv2WithPreview;
    TextView textView;
    AutoFitTextureView textureView;
    HashSet<String> number;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == 0) {
                textView.setText(number.toString());
            }
        }
    };
    private Intelligence systemLogic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();

        boolean showpreview = intent.getBooleanExtra("showpreview", false);

        textureView = findViewById(R.id.textureview);
        textView = findViewById(R.id.textView);

        ccv2WithPreview = new CameraControllerV2WithPreview(MainActivity.this, textureView);
        ccv2WithPreview.imgAvailableListener = new ImageAvailableListener() {
            @Override
            public void onImageAvailable(Image image) {
                new Thread(new ImageScanner(image)).run();
            }
        };


        findViewById(R.id.getpicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ccv2WithPreview != null) {
                    ccv2WithPreview.takePicture();
                }

                Toast.makeText(getApplicationContext(), "Picture Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        getPermissions();
    }

    public void recognize(byte[] data, int width, int height) throws IOException {
        CarSnapshot c;
        try {
            Log.d(TAG,"w:: " + width + "  h:: " + height);
            c = new CarSnapshot(NativeGraphics.yuvToRGB(data, width, height), 1);
            number = systemLogic.recognize(c);
            Log.d(TAG, "recognized: " + number);

            handler.sendEmptyMessage(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(ccv2WithPreview != null) {
//            ccv2WithPreview.closeCamera();
//        }
//        if(ccv2WithoutPreview != null) {
//            ccv2WithoutPreview.closeCamera();
//        }
    }

    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Requesting permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override //Override from ActivityCompat.OnRequestPermissionsResultCallback Interface
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                }
                return;
            }
        }
    }


    public void onImageAvailable(ImageReader reader) {
        new ImageScanner(reader.acquireLatestImage());
    }

    private class ImageScanner implements Runnable {

        private final Image mImage;


        ImageScanner(Image mImage) {
            this.mImage = mImage;
        }

        @Override
        public void run() {

            if (mImage != null) {

                ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);

                try {
                    recognize(bytes, mImage.getWidth(), mImage.getHeight());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}