package io.github.shinem.recyclerviewcachedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import static io.github.shinem.recyclerviewcachedemo.DemoListActivity.EXTRA_USE_PRE_CACHE;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.pre_cache_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoList(true);
            }
        });
        findViewById(R.id.no_cache_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoList(false);
            }
        });
    }

    private void gotoList(boolean useCache) {
        Intent intent = new Intent(MainActivity.this, DemoListActivity.class);
        intent.putExtra(EXTRA_USE_PRE_CACHE, useCache);
        startActivity(intent);
    }
}
