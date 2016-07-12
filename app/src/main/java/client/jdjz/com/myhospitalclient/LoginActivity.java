package client.jdjz.com.myhospitalclient;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import client.jdjz.com.exception.XXAdressMalformedException;
import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;
import client.jdjz.com.util.XMPPHelper;
import android.support.design.widget.Snackbar;
/**
 * Created by tchl on 2016-07-12.
 */
public class LoginActivity extends FragmentActivity implements
         TextWatcher {
    public static final String TAG = "LoginActivity";
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
/*    @Bind(R.id.pulldoor_close_tips)
    TextView pulldoorCloseTips;*/

    private Animation mTipsAnimation;


    TextView mTipsTextView;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        //StartService(new Intent(LoginActivity.this,XXService.class));
        setContentView(R.layout.loginpage);
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
          //  Snackbar.make(accountInput,getString(R.string.wrong_account),Snackbar.LENGTH_LONG).show();
        }

    }
}
