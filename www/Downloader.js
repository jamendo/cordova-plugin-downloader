
var eventCallbackList = {};

function checkEvent(event) {
    if (event in eventCallbackList) {
        return;
    }
    eventCallbackList[event] = [];
}

exports.Downloader = {
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
