package com.newpunch.administrator.how_old;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private ImageView imageView;
    private static final int PICK_CODE = 1;
    private static final int CAMERA_CODE = 2;
    private ImageButton Detect;
    private ImageButton Picture;
    private ImageButton Camera;
    private TextView Tips;
    private View Waitting;
    private String currentPhotoStr;
    private Bitmap photoImg;
    private String filename;
    private Uri imageUri;
    private Paint mPaint;
    private File outputImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvents();
        mPaint = new Paint();
    }

    private void initEvents() {
        ButtonAction.setButtonFocusChanged(Picture);
        ButtonAction.setButtonFocusChanged(Detect);
        ButtonAction.setButtonFocusChanged(Camera);
        Picture.setOnClickListener(this);
        Detect.setOnClickListener(this);
        Camera.setOnClickListener(this);

    }

    private void initViews() {
        Picture = (ImageButton) findViewById(R.id.picture);
        Detect = (ImageButton) findViewById(R.id.detect);
        Camera = (ImageButton) findViewById(R.id.camera);
        imageView = (ImageView) findViewById(R.id.image_view);
        Tips = (TextView) findViewById(R.id.textTip);
        Waitting = findViewById(R.id.waitting);
    }

    private static final int MSG_SUCESS = 0x111;
    private static final int MSG_ERROR = 0x112;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_SUCESS:
                    Waitting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);
                    imageView.setImageBitmap(photoImg);
                    break;
                case  MSG_ERROR:
                    Waitting.setVisibility(View.GONE);
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)){
                        Tips.setText("Error:");
                    }else {
                        Tips.setText(errorMsg);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(photoImg.getWidth(), photoImg.getHeight(),photoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(photoImg, 0, 0, null);
        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            if (faceCount == 0){
                Tips.setText("Error: Could not detect any faces.");
            }
            for(int i = 0; i < faceCount ; i++){
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();
                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);
                // »­Box
                canvas.drawLine(x - w / 2 , y - h / 2 , x - h / 2 , y + h / 2 , mPaint);
                canvas.drawLine(x - w / 2 , y - h / 2 , x + h / 2 , y - h / 2 , mPaint);
                canvas.drawLine(x + w / 2 , y - h / 2 , x + h / 2 , y+h / 2 , mPaint);
                canvas.drawLine(x - w / 2 , y + h / 2 , x + h / 2 , y + h / 2 , mPaint);

                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age , "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();
                if(bitmap.getWidth() < imageView.getWidth() && bitmap.getHeight() < imageView.getHeight()){
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / imageView.getWidth(),bitmap.getHeight() * 1.0f / imageView.getHeight());
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap , (int) (ageWidth * ratio) ,(int) (ageHeight * ratio)  , false);
                }

                canvas.drawBitmap(ageBitmap , x - ageBitmap.getWidth() / 2 , y - h / 2 - ageBitmap.getHeight() , null);

                photoImg = bitmap;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) Waitting.findViewById(R.id.age_gender);
        tv.setText(age + "");
        if(isMale){
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);
        }else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.picture:
                Intent pictures = new Intent(Intent.ACTION_PICK);
                pictures.setType("image/*");
                startActivityForResult(pictures, PICK_CODE);;
                break;
            case R.id.camera:
                File path = Environment.getExternalStorageDirectory();
                outputImage = new File(path, "testImg.jpg");
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage);
                Intent cameras = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameras.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(cameras,CAMERA_CODE);
                break;
            case  R.id.detect:
                Waitting.setVisibility(View.VISIBLE);
                FaceppDetect.detect(photoImg, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getErrorMessage();
                        mHandler.sendMessage(msg);
                    }
                });
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        if(requestCode == PICK_CODE){
            if (data != null){
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                currentPhotoStr = cursor.getString(idx);
                cursor.close();

                resizePhoto();
                imageView.setImageBitmap(photoImg);
            }
        }
        if(requestCode == CAMERA_CODE){
            currentPhotoStr = outputImage.getPath().toString();
            resizePhoto();
            imageView.setImageBitmap(photoImg);

        }
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoStr, options);
        options.inSampleSize = calculateInSampleSize(options,480, 800);
        options.inJustDecodeBounds = false;
        photoImg = BitmapFactory.decodeFile(currentPhotoStr,options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                            int reqHeight){
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if(height > reqHeight || width > reqHeight){
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
}
