package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

import static android.os.Environment.getExternalStorageDirectory;

public class CheckingPhoto extends Activity {

    ImageView imageView;
    Button yes, cancel;

    Handler h = new Handler();

    MyCameraPreview myCameraPreview;

    private static final String[] LABELS = {"냉면", "크림파스타", "마카롱", "마라탕", "돈까스",
            "쌀국수", "연어초밥", "만두", "토마토파스타", "수플레"};

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check);


        myCameraPreview = new MyCameraPreview(this);

        String photoName = myCameraPreview.returnTime();

        Log.i("사진이름2: ", photoName);

        imageView = (ImageView) findViewById(R.id.photos);
        String name = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Foodiary/" + photoName + ".jpg";

        Log.d("이름2", name);
        try {
            File imgFile = new File(name);
            Log.d("경로2: ", imgFile.getAbsolutePath());
            Log.d("있냐?", Boolean.toString(imgFile.exists()));

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
        final Intent intent = new Intent(this, MainActivity.class);
        cancel = (Button)findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                startActivity(intent);
            }
        });
        yes = (Button)findViewById(R.id.yes);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivityForResult();
            }
        });
    }
}
