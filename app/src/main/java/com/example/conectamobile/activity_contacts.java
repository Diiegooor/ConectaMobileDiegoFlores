package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class activity_contacts extends AppCompatActivity {

    private EditText searchInput;
    private Button addContactButton;
    private ListView contactListView;
    private ArrayAdapter<String> adapter;
    private List<String> contactList;
    private Map<String, String> contactIdMap; // Mapeo para obtener el contact_id por el nombre

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // Referencias a los elementos de la interfaz
        searchInput = findViewById(R.id.searchInput);
        addContactButton = findViewById(R.id.addContactButton);
        Button addUserButton = findViewById(R.id.addUserButton); // Nuevo botón para añadir usuario
        contactListView = findViewById(R.id.contactListView);

        // Inicialización de la lista y el adaptador
        contactList = new ArrayList<>();
        contactIdMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactList);
        contactListView.setAdapter(adapter);

        // Inicialización de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Cargar los contactos al inicio
        loadContacts();

        // Acción al hacer clic en el botón de agregar contacto
        addContactButton.setOnClickListener(v -> addContact());

        // Acción al hacer clic en el botón de añadir usuario
        addUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity_contacts.this, AddUserActivity.class);
            startActivity(intent);
        });

        // Configurar el evento de clic en un contacto
        contactListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedContactName = contactList.get(position);
            String contactId = contactIdMap.get(selectedContactName); // Obtener el ID correspondiente al nombre
            if (contactId != null) {
                openChatActivity(selectedContactName, contactId);
            } else {
                Toast.makeText(activity_contacts.this, "Error: No se pudo obtener el ID del contacto", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar el evento de long click para eliminar un contacto
        contactListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedContactName = contactList.get(position);
            String contactId = contactIdMap.get(selectedContactName); // Obtener el ID correspondiente al nombre
            if (contactId != null) {
                showDeleteContactDialog(selectedContactName, contactId);
            } else {
                Toast.makeText(activity_contacts.this, "Error: No se pudo obtener el ID del contacto", Toast.LENGTH_SHORT).show();
            }
            return true; // Indica que el evento fue manejado
        });

        Button editProfileButton = findViewById(R.id.editProfileButton);

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity_contacts.this, UserProfileActivity.class);
            startActivity(intent);
        });
    }

    // Cargar los contactos del usuario actual
    private void loadContacts() {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference contactsRef = databaseReference.child("users").child(currentUserId).child("contacts");

        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear(); // Limpiar la lista antes de recargar
                contactIdMap.clear(); // Limpiar el mapeo de IDs

                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    String contactId = contactSnapshot.getKey();
                    String name = contactSnapshot.child("name").getValue(String.class);
                    String topic = contactSnapshot.child("topic").getValue(String.class);

                    if (contactId != null && name != null && topic != null) {
                        contactList.add(name); // Añadir el nombre a la lista de contactos
                        contactIdMap.put(name, contactId); // Mapear el nombre con el ID del contacto
                        contactIdMap.put(name + "_topic", topic); // Mapear el tópico del contacto
                    }
                }

                adapter.notifyDataSetChanged(); // Notificar al adaptador para actualizar la vista
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_contacts.this, "Error al cargar contactos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para mostrar el diálogo de confirmación de eliminación
    private void showDeleteContactDialog(String contactName, String contactId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setMessage("¿Estás seguro de que deseas eliminar el contacto \"" + contactName + "\"?")
                .setPositiveButton("Sí", (dialog, which) -> deleteContact(contactId))
                .setNegativeButton("No", null)
                .show();
    }

    // Método para eliminar un contacto de Firebase
    private void deleteContact(String contactId) {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference contactRef = databaseReference.child("users").child(currentUserId).child("contacts").child(contactId);

        contactRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(activity_contacts.this, "Contacto eliminado con éxito", Toast.LENGTH_SHORT).show();
                    loadContacts(); // Recargar la lista de contactos
                })
                .addOnFailureListener(e -> Toast.makeText(activity_contacts.this, "Error al eliminar el contacto", Toast.LENGTH_SHORT).show());
    }

    // Método para agregar un nuevo contacto
    private void addContact() {
        String email = searchInput.getText().toString().trim(); // Obtener correo ingresado

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Introduce un correo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Buscar al usuario por correo electrónico en Firebase
        databaseReference.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Si el usuario existe, agregarlo como contacto
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String contactId = userSnapshot.getKey(); // ID del contacto encontrado
                        String currentUserId = firebaseAuth.getCurrentUser().getUid(); // ID del usuario actual

                        // Agregar el contacto al nodo "contacts" del usuario actual
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/users/" + currentUserId + "/contacts/" + contactId, true);

                        // Actualizar la base de datos
                        databaseReference.updateChildren(updates)
                                .addOnSuccessListener(unused -> Toast.makeText(activity_contacts.this, "Contacto agregado", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(activity_contacts.this, "Error al agregar contacto", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Si no se encuentra el usuario, mostrar mensaje
                    Toast.makeText(activity_contacts.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_contacts.this, "Error en la búsqueda", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Abrir la actividad de chat
    private void openChatActivity(String contactName, String contactId) {
        String contactTopic = contactIdMap.get(contactName + "_topic"); // Recuperar el tópico del contacto

        if (contactTopic != null) {
            Intent intent = new Intent(activity_contacts.this, ChatActivity.class);
            intent.putExtra("contact_name", contactName);
            intent.putExtra("contact_id", contactId);
            intent.putExtra("contact_topic", contactTopic); // Pasar el tópico
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el tópico del contacto", Toast.LENGTH_SHORT).show();
        }
    }
}
