package com.homelinux.michele.ipconfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.widget.RemoteViews;

public class IpWidgetProvider extends AppWidgetProvider {

	private static final String GW_REMOTE = "192.168.1.200";
	private static final String GW_LOCAL = "192.168.1.254";
	public static String WIDGET_BUTTON = "com.michele.homelinux.WIDGET_BUTTON";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals(WIDGET_BUTTON)) {
			String gw = getCurrentGateway(context);
			try {

				if (GW_LOCAL.equals(gw)) {
					setCurrentGateway(context, InetAddress.getByName(GW_REMOTE));
				} else if (GW_REMOTE.equals(gw)) {
					setCurrentGateway(context, InetAddress.getByName(GW_LOCAL));
				}
//				AppWidgetManager appWidgetManager = AppWidgetManager
//						.getInstance(context);
//				updateView(context, appWidgetManager);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} else {
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			updateView(context, appWidgetManager);
		}
		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		updateView(context, appWidgetManager);
	}

	private void updateView(Context context, AppWidgetManager appWidgetManager) {
		// Get all ids
		ComponentName thisWidget = new ComponentName(context,
				IpWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout);

			String gw = getCurrentGateway(context);
			remoteViews.setTextViewText(R.id.update, gw);

			// Register an onClickListener
			Intent intent = new Intent(context, IpWidgetProvider.class);

			intent.setAction(WIDGET_BUTTON);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
	}

	private String getCurrentGateway(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
		if (networks != null) {
			for (WifiConfiguration wifiConfiguration : networks) {
				if ("\"michele\"".equals(wifiConfiguration.SSID)) {
					try {
						InetAddress gateway = getGateway(wifiConfiguration);
						if (gateway == null) {
							return "NULL";
						} else {
							return gateway.getHostAddress();
						}
					} catch (Exception e) {
						e.printStackTrace();
						return "ERROR";
					}
				}
			}
		}
		return "NULL";
	}

	private Object getDeclaredField(Object obj, String name)
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		Object out = f.get(obj);
		return out;
	}

	private Object getField(Object obj, String name) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		Field f = obj.getClass().getField(name);
		Object out = f.get(obj);
		return out;
	}

	private InetAddress getGateway(WifiConfiguration wifiConf)
			throws SecurityException, IllegalArgumentException,
			NoSuchFieldException, IllegalAccessException,
			ClassNotFoundException, NoSuchMethodException,
			InstantiationException, InvocationTargetException {
		Object linkProperties = getField(wifiConf, "linkProperties");
		if (linkProperties == null)
			return null;

		ArrayList mRoutes = (ArrayList) getDeclaredField(linkProperties,
				"mRoutes");
		if (mRoutes != null && mRoutes.size() > 0) {
			Object mGateway = mRoutes.get(0);
			Class cls = Class.forName("android.net.RouteInfo");
			Method method = cls.getDeclaredMethod("getGateway", null);
			return (InetAddress) method.invoke(mGateway, null);
		} else {
			return null;
		}
	}

	private void setCurrentGateway(Context context, InetAddress gateway) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null && wifiManager.isWifiEnabled()) {
			List<WifiConfiguration> networks = wifiManager
					.getConfiguredNetworks();
			for (WifiConfiguration wifiConfiguration : networks) {
				if ("\"michele\"".equals(wifiConfiguration.SSID)) {
					try {
						setGateway(gateway, wifiConfiguration);
						wifiManager.updateNetwork(wifiConfiguration);
						wifiManager.setWifiEnabled(false);
						wifiManager.setWifiEnabled(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void setGateway(InetAddress gateway, WifiConfiguration wifiConf)
			throws SecurityException, IllegalArgumentException,
			NoSuchFieldException, IllegalAccessException,
			ClassNotFoundException, NoSuchMethodException,
			InstantiationException, InvocationTargetException {
		Object linkProperties = getField(wifiConf, "linkProperties");
		if (linkProperties == null)
			return;
		Class routeInfoClass = Class.forName("android.net.RouteInfo");
		Constructor routeInfoConstructor = routeInfoClass
				.getConstructor(new Class[] { InetAddress.class });
		Object routeInfo = routeInfoConstructor.newInstance(gateway);

		ArrayList mRoutes = (ArrayList) getDeclaredField(linkProperties,
				"mRoutes");
		mRoutes.clear();
		mRoutes.add(routeInfo);
	}
}
