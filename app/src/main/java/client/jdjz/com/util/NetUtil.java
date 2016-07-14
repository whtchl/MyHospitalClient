package client.jdjz.com.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.util.Log;

public class NetUtil {
	public static final String TAG="tchl NetUtil";
	public static final int NETWORN_NONE = 0;
	public static final int NETWORN_WIFI = 1;
	public static final int NETWORN_MOBILE = 2;

	public static int getNetworkState(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Wifi
		State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();

		if (state == State.CONNECTED || state == State.CONNECTING) {
            Log.i(TAG,"NETWORN_WIFI");
			return NETWORN_WIFI;
		}
		
		// 3G
		state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState();
		if (state == State.CONNECTED || state == State.CONNECTING) {
            Log.i(TAG,"NETWORN_MOBILE");
			return NETWORN_MOBILE;
		}
        Log.i(TAG,"NETWORN_NONE");
		return NETWORN_NONE;
	}
}
