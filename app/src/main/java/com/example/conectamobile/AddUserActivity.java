package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends AppCompatActivity {

    private EditText userNameInput, userEmailInput, userTopicInput;
    private Button saveUserButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        // Inicializar vistas
        userNameInput = findViewById(R.id.userNameInput);
        userEmailInput = findViewById(R.id.userEmailInput);
        userTopicInput = findViewById(R.id.userTopicInput);
        saveUserButton = findViewById(R.id.saveUserButton);

        // Inicializar referencia a Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("contacts");

        // Acción del botón para guardar usuario
        saveUserButton.setOnClickListener(v -> saveUser());
    }

    private void saveUser() {
        String name = userNameInput.getText().toString().trim();
        String topic = userTopicInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(topic)) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("contacts");

        // Crear un ID único para el contacto
        String contactId = contactsRef.push().getKey();

        // Crear el objeto del contacto
        Map<String, Object> contactData = new HashMap<>();
        contactData.put("name", name);
        contactData.put("topic", topic);

        // Guardar en Firebase
        if (contactId != null) {
            contactsRef.child(contactId).setValue(contactData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Usuario guardado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Regresar a la pantalla de contactos
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar usuario", Toast.LENGTH_SHORT).show());
        }
    }


}
