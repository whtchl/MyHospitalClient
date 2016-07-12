package client.jdjz.com.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by tchl on 2016-07-12.
 */
public class PreferenceUtils {
    public static String getPrefString(Context context,String key,final String defaultValue){
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key,defaultValue);
    }
}
