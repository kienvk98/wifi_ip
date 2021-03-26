package com.example.wifiip;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.content.Context.WIFI_SERVICE;

/** WifiIpPlugin */
public class WifiIpPlugin implements MethodCallHandler {
  final String TAG= "WifiIpPlugin";
  final Context context;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.lulu.plugin/get_wifi_ip");
    channel.setMethodCallHandler(new WifiIpPlugin(registrar.context()));
  }

  WifiIpPlugin(Context ctx)
  {
    context = ctx;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("getWifiIp")) {
      result.success(getWifiIP());
    } else {
      result.notImplemented();
    }
  }

  public HashMap<String, String> getWifiIP() {
    HashMap<String, String> result;
    try {
      WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
      DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

      result = new HashMap<>();
      result.put("ip", intToIp(dhcpInfo.ipAddress));
//      result.put("netmask", intToIp(dhcpInfo.netmask));
      result.put("broadcastIP", intToIp((dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask));

      int bit = 24;
      try {
        List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface nif : all) {
          if (nif.getName().equalsIgnoreCase("wlan0")) {
            try {
              bit = nif.getInterfaceAddresses().get(1).getNetworkPrefixLength();
            } catch (Exception ignored) {
            }
          } else if (nif.getName().equalsIgnoreCase("eth0")) {
            try {
              bit = nif.getInterfaceAddresses().get(1).getNetworkPrefixLength();
            } catch (Exception ignored) {
            }
          }
        }
      } catch (Exception ex) {
        bit = 24;
      }

      result.put("netmask", getSubnetMark(bit));
      return result;
    } catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
    return null;
  }

  String getSubnetMark(int bit) {
    StringBuilder subnetString = new StringBuilder();
    for (int i = 1; i<=32; i++) {
      if (i <= bit) {
        subnetString.append("1");
      } else {
        subnetString.append("0");
      }
      if (i % 8 == 0 && i != 32) {
        subnetString.append(".");
      }
    }
    String[] subnets = subnetString.toString().split("\\.");
    StringBuilder subnet = new StringBuilder();
    for (int i = 0; i< subnets.length; i++ ){
      subnet.append(Integer.parseInt(subnets[i], 2));
      if(i< subnets.length - 1){
        subnet.append(".");
      }
    }
    return subnet.toString();
  }

  String intToIp(int i) {
    return (i & 0xFF) + "." +
            ((i >> 8) & 0xFF) + "." +
            ((i >> 16) & 0xFF) + "." +
            ((i >> 24) & 0xFF);
  }
}
