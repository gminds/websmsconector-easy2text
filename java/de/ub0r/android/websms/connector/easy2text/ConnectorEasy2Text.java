/*
 * Copyright (C) 2010-2011 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms.connector.easy2text;

import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.BasicConnector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import org.apache.http.HttpResponse;
import android.content.Intent;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * AsyncTask to manage IO to easy2text.com API.
 * 
 * @author flx
 */
public class ConnectorEasy2Text extends Connector {
	/** Tag for output. */
	private static final String TAG = "easy2text";

	/** easy2text Gateway URL. */
	private static final String URL_SEND = // .
	"http://easy2text.com/index.php?app=webservices&ta=pv";
	/** easy2text Gateway URL. */
	private static final String URL_BALANCE = // .
	"http://easy2text.com/index.php?app=webservices&ta=cr";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_easy2text_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
				context.getString(R.string.connector_easy2text_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector(TAG, name, SubConnectorSpec.FEATURE_MULTIRECIPIENTS);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Check return code from easy2text.com.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ret
	 *            return code
	 * @return true if no error code
	 */
	private boolean checkReturnCode(final Context context, final int ret) {
		Log.d(TAG, "ret=" + ret);
		/*
		  switch (ret) {
                case 'OK':
                        return true;
                case 'ERR 100':
                        throw new WebSMSException(context, R.string.error_cherry_10);
                case 'ERR 101':
                        throw new WebSMSException(context, R.string.error_cherry_20);
                case 'ERR 102':
                        throw new WebSMSException(context, R.string.error_cherry_30);
                case 'ERR 200':
                        throw new WebSMSException(context, R.string.error_cherry_31);
                case 'ERR 201':
                        throw new WebSMSException(context, R.string.error_cherry_40);              
                default:
                        throw new WebSMSException(context, R.string.error, " code: " + ret);
                }
		
		*/
		
		
		
				
		if (ret < 200) {
			return true;
		} else if (ret < 300) {
			throw new WebSMSException(context, R.string.error_input);
		} else {
			if (ret == 401) {
				throw new WebSMSException(context, R.string.error_pw);
			}
			throw new WebSMSException(context, R.string.error_server, // .
					" " + ret);
		}
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 */
	private void sendData(final Context context, final ConnectorCommand command) {
		// do IO
		try { // get Connection
			final String text = command.getText();
			final boolean checkOnly = (text == null || text.length() == 0);
			final StringBuilder url = new StringBuilder();
			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (checkOnly) {
				url.append(URL_BALANCE);
			} else {
				url.append(URL_SEND);
			}
			 url.append("?u=");
                        url.append(URLEncoder.encode(p
                                        .getString(Preferences.PREFS_USER, "")));
			
			url.append("&p=");
			url.append(p.getString(Preferences.PREFS_PASSWORD, ""));

			if (!checkOnly) {
			url.append("&from=");
			url.append(Utils.getSender(context, command.getDefSender())
					.replace("+", ""));
					
				url.append("&msg=");
				url.append(URLEncoder.encode(text));
				url.append("&to=");
				url.append(Utils.joinRecipientsNumbers(command.getRecipients(),
						",", true));
			}
			// send data
			HttpResponse response = Utils.getHttpClient(url.toString(), null,
					null, null, null, false);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				this.checkReturnCode(context, resp);
				throw new WebSMSException(context, R.string.error_http, " "
						+ resp);
			}
			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();
			String[] lines = htmlText.split("\n");
			Log.d(TAG, "--HTTP RESPONSE--");
			Log.d(TAG, htmlText);
			Log.d(TAG, "--HTTP RESPONSE--");
			htmlText = null;
			for (String s : lines) {
				if (s.startsWith("OK ")) {
					cs.setBalance(s.split(" ")[1].trim());
				}
			}
		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent) {
		this.sendData(context, new ConnectorCommand(intent));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent) {
		this.sendData(context, new ConnectorCommand(intent));
	}
}
