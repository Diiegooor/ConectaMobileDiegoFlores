package com.example.conectamobile;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private EditText nameInput, emailInput;
    private ImageView profileImageView;
    private Button saveButton, changePhotoButton;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    private Uri selectedImageUri;

    private final List<Integer> predefinedImages = Arrays.asList(
            R.drawable.profile1,
            R.drawable.profile2,
            R.drawable.profile3,
            R.drawable.profile4,
            R.drawable.profile5,
            R.drawable.profile6,
            R.drawable.profile7,
            R.drawable.profile8
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Inicializar vistas
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        profileImageView = findViewById(R.id.profileImageView);
        saveButton = findViewById(R.id.saveButton);
        changePhotoButton = findViewById(R.id.changePhotoButton);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        if (currentUser != null) {
            loadUserProfile();
        }

        // Guardar cambios
        saveButton.setOnClickListener(v -> saveUserProfile());

        // Cambiar foto de perfil
        changePhotoButton.setOnClickListener(v -> showPhotoSelectionMenu());

        Button backToContactsButton = findViewById(R.id.backToContactsButton);

        backToContactsButton.setOnClickListener(v -> {
            finish();
        });

    }

    private void loadUserProfile() {
        emailInput.setText(currentUser.getEmail());
        databaseReference.child(currentUser.getUid()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String name = snapshot.child("name").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                nameInput.setText(name);

                if (!TextUtils.isEmpty(profileImageUrl)) {
                    Picasso.get().load(profileImageUrl).into(profileImageView);
                }
            }
        });
    }

    private void saveUserProfile() {
        String name = nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = databaseReference.child(currentUser.getUid());
        userRef.child("name").setValue(name);

        if (selectedImageUri != null) {
            userRef.child("profileImageUrl").setValue(selectedImageUri.toString());
        }

        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
    }

    private void showPhotoSelectionMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_photo_selection, null);
        builder.setView(dialogView);

        GridView gridView = dialogView.findViewById(R.id.photoGridView);
        Button uploadPhotoButton = dialogView.findViewById(R.id.uploadPhotoButton);

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, predefinedImages);
        gridView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedImageRes = predefinedImages.get(position);
            profileImageView.setImageResource(selectedImageRes);
            selectedImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + selectedImageRes);
            dialog.dismiss();
        });

        uploadPhotoButton.setOnClickListener(v -> {
            selectImage();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }
}
