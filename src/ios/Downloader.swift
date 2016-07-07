import Foundation

@objc(Downloader) class Downloader : CDVPlugin {
    func download(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        var isError = false

        let args = command.arguments[0] as! NSDictionary
        let url = NSURL(string: args["url"] as! String)
        let targetFile = args["path"] as !String

        let documentsUrl =  NSFileManager.defaultManager().URLsForDirectory(.DocumentDirectory, inDomains: .UserDomainMask).first as NSURL!
        let destinationUrl = documentsUrl.URLByAppendingPathComponent(targetFile)

        if NSFileManager().fileExistsAtPath(destinationUrl.path!) {
            print("file already exists [\(destinationUrl.path!)]")
            do {
                try NSFileManager().removeItemAtPath(destination.path!)
            }
            catch let error as NSError {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAsString: error.localizedDescription
                )

                self.commandDelegate!.sendPluginResult(
                    pluginResult,
                    callbackId: command.callbackId
                )

                isError = true
            }
        }

        if !(isError) {
            let sessionConfig = NSURLSessionConfiguration.defaultSessionConfiguration()
            let session = NSURLSession(configuration: sessionConfig, delegate: nil, delegateQueue: nil)
            let request = NSMutableURLRequest(URL: url!)
            request.HTTPMethod = "GET"
            let task = session.dataTaskWithRequest(request, completionHandler: { (data: NSData?, response: NSURLResponse?, error: NSError?) -> Void in
                if (error == nil) {
                    if let response = response as? NSHTTPURLResponse {
                        println("response=\(response)")
                        if response.statusCode == 200 {
                            if data!.writeToURL(destinationUrl, atomically: true) {
                                pluginResult = CDVPluginResult(
                                    status: CDVCommandStatus_OK,
                                    messageAsDictionnary: [
                                        "folder": destinationUrl.path!,
                                        "file": targetFile
                                    ]
                                )

                                self.commandDelegate!.sendPluginResult(
                                    pluginResult, 
                                    callbackId: command.callbackId
                                )
                            }
                        }
                    }
                }
            })
            task.resume()
        }
    }
}