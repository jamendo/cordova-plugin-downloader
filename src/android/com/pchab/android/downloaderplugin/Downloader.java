package com.pchab.android.downloaderplugin;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;

public class Downloader extends CordovaPlugin {

    public static final String ACTION_DOWNLOAD = "download";

    private static final String TAG = "DownloaderPlugin";

    private DownloadManager downloadManager;
    private HashMap<long, Download> downloadMap;

    @Override
    protected void pluginInitialize()
    {
        Log.d(TAG, "PluginInitialize");

        this.downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        this.downloadMap = new HashMap<>();

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, intentFilter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "CordovaPlugin: execute " + action);

        try {

            if (ACTION_DOWNLOAD.equals(action)) {

                Log.d(TAG, "CordovaPlugin: load " + action);
                this.download(args, callbackContext);

            }

            return true;

        } catch (Exception e) {

            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());

            return false;
        }
    }    

    private void download(JSONArray args, CallbackContext callbackContext)
    {
        Log.d(TAG, "CordovaPlugin: " + ACTION_DOWNLOAD);

        JSONObject arg_object = args.getJSONObject(0);

        Uri uri = Uri.parse(arg_object.getString("url"));
        Download mDownload = new Download(arg_object.getString("path"), callbackContext);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        long downloadId = this.downloadManager.enqueue(request);

        // save the download
        this.downloadMap.put(downloadId, mDownload);
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
  
        @Override
        public void onReceive(Context context, Intent intent) {

            DownloadManager.Query query = new DownloadManager.Query();
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            query.setFilterById(downloadId);
            Cursor cursor = this.downloadManager.query(query);

            if (cursor.moveToFirst()){

                //Retrieve the saved download
                Download currentDownload = this.downloadMap.get(downloadId);
                this.downloadMap.remove(currentDownload);

                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int reason = cursor.getInt(columnReason);
                
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        currentDownload.callbackContext.success();
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