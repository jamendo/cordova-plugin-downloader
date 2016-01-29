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

function install() {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.Downloader = Downloader;
    return window.plugins.Downloader;
};

cordova.addConstructor(install);
