package com.moko.mknbplughex.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.moko.mknbplughex.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import androidx.core.content.FileProvider;

public class Utils {

    /**
     * @Date 2021/12/27
     * @Author wenzheng.liu
     * @Description 兼容Android 11
     * @ClassPath com.moko.mknbplughex.utils.Utils
     */
    public static void sendEmail(Context context, String address, String body, String subject, String tips, File... files) {
        if (files.length == 0) {
            return;
        }
        Intent intent;
        if (files.length == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri fileUri = IOUtils.insertDownloadFile(context, files[0]);
                intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri;
                if (BuildConfig.IS_LIBRARY) {
                    uri = FileProvider.getUriForFile(context, "com.moko.mknbplug.fileprovider", files[0]);
                } else {
                    uri = FileProvider.getUriForFile(context, "com.moko.mknbplughex.fileprovider", files[0]);
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
            } else {
                Uri uri = Uri.fromFile(files[0]);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
            }
            intent.putExtra(Intent.EXTRA_TEXT, body);
        } else {
            ArrayList<Uri> uris = new ArrayList<>();
            ArrayList<CharSequence> charSequences = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Uri fileUri = IOUtils.insertDownloadFile(context, files[i]);
                    uris.add(fileUri);
                }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri uri;
                    if (BuildConfig.IS_LIBRARY) {
                        uri = FileProvider.getUriForFile(context, "com.moko.mknbplug.fileprovider", files[0]);
                    } else {
                        uri = FileProvider.getUriForFile(context, "com.moko.mknbplughex.fileprovider", files[0]);
                    }
                    uris.add(uri);
                } else {
                    uris.add(Uri.fromFile(files[i]));
                }
                charSequences.add(body);
            }
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            intent.putExtra(Intent.EXTRA_TEXT, charSequences);
        }
        String[] addresses = {address};
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.setType("message/rfc822");
        Intent.createChooser(intent, tips);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static String getWifiSSID(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        return info.getSSID();
    }

    /**
     * 检查网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }

        return true;
    }

    public static boolean pingIPAddress(String ipAddress) {
        try {
            //-c 1是指ping的次数为1次，-w 3是指超时时间为3s
            Process process = Runtime.getRuntime()
                    .exec("ping -c 1 -w 3 " + ipAddress);
            //status为0表示ping成功
            int status = process.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getVersionInfo(Context context) {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packInfo != null) {
            String version = packInfo.versionName;
            return version;
        }
        return "";
    }

    /**
     * 手机是否开启位置服务，如果没有开启那么所有app将不能使用定位功能
     */
    public static boolean isLocServiceEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    public static DecimalFormat getDecimalFormat(String pattern) {
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(dfs);
        return decimalFormat;
    }

    public static String calendar2strDate(Calendar calendar, String pattern, String timeZoneId) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZoneId));
        return sdf.format(calendar.getTime());
    }

    public static String getRandomStr(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            builder.append(str.charAt(number));
        }
        return builder.toString();
    }
}
