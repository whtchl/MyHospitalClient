package client.jdjz.com.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.TypedValue;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import client.jdjz.com.exception.XXAdressMalformedException;

public class XMPPHelper {
	private static final Pattern EMOTION_URL = Pattern.compile("\\[(\\S+?)\\]");

	public static void verifyJabberID(String jid)
			throws XXAdressMalformedException {
		if (jid != null) {
			Pattern p = Pattern
					.compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
			Matcher m = p.matcher(jid);

			if (!m.matches()) {
				throw new XXAdressMalformedException(
						"Configured Jabber-ID is incorrect!");
			}
		} else {
			throw new XXAdressMalformedException("Jabber-ID wasn't set!");
		}
	}

	public static void verifyJabberID(Editable jid)
			throws XXAdressMalformedException {
		verifyJabberID(jid.toString());
	}

	public static int tryToParseInt(String value, int defVal) {
		int ret;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException ne) {
			ret = defVal;
		}
		return ret;
	}

	public static int getEditTextColor(Context ctx) {
		TypedValue tv = new TypedValue();
		boolean found = ctx.getTheme().resolveAttribute(
				android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			return ctx.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			return ctx.getResources().getColor(
					android.R.color.primary_text_light);
		}
	}

}
