package client.jdjz.com.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;

import client.jdjz.com.exception.XXException;
import client.jdjz.com.smack.SmackImpl;
import client.jdjz.com.util.NetUtil;
import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;

public class XXService extends Service {
    public static final String TAG = "tchl XXService";
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = -1;
    public static final int CONNECTING = 1;
    public static final String PONG_TIMEOUT = "pong timeout";// 连接超时
    public static final String NETWORK_ERROR = "network error";// 网络错误
    public static final String LOGOUT = "logout";// 手动退出
    public static final String LOGIN_FAILED = "login failed";// 登录失败
    public static final String DISCONNECTED_WITHOUT_WARNING = "disconnected without warning";// 没有警告的断开连接

    private IBinder mBinder = new XXBinder();
    private IConnectionStatusCallback mConnectionStatusCallback;
    private SmackImpl mSmackable;
    private Thread mConnectingThread;
    private Handler mMainHandler = new Handler();

    private boolean mIsFirstLoginAction;
    // 自动重连 start
    private static final int RECONNECT_AFTER = 5;
    private static final int RECONNECT_MAXIMUM = 10 * 60;// 最大重连时间间隔
    private static final String RECONNECT_ALARM = "com.way.xx.RECONNECT_ALARM";
    // private boolean mIsNeedReConnection = false; // 是否需要重连
    private int mConnectedState = DISCONNECTED; // 是否已经连接
    private int mReconnectTimeout = RECONNECT_AFTER;
    private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
    private PendingIntent mPAlarmIntent;
   // private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
    // 自动重连 end
    private ActivityManager mActivityManager;
    private HashSet<String> mIsBoundTo = new HashSet<String>();//用来保存当前正在聊天对象的数组

    public XXService() {
    }

    public class XXBinder extends Binder {
        public XXService getService() {
            return XXService.this;
        }
    }

    /**
     * 注册注解面和聊天界面时连接状态变化回调
     *
     * @param cb
     */
    public void registerConnectionStatusCallback(IConnectionStatusCallback cb) {
        mConnectionStatusCallback = cb;
    }

    public void unRegisterConnectionStatusCallback() {
        mConnectionStatusCallback = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println(TAG+" ---> " + Thread.currentThread().getId());
/*        XXBroadcastReceiver.mListeners.add(this);
        BaseActivity.mListeners.add(this);
        mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));*/
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"XXService onBind()");
        // TODO: Return the communication channel to the service.
       return mBinder;
    }

    // 登录
    public void Login(final String account, final String password) {
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
            connectionFailed(NETWORK_ERROR);
            return;
        }
        if (mConnectingThread != null) {
            Log.i(TAG,"a connection is still goign on!");
            return;
        }
        mConnectingThread = new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println(TAG+" mConnectingThread ---> " + Thread.currentThread().getId());
                    postConnecting();
                    mSmackable = new SmackImpl(XXService.this);
                    if (mSmackable.login(account, password)) {
                        // 登陆成功
                        postConnectionScuessed();
                    } else {
                        // 登陆失败
                        postConnectionFailed(LOGIN_FAILED);
                    }
                } catch (XXException e) {
                    String message = e.getLocalizedMessage();
                    // 登陆失败
                    if (e.getCause() != null)
                        message += "\n" + e.getCause().getLocalizedMessage();
                    postConnectionFailed(message);
                    Log.i(TAG, "YaximXMPPException in doConnect():");
                    e.printStackTrace();
                } finally {
                    if (mConnectingThread != null)
                        synchronized (mConnectingThread) {
                            mConnectingThread = null;
                        }
                }
            }

        };
        mConnectingThread.start();
    }

    /**
     * 非UI线程连接失败反馈
     *
     * @param reason
     */
    public void postConnectionFailed(final String reason) {
        mMainHandler.post(new Runnable() {
            public void run() {
                System.out.println(TAG+" postConnectionFailed ---> " + Thread.currentThread().getId());
                connectionFailed(reason);
            }
        });
    }

    private void postConnectionScuessed() {
        mMainHandler.post(new Runnable() {
            public void run() {
                connectionScuessed();
            }

        });
    }

    private void connectionScuessed() {
        mConnectedState = CONNECTED;// 已经连接上
        mReconnectTimeout = RECONNECT_AFTER;// 重置重连的时间

        if (mConnectionStatusCallback != null)
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState,
                    "");
    }

    // 连接中，通知界面线程做一些处理
    private void postConnecting() {
        // TODO Auto-generated method stub
        mMainHandler.post(new Runnable() {
            public void run() {
                System.out.println(TAG+" postConnecting ---> " + Thread.currentThread().getId());
                connecting();
            }
        });
    }
    private void connecting() {
        // TODO Auto-generated method stub
        mConnectedState = CONNECTING;// 连接中
        if (mConnectionStatusCallback != null)
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState,
                    "");
    }
    /**
     * UI线程反馈连接失败
     *
     * @param reason
     */
    private void connectionFailed(String reason) {
        Log.i(TAG, "connectionFailed: " + reason);
        mConnectedState = DISCONNECTED;// 更新当前连接状态
//		if (mSmackable != null)
//			mSmackable.setStatusOffline();// 将所有联系人标记为离线
        if (TextUtils.equals(reason, LOGOUT)) {// 如果是手动退出
                 /*tchl
                 ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);*/
            return;
        }
        // 回调
        if (mConnectionStatusCallback != null) {
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState,
                    reason);
           /* tchl

            if (mIsFirstLoginAction)// 如果是第一次登录,就算登录失败也不需要继续
                return;*/
        }

        // 无网络连接时,直接返回
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
         /* tchl     ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);*/
            return;
        }

        String account = PreferenceUtils.getPrefString(XXService.this,
                PreferenceConstants.ACCOUNT, "");
        String password = PreferenceUtils.getPrefString(XXService.this,
                PreferenceConstants.PASSWORD, "");
        // 无保存的帐号密码时，也直接返回
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            Log.d(TAG,"account = null || password = null");
            return;
        }
        // 如果不是手动退出并且需要重新连接，则开启重连闹钟
        if (PreferenceUtils.getPrefBoolean(this,
                PreferenceConstants.AUTO_RECONNECT, true)) {
            Log.d(TAG,"connectionFailed(): registering reconnect in "
                    + mReconnectTimeout + "s");
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(
                    AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                            + mReconnectTimeout * 1000, mPAlarmIntent);
            mReconnectTimeout = mReconnectTimeout * 2;
            if (mReconnectTimeout > RECONNECT_MAXIMUM)
                mReconnectTimeout = RECONNECT_MAXIMUM;
        } else {
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);
        }

    }
}
