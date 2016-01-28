
var eventCallbackList = {};

function checkEvent(event) {// add namespace ?
    if (event in eventCallbackList) {
        return;
    }
    eventCallbackList[event] = [];
}

exports.Downloader = {
    download: function (arguments, successCallback, errorCallback) {
        console.group("Downloader::download");
        console.log(infos);
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
