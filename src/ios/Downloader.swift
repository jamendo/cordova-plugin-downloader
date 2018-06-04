import Foundation

@objc(Downloader) class Downloader : CDVPlugin {
    func download(_ command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )

        let args = command.arguments[0] as! NSDictionary
        let url = URL(string: args["url"] as! String)
        let targetFile = args["path"] as! String
        let headers = args["headers"] != nil ? args["headers"] as! NSDictionary : nil

        let documentsUrl =  FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first as URL!
        let destinationUrl = documentsUrl?.appendingPathComponent(targetFile)

        if FileManager().fileExists(atPath: destinationUrl!.path) {
            do {
                try FileManager().removeItem(atPath: destinationUrl!.path)
            } catch let error as NSError {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: error.localizedDescription
                )
                
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                
                return
            }
        }

        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest = 5.0
        let session = URLSession(configuration: sessionConfig, delegate: nil, delegateQueue: nil)

        var request = URLRequest(url: url!)
        request.httpMethod = "GET"
        
        if headers != nil {
            for (headerName, headerValue) in headers! {
                request.addValue(headerValue as! String, forHTTPHeaderField: headerName as! String)
            }
        }

        let task = session.dataTask(with: request, completionHandler: { (data: Data?, response: URLResponse?, error: Error?) -> Void in
            if (error != nil) {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: error?.localizedDescription
                )
        
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                
                return
            }
            
            if let response = response as? HTTPURLResponse {
                if response.statusCode == 200 {
                    if (try? data!.write(to: destinationUrl!, options: [.atomic])) != nil {
                        pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_OK,
                            messageAs: documentsUrl?.path
                        )
                        
                        self.commandDelegate!.send(
                            pluginResult,
                            callbackId: command.callbackId
                        )
                        
                        return
                    } else {
                        pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR,
                            messageAs: "unable to write downloaded file to \(destinationUrl?.path)"
                        )
                        
                        self.commandDelegate!.send(
                            pluginResult,
                            callbackId: command.callbackId
                        )
                        
                        return
                    }
                } else {
                    pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_ERROR,
                        messageAs: "got response status code \(response.statusCode) (\(destinationUrl?.path))"
                    )
            
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                    
                    return
                }
            } else {
                pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "unable to get response object"
                )
        
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                
                return
            }
        })
        task.resume()
    }
}
