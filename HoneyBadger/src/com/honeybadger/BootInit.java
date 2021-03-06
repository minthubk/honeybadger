package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 *
 * Edit 2.1: Loads app rules
 *
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.Fetcher;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.databases.AppsDBAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootInit extends BroadcastReceiver
{
	SharedPreferences settings;
	private AppsDBAdapter appAdapter;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String startScript = "";

		settings = context.getSharedPreferences("main", 1);

		startScript = SharedMethods.initialString(startScript, context);

		startScript = SharedMethods.setLogging(startScript, settings, context);

		startScript = SharedMethods.setBlock(startScript, settings, context);

		// Launch Script
		Intent script = new Intent();
		script.setClass(context, Scripts.class);
		script.putExtra("script", startScript);
		context.startService(script);

		// reload rules
		Intent reload = new Intent();
		reload.setClass(context, Blocker.class);
		reload.putExtra("reload", "true");
		context.startService(reload);

		// reload app rules
		Intent reloadApps = new Intent();
		reloadApps.setClass(context, AppBlocker.class);
		context.startService(reloadApps);

		// reload auto-generated rules if specified
		if (settings.getBoolean("generate", false) | settings.getBoolean("autoUpdate", false))
		{
			Intent generate = new Intent();
			generate.setClass(context, Fetcher.class);
			generate.putExtra(
					"script",
					context.getDir("bin", 0)
							+ "/iptables -F FETCH"
							+ "\n"
							+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
							+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");
			context.startService(generate);
		}

		SharedMethods.loadApps(context, settings, appAdapter);
	}

}
