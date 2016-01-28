package com.jamendo.cache;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.jamendo.JamendoContext;
import com.jamendo.data.Track;

import java.io.File;

/**
 * Utility class for downloading files. Once the requested file is downloaded
 * the callback, specified at request time, is invoked.
 */
public class FileDownloader {

    /**
     * Extension for downloaded files
     */
    private final static String TRACK_FILE_EXT = ".mp3";

    /**
     * Local folder in {@see Environment.DIRECTORY_MUSIC} for Jamendo downloaded tracks.
     */
    private final static String JAMENDO_FOLDER = "Jamendo";

    /**
     * For logging purposes.
     */
    private final static String TAG = "FileDownloader";

    /**
     * To be used only if given track download URL isn't set
     */
    private final static String BASE_DOWNLOAD_URL = "https://storage-new.newjamendo.com/download/track/%s/mp32/";
    
    /**
     * Application's context.
     */
    private JamendoContext mJamendoContext;
    
    /**
     * Download service manager.
     */
    private DownloadManager mDownloadManager;
    
    /**
     * Maps the requests ids onto the download details descripting structure.
     */
    private LongSparseArray<TrackDownloadInfo> downloadRequests =
            new LongSparseArray<TrackDownloadInfo>();

    private DownloadedTracksDataSource mDownloadedTracksDataSource;
    private Uri destinationFolderUri;

    /**
     * Store an instance of system DownloadManager.
     * Create local destination folder if needed. Fallback to tmp folder if can't create defined folder.
     *
     * @param context
     *            application's context
     * @param downloadManager
     *            system download service manager
     */
    public FileDownloader(JamendoContext context, DownloadManager downloadManager) {
        mJamendoContext = context;
        mDownloadManager = downloadManager;
        mDownloadedTracksDataSource = new DownloadedTracksDataSource(context);

        // Set, and create if needed, local destination folder
        File destinationFolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC).getAbsolutePath() + File.separator + JAMENDO_FOLDER);
        if (destinationFolder.mkdirs())
            // Clear downloaded tracks table
            mDownloadedTracksDataSource.removeAll();

        // Fallback directory
        if (!destinationFolder.isDirectory()) {
            Log.e(TAG, "Can't create Jamendo directory in " + destinationFolder.toString() +
                    ". Using folder in app reserved memory");
            destinationFolder = mJamendoContext.getDir(JAMENDO_FOLDER,
                    Context.MODE_WORLD_WRITEABLE);
        }
        this.destinationFolderUri = Uri.fromFile(destinationFolder);

        // Register receiver for download completion and notification click
        mJamendoContext.registerReceiver(trackDownloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Helper method to download a Track, storing it in local downloaded tracks db.
     *
     * @return ID of the download request
     */
    public long downloadTrack(Track track, DownloadCompletionCallback callback) {
        if (mDownloadManager != null) {
            Log.d(TAG, "Downloading track " + track.getId());

            String artistNameTrackName = track.getArtistName() + " - " + track.getName();
            Uri.Builder destFileUriBuilder = destinationFolderUri.buildUpon().appendPath(
                    artistNameTrackName + TRACK_FILE_EXT);

            // Prepare DownloadManager request
            DownloadManager.Request request;
            if (track.getAudioDownload() == null)
                request = new Request(Uri.parse(String.format(BASE_DOWNLOAD_URL, track.getId())));
            else
                request = new Request(Uri.parse(track.getAudioDownload()));
            request.setDestinationUri(destFileUriBuilder.build());
            request.allowScanningByMediaScanner();
            request.setMimeType("audio/mpeg");
            // Download is visible and shows in the notifications while in progress and after completion.
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
            request.setTitle(artistNameTrackName);

            // Enqueue download
            long downloadId = mDownloadManager.enqueue(request);

            // Store custom DownloadInfo in requests list
            TrackDownloadInfo downloadInfo = new TrackDownloadInfo(track, callback,
                    destFileUriBuilder.build().getPath(), artistNameTrackName);
            downloadRequests.append(downloadId, downloadInfo);

            return downloadId;
        } else {
            throw new IllegalStateException("Download manager not set");
        }
    }

    /* Private methods */

    /**
     * Checks whether the download of the given id is completed or not.
     *
     * @param id
     *            download id to check
     * @return true if download is completed, false otherwise
     */
    private boolean isDownloadCompleted(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor cursor = mDownloadManager.query(query);
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(columnIndex);

            return status == DownloadManager.STATUS_SUCCESSFUL;
        }

        return false;
    }

    /* Interfaces */

    /**
     * Interface for the finished download notification.
     */
    public interface DownloadCompletionCallback {
        /**
         * Notifies that the download has been just completed.
         *
         * @param description
         *            download details
         */
        public void downloadCompleted(String description);

        /**
         * Notifies that the download has failed.
         *
         * @param description
         *            download details
         */
        public void downloadFailed(String description);
    }

    /* Private classes */

    /**
     * Class containing details about the track download.
     */
    private class TrackDownloadInfo {
        private Track track;
        private DownloadCompletionCallback callback;
        private String filePath;
        private String description;

        public TrackDownloadInfo(Track track, DownloadCompletionCallback callback,
                                 String filePath, String description) {
            this.track = track;
            this.callback = callback;
            this.filePath = filePath;
            this.description = description;
        }

        public Track getTrack() {
            return track;
        }

        public DownloadCompletionCallback getCallback() {
            return callback;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getDescription() {
            return description;
        }
    }

    /* Private instances */

    /**
     * Receive broadcast when a track download finishes.
     * Store an entry for downloaded track in SQLite db.
     */
    private BroadcastReceiver trackDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            TrackDownloadInfo downloadInfo = downloadRequests.get(downloadId);

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())
                    && downloadId != -1 && downloadInfo != null
                    && downloadInfo.getCallback() != null) {
                Track track = downloadInfo.getTrack();
                DownloadCompletionCallback callback = downloadInfo.getCallback();

                if (isDownloadCompleted(downloadId)) {
                    // Successful download, store track in local db
                    mDownloadedTracksDataSource.saveTrack(track,
                            downloadInfo.getFilePath());

                    callback.downloadCompleted(downloadInfo.getDescription());
                } else {
                    callback.downloadFailed(downloadInfo.getDescription());
                }
            }
            /* TODO handle download notification click
            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                Intent downloadedTracksFragmentIntent = new Intent(mJamendoContext,
                        MainActivity.class);
                downloadedTracksFragmentIntent.putExtra(IntentKeys.OPEN_FRAGMENT, Screen.FAVOURITES);
                downloadedTracksFragmentIntent.putExtra(IntentKeys.OPEN_FRAGMENT_TAB, Tabs.DOWNLOADED);
                mJamendoContext.startActivity(downloadedTracksFragmentIntent);
            }
            */
        }
    };
}
