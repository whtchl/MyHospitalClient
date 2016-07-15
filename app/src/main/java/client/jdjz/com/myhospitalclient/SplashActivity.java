package client.jdjz.com.myhospitalclient;

import android.content.Intent;
import android.os.Handler;
import android.preference.Preference;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;

public class SplashActivity extends FragmentActivity {
    private Handler mHandler;
    Runnable gotoMainAct = new Runnable() {
        @Override
        public void run() {
              startActivity(new Intent(SplashActivity.this,MainActivity.class));
            finish();
        }
    };
    Runnable gotoLoginAct = new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this,LoginActivity.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Default_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mHandler = new Handler();
        String password = PreferenceUtils.getPrefString(this, PreferenceConstants.PASSWORD,"");
        if(!TextUtils.isEmpty(password)){
            mHandler.postDelayed(gotoMainAct,3000);
        }else{
            mHandler.postDelayed(gotoLoginAct,3000);
        }
    }
}
