package com.example.conectamobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private EditText messageInput;
    private Button sendButton;
    private ListView messagesListView;

    private ArrayList<String> messagesList;
    private MessagesAdapter adapter;

    private String contactName;
    private String contactId;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String chatKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Inicializar vistas
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        messagesListView = findViewById(R.id.messagesListView);

        // Obtener el nombre y ID del contacto
        contactName = getIntent().getStringExtra("contact_name");
        contactId = getIntent().getStringExtra("contact_id");

        if (contactId == null) {
            Toast.makeText(this, "Error: contactId es null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar Firebase Auth y DatabaseReference
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (currentUserId == null) {
            Toast.makeText(this, "Error: currentUserId es null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Crear una clave única para el chat
        chatKey = currentUserId.compareTo(contactId) < 0 ? currentUserId + "_" + contactId : contactId + "_" + currentUserId;

        // Configurar la lista de mensajes
        messagesList = new ArrayList<>();
        adapter = new MessagesAdapter(this, messagesList);
        messagesListView.setAdapter(adapter);

        // Cargar mensajes previos del chat desde Firebase
        loadMessages();

        // Configurar MQTT
        setupMqttClient();

        // Acción del botón de enviar mensaje
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupMqttClient() {
        try {
            mqttClient = new MqttClient("tcp://test.mosquitto.org:1883", MqttClient.generateClientId(), null);
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            mqttClient.setCallback(new MqttCallbackHandler());
            mqttClient.connect(mqttConnectOptions);

            // Suscribirse al canal del chat
            mqttClient.subscribe("chat/messages/" + chatKey);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al conectar con MQTT", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        DatabaseReference messagesRef = databaseReference.child("messages").child(chatKey);

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        String sender = message.getSenderId().equals(firebaseAuth.getCurrentUser().getUid()) ? "Tú" : contactName;
                        messagesList.add(sender + ": " + message.getMessageContent());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error al cargar los mensajes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear un mensaje MQTT
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish("chat/messages/" + chatKey, mqttMessage);

            // Almacenar el mensaje en Firebase
            storeMessageInDatabase(message);

            // Agregar el mensaje localmente
            messagesList.add("Tú: " + message);
            adapter.notifyDataSetChanged();

            // Limpiar el campo de entrada
            messageInput.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void storeMessageInDatabase(String messageContent) {
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference messageRef = databaseReference.child("messages").child(chatKey).push();

        Message newMessage = new Message(currentUserId, contactId, messageContent);
        messageRef.setValue(newMessage)
                .addOnSuccessListener(aVoid -> Log.d("Chat", "Mensaje guardado"))
                .addOnFailureListener(e -> Log.e("Chat", "Error al guardar el mensaje", e));
    }

    private class MqttCallbackHandler implements org.eclipse.paho.client.mqttv3.MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            Toast.makeText(ChatActivity.this, "Conexión perdida", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            messagesList.add(contactName + ": " + new String(message.getPayload()));
            adapter.notifyDataSetChanged();
        }

        @Override
        public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
