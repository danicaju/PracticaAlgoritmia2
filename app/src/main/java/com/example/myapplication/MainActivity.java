package com.example.myapplication;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Forzar la orientación apaisada
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 2. Configurar el TextView de mensajes para que tenga scroll
        // (Asegúrate de que el ID coincida con el que le pongas en el XML, aquí uso 'textViewMissatges' de ejemplo)
        TextView tvMissatges = findViewById(R.id.tituloJuego);
        if (tvMissatges != null) {
            tvMissatges.setMovementMethod(new ScrollingMovementMethod());
        }
    }
}