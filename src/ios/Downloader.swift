import Foundation

@objc(Downloader) class Downloader : CDVPlugin {
    func download(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
          status: CDVCommandStatus_ERROR
        )

        let url = command.arguments[0]
        let targetFile = command.arguments[1]

        let documentsUrl =  NSFileManager.defaultManager().URLsForDirectory(.DocumentDirectory, inDomains: .UserDomainMask).first as! NSURL
        let destinationUrl = documentsUrl.URLByAppendingPathComponent(targetFile)

        if NSFileManager().fileExistsAtPath(destinationUrl.path!) {
            println("file already exists [\(destinationUrl.path!)]")
            completion(path: destinationUrl.path!, error:nil)
        } else {
            let sessionConfig = NSURLSessionConfiguration.defaultSessionConfiguration()
            let session = NSURLSession(configuration: sessionConfig, delegate: nil, delegateQueue: nil)
            let request = NSMutableURLRequest(URL: url)
            request.HTTPMethod = "GET"
            let task = session.dataTaskWithRequest(request, completionHandler: { (data: NSData!, response: NSURLResponse!, error: NSError!) -> Void in
                if (error == nil) {
                    if let response = response as? NSHTTPURLResponse {
                        println("response=\(response)")
                        if response.statusCode == 200 {
                            if data.writeToURL(destinationUrl, atomically: true) {
                                pluginResult = CDVPluginResult(
                                    status: CDVCommandStatus_OK,
                                    folder: destinationUrl.path!,
                                    file: targetFile
                                )
                            }
                        }
                    }
                }
            })
            task.resume()
        }
        
        self.commandDelegate!.sendPluginResult(
            pluginResult, 
            callbackId: command.callbackId
        )
    }
}