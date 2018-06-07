package com.pchab.android.plugin;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

public class Downloader extends CordovaPlugin {

    public static final String ACTION_DOWNLOAD = "download";

    private static final String TAG = "DownloaderPlugin";

    private Activity cordovaActivity;
    private DownloadManager downloadManager;
    private HashMap<Long, Download> downloadMap;

    @Override
    protected void pluginInitialize()
    {
        Log.d(TAG, "PluginInitialize");

        cordovaActivity = this.cordova.getActivity();

        downloadManager = (DownloadManager) cordovaActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadMap = new HashMap();

        // Register receiver for Notification actions
        cordovaActivity.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "CordovaPlugin: execute " + action);

        if (ACTION_DOWNLOAD.equals(action)) {

            Log.d(TAG, "CordovaPlugin: load " + action);
            return download(args, callbackContext);

        }

        return false;


    }    

    private boolean download(JSONArray args, CallbackContext callbackContext)
    {
        Log.d(TAG, "CordovaPlugin: " + ACTION_DOWNLOAD);

        try {

            JSONObject arg_object = args.getJSONObject(0);
            String path = arg_object.getString("path");

            Boolean hideNotification = (
                arg_object.has("hideNotification") 
                    ? arg_object.getBoolean("hideNotification")
                    : false
            );

            Uri uri = Uri.parse(arg_object.getString("url"));
            Download mDownload = new Download(path, callbackContext);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            //Restrict the types of networks over which this download may proceed.
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            //Set whether this download may proceed over a roaming connection.
            request.setAllowedOverRoaming(true);
            //Set the title of this download, to be displayed in notifications (if enabled).
            request.setTitle(path);
            //Set a description of this download, to be displayed in notifications (if enabled)
            request.setDescription("Jamendo Music");
            //Set the local destination for the downloaded file to a path within the application's external files directory
            request.setDestinationInExternalFilesDir(cordovaActivity, Environment.DIRECTORY_DOWNLOADS, path);
            // Set the notification "visible notify completed" to stay the notification display at the end of the download
            request.setNotificationVisibility(
                hideNotification
                    ? DownloadManager.Request.VISIBILITY_HIDDEN
                    : DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            // Add headers to the request if necessary
            if (arg_object.has("headers")) {
                JSONObject headers = arg_object.getJSONObject("headers");
                JSONArray headerNames = headers.names();

                for (int i = 0; i < headerNames.length(); ++i) {
                    String headerName = headerNames.getString(i);
                    request.addRequestHeader(headerName, headers.getString(headerName));

                }
            }

            // save the download
            downloadMap.put(downloadManager.enqueue(request), mDownload);

        return true;

        } catch (Exception e) {

            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());

            return false;
        }
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
  
        @Override
        public void onReceive(Context context, Intent intent) {

            DownloadManager.Query query = new DownloadManager.Query();
            Long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            query.setFilterById(downloadId);
            Cursor cursor = downloadManager.query(query);

            if (cursor.moveToFirst()){

                //Retrieve the saved download
                Download currentDownload = downloadMap.get(downloadId);
                downloadMap.remove(currentDownload);

                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int reason = cursor.getInt(columnReason);
                
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        try {
                            currentDownload.callbackContext.success(Environment.DIRECTORY_DOWNLOADS);
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            currentDownload.callbackContext.error(e.getMessage());
                        }
                        break;
                    case DownloadManager.STATUS_FAILED:
                        currentDownload.callbackContext.error(reason);
                        break;
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_PENDING:
                    case DownloadManager.STATUS_RUNNING:
                    default:
                        break;
                }
            }
        }

    };

    private class Download {
        public String path;
        public CallbackContext callbackContext;

        public Download(String path, CallbackContext callbackContext) {
            this.path = path;
            this.callbackContext = callbackContext;
        }
    }

}
