package rogatkin.mobile.app.homesafe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import rogatkin.mobile.data.pertusin.UIAssistant;

public class HomeSafeActivity extends Activity {
	public static final String TAG = "HomeSafe";
	public static final int RECHECK_LOC_INTERVAL_MIN = 2;
	static final int JOB_ID = 11;
	static int REQ_SMS_PERM = 1;
	UIAssistant vc;
	Model model;
	//static boolean permGranted;
	int cachedTheme;

	static boolean jobSchedulerActive;

	static final boolean __debug = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		super.onCreate(savedInstanceState);
		vc = new UIAssistant(this);
		model = new Model(this);
		applyTheme();
		setContentView(R.layout.activity_home_safe);
		if (!checkLocPermission()) {
			//permGranted = false;
			return;
		} //else
			//permGranted = true;

		try {
			loadHomeList();
		} catch (Exception e) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setPositiveButton("OK", null)
					.setTitle(getString(R.string.app_name))
					.setMessage(
							String.format(getString(R.string.err_db_integrity),
									e))
					.setCancelable(false)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									finish();
								}
							}).show();
			return;

		}
		setupAlarm();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mMessageReceiver,
				new IntentFilter(TrackerSrv.EVENT_UPDATE_HOME));
		try {
			refreshHomeList();
		} catch (Exception e) {

		}
		// updateLocationInfo(); // TODO async task
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home_safe, menu);
		return true;
	}

	@Override
	protected void onPause() {
		// Unregister since the activity is about to be closed.
		unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	boolean checkLocPermission() {
		if (moreSecure()) {
			if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
				return false;
			}
		}
		else { //permission is automatically granted on sdk<23 upon installation
			return true;
		}
	}

	void applyTheme() {
		Smtp settings = new Smtp();
		model.load(settings);
		if (settings.theme == 0) {
			setTheme(R.style.AppThemeDark);
		} else if (settings.theme == 2 && android.os.Build.VERSION.SDK_INT >= 29 /*android.os.Build.VERSION_CODES.*/) {
			switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
				case Configuration.UI_MODE_NIGHT_YES:
					setTheme(R.style.AppThemeDark);
					break;
				case Configuration.UI_MODE_NIGHT_NO:
					// do nothing
					break;
			}
		} else
			setTheme(R.style.AppTheme);
		cachedTheme = settings.theme;
	}

	public static boolean moreSecure() {
		return Build.VERSION.CODENAME.equals("MNC") || Build.VERSION.SDK_INT >= 23;
	}

	public void onAddHome(View btn) {
		Home h = new Home();
		h.active = true;
		setContentView(R.layout.home_det);
		setTitle(String.format(getString(R.string.lb_title_fmt),
				getString(R.string.app_name), getString(R.string.lb_new)));
		vc.fillView(this, this, h);
	}

	public void onLandAddress(View btn) {
		Home h = new Home();
		vc.fillModel(this, this, h);
		new AsyncTask<Home, Void, Home>() {

			@Override
			protected Home doInBackground(Home... params) {
				LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				if (locManager == null) {
					return null;
				}
				Location loc = model.getCurrentLocation(locManager);
				if (loc != null)
					model.fillAddress(loc, params[0]);
				return params[0];
			}

			@Override
			protected void onPostExecute(Home h) {
				if (h == null)
					Toast.makeText(HomeSafeActivity.this,
							R.string.err_no_location, Toast.LENGTH_LONG).show();
				else {
					vc.fillView(HomeSafeActivity.this, HomeSafeActivity.this, h);
				}
				super.onPostExecute(h);
			}

		}.execute(h);
	}

	public void onManageBen(View btn) {
		Home h = new Home();
		vc.fillModel(this, this, h);
		model.save(h);
		setContentView(R.layout.benf_lt);
		setTitle(String.format(getString(R.string.lb_title_fmt),
				getString(R.string.app_name), h.name));
		loadBenList(h);
	}

	public void onUpdateBen(View btn) {
		Long id = (Long)btn.getTag();
		if (id == null || id <= 0)
			return;
		Home h = new Home();
		h.id = id;
		h = model.loadOne(model.createFilter(h, true, "id"), h);
		setContentView(R.layout.benf_lt);
		setTitle(String.format(getString(R.string.lb_title_fmt),
				getString(R.string.app_name), h.name));
		loadBenList(h);
	}

	public void onAddBenf(View btn) {
		Holder<Home> hl = new Holder<Home>(null);
		vc.fillModel(this, this, hl);
		setContentView(R.layout.benf_det);
		Beneficiary b = new Beneficiary();
		vc.fillView(this, this, b);
		vc.fillView(HomeSafeActivity.this, this, hl);
		setTitle(String.format(getString(R.string.lb_title_fmt),
				getString(R.string.app_name), hl.get().name));
	}

	public void onTakePhoto(View btn) {
		Beneficiary b = new Beneficiary();
		vc.fillModel(this, this, b);
		Log.d(TAG, "Before photo " + b);
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		float scale = getResources().getDisplayMetrics().density;
		intent.putExtra("outputX", (int ) (48 * scale));
		intent.putExtra("outputY", (int ) (48 * scale));
		intent.putExtra("scale", true);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", false);
		if (intent.resolveActivity(getPackageManager()) != null)
			startActivityForResult(Intent.createChooser(intent,getString(R.string.tit_resizeim)), 1);
	}

	synchronized public void onTestEmail(View btn) {
		Smtp smtp = new Smtp();
		vc.fillModel(this, this, smtp);
		model.save(smtp);
		new AsyncTask<Void, Void, String>() {

			ProgressDialog progDlg;

			@Override
			protected String doInBackground(Void... params) {
				try {
					model.sendTest(getString(R.string.lb_email_working));

				} catch (IOException e) {
					if (__debug)
						Log.e(TAG, "", e);
					return String.format(getString(R.string.err_send_email), e);
				}
				return getString(R.string.lb_email_working);
			}

			@Override
			protected void onPostExecute(String result) {
				progDlg.dismiss(); // TODO localize
				Toast.makeText(getApplicationContext(), result,
						Toast.LENGTH_LONG).show();
				super.onPostExecute(result);
			}

			@Override
			protected void onPreExecute() {
				progDlg = ProgressDialog.show(HomeSafeActivity.this,
						getString(R.string.lb_email_test),
						getString(R.string.lb_wait), true);
				super.onPreExecute();
			}
		}.execute();
	}

	public void onClearPhoto(View btn) {
		Beneficiary b = new Beneficiary();
		vc.fillModel(this, this, b);
		b.photo = null;
		vc.fillView(this, this, b);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			// STack current view and then return it back
			Smtp em = new Smtp();
			model.load(em);
			setContentView(R.layout.em_det);
			vc.fillView(this, this, em);
			setTitle(String.format(getString(R.string.lb_title_fmt),
					getString(R.string.app_name), em.address == null ? "?"
							: em.address));
			return true;
		} else if (item.getItemId() == R.id.action_clear) {
			model.delete(new NotifTask(), true);
			// TODO update pending nots count
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (findViewById(R.id.ib_land_ad) != null) {
			Home h = new Home();
			vc.fillModel(this, this, h);
			if (h.name != null && h.name.length() > 0) {
				if (h.latitude == 0 || h.longitude == 0)
					if (h.address != null && h.address.length() > 0)
						model.resolveAddress(h);
				if (__debug)
					Log.d(TAG, "Save " + h.name);
				model.save(h);
				setupAlarm();
			} else if (h.address != null && h.address.length() > 1
					|| h.latitude != 0 || h.longitude != 0) {
				Toast.makeText(this, R.string.err_no_tag_name,
						Toast.LENGTH_SHORT).show();
				return;
			}

			returnMainView();
			return;
		} else if (findViewById(R.id.cb_ssl) != null) {
			Smtp em = new Smtp();
			vc.fillModel(this, this, em);
			model.save(em);
			if (em.theme != cachedTheme)
				recreate();
			returnMainView();
			return;
		} else if (findViewById(R.id.eb_templ) != null) {
			Beneficiary b = new Beneficiary();
			vc.fillModel(this, this, b);
			if (b.name != null && b.name.length() > 0) {
				if (b.safeMessage == null || b.safeMessage.length() == 0)
					b.safeMessage = getString(R.string.lb_mes_tmpl);
				if (__debug)
					Log.d(TAG, "Saving " + b);
				model.save(b);
				if (b.sendSms) {
					if (moreSecure()) {
						if (checkSelfPermission(Manifest.permission.SEND_SMS)
								!= PackageManager.PERMISSION_GRANTED) {
							requestPermissions(new String[]{android.Manifest.permission.SEND_SMS}, REQ_SMS_PERM);
						}
					}
				}
				Home2Bens h2b = new Home2Bens();
				h2b.idBeneficary = b.id;
				Holder<Home> hl = new Holder<Home>(null);
				vc.fillModel(this, this, hl);
				h2b.idHome = hl.get().id;
				model.delete(h2b, false, "id");
				model.save(h2b);
			}
			returnBenfView();
			return;
		} else if (findViewById(R.id.bt_add_benf) != null) {
			returnMainView();
			return;
		}
		super.onBackPressed();
	}

	void returnMainView() {
		setContentView(R.layout.activity_home_safe);
		setTitle(R.string.app_name);
		updateLocationInfo();
		loadHomeList();
	}

	void returnBenfView() {
		Holder<Home> hl = new Holder<Home>(null);
		vc.fillModel(this, this, hl);
		setContentView(R.layout.benf_lt);
		setTitle(String.format(getString(R.string.lb_title_fmt),
				getString(R.string.app_name), hl.get().name));
		loadBenList(hl.get());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		ListView listView = (ListView) findViewById(R.id.lt_homes);
		if (listView != null) {
			Home h = (Home) listView.getItemAtPosition(info.position);
			switch (item.getItemId()) {
			case R.id.cm_modify:
				setContentView(R.layout.home_det);
				setTitle(String.format(getString(R.string.lb_title_fmt),
						getString(R.string.app_name), h.name));
				vc.fillView(this, this, h);
				break;
			case R.id.cm_delete:
				model.delete(h, true, "id");
				Home2Bens hb = new Home2Bens();
				hb.idHome = h.id;
				model.delete(hb, true, "idHome");
				((ArrayAdapter) listView.getAdapter()).remove(h);
				((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
				break;
			default:
				return super.onContextItemSelected(item);
			}
			return true;
		}
		listView = (ListView) findViewById(R.id.lt_benfs);
		if (listView != null) {
			Beneficiary b = (Beneficiary) listView
					.getItemAtPosition(info.position);
			switch (item.getItemId()) {
			case R.id.cm_modify:
				Holder<Home> hl = new Holder<Home>(null);
				vc.fillModel(this, this, hl);
				setContentView(R.layout.benf_det);
				vc.fillView(this, this, b);
				vc.fillView(HomeSafeActivity.this, this, hl);
				setTitle(String.format(getString(R.string.lb_title_fmt),
						getString(R.string.app_name), hl.get().name));
				if (b.photo == null && b.email != null && b.email.indexOf('@') > 0) {
					new AsyncTask<Beneficiary, Void, Bitmap>() {

						@Override
						protected void onPostExecute(Bitmap result) {
							if (result != null) {
								ImageView iv = (ImageView)findViewById(R.id.im_photo);
								iv.setImageBitmap(result);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								result.compress(Bitmap.CompressFormat.PNG, 85, bos);
								iv.setTag(bos.toByteArray());
							}
							super.onPostExecute(result);
						}

						@Override
						protected Bitmap doInBackground(Beneficiary... params) {
							try {
								return model.pullAvatar(params[0]);
							} catch (IOException e) {
								if (__debug)
									Log.e(TAG, "", e);
								return null;
							}
						}

					}.execute(b);
				}
				break;
			case R.id.cm_delete:
				model.delete(b, true, "id");
				Home2Bens hb = new Home2Bens();
				hb.idBeneficary = b.id;
				model.delete(hb, true, "idBeneficary");
				((ArrayAdapter) listView.getAdapter()).remove(b);
				((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
				break;
			default:
				return super.onContextItemSelected(item);
			}
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		// Log.d(TAG, "" + v + "  " + v.findViewById(R.id.lt_shop_it));
		if (v instanceof ListView) {
			inflater.inflate(R.menu.ctm_home, menu);
			// TODO check id to issue right header
			menu.setHeaderTitle(R.string.lb_mh_home);
		}
	}

	void refreshHomeList() {
		ListView listView = (ListView) findViewById(R.id.lt_homes);
		if (listView == null)
			return;
		updateLocationInfo();
		ArrayAdapter<Home> aa = (ArrayAdapter<Home>) listView.getAdapter();
		aa.clear();
		for (Home h : model.load(null, Home.class, null)) {
			model.countBenefs(h);
			aa.add(h);
		}
		aa.notifyDataSetChanged();
	}

	void updateLocationInfo() {
		new AsyncTask<Void, Void, HomeAddr>() {

			@Override
			protected HomeAddr doInBackground(Void... params) {
				HomeAddr h = new HomeAddr();
				LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				if (locManager == null) {
					return null;
				}
				Location loc = model.getCurrentLocation(locManager);
				if (loc != null)
					model.fillAddress(loc, h);
				return h;
			}

			@Override
			protected void onPostExecute(HomeAddr ha) {
				if (ha == null)
					Toast.makeText(HomeSafeActivity.this,
							R.string.err_no_location, Toast.LENGTH_LONG).show();
				else {
					if (findViewById(R.id.bt_add_home) != null)
						vc.fillView(HomeSafeActivity.this,
								HomeSafeActivity.this, ha);
				}
				super.onPostExecute(ha);
			}

		}.execute();

	}

	void loadHomeList() {
		ListView listView = (ListView) findViewById(R.id.lt_homes);
		if (listView == null)
			return;
		registerForContextMenu(listView); // ///

		listView.setAdapter(new ArrayAdapter<Home>(this,
				android.R.layout.simple_list_item_1, model.fillBefefsCount(model.load(null,
						Home.class, null))) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Home si = getItem(position);
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.home_it,
							null);
				}
				vc.fillView(HomeSafeActivity.this, convertView, si, true);
				// TODO ADD POSSIBILITY TO SPECIFY ELEMENT TAGS
				View vv = convertView.findViewById(R.id.tx_benfs);
				if (vv != null)
					vv.setTag( si.id);
				return convertView;
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Home h = (Home) parent.getAdapter().getItem(position);
				h.active = !h.active;
				// TODO check if active and Home location matches current, then
				// warn
				vc.fillView(HomeSafeActivity.this, v, h, true);
				model.save(h);
				if (h.active && isAlarmActive() == false)
					setupAlarm();
			}
		});
		ArrayList<NotifTask> ntasks = model.load(null, NotifTask.class, "");
		((TextView)findViewById(R.id.eb_queue)).setText(""+ntasks.size());
	}

	void loadBenList(Home home) {
		vc.fillView(this, this, new Holder<Home>(home));
		ListView listView = (ListView) findViewById(R.id.lt_benfs);
		if (listView == null)
			return;
		registerForContextMenu(listView); // ///
		listView.setAdapter(new ArrayAdapter<Beneficiary>(this,
				android.R.layout.simple_list_item_1, model.load(null,
						Beneficiary.class, null)) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Beneficiary b = getItem(position);
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.ben_it,
							null);
					/*
					 * Holder<Home> hl = new Holder<Home>(null);
					 * vc.fillModel(HomeSafeActivity.this,
					 * HomeSafeActivity.this, hl);
					 * vc.fillView(HomeSafeActivity.this, convertView, hl,
					 * true);
					 */
				}
				Holder<Home> hl = new Holder<Home>(null);
				vc.fillModel(HomeSafeActivity.this, HomeSafeActivity.this, hl);
				Home2Bens lnk = new Home2Bens();
				lnk.idBeneficary = b.id;
				lnk.idHome = hl.get().id;
				b.linked = model.load(model.createFilter(lnk, false, "id"),
						Home2Bens.class, null).size() == 1;
				vc.fillView(HomeSafeActivity.this, convertView, b, true);
				return convertView;
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Beneficiary b = (Beneficiary) parent.getAdapter().getItem(
						position);
				Holder<Home> hl = new Holder<Home>(null);
				vc.fillModel(HomeSafeActivity.this, HomeSafeActivity.this, hl);
				Home2Bens lnk = new Home2Bens();
				lnk.idBeneficary = b.id;
				lnk.idHome = hl.get().id;
				b.linked = true;
				if (model.delete(lnk, false, "id") == 0)
					model.save(lnk);
				else
					b.linked = false;
				vc.fillView(HomeSafeActivity.this, v, b, true);
			}
		});

	}

	void setupAlarm() {
		ArrayList<Home> homes = model.load(
				model.createFilter(new Home(true), true, "active"), Home.class, null);
		if (__debug)
			Log.d(TAG, "Set alarm for "+homes.size());
		if (homes.size() == 0)
			return;
		scheduleJob(this);
	}

	boolean isAlarmActive() {
		if (__debug)
			Log.d(TAG, "query alarm");

		return jobSchedulerActive;
	}

	static void cancelAlarm(Context ctx) {
		JobScheduler jobScheduler =
				(JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		jobScheduler.cancel(JOB_ID);
		jobSchedulerActive = false;
	}

	public static void scheduleJob(Context context) {
		ComponentName serviceComponent = new ComponentName(context, TrackerSrv.class);
		JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
		builder.setPeriodic(RECHECK_LOC_INTERVAL_MIN * 60 * 1000);
		//if (android.os.Build.VERSION.SDK_INT >= 28)
		//	builder.setImportantWhileForeground(true);
		//builder.setOverrideDeadline(60 * 1000);
		JobScheduler jobScheduler =
				(JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
		jobScheduler.schedule(builder.build());
		jobSchedulerActive = true;
		if (__debug)
			Log.d(TAG, "Schedule job");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Beneficiary b = new Beneficiary();
		vc.fillModel(this, this, b);
		Log.d(TAG, "After photo " + b+" data: "+data+" code: "+requestCode);
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (data == null) {
				Toast.makeText(this, R.string.err_no_photo_taken,
						Toast.LENGTH_SHORT).show();
				return;
			}
			ImageView myImage = (ImageView) findViewById(R.id.im_photo);
			final Bundle extras = data.getExtras();
			Bitmap imageBitmap = null;
			if (extras != null) {
				Log.d(TAG, "extras " + extras+" data: "+ extras.getParcelable("data"));
				if (data.getAction() != null) {
					imageBitmap = (Bitmap) extras.get("data");
				}
			} else {
				Uri imageUri = data.getData();
				// Can imageUri be null?
				Log.d(TAG, "photo uri " + imageUri);
				try {
					float scale = getResources().getDisplayMetrics().density;
					imageBitmap = getBitmapFromReturnedImage(imageUri,  (int)(48 * scale),  (int)(48 * scale));
				} catch (IOException ioe) {
					if (__debug)
						Log.e(TAG, "", ioe);
				}
			}
			if (myImage != null && imageBitmap != null) {
				myImage.setImageBitmap(imageBitmap);
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				try {
					imageBitmap.compress(Bitmap.CompressFormat.PNG, 85,
							bOut);
					bOut.flush();
					myImage.setTag(bOut.toByteArray());
				} catch (IOException ioe) {
					if (__debug)
						Log.e(TAG, "", ioe);
				} finally {
					try {
						bOut.close();
					} catch (Exception e) {
					}
				}
			}
			b = new Beneficiary();
			vc.fillModel(this, this, b);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
										   int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_SMS_PERM) {
			if (checkSelfPermission(Manifest.permission.SEND_SMS)
					!= PackageManager.PERMISSION_GRANTED)
				Toast.makeText(this,R.string.err_no_perm_sms, Toast.LENGTH_SHORT).show();
		}
	}

	public Bitmap getBitmapFromReturnedImage(Uri selectedImage, int reqWidth, int reqHeight) throws IOException {
		InputStream inputStream = getContentResolver().openInputStream(selectedImage);

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(inputStream, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// close the input stream
		inputStream.close();

		// reopen the input stream
		inputStream = getContentResolver().openInputStream(selectedImage);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeStream(inputStream, null, options);
	}

	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;

		int stretch_width = Math.round((float)width / (float)reqWidth);
		int stretch_height = Math.round((float)height / (float)reqHeight);

		if (stretch_width <= stretch_height)
			return stretch_height;
		else
			return stretch_width;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			String message = intent.getStringExtra("message");
			Log.d(TAG, "Got message: " + message);
			try {
				refreshHomeList();
			} catch (Exception e) {

			}
		}
	};
}
