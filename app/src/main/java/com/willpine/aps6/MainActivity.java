package com.willpine.aps6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button botao01, botao02;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botao01 = (Button) findViewById(R.id.botao_01);
        botao02 = (Button) findViewById(R.id.botao_02);


        botao01.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                botao01Activity();

            }

        });

        botao02.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                botao02Activity();

            }

        });




    }

    private void botao01Activity() {

        startActivity(new Intent(MainActivity.this, CameraActivity.class));

    }

    private void botao02Activity() {

        startActivity(new Intent(MainActivity.this, CreditosActivity.class));

    }


}
