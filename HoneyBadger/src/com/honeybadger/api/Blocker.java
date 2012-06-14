package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 June 2012
 * Source Info: n/a
 * 
 * Edit 1.3: Effected by move of database adapter
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.databases.RulesDBAdapter;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class Blocker extends Service
{
	private Cursor c;
	private RulesDBAdapter ruleAdapter = new RulesDBAdapter(this);;
	private String rule = "";

	private final IBinder mBinder = new MyBinder();

	@Override
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		c.close();
	}

	public class MyBinder extends Binder
	{
		Blocker getService()
		{
			return Blocker.this;
		}
	}

	/**
	 * Called when service is started; loops through rules in rule database,
	 * generates script of rules to apply to IPTables.
	 */
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Bundle extras = intent.getExtras();
		
		// Opens rules database and creates cursor to iterate through all
		// entries
		ruleAdapter.open();
		c = ruleAdapter.fetchAllEntries();

		// Loop through rows of database
		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();

			// If rule has not yet been applied to IPTables, add it to the
			// script string. Generate its components based on the values in the
			// cells.
			if (c.getString(5).contains("false") | extras.getString("reload").contains("true"))
			{
				String drop;
				String inOut;
				if (c.getString(3).contains("allow"))
				{
					drop = "ACCEPT";
				}
				else
				{
					drop = "DROP";
				}

				if (c.getString(2).contains("out"))
				{
					inOut = "OUT";
					if (c.getString(4).contains("domain"))
					{
						// create chain with name of domain name
						rule += this.getDir("bin", 0) + "/iptables -N "
								+ c.getString(0)
								+ inOut
								+ "\n"
								// create rule(s) for domain in chain
								+ this.getDir("bin", 0) + "/iptables -A " + c.getString(0) + inOut
								+ " -d " + c.getString(0)
								+ " -m state --state NEW,RELATED,ESTABLISHED -j " + drop + inOut + "\n"
								// create rule to jump to the chain
								+ this.getDir("bin", 0) + "/iptables -I " + inOut + "PUT" + " -j "
								+ c.getString(0) + inOut + "\n";

					}
					else
					{
						rule += this.getDir("bin", 0) + "/iptables -I " + inOut + "PUT" + " -d "
								+ c.getString(0) + " -m state --state NEW,RELATED,ESTABLISHED -j "
								+ drop + inOut + "\n";
					}
				}
				else
				{
					inOut = "IN";
					if (c.getString(4).contains("domain"))
					{
						// create chain with name of domain name + direction
						rule += this.getDir("bin", 0) + "/iptables -N " + c.getString(0)
								+ inOut
								+ "\n"
								// create rule(s) for domain in chain
								+ this.getDir("bin", 0) + "/iptables -A " + c.getString(0) + inOut
								+ " -s " + c.getString(0) + " -j " + drop + inOut+ "\n"
								// create rule to jump to the chain
								+ this.getDir("bin", 0) + "/iptables -I " + inOut + "PUT" + " -j "
								+ c.getString(0) + inOut + "\n";
					}
					else
					{
						rule += this.getDir("bin", 0) + "/iptables -I " + inOut + "PUT" + " -s "
								+ c.getString(0) + " -j " + drop + inOut + "\n";
					}
				}

				// Mark rule as having been applied
				ruleAdapter.changeSaved(c.getString(0));

				// Create intent to start service which applies rules script.
				Intent intent2 = new Intent();
				intent2.setClass(this, Scripts.class);
				intent2.putExtra("script", rule);
				startService(intent2);
			}
			rule = "";
		}
		return START_STICKY;
	}

}
