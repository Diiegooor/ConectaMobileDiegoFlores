package com.example.conectamobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MessagesAdapter extends ArrayAdapter<String> {

    public MessagesAdapter(Context context, List<String> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_message, parent, false);
        }

        // Obtener elementos del diseño
        TextView senderView = convertView.findViewById(R.id.messageSender);
        TextView contentView = convertView.findViewById(R.id.messageContent);

        // Dividir el mensaje en remitente y contenido
        String message = getItem(position);
        if (message != null) {
            String[] parts = message.split(": ", 2); // Dividir en "Remitente: Contenido"
            if (parts.length == 2) {
                senderView.setText(parts[0]); // Remitente
                senderView.setTypeface(null, android.graphics.Typeface.BOLD); // Estilo en negrita
                contentView.setText(parts[1]); // Contenido
            } else {
                // En caso de que no haya separador ": ", mostrar todo en contenido
                senderView.setText("Desconocido");
                contentView.setText(message);
            }
        }

        return convertView;
    }
}

