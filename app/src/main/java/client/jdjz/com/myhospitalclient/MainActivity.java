package client.jdjz.com.myhospitalclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import client.jdjz.com.service.IConnectionStatusCallback;
import client.jdjz.com.service.XXService;
import client.jdjz.com.util.NetUtil;
import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;
import client.jdjz.com.util.XMPPHelper;

/**
 * Created by tchl on 2016-07-15.
 */
public class MainActivity extends FragmentActivity
   implements IConnectionStatusCallback,View.OnClickListener {
    public static final String TAG = MainActivity.class.getClass().getName()+"tchl ";
    @Bind(R.id.ivTitleName)
    TextView mTitleNameView;
    @Bind(R.id.ivTitleStatus)
    ImageView mTitleStatusView;
    @Bind(R.id.ivTitleProgress)
    ProgressBar mTitleProgressBar;
    @Bind(R.id.show_left_fragment_btn)
    ImageButton mLeftBtn;
    @Bind(R.id.show_right_fragment_btn)
    ImageButton mRightBtn;
    @Bind(R.id.rlCommenTitle)
    RelativeLayout rlCommenTitle;
    @Bind(R.id.net_status_bar_info_top)
    TextView netStatusBarInfoTop;
    @Bind(R.id.net_status_bar)
    LinearLayout netStatusBar;
    @Bind(R.id.net_status_bar_top)
    LinearLayout mNetErrorView;
    private XXService mXxService;

    public static HashMap<String, Integer> mStatusMap;
    static {
        mStatusMap = new HashMap<String, Integer>();
        mStatusMap.put(PreferenceConstants.OFFLINE, -1);
        mStatusMap.put(PreferenceConstants.DND, R.drawable.status_shield);
        mStatusMap.put(PreferenceConstants.XA, R.drawable.status_invisible);
        mStatusMap.put(PreferenceConstants.AWAY, R.drawable.status_leave);
        mStatusMap.put(PreferenceConstants.AVAILABLE, R.drawable.status_online);
        mStatusMap.put(PreferenceConstants.CHAT, R.drawable.status_qme);
    }
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(MainActivity.this);
            // 开始连接xmpp服务器
            if (!mXxService.isAuthenticated()) {
                Log.e(TAG,"isAuthenticated is false");
                String usr = PreferenceUtils.getPrefString(MainActivity.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        MainActivity.this, PreferenceConstants.PASSWORD, "");
                mXxService.Login(usr, password);
                // mTitleNameView.setText(R.string.login_prompt_msg);
                // setStatusImage(false);
                // mTitleProgressBar.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG,"isAuthenticated is true");
                mTitleNameView.setText(XMPPHelper
                        .splitJidAndServer(PreferenceUtils.getPrefString(
                                MainActivity.this, PreferenceConstants.ACCOUNT,
                                "")));
                setStatusImage(true);
                mTitleProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(MainActivity.this, XXService.class));
        setContentView(R.layout.main_center_layout);

        ButterKnife.bind(this);
        initViews();


    }

    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
        setStatusImage(isConnected());

        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
            mNetErrorView.setVisibility(View.VISIBLE);
        else
            mNetErrorView.setVisibility(View.GONE);
    }
    private boolean isConnected() {
        return mXxService != null && mXxService.isAuthenticated();
    }
    @Override
    protected void onPause() {
        super.onPause();
        unbindXMPPService();
    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            Log.i(TAG, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Service wasn't bound!");
        }
    }

    private void bindXMPPService() {
        Log.i(TAG, "[SERVICE] Unbind");
        bindService(new Intent(MainActivity.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    private void setStatusImage(boolean isConnected) {
        if (!isConnected) {
            mTitleStatusView.setVisibility(View.GONE);
            return;
        }
        String statusMode = PreferenceUtils.getPrefString(this,
                PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
        int statusId = mStatusMap.get(statusMode);
        if (statusId == -1) {
            mTitleStatusView.setVisibility(View.GONE);
        } else {
            mTitleStatusView.setVisibility(View.VISIBLE);
            mTitleStatusView.setImageResource(statusId);
        }
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        Log.e(TAG,"XXService State:"+connectedState);
        switch (connectedState) {
            case XXService.CONNECTED:
                mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
                        .getPrefString(MainActivity.this,
                                PreferenceConstants.ACCOUNT, "")));
                mTitleProgressBar.setVisibility(View.GONE);
                // mTitleStatusView.setVisibility(View.GONE);
                setStatusImage(true);
                break;
            case XXService.CONNECTING:
                mTitleNameView.setText(R.string.login_prompt_msg);
                mTitleProgressBar.setVisibility(View.VISIBLE);
                mTitleStatusView.setVisibility(View.GONE);
                break;
            case XXService.DISCONNECTED:
                mTitleNameView.setText(R.string.login_prompt_no);
                mTitleProgressBar.setVisibility(View.GONE);
                mTitleStatusView.setVisibility(View.GONE);
                Toast.makeText(this,reason,Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }
    }

    private void initViews() {
        /*mSlidingMenu.setSecondaryMenu(R.layout.main_right_layout);
        FragmentTransaction mFragementTransaction = getSupportFragmentManager()
                .beginTransaction();
        Fragment mFrag = new SettingsFragment();
        mFragementTransaction.replace(R.id.main_right_fragment, mFrag);
        mFragementTransaction.commit();*/


        mLeftBtn.setVisibility(View.VISIBLE);
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
                .getPrefString(this, PreferenceConstants.ACCOUNT, "")));
        mTitleNameView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

    }
}
