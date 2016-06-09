package com.webview.xialiangtong.sincere;

import android.app.Activity;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends Activity {
    private Button button;
    private Spinner spinner_rank;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Context context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.search_BTN);
        final EditText edit = (EditText) findViewById(R.id.key_word);
        spinner_rank = (Spinner)findViewById(R.id.spinner_rank);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String para_q = edit.getText().toString();
                String para_rank = String.valueOf(spinner_rank.getSelectedItem());
                Intent intent = new Intent(context, WebViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("q",para_q);
                bundle.putString("orderBy",para_rank);
                intent.putExtras(bundle);
                startActivity(intent);
            }

        });
    }

}
