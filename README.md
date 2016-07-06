# cordova-plugin-downloader
cordova plugin for downloading feature in jamendo music app.

```javascript
var Downloader = window.plugins.Downloader;
var downloadSuccessCallback = function (entry) {
  //entry: { folder: string, file: string }
};
var downloadErrorCallback = function (error) {
  //error: string
};
Downloader.download({
    url: url, //url of ressource to download: string
    path: fileName //path where to store ressource: string
  },
  downloadSuccessCallback,
  downloadErrorCallback
);
```

The ressource will be downloaded within the application's external files directory.
