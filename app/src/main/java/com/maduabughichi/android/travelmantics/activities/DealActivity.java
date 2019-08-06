package com.maduabughichi.android.travelmantics.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.maduabughichi.android.travelmantics.R;
import com.maduabughichi.android.travelmantics.model.TravelDeal;
import com.maduabughichi.android.travelmantics.util.FirebaseUtil;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private static final int PICTURE_RESULT = 42;
    EditText mTxtTitle;
    EditText mTxtDescription;
    EditText mTxtPrice;
    ImageView mImageView;
    TravelDeal mDeal;
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        initViews();
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.mDeal = deal;
        mTxtTitle.setText(deal.getTitle());
        mTxtDescription.setText(deal.getDescription());
        mTxtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(view -> {
            Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
            intent1.setType("image/jpeg");
            intent1.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(intent1, "Insert Picture"), PICTURE_RESULT);
        });
    }

    private void initViews() {
        mTxtTitle = findViewById(R.id.txtTitle);
        mTxtDescription = findViewById(R.id.txtDescription);
        mTxtPrice = findViewById(R.id.txtPrice);
        mImageView = findViewById(R.id.image);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload_menu:
                saveDeal();
                Toast.makeText(this, "Deal Uploaded", Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.upload_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.upload_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);
        }


        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, taskSnapshot -> {
                if (taskSnapshot.getDownloadUrl() != null) {
                    String url = taskSnapshot.getDownloadUrl().toString();
                    String pictureName = taskSnapshot.getStorage().getPath();
                    mDeal.setImageUrl(url);
                    mDeal.setImageName(pictureName);
                    Log.d("Url: ", url);
                    Log.d("Name", pictureName);
                    showImage(url);
                } else {
                    Toast.makeText(DealActivity.this, "FAILED!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(DealActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show();
        }
    }
//TextUtils.isEmpty()
    private void saveDeal() {
        initViews();
        if (TextUtils.isEmpty(mTxtTitle.getText().toString())|| TextUtils.isEmpty(mTxtPrice.getText().toString()) || TextUtils.isEmpty((mTxtDescription.getText().toString()))){
            mTxtTitle.setError("The field must not be empty!");
            mTxtPrice.setError("The field must not be empty!");
            mTxtDescription.setError("The field must not be empty!");
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
        } else {
            mDeal.setTitle(mTxtTitle.getText().toString().trim());
            mDeal.setDescription(mTxtDescription.getText().toString().trim());
            mDeal.setPrice(mTxtPrice.getText().toString().trim());
            if (mDeal.getId() == null) {
                // Creating a new entry
                mDatabaseReference.push().setValue(mDeal).addOnSuccessListener(aVoid ->
                        Toast.makeText(DealActivity.this, "Deal Saved!", Toast.LENGTH_SHORT).show());
            } else {
                // Updating an existing entry
                mDatabaseReference.child(mDeal.getId()).setValue(mDeal);
            }
        }

    }

    private void deleteDeal() {
        if (mDeal == null) {
            Toast.makeText(this, "Please save the mDeal before deleting", Toast.LENGTH_SHORT).show();
            return;
        }
        mDatabaseReference.child(mDeal.getId()).removeValue();
        Log.d("image name", mDeal.getImageName());
        //if (mDeal.getImageName() != null && mDeal.getImageName().isEmpty() == false)
        if (mDeal.getImageName() != null && !mDeal.getImageName().isEmpty()) {
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(mDeal.getImageName());
            picRef.delete().addOnSuccessListener(aVoid -> Log.d("Delete Image", "Image Successfully Deleted")).addOnFailureListener(e -> Log.d("Delete Image", e.getMessage()));
        }

    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
        finish();
    }

    private void clean() {
        mTxtTitle.setText("");
        mTxtPrice.setText("");
        mTxtDescription.setText("");
        mTxtTitle.requestFocus();
    }

    private void enableEditTexts(boolean isEnabled) {
        mTxtTitle.setEnabled(isEnabled);
        mTxtDescription.setEnabled(isEnabled);
        mTxtPrice.setEnabled(isEnabled);
    }

    private void showImage(String url) {
        //if (url != null && url.isEmpty() == false)
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            //Picasso.with(this)
            Picasso.get()
                    .load(url)
                    .error(R.drawable.launcher_icon)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(mImageView);
        }
    }
}
