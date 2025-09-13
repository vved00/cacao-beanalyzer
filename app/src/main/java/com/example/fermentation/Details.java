package com.example.fermentation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class Details extends Activity {
    @SuppressLint("Range")
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.details);
        Intent intent = getIntent();
        String levelName = intent.getStringExtra("levelName");
        String imageUriString = getIntent().getStringExtra("imageUri");


        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            ImageView imageView = findViewById(R.id.imageDetail);
            imageView.setImageURI(imageUri);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.openDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cacao_fermentation_stages WHERE fermentation_type = ?", new String[]{levelName});

        if (cursor.moveToFirst()){
            do {
                TextView levelView = findViewById(R.id.level_name_detail);
                TextView descView = findViewById(R.id.details_desc);
                TextView flavorView = findViewById(R.id.details_flavor);
                TextView recoView = findViewById(R.id.details_reco);

                String desc = cursor.getString(cursor.getColumnIndex("description"));
                String flavor = cursor.getString(cursor.getColumnIndex("flavor_profile"));
                String reco = cursor.getString(cursor.getColumnIndex("recommendation"));

                levelView.setText(levelName);
                descView.setText(desc);
                flavorView.setText(flavor);
                recoView.setText(reco);
                break;
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
    }
}
