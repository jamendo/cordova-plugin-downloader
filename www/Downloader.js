var Downloader = {
    download: function (arguments, successCallback, errorCallback) {
        console.group("Downloader::download");
        console.log(arguments);
        console.groupEnd();

		cordova.exec(
			successCallback,
			errorCallback,
			'Downloader',
			'download',
			[arguments]
		);
    }
};

Downloader.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.toast = new Downloader();
    return window.plugins.Downloader;
};

cordova.addConstructor(Downloader.install);
