package com.willpine.aps6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class CreditosActivity extends AppCompatActivity {

    private Button botaoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creditos);

        botaoBack = (Button) findViewById(R.id.botao_back);


        botaoBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                botaoBackActivity();

            }

        });
    }

    private void botaoBackActivity() {

        startActivity(new Intent(CreditosActivity.this, MainActivity.class));

    }
}
