package rogatkin.mobile.app.homesafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import rogatkin.mobile.data.pertusin.DataAssistant;
import rogatkin.mobile.data.pertusin.IOAssistant;
import rogatkin.mobile.data.pertusin.NetAssistant;

public class Model extends SQLiteOpenHelper {
	private final static int DB_VERSION = 3;
	private static final String TAG = "HomeSafe-model";
	DataAssistant helper;
	Context ctx;

	public static final int K_EMAIL = 1;

	public static final int K_SMS = 2;


	public Model(Context context) {
		super(context, "homesafe.db", null, DB_VERSION);
		helper = new DataAssistant(context);
		ctx = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(helper.getCreateQuery(Home.class));
		db.execSQL(helper.getCreateQuery(Beneficiary.class));
		db.execSQL(helper.getCreateQuery(Home2Bens.class));
		db.execSQL(helper.getCreateQuery(NotifTask.class));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int ov, int nv) {
		if (ov == 1 && nv == 2) {
			db.execSQL("alter table Home add onLeave INTEGER");
		} else if (ov < 1) {
			db.execSQL(helper.getDropQuery(Home.class));
			db.execSQL(helper.getDropQuery(Beneficiary.class));
			db.execSQL(helper.getDropQuery(Home2Bens.class));
			onCreate(db);
		}
		if (nv == 3 && ov < 3)
			db.execSQL(helper.getCreateQuery(NotifTask.class));
	}

	void save(Smtp smtp) {
		helper.storePreferences(smtp, false);
	}

	void load(Smtp smtp) {
		helper.loadPreferences(smtp, false);
	}

	void save(ID r) {
		SQLiteDatabase db = getWritableDatabase();
		String name = helper.resolveStoreName(r.getClass());
		try {
			if (r.id > 0) {
				db.update(name, helper.asContentValues(r, false, "id"), "_id="
						+ r.id, null);
			} else {
				r.id = db.insert(name, null,
						helper.asContentValues(r, false, "id"));
			}
		} finally {
			db.close();
		}
	}

	<T> ContentValues createFilter(T pojo, boolean rev, String... fields) {
		return helper.asContentValues(pojo, rev, fields);
	}

	<T> ArrayList<T> load(ContentValues filter, Class<T> pojo, String order,
			String... fields) {
		SQLiteDatabase database = this.getWritableDatabase();
		try {
			return new ArrayList<T>(helper.select(database, pojo, filter,
					order, null, false, fields));
		} catch (NullPointerException npe) {
			return new ArrayList<T>();
		} finally {
			database.close();
		}
	}

	<T> T loadOne(ContentValues filter, T pojo,
				  String... fields) {
		SQLiteDatabase database = this.getWritableDatabase();
		try {
			return helper.select(database, pojo, filter, false, fields);
		} catch (NullPointerException npe) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, "Home", npe);
			return null;
		} finally {
			database.close();
		}
	}

	ArrayList<Home> fillBefefsCount(ArrayList<Home> homes) {
		for (Home h:homes ) {
			countBenefs(h);
		}
		return homes;
	}

	<A extends Home> void countBenefs(A a) {
		SQLiteDatabase database = this.getWritableDatabase();
		try {
			Cursor c = database.rawQuery("SELECT COUNT(*) FROM "+"Home2Bens"+" where idHome=?", new String[]{""+a.id});
			if (c.moveToNext())
			    a.contacts = c.getInt(0);
		} catch (Exception e) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, "Count", e);
		} finally {
			database.close();
		}
	}

	<T> int delete(T pojo, boolean rev, String... keys) {
		SQLiteDatabase database = this.getWritableDatabase();
		try {
			return database.delete(helper.resolveStoreName(pojo.getClass()),
					keys.length==0?"":helper.asWhere(createFilter(pojo, rev, keys)), null);
		} finally {
			database.close();
		}
	}

	Home resolveAddress(Home h) {
		Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
		try {
			List<Address> addresses = geocoder.getFromLocationName(h.address
					+ "," + h.city + " " + h.postCode, 1); // TODO more robust
															// for empty and
															// country
			if (addresses.size() > 0) {
				h.latitude = addresses.get(0).getLatitude();
				h.longitude = addresses.get(0).getLongitude();
			}
			return h;
		} catch (IOException e) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, "", e);
		}
		return null;
	}

	<A extends HomeAddr> A fillAddress(Location loc, A h) {
		if (loc == null)
			return null;
		h.latitude = loc.getLatitude();
		h.longitude = loc.getLongitude();
		Geocoder gcd = new Geocoder(ctx, Locale.getDefault());
		List<Address> addresses;
		try {
			addresses = gcd.getFromLocation(loc.getLatitude(),
					loc.getLongitude(), 1);
			if (addresses.size() > 0) {
				Address a = addresses.get(0);
				h.city = a.getLocality();
				h.address = a.getAddressLine(0);
				h.postCode = a.getPostalCode();
				h.country = a.getCountryName();
			}
		} catch (IOException e) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, "", e);
		}
		return h;
	}

	Location getCurrentLocation(LocationManager locManager) {
		if (locManager == null)
			return null;
		Location loc = getBetterLocation(
				locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER),
				null);
		loc = getBetterLocation(
				locManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
				loc);
		return getBetterLocation(
				locManager
						.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
				loc);
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected Location getBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return location;
		}
		if (location == null)
			return currentBestLocation;

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return location;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return currentBestLocation;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return location;
		} else if (isNewer && !isLessAccurate) {
			return location;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return location;
		}
		return currentBestLocation;
	}

	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	void sendTest(String m) throws IOException {
		Properties mailProp = new Properties();
		Smtp smtp = new Smtp();
		load(smtp);
		mailProp.setProperty(NetAssistant.PROP_SECURE, smtp.ssl ? "true"
				: "false");
		mailProp.setProperty(NetAssistant.PROP_MAILHOST, smtp.server);
		mailProp.setProperty(NetAssistant.PROP_PASSWORD, smtp.password);
		mailProp.setProperty(NetAssistant.PROP_POPACCNT, smtp.address);
		mailProp.setProperty(NetAssistant.PROP_MAILPORT, "" + smtp.port);
		NetAssistant net = new NetAssistant(mailProp);
		net.send(ctx.getString(R.string.app_name), smtp.address,
				ctx.getString(R.string.lb_email_test), m, null);
	}

	Bitmap pullAvatar(Beneficiary ben) throws IOException {
		URL url = new URL("http://www.gravatar.com/avatar/"
				+ IOAssistant.MD5_Hash(ben.email.trim().toLowerCase())
				+ "?s=96");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.connect();
		return BitmapFactory.decodeStream(connection.getInputStream());
	}
}
