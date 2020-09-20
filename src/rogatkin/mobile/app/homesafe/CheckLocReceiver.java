package rogatkin.mobile.app.homesafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** This class is used to check loc after boot
 *
 */
public class CheckLocReceiver extends BroadcastReceiver {

	private static final String TAG = "HS-broadcast";

	@Override
	public void onReceive(Context ctx, Intent itn) {
		if (HomeSafeActivity.__debug)
			Log.d(TAG, "Got job to get a location");
		//startWakefulService(ctx, new Intent(ctx, TrackerSrv.class));
		//if (HomeSafeActivity.jobSchedulerActive)
			HomeSafeActivity.scheduleJob(ctx);
	}

}
