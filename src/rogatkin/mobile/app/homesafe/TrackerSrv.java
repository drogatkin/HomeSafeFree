package rogatkin.mobile.app.homesafe;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import rogatkin.mobile.data.pertusin.NetAssistant;
import rogatkin.mobile.data.pertusin.StoreA;

import static rogatkin.mobile.app.homesafe.HomeSafeActivity.moreSecure;

public class TrackerSrv extends JobService {

	public static final  String EVENT_UPDATE_HOME = "udate-home-event";;

	private static final String TAG = "HS-service";

	@Override
	public boolean onStartJob(JobParameters params) {
		new PositionChecker(params).execute();
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		return true;
	}



	private class PositionChecker extends AsyncTask<Void, Void, Void> {
		JobParameters params;

		public PositionChecker (JobParameters _params) {
			params = _params;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Log.d(TAG, "Clean up the task here and call jobFinished...");
			jobFinished(params, true);
			super.onPostExecute(aVoid);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "Working here...");
			checkLoc();
			return null;
		}
	}

	void checkLoc() {
		Model model = new Model(getBaseContext());
		Home h = new Home();
		h.active = true;
		ArrayList<Home> homes = model.load(
				model.createFilter(h, true, "active"), Home.class, null);
		LocationManager locManager = homes.size()==0?null:(LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!checkLocPermission())
			locManager = null;
		LocationListener locListener = locManager == null?null:new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatusChanged(String provider, int status,
										Bundle extras) {
				// TODO Auto-generated method stub

			}

		};
		if (locManager != null && locManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			locManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
					locListener, Looper.getMainLooper());
		Location loc = model.getCurrentLocation(locManager);
		// loc = new Location(LocationManager.GPS_PROVIDER);
		boolean need = false;
		if (loc != null) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, " location" + loc.getLatitude());

			float[] distance = new float[3];
			for (Home h1 : homes) {
				Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
						h1.latitude, h1.longitude, distance);
				// sendDebug(String.format("%f,  %f <> %f, %f = %f",
				// loc.getLatitude(), loc.getLongitude(),
				// h1.latitude, h1.longitude, distance[0]), model);
				if (h1.onLeave && distance[0] > 1000 || !h1.onLeave
						&& distance[0] >= 0 && distance[0] < 200) { // TODO
					// configurable
					notifyBenefs(h1, model);
					h1.active = false;
					model.save(h1);
				} else
					need = true;
			}
			if (need == false) {
				if (HomeSafeActivity.__debug)
					Log.d(TAG, "all done in " + loc);
				sendMessage();
			}
		} else if (HomeSafeActivity.__debug)
			Log.d(TAG, "can't detect location");
		if (processQueue() == false && need == false){
			HomeSafeActivity.cancelAlarm(getApplicationContext());
		}
	}

	boolean checkLocPermission() {
		if (moreSecure()) {
			if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				//requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
				return false;
			}
		}
		else { //permission is automatically granted on sdk<23 upon installation
			return true;
		}
	}

	void notifyBenefs(Home h1, Model model) {
		BensByHome benf = new BensByHome();
		benf.idHome = h1.id;
		ArrayList<BensByHome> benfs = model.load(
				model.createFilter(benf, true, "idHome"), BensByHome.class,
				null, "id");

		NotifTask nt = new NotifTask();
		nt.home = h1.id;
		nt.whenCreated = System.currentTimeMillis();
		nt.name =  h1.name;

		for (BensByHome b : benfs) {
			if (HomeSafeActivity.__debug)
				Log.d(TAG, "sending " + b);

			nt.message = b.safeMessage;
			if (b.sendEmail) {
				nt.address = b.email;
				nt.kind = Model.K_EMAIL;
				queuing(nt, model);
			}
			if (b.sendSms && ((moreSecure() && checkSelfPermission(Manifest.permission.SEND_SMS)
					== PackageManager.PERMISSION_GRANTED) || !moreSecure()) ) {
				nt.address = b.sms_number;
				nt.kind = Model.K_SMS;
				queuing(nt, model);
			}
		}
	}

	void queuing(NotifTask nt, Model m) {
		m.save(nt);
	}

	private void sendMessage() {
		Log.d(TAG, "Broadcasting message");
		Intent intent = new Intent(EVENT_UPDATE_HOME);
		// You can also include some extra data.
		intent.putExtra("message", "home list updated");
		getApplicationContext( ).sendBroadcast(intent);
	}

	boolean processQueue() {
		Model model = new Model(getBaseContext());
		boolean need = false;
		ArrayList<NotifTask> notifs = model.load(null, NotifTask.class, "");
		if (notifs.size() == 0)
			return need;
		Properties mailProp;
		Smtp smtp = new Smtp();
		model.load(smtp);
		NetAssistant net = null;
		if (smtp.server == null || smtp.server.length() == 0
				|| smtp.address == null || smtp.address.length() == 0)
			mailProp = null;
		else {
			mailProp = new Properties();
			mailProp.setProperty(NetAssistant.PROP_SECURE, smtp.ssl ? "true"
					: "false");
			mailProp.setProperty(NetAssistant.PROP_MAILHOST, smtp.server);
			if (smtp.password != null && smtp.password.length() > 0)
				mailProp.setProperty(NetAssistant.PROP_PASSWORD, smtp.password);
			mailProp.setProperty(NetAssistant.PROP_POPACCNT, smtp.address);
			mailProp.setProperty(NetAssistant.PROP_MAILPORT, "" + smtp.port);
			net = new NetAssistant(mailProp);
		}
		SmsManager sms = SmsManager.getDefault();
		for(NotifTask nt:notifs) {
			switch(nt.kind) {
				case Model.K_EMAIL:
					try {
						if (net == null)
							throw new IOException(("No mail setup"));
						net.send(getString(R.string.app_name), nt.address, String
										.format(getString(R.string.lb_notif_subj),
												nt.whenCreated, nt.name),
								nt.message, null);
						model.delete(nt, true, "id");
					} catch (IOException e) {
						nt.failMessage = e.toString();
						nt.lastFailed = System.currentTimeMillis();
						model.save(nt);
						need = true;
						if (HomeSafeActivity.__debug)
							Log.e(TAG, "", e);
					}
					break;
				case Model.K_SMS:
					if ((moreSecure() && checkSelfPermission(Manifest.permission.SEND_SMS)
							== PackageManager.PERMISSION_GRANTED) || !moreSecure())
						try {
							sms.sendTextMessage(nt.address, null, nt.message, null,
									null);
							model.delete(nt, true, "id");
						} catch(Exception e) {
							nt.failMessage = e.toString();
							nt.lastFailed = System.currentTimeMillis();
							model.save(nt);
							need = true;
							if (HomeSafeActivity.__debug)
								Log.e(TAG, "", e);
						}
					else {
						nt.failMessage = "No SMS permission";
						nt.lastFailed = System.currentTimeMillis();
						model.save(nt);
						need = true;
						if (HomeSafeActivity.__debug)
							Log.e(TAG, "no sms permission");
					}
					break;
					default:
			}
		}
		return need;
	}


	@StoreA(sql = "Beneficiary inner join Home2Bens on (Beneficiary._id=Home2Bens.idBeneficary)")
	public static class BensByHome extends Beneficiary {
		@StoreA()
		public long idHome;
	}
}
