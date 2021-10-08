package com.hawkxeye.online.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.apache.http.util.EncodingUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import static android.content.Context.ACTIVITY_SERVICE;

public class Utils {
    public static boolean checkTetheringPermissions(final Context context, final String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canWrite = android.provider.Settings.System.canWrite(context);

            if (canWrite == false) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setMessage("Open \"Device Settings\" to permit m-View broadcaster to configure Wifi hotspot.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                intent.setData(Uri.parse("package:" + packageName));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                try {
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e("MainActivity", "error starting permission intent", e);
                                }
                            }
                        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(packageName, "User selected not to open Device settings.");
                            }
                        });

                builder.create().show();

                /*Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);*/
            }

            return canWrite;
        }

        return true;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    //convert short to byte
    public static byte[] short2byte(short[] sData, int length) {
        int shortArrsize = length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;
    }

    public static String byteArrayToHexString(byte [] array) {
        final StringBuilder builder = new StringBuilder();
        for (byte b: array) {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }

    public static byte[] NV12toNewNV21(byte[] nv12) {
        byte[] nv21 = new byte[nv12.length];
        // Copy Luma.
        System.arraycopy(nv12, 0, nv21, 0, nv12.length / 2);
        // Swap chroma // while coping.
        for (int i = nv12.length / 2; i < nv12.length; i += 2) {
            nv21[i] = nv12[i + 1];
            nv21[i + 1] = nv12[i];
        }

        return nv21;
    }

    public static void NV12toNV21(byte[] nv12) {
        byte temp;

        // Swap chroma // while coping.
        for (int i = nv12.length / 2; i < nv12.length; i += 2) {
            temp = nv12[i];
            nv12[i] = nv12[i + 1];
            nv12[i + 1] = temp;
        }
    }

    public static long getAppFreeMemorySpace(Context context) {
        File app_dir = context.getFilesDir();
        return app_dir.getUsableSpace();
    }

    public static void openRecordingDir() {
        String[] rec_dir = {"mkdir", Constants.RECORDING_DIR};
        String[] onphone_dir = {"mkdir", Constants.ONPHONE_RECORDING_DIR};
        File rec_file_dir = new File(Constants.RECORDING_DIR);
        File rec_onphone_dir = new File(Constants.ONPHONE_RECORDING_DIR);
        if (!rec_file_dir.exists()) {
            try {
                Process ps = Runtime.getRuntime().exec(rec_dir);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!rec_onphone_dir.exists()) {
            try {
                Process ps = Runtime.getRuntime().exec(onphone_dir);
                try {
                    ps.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        return path.getUsableSpace();
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            return path.getUsableSpace();
        } else {
            return 0;
        }
    }

    public static String getStringHashPassword(String sPassword) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] pwd = EncodingUtils.getAsciiBytes(sPassword);
            byte[] messageDigest = digest.digest(pwd);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String tmp = Integer.toHexString(0xFF & messageDigest[i]);
                if (tmp.length() > 1)
                    hexString.append(tmp);
                else {
                    hexString.append("0" + tmp);
                }
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getHashOfFile(String method, String filePath, int bufLen) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[bufLen];
            MessageDigest digest = MessageDigest.getInstance(method);
            int numRead = 0;
            while ((numRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, numRead);
            }
            return digest.digest();
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean checkHashOfFile(String method, File file, int bufLen) {
        FileInputStream inputStream = null;
        boolean changed = false;
        try {
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[bufLen];
            MessageDigest digest = MessageDigest.getInstance(method);
            int digestLen = digest.getDigestLength();
            long totalLen = file.length() - digestLen;
            long totalRead = 0;
            int numRead = 0;
            int maxRead = (int) (totalLen - totalRead);
            if (maxRead > bufLen) {
                maxRead = bufLen;
            }

            while ((numRead = inputStream.read(buffer, 0, maxRead)) != -1) {
                digest.update(buffer, 0, numRead);
                totalRead += numRead;
                if (totalRead == totalLen) {
                    inputStream.read(buffer, 0, digestLen);
                    break;
                }
                maxRead = (int) (totalLen - totalRead);
                if (maxRead > bufLen) {
                    maxRead = bufLen;
                }
            }
            byte[] digestMsg = digest.digest();
            for (int i = 0; i < digestLen; i++) {
                if (buffer[i] != digestMsg[i]) {
                    changed = true;
                    break;
                }
            }
            return changed;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return changed;
        }
    }

    public static boolean checkNetwork(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo netInfo = connManager.getActiveNetworkInfo();
            if (netInfo != null) {
                //Toast.makeText( mViewMobileLive.this,"network type "+ netInfo.getTypeName(), Toast.LENGTH_SHORT).show();
                return netInfo.isConnected();
            }
        } else {
            Toast.makeText(ctx, "Connectivity manager not found", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

	// Don't call this method in main/UI thread, call in AsyncTask.
    public static boolean checkInternet(Context ctx) {

        boolean networkConnected = checkNetwork(ctx);

        if (networkConnected){
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("http://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 204 &&
                        urlc.getContentLength() == 0);
            } catch (IOException e) {
                //Log.e(TAG, "Error checking internet connection", e);
                e.printStackTrace();
            }
        }

        return false;
    }

    static public void decodeYUV420SP(int[] rgba, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int r, g, b, y1192, y, i, uvp, u, v;
        for (int j = 0, yp = 0; j < height; j++) {
            uvp = frameSize + (j >> 1) * width;
            u = 0;
            v = 0;
            for (i = 0; i < width; i++, yp++) {
                y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);

                //if (r < 0) r = 0; else if (r > 262143) r = 262143;
                //if (g < 0) g = 0; else if (g > 262143) g = 262143;
                //if (b < 0) b = 0; else if (b > 262143) b = 262143;
                r = Math.max(0, Math.min(r, 262143));
                g = Math.max(0, Math.min(g, 262143));
                b = Math.max(0, Math.min(b, 262143));
                rgba[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                //rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000) | ((b >> 2) | 0xff00);
            }
        }
    }

    public byte[] YUV420PlanarToNV21(byte[] yuvFrame, int width, int height) {
        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];
        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
}
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = nv[i];
            bytes[y.length + (i * 2) + 1] = nu[i];
        }

        return bytes;
    }

    public static void postToastMessageOnUi(final Context context, final String message, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, duration).show();
            }
        });
    }

    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     *
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     *
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static String getHostAddressFromArpTable(String MAC) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String mac = splitted[3];
                    if (mac.matches(MAC)) {
                        return splitted[0];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static boolean isServiceRunning(Context context,String serviceClassName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(service.service.getClassName().contains(serviceClassName)) {
                return true;
            }
        }
        return false;
    }
}
