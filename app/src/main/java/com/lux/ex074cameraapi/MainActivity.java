package com.lux.ex074cameraapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.view.video.OutputFileOptions;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    //CameraX JetPack Library - Camera API 를 쉽게 다루기 위한 새로운 라이브러리
    //cameraX Library 부터 적용 - Build.gradle [개발자 사이트 참고]

    //프리뷰 보여주는 뷰
    PreviewView pvv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1. 카메라, 오디오 녹음(동영상 촬영할 때 필요), 외부저장소에 대한 동적 퍼미션
        String[] permissions=new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (checkSelfPermission(permissions[0])== PackageManager.PERMISSION_DENIED){
            requestPermissions(permissions,0);
        }else {
            //프리뷰 시작
            startPreview();
        }

        //2. 프리뷰를 보여주는 뷰
        pvv=findViewById(R.id.pvv);

        //3. 이미지 캡쳐버튼 반응
        findViewById(R.id.fab).setOnClickListener(view -> captureImage());
        //이미지 캡쳐 기능을 가진 객체 생성
        imageCapture=new ImageCapture.Builder().build();
    }

    //프리뷰 시작하는 기능 메소드
    void startPreview(){
        //카메라 기능 사용 요청 및 가능여부 리스너
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture=ProcessCameraProvider.getInstance(this);

        //카메라 사용가능여부 리스너에게 리스너를 추가
        cameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {


                try {
                    //카메라 기능 제공자 객체 소환
                    ProcessCameraProvider cameraProvider=cameraProviderListenableFuture.get();

                    //프리뷰 객체 생성 및 Surface(서피스) 라는 고속버퍼 뷰 설정
                    Preview preview=new Preview.Builder().build();
                    preview.setSurfaceProvider(pvv.getSurfaceProvider());

                    //기존에 카메라 제공자와 연결된 것들이 있을수도 있기에
                    cameraProvider.unbindAll(); //모든 연결을 제거

                  //카메라 종류 선택
                    CameraSelector cameraSelector=CameraSelector.DEFAULT_BACK_CAMERA;

                    //카메라 제공자에게 preview 연결
                    //cameraProvider.bindToLifecycle(MainActivity.this,cameraSelector,preview);

                    //3. 이미지캡쳐와 카메라도 같이 연결
                    cameraProvider.bindToLifecycle(MainActivity.this,cameraSelector,preview,imageCapture);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }, ContextCompat.getMainExecutor(this)); //MainThread의 능력이 있어야 Ui 변경작업을 할 수 있음.
    }

    //퍼미션 요청 결과 반응하기

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==0){
            //퍼미션 중에 하나라도 허가하지 않으면 미리보기 안 되도록
            for (int result:grantResults){
                if (result==PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this, "카메라 api를 사용 불가\n 이 앱을 종료합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;  //이 메소드를 종료
                }
            }//for
            Toast.makeText(this, "카메라 api 사용 가능", Toast.LENGTH_SHORT).show();
            startPreview();
        }//if
    }
    //이미지 캡쳐 기능 객체 참조변수
    ImageCapture imageCapture;
    //3. 이미지 캡쳐 기능 메소드  - android 12(api 31)버전에서는 이미지 캡쳐 안됨, 구글링 or 다음 버전업 튜토리얼 참고
    void captureImage(){
        //캡쳐한 이미지를 저장할 경로와 파일명
        File path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName="Image_"+sdf.format(new Date())+".jpg";
        File file=new File(path,fileName);

        ImageCapture.OutputFileOptions outputFileOptions=new ImageCapture.OutputFileOptions.Builder(file).build();

        //위에서 만든 옵션경로의 현재 프리뷰의 이미자데이터를 캡쳐하여 저장
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Toast.makeText(MainActivity.this, "saved", Toast.LENGTH_SHORT).show();

                //이미지뷰에 설정
                ImageView iv=findViewById(R.id.iv);
                //Glide.with(MainActivity.this).load(file).into(iv);

                //저장된 경로를 uri로 가져오기
                Uri savedUri=outputFileResults.getSavedUri();
                Glide.with(MainActivity.this).load(savedUri).into(iv);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(MainActivity.this, ""+exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}