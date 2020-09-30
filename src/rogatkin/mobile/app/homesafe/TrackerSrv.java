package rogatkin.mobile.app.homesafe;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

public class TrackerSrv extends JobService {

	private static final String TAG = "HS-service";

	@Override
	public boolean onStartJob(JobParameters params) {
		Intent service = new Intent(getApplicationContext(), LocationChkrSrv.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getApplicationContext().startForegroundService(service);
		} else
			getApplicationContext().startService(service);

		return false;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		return true;
	}

}
