package com.example.fermentation;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.tensorflow.lite.DataType;

import com.example.fermentation.ml.RfModel;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class Starting extends Activity {
    int imageSize = 100;
    String[] classes = {"Over Fermented", "Under Fermented", "Well Fermented"};
    int maxPos = 0;
    float maxCon = 0;

    Bitmap imageBitMap;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        Button gallery = findViewById(R.id.scanFromGalleryButton);
        gallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });

        Button cam = findViewById(R.id.scanCacao);
        cam.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 101);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            setContentView(R.layout.result);

            switch (requestCode) {
                case 100:
                    handleGalleryResult(data);
                    break;
                case 101:
                    handleCameraResult(data);
                    break;
            }
        }
    }

    private void handleGalleryResult(Intent data) {
        if (data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                        data.getData());
                handleBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showToast("Failed to get image from gallery");
        }
    }

    private void handleCameraResult(Intent data) {
        try {
            Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            handleBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBitmap(Bitmap bitmap) {
        imageBitMap = bitmap;

        if (bitmap != null) {
            ImageView imageView = findViewById(R.id.imageResult);
            imageView.setImageBitmap(bitmap);
            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

            bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
            classifyImage(bitmap);
        }
    }

    @SuppressLint("Range")
    public void classifyImage(Bitmap bitmap) {
        try {
            maxPos = 0;
            maxCon = 0;
            RfModel model = RfModel.newInstance(getApplicationContext());

            // Create TensorBuffer for KNN model (10000 features)
            int numFeatures = imageSize * imageSize;
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, numFeatures}, DataType.FLOAT32);
            float[] featureData = new float[numFeatures];
            extractImageFeatures(bitmap, featureData);

            // Assuming you're using a method to extract features, like flattening the image or extracting statistics
            float sum = 0;
            for (float val : featureData) {
                sum += val;
            }
            Log.d("ModelInput", "Feature data sum: " + sum + ", avg: " + (sum/featureData.length));


            // Load the extracted features into the TensorBuffer
            inputFeature0.loadArray(featureData);

            // Debug check to verify TensorBuffer contains the correct data
            float[] checkArray = inputFeature0.getFloatArray();
            float checkSum = 0;
            for (float val : checkArray) {
                checkSum += val;
            }
            Log.d("ModelInput", "TensorBuffer data sum after loading: " + checkSum);

            RfModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] confidence = outputFeature0.getFloatArray();

            // finding the highest confidence
            for (int i = 0; i < confidence.length; i++) {
                Log.d("Confidence", "Element at index " + i + ": " + confidence[i] + " MAX CON: " + maxCon);
                if (confidence[i] > maxCon) {
                    maxCon = confidence[i];
                    maxPos = i;
                }
            }

            // Code Debug after classification
            float sumConfidence = 0;
            for (float conf : confidence) {
                sumConfidence += conf;
            }
            Log.d("ModelOutput", "Confidence sum: " + sumConfidence);

            TextView res = findViewById(R.id.level_name);
            res.setText(classes[maxPos]);

            model.close();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }

        // scan button
        Button tAp = findViewById(R.id.scan_another_cacao);
        tAp.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 101);
        });

        // view details button
        Button oD = findViewById(R.id.openDetails);
        oD.setOnClickListener(v -> {
            Intent intent = new Intent(this, Details.class);
            intent.putExtra("levelName", classes[maxPos]);

            Uri imageUri = saveBitmapToCache(imageBitMap);
            if (imageUri != null) {
                intent.putExtra("imageUri", imageUri.toString());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                Log.e("ImageError", "Image URI is null");
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        // gallery button
        Button oG = findViewById(R.id.look_for_another_cacao);
        oG.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });
    }

    private void extractImageFeatures(Bitmap bitmap, float[] featureData) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixelIndex = 0;

        // Debug
        int nonZeroPixels = 0;

        // Normalize the entire image to 0-1 range
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = bitmap.getPixel(j, i);
                float red = ((pixel >> 16) & 0xFF) / 255.0f;
                float green = ((pixel >> 8) & 0xFF) / 255.0f;
                float blue = (pixel & 0xFF) / 255.0f;

                // Use grayscale as the feature
                float grayscale = (red + green + blue) / 3.0f;
                grayscale = Math.max(0.0f, Math.min(1.0f, grayscale));

                if (grayscale > 0) nonZeroPixels++;

                if (pixelIndex < featureData.length) {
                    featureData[pixelIndex] = grayscale;
                    pixelIndex++;
                }
            }
        }

        // Log the range of values and non-zero count
        float min = 1.0f, max = 0.0f, sum = 0.0f;
        int zeros = 0, nonZeros = 0;
        for (float val : featureData) {
            min = Math.min(min, val);
            max = Math.max(max, val);
            sum += val;
            if (val == 0.0f) zeros++;
            else nonZeros++;
        }
        Log.d("FeatureExtraction", "Feature range: min=" + min + ", max=" + max +
                ", sum=" + sum + ", zeros=" + zeros + ", nonZeros=" + nonZeros +
                ", nonZeroPixels=" + nonZeroPixels);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                boolean wasCreated = cachePath.mkdirs();
                if (!wasCreated) {
                    Log.e("CacheDir", "Failed to create image cache directory");
                    return null;
                }
            }
            File file = new File(cachePath, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider", // must match what's in AndroidManifest
                    file
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
