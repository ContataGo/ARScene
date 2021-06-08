package com.contata.FlaskClient.Uploader;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class ARActivity extends AppCompatActivity implements View.OnClickListener {

    private ArFragment arFragment;
    private AnchorNode anchorNode;
    private ModelAnimator animator;
    private ModelRenderable animationModel;
    private int nextAnimation;
    private Button button;
    private TransformableNode transformableNode;



    private int i = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        String s = "nathan.sfb";

        //三个按键设置
        Button nathan = findViewById(R.id.nathan);
        Button manuel = findViewById(R.id.manuel);
        Button sophia = findViewById(R.id.sophia);

        nathan.setOnClickListener(this);
        manuel.setOnClickListener(this);
        sophia.setOnClickListener(this);



        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        arFragment.setOnTapArPlaneListener(new BaseArFragment.OnTapArPlaneListener() {
            public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent){
                if (animationModel == null)return;
                //设置点击处为元节点
                Anchor anchor = hitResult.createAnchor();
                if (anchorNode == null){
                    anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    //T-Node可变节点设置
                    transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                    transformableNode.getScaleController().setMinScale(0.3f);
                    transformableNode.setParent(anchorNode);
                    transformableNode.setRenderable(animationModel);
                }
            }
        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {

            }
        });

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(animator == null || !animator.isRunning()){
                    //获取动画数据与播放
                    AnimationData data = animationModel.getAnimationData(nextAnimation);
                    nextAnimation = (nextAnimation+1)%animationModel.getAnimationDataCount();
                    animator = new ModelAnimator(data,animationModel);
                    animator.start();
                }
            }
        });
        // setupModel(s);
    }

    public void onClick(View v) {
        //选择模型
        switch (v.getId()) {
            case R.id.nathan:
                setupModel("nathan.sfb");
                break;
            case R.id.manuel:
                setupModel("manuel.sfb");
                break;
            case R.id.sophia:
                setupModel("sophia.sfb");
                break;

        }
    }

    private void setupModel(String s){
        ModelRenderable.builder()
                //获取模型并渲染
                .setSource(this,Uri.parse(s))
                .build()
                .thenAccept(renderable -> animationModel = renderable)
                .exceptionally(throwable -> {
                    Toast.makeText(this,"" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }
}