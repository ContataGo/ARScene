package com.contata.FlaskClient.Uploader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.webkit.CookieManager.getInstance;

public class MainActivity extends AppCompatActivity  {
    private static final Pattern IP_ADDRESS
            = Pattern.compile(  //IP格式
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    final int SELECT_MULTIPLE_FILES = 1;
    ArrayList<String> selectedfilesPaths; // 文件路径string.
    boolean filesSelected = false; // 文件选择flag.
    private Button button;
    ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //权限
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 2);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
        setContentView(R.layout.activity_main);

        findViewById(R.id.recVideo).setOnClickListener(new View.OnClickListener() {
            //调用系统相机
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(intent, 8);
            }
        });

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new  View.OnClickListener(){
            //ARActivity按键
            @Override
            public void onClick(View v){
                openARActivity();
            }
        });

        findViewById(R.id.downloadServer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView responseText = findViewById(R.id.responseText);

                //获取文本框内容
                EditText ipv4AddressView = findViewById(R.id.IPAddress);
                String ipv4Address = ipv4AddressView.getText().toString();
                EditText portNumberView = findViewById(R.id.portNumber);
                String portNumber = portNumberView.getText().toString();
                EditText modelNameView = findViewById(R.id.modelName);
                String modelName = modelNameView.getText().toString();

                //检查IP合法性
                Matcher matcher = IP_ADDRESS.matcher(ipv4Address);
                if (!matcher.matches()) {
                    responseText.setText("无效IP地址，请重新输入！");
                    return;
                }
                //组装下载链接
                String downloadUrl = "http://" + ipv4Address + ":" + portNumber + "/get-models/"+ modelName;
                responseText.setText("从http://" + ipv4Address + ":" + portNumber + "/处下载" + modelName );

                //downloadmanager下载器
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
                String title = URLUtil.guessFileName(downloadUrl,null,null);
                request.setTitle(title);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title);

                DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                downloadManager.enqueue(request);

            }
        });

    }

    public void openARActivity(){
        //开启ARActivity
        Intent intent = new Intent(this, ARActivity.class);
        startActivity(intent);
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //权限检查反馈
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 //   Toast.makeText(getApplicationContext(), "访问存储成功！", Toast.LENGTH_SHORT).show();
                } else {
                 //   Toast.makeText(getApplicationContext(), "访问存储失败，请检查！", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 //   Toast.makeText(getApplicationContext(), "连接网络成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "未连接到互联网，请检查网络设置与连接情况！", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void connectServer(View v) {
        //上传按键反馈
        TextView responseText = findViewById(R.id.responseText);
        if (!filesSelected) {
            responseText.setText("请选择至少一个要上传的视频文件");
            return;
        }
        responseText.setText("正在上传，请稍后...");

        //获取文本框内容
        EditText ipv4AddressView = findViewById(R.id.IPAddress);
        String ipv4Address = ipv4AddressView.getText().toString();
        EditText portNumberView = findViewById(R.id.portNumber);
        String portNumber = portNumberView.getText().toString();

        //检查IP合法性
        Matcher matcher = IP_ADDRESS.matcher(ipv4Address);
        if (!matcher.matches()) {
            responseText.setText("无效IP地址，请重新输入！");
            return;
        }

        //组装上传链接
        String postUrl = "http://" + ipv4Address + ":" + portNumber + "/";

        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        InputStream io = null;
        //ByteArrayOutputStream out = null;

        //文件路径获取
        String s = "" + selectedfilesPaths;
        s = s.replaceAll("\\[", "");
        s = s.replaceAll("\\]", "");

        //文件转byte[]
        File file = new File(s);
        int size = Integer.parseInt(String.valueOf(file.length()));
        byte[] buff = new byte[size];
        try {
            //URL取文件
            URL url = new URL("file://" + s);
            URLConnection conn = url.openConnection();
            io = new FileInputStream(file); // added
            //io = conn.getInputStream();
            //out = new ByteArrayOutputStream();
            //int byteWritten = 0;
            //int byteCount = 0;
            //out.write(buff, byteWritten, byteCount);
            io.read(buff, 0, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //组装文件名
        multipartBodyBuilder.addFormDataPart("file", "Android_Flask_" + ".mp4", RequestBody.create(MediaType.parse("video/*mp4"), buff));
        RequestBody postBodyfile = multipartBodyBuilder.build();
        //调用上传函数
        postRequest(postUrl, postBodyfile);
    }


    void postRequest(String postUrl, RequestBody postBody) {
        //上传函数

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 失败时取消上传
                call.cancel();
                Log.d("FAIL", e.getMessage());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        responseText.setText("服务器连接失败，请重试！.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // 成功链接与反馈
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        try {
                            responseText.setText("服务器说：" + response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void selectFile(View v) {
        //文件浏览
        Intent intent = new Intent();
        intent.setType("* /*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "选择一个视频"), SELECT_MULTIPLE_FILES);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //获取真实路径
        if (resultCode == RESULT_OK && requestCode == 8){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            TextView textView = new TextView(this);
        }
        try {
            if (requestCode == SELECT_MULTIPLE_FILES && resultCode == RESULT_OK && null != data) {
                String currentfilePath;
                selectedfilesPaths = new ArrayList<>();
                TextView numSelectedFiles = findViewById(R.id.numSelectedFiles);
                if (data.getData() != null) {
                    Uri uri = data.getData();

                    currentfilePath = getPath(getApplicationContext(), uri);
                    Log.d("fileDetails", "Single file URI : " + uri);
                    Log.d("fileDetails", "Single file Path : " + currentfilePath);
                    selectedfilesPaths.add(currentfilePath);
                    filesSelected = true;
                    numSelectedFiles.setText("已选中文件数量; " + selectedfilesPaths.size());
                } else {
                   if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {

                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();

                            currentfilePath = getPath(getApplicationContext(), uri);
                            selectedfilesPaths.add(currentfilePath);
                            Log.d("fileDetails", "file URI " + i + " = " + uri);
                            Log.d("fileDetails", "file Path " + i + " = " + currentfilePath);
                            filesSelected = true;
                            numSelectedFiles.setText("已选中文件数量; " + selectedfilesPaths.size());
                        }
                    }
                }
            } else {
                Toast.makeText(this, "你还没有选择任何文件.", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getApplicationContext(), selectedfilesPaths.size() + " 个文件已选择", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "啊哦！有什么东西出错了！请重试", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(final Context context, final Uri uri) {
        //从provider接口获取路径
        final boolean sdkVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (sdkVersion && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("file".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


}



