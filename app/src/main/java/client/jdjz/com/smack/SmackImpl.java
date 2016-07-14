package client.jdjz.com.smack;

import android.content.ContentResolver;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;

import client.jdjz.com.exception.XXException;
import client.jdjz.com.myhospitalclient.R;
import client.jdjz.com.service.XXService;
import client.jdjz.com.util.PreferenceConstants;
import client.jdjz.com.util.PreferenceUtils;

public class SmackImpl implements Smack {
    public static final String TAG="SmackImpl";
	// 客户端名称和类型。主要是向服务器登记，有点类似QQ显示iphone或者Android手机在线的功能
	public static final String XMPP_IDENTITY_NAME = "XMPP";// 客户端名称
	public static final String XMPP_IDENTITY_TYPE = "phone";// 客户端类型

	private static final int PACKET_TIMEOUT = 30000;// 超时时间
    private ConnectionConfiguration mXMPPConfig;// 连接配置
    private XMPPConnection mXMPPConnection;// 连接对象
    private XXService mService;// 主服务
    /*private final ContentResolver mContentResolver;// 数据库操作对象*/
	// ping-pong服务器

	public SmackImpl(XXService service) {
		String customServer = PreferenceUtils.getPrefString(service,
				PreferenceConstants.CUSTOM_SERVER, "");// 用户手动设置的服务器名称，本来打算给用户指定服务器的
		int port = PreferenceUtils.getPrefInt(service,
				PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);// 端口号，也是留给用户手动设置的
		String server = PreferenceUtils.getPrefString(service,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER);// 默认的服务器，即谷歌服务器
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.SMACKDEBUG, false);// 是否需要smack debug
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.REQUIRE_TLS, false);// 是否需要ssl安全配置
		if (customServer.length() > 0
				|| port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port,
					server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(false);
		this.mXMPPConfig.setSendPresence(false);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(smackdebug);
		if (requireSsl)
			this.mXMPPConfig
					.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		/*mContentResolver = service.getContentResolver();*/
	}


    @Override
    public boolean login(String account, String password) throws XXException {// 登陆实现
        try {
            if (mXMPPConnection.isConnected()) {// 首先判断是否还连接着服务器，需要先断开
                try {
                    mXMPPConnection.disconnect();
                } catch (Exception e) {
                    Log.d(TAG,"conn.disconnect() failed: " + e);
                }
            }
            SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);// 设置超时时间
            SmackConfiguration.setKeepAliveInterval(-1);
            SmackConfiguration.setDefaultPingInterval(0);
            //registerRosterListener();// 监听联系人动态变化
            mXMPPConnection.connect();
            if (!mXMPPConnection.isConnected()) {
                throw new XXException("SMACK connect failed without exception!");
            }
            mXMPPConnection.addConnectionListener(new ConnectionListener() {
                public void connectionClosedOnError(Exception e) {
                    mService.postConnectionFailed(e.getMessage());// 连接关闭时，动态反馈给服务
                }

                public void connectionClosed() {
                }

                public void reconnectingIn(int seconds) {
                }

                public void reconnectionFailed(Exception e) {
                }

                public void reconnectionSuccessful() {
                }
            });
            initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
            // SMACK auto-logins if we were authenticated before
            if (!mXMPPConnection.isAuthenticated()) {
                String ressource = PreferenceUtils.getPrefString(mService,
                        PreferenceConstants.RESSOURCE, XMPP_IDENTITY_NAME);
                mXMPPConnection.login(account, password, ressource);
            }
            setStatusFromConfig();// 更新在线状态

        } catch (XMPPException e) {
            throw new XXException(e.getLocalizedMessage(),
                    e.getWrappedThrowable());
        } catch (Exception e) {
            // actually we just care for IllegalState or NullPointer or XMPPEx.
            Log.e(TAG, "login(): " + Log.getStackTraceString(e));
            throw new XXException(e.getLocalizedMessage(), e.getCause());
        }
        //registerAllListener();// 注册监听其他的事件，比如新消息
        return mXMPPConnection.isAuthenticated();
    }

	@Override
	public boolean logout() {
		return false;
	}

	@Override
	public boolean isAuthenticated() {
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group) throws XXException {

	}

	@Override
	public void removeRosterItem(String user) throws XXException {

	}

	@Override
	public void renameRosterItem(String user, String newName) throws XXException {

	}

	@Override
	public void moveRosterItemToGroup(String user, String group) throws XXException {

	}

	@Override
	public void renameRosterGroup(String group, String newGroup) {

	}

	@Override
	public void requestAuthorizationForRosterItem(String user) {

	}

	@Override
	public void addRosterGroup(String group) {

	}

    @Override
    public void setStatusFromConfig() {// 设置自己的当前状态，供外部服务调用
        boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService,
                PreferenceConstants.MESSAGE_CARBONS, true);
        String statusMode = PreferenceUtils.getPrefString(mService,
                PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
        String statusMessage = PreferenceUtils.getPrefString(mService,
                PreferenceConstants.STATUS_MESSAGE,
                mService.getString(R.string.status_online));
        int priority = PreferenceUtils.getPrefInt(mService,
                PreferenceConstants.PRIORITY, 0);
        if (messageCarbons)
            CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
                    true);

        Presence presence = new Presence(Presence.Type.available);
        Presence.Mode mode = Presence.Mode.valueOf(statusMode);
        presence.setMode(mode);
        presence.setStatus(statusMessage);
        presence.setPriority(priority);
        mXMPPConnection.sendPacket(presence);
    }

	@Override
	public void sendMessage(String user, String message) {

	}

	@Override
	public void sendServerPing() {

	}

	@Override
	public String getNameForJID(String jid) {
		return null;
	}

    /**
     * 与服务器交互消息监听,发送消息需要回执，判断对方是否已读此消息
     */
    private void initServiceDiscovery() {
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager
                .getInstanceFor(mXMPPConnection);
        if (sdm == null)
            sdm = new ServiceDiscoveryManager(mXMPPConnection);

        sdm.addFeature("http://jabber.org/protocol/disco#info");

        // reference PingManager, set ping flood protection to 10s
        PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
                10 * 1000);
        // reference DeliveryReceiptManager, add listener

        DeliveryReceiptManager dm = DeliveryReceiptManager
                .getInstanceFor(mXMPPConnection);
        dm.enableAutoReceipts();
        dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener() {
            public void onReceiptReceived(String fromJid, String toJid,
                                          String receiptId) {
                Log.d(TAG, "got delivery receipt for " + receiptId);
               //changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);// 标记为对方已读，实际上遇到了点问题，所以其实没有用上此状态
            }
        });
    }
}
