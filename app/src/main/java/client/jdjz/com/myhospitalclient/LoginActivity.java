package client.jdjz.com.myhospitalclient;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import client.jdjz.com.exception.XXAdressMalformedException;
import client.jdjz.com.service.IConnectionStatusCallback;
import client.jdjz.com.service.XXService;
import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;
import client.jdjz.com.util.T;
import client.jdjz.com.util.XMPPHelper;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tchl on 2016-07-12.
 */
public class LoginActivity extends FragmentActivity implements
         TextWatcher ,IConnectionStatusCallback {
    public static final String TAG = "tchl LoginActivity";
    public static final String LOGIN_ACTION = "com.jdjz.action.LOGIN";
    private static final int LOGIN_OUT_TIME = 0;
    private Dialog mLoginDialog;
    private XXService mXxService;

    private ConnectionOutTimeProcess mLoginOutTimeProcess;
/*    @Bind(R.id.face)
    ImageView face;*/
    @Bind(R.id.account_input)
    EditText accountInput;
    @Bind(R.id.password)
    EditText password;
    @Bind(R.id.textView2)
    TextView textView2;
    @Bind(R.id.relativeLayout1)
    RelativeLayout relativeLayout1;
    @Bind(R.id.input)
    LinearLayout input;
    @Bind(R.id.login)
    Button login;
    @Bind(R.id.loginlayout)
    LinearLayout loginlayout;
    @Bind(R.id.loginInputView)
    RelativeLayout loginInputView;
    @Bind(R.id.scrollAreaLayout)
    LinearLayout scrollAreaLayout;
    @Bind(R.id.framelayout)
    FrameLayout framelayout;
/*    @Bind(R.id.pulldoor_close_tips)
    TextView pulldoorCloseTips;*/

    private Animation mTipsAnimation;
    private String mAccount;
    private String mPassword;
    TextView mTipsTextView;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOGIN_OUT_TIME:
                    if (mLoginOutTimeProcess != null
                            && mLoginOutTimeProcess.running.get())
                        mLoginOutTimeProcess.stop();
                    if (mLoginDialog != null && mLoginDialog.isShowing())
                        mLoginDialog.dismiss();
                    T.showShort(LoginActivity.this, R.string.timeout_try_again);
                    break;

                default:
                    break;
            }
        }
    };
    private void initView() {
        mTipsAnimation = AnimationUtils.loadAnimation(this, R.anim.connection);
        mTipsTextView = (TextView) findViewById(R.id.pulldoor_close_tips);
        String account = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT,"");
        String pwd = PreferenceUtils.getPrefString(this,PreferenceConstants.PASSWORD,"");
        if(!TextUtils.isEmpty(account)){
            accountInput.setText(account);
        }
        if(!TextUtils.isEmpty(pwd)){
            password.setText(pwd);
        }
        accountInput.addTextChangedListener(this);
    }


    public void onLoginClick(View v) {
        mAccount = accountInput.getText().toString();
        mAccount = splitAndSaveServer(mAccount);
        mPassword = password.getText().toString();
        if (TextUtils.isEmpty(mAccount)) {
            T.showShort(this, R.string.null_account_prompt);
            return;
        }
        if (TextUtils.isEmpty(mPassword)) {
            T.showShort(this, R.string.password_input_prompt);
            return;
        }
        if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running.get())
            mLoginOutTimeProcess.start();
        if (mLoginDialog != null && !mLoginDialog.isShowing())
            mLoginDialog.show();
        if (mXxService != null) {
            mXxService.Login(mAccount, mPassword);
        }
    }
    private void bindXMPPService() {
        Log.i(TAG, "[SERVICE] Unbind");
        Intent mServiceIntent = new Intent(this, XXService.class);
        mServiceIntent.setAction(LOGIN_ACTION);
        bindService(mServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            Log.i(TAG, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Service wasn't bound!");
        }
    }

     private String splitAndSaveServer(String account){
         if(!account.contains("@")){
             return account;
         }
         String customServer = PreferenceUtils.getPrefString(this,PreferenceConstants.CUSTOM_SERVER,"");
         String[] res = account.split("@");
         String userName = res[0];
         String server = res[1];
         PreferenceUtils.setPrefString(this,PreferenceConstants.Server,server);
         return userName;
     }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(TAG+" ---> " + Thread.currentThread().getId());
        //StartService(new Intent(LoginActivity.this,XXService.class));
        setContentView(R.layout.loginpage);
        ButterKnife.bind(this);
        bindXMPPService();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mTipsTextView != null && mTipsAnimation != null)
            Log.i(TAG, "startAnimation");
        mTipsTextView.startAnimation(mTipsAnimation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        unbindXMPPService();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        try {
            XMPPHelper.verifyJabberID(s);
            login.setEnabled(true);
            accountInput.setTextColor(Color.parseColor("#ff333333"));
        } catch (XXAdressMalformedException e) {
            login.setEnabled(false);
            accountInput.setTextColor(Color.RED);
            //Snackbar.make(framelayout,getString(R.string.wrong_account),Snackbar.LENGTH_LONG).show();
            //Toast.makeText(LoginActivity.this, getString(R.string.wrong_account), Toast.LENGTH_LONG).show();
            //T.showLong(LoginActivity.this,getString(R.string.wrong_account));
        }
    }

    // 登录超时处理线程
    class ConnectionOutTimeProcess implements Runnable{
        public  AtomicBoolean running = new AtomicBoolean(false);
        private long startTime = 0L;
        private Thread thread = null;

        public void run() {
            System.out.println(TAG+" ConnectionOutTimeProcess ---> " + Thread.currentThread().getId());
            while (true) {
                if (!running.get())
                    return;
                if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
                    mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
                }
                try {
                    Thread.sleep(10L);
                } catch (Exception localException) {
                }
            }
        }


        public void start() {
            try {
                this.thread = new Thread(this);
                running.set(true);
                this.startTime = System.currentTimeMillis();
                this.thread.start();
            } finally {
            }
        }


        public void stop() {
            try {
                running.set(false);
                this.thread = null;
                this.startTime = 0L;
            } finally {
            }
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(LoginActivity.this);
            // 开始连接xmpp服务器
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }

    };

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        // TODO Auto-generated method stub
        if (mLoginDialog != null && mLoginDialog.isShowing())
            mLoginDialog.dismiss();
        if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running.get()) {
            mLoginOutTimeProcess.stop();
            mLoginOutTimeProcess = null;
        }
        if (connectedState == XXService.CONNECTED) {
            save2Preferences();
            Log.i(TAG,"client connected service!");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (connectedState == XXService.DISCONNECTED)
            T.showLong(LoginActivity.this, getString(R.string.request_failed)
                    + reason);
    }

    private void save2Preferences() {
        PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,mAccount);// 帐号是一直保存的
        PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,mPassword);
    }
}
