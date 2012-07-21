package de.danoeh.antennapod.util;

import java.util.EnumSet;

import org.shredzone.flattr4j.FlattrFactory;
import org.shredzone.flattr4j.FlattrService;
import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.model.Thing;
import org.shredzone.flattr4j.oauth.AccessToken;
import org.shredzone.flattr4j.oauth.AndroidAuthenticator;
import org.shredzone.flattr4j.oauth.Scope;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.activity.FlattrAuthActivity;
import de.danoeh.antennapod.asynctask.FlattrTokenFetcher;
import de.danoeh.antennapod.R;

/** Utility methods for doing something with flattr. */
public class FlattrUtils {
	private static final String TAG = "FlattrUtils";

	private static final String HOST_NAME = "de.danoeh.antennapod";
	private static final String APP_KEY = "oJ5B1Z90x0gpHbu84F81fWgZWjRpKj4Y";
	private static final String APP_SECRET = "S0qKPpAcfGrNHMLCiKtnwAciJUyj3t8MjomZEN2KkjnSWZk4zEiW4k4D5sNZMLBV";

	private static final String PREF_ACCESS_TOKEN = "de.danoeh.antennapod.preference.flattrAccessToken";

	/** Flattr URL for this app. */
	public static final String APP_URL = "http://antennapod.com"; 
	public static final String APP_THING_ID = "745609";
	
	
	private static AndroidAuthenticator createAuthenticator() {
		return new AndroidAuthenticator(HOST_NAME, APP_KEY, APP_SECRET);
	}

	public static void startAuthProcess(Context context) throws FlattrException {
		AndroidAuthenticator auth = createAuthenticator();
		auth.setScope(EnumSet.of(Scope.FLATTR));
		Intent intent = auth.createAuthenticateIntent();
		context.startActivity(intent);
	}

	/**
	 * Returns the access token from the preferences or null if no access token
	 * was saved before.
	 */
	private static AccessToken retrieveToken() {
		Log.d(TAG, "Retrieving access token");
		String token = PreferenceManager.getDefaultSharedPreferences(
				PodcastApp.getInstance()).getString(PREF_ACCESS_TOKEN, null);
		if (token != null) {
			Log.d(TAG, "Found access token");
			return new AccessToken(token);
		} else {
			Log.d(TAG, "No access token found");
			return null;
		}
	}

	/** Returns true if the application has saved an access token */
	public static boolean hasToken() {
		return retrieveToken() != null;
	}

	/** Stores the token as a preference */
	public static void storeToken(AccessToken token) {
		Log.d(TAG, "Storing token");
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(PodcastApp.getInstance()).edit();
		if (token != null) {
			editor.putString(PREF_ACCESS_TOKEN, token.getToken());
		} else {
			editor.putString(PREF_ACCESS_TOKEN, null);
		}
		editor.commit();
	}

	public static void deleteToken() {
		Log.d(TAG, "Deleting flattr token");
		storeToken(null);
	}
	
	/** Get the thing that represents this app */
	public static Thing getAppThing(Context context) {
		FlattrService fs = FlattrServiceCreator.getService(retrieveToken());
		try {
			Thing thing = fs.getThing(Thing.withId(APP_THING_ID));
			Log.i(TAG, "URL is " + thing.getUrl());
			return thing;
		} catch (FlattrException e) {
			e.printStackTrace();
			showErrorDialog(context, e.getMessage());
			return null;
		}
	}

	public static void clickUrl(Context context, String url)
			throws FlattrException {
		if (hasToken()) {
			FlattrService fs = FlattrServiceCreator.getService(retrieveToken());
			fs.click(url);
		} else {
			Log.e(TAG, "clickUrl was called with null access token");
		}
	}

	public static void handleCallback(Context context, Uri uri) {
		AndroidAuthenticator auth = createAuthenticator();
		new FlattrTokenFetcher(context, auth, uri).executeAsync();
	}

	public static void revokeAccessToken(Context context) {
		Log.d(TAG, "Revoking access token");
		deleteToken();
		showRevokeDialog(context);
	}

	// ------------------------------------------------ DIALOGS

	public static void showRevokeDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.access_revoked_title);
		builder.setMessage(R.string.access_revoked_info);
		builder.setNeutralButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.create().show();
	}

	public static void showNoTokenDialog(final Context context,
			final String url) {
		Log.d(TAG, "Creating showNoTokenDialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.no_flattr_token_title);
		builder.setMessage(R.string.no_flattr_token_msg);
		builder.setPositiveButton(R.string.authenticate_now_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivity(new Intent(context,
								FlattrAuthActivity.class));
					}

				});
		builder.setNegativeButton(R.string.visit_website_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse(url);
						context.startActivity(new Intent(Intent.ACTION_VIEW,
								uri));
					}

				});
		builder.create().show();
	}

	public static void showForbiddenDialog(final Context context,
			final String url) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.action_forbidden_title);
		builder.setMessage(R.string.action_forbidden_msg);
		builder.setPositiveButton(R.string.authenticate_now_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivity(new Intent(context,
								FlattrAuthActivity.class));
					}

				});
		builder.setNegativeButton(R.string.visit_website_label,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse(url);
						context.startActivity(new Intent(Intent.ACTION_VIEW,
								uri));
					}

				});
		builder.create().show();
	}

	public static void showErrorDialog(final Context context, final String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.error_label);
		builder.setMessage(msg);
		builder.setNeutralButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.create().show();
	}
	

}