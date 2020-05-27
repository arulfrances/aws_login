import Flutter
import UIKit
import AWSMobileClient

public class SwiftAwsloginPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "awslogin", binaryMessenger: registrar.messenger())
        let instance = SwiftAwsloginPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if (call.method == "initialize") {
            AWSMobileClient.default().initialize { (userState, error) in
                if let userState = userState {
                    result(userState.rawValue)
                    print("UserState: \(userState.rawValue)")
                } else if let error = error {
                    result(FlutterError(code: "AWS_INIT_ERROR", message: "AWS Initialization error.", details: error.localizedDescription));
                    print("error: \(error.localizedDescription)")
                }
            }
        } else if (call.method == "get_username") {
            let username = AWSMobileClient.default().username
            result(username)
        } else if (call.method == "is_signed_in") {
            let isSignedIn = AWSMobileClient.default().isSignedIn
            result(isSignedIn)
        } else if (call.method == "get_identity_id") {
            let identityId = AWSMobileClient.default().identityId
            result(identityId)
        } else if (call.method == "get_session_tokens") {
            AWSMobileClient.default().getTokens { (tokens, error) in
                if let error = error {
                    print("Error getting token \(error.localizedDescription)")
                    result(FlutterError(code: "AWS_TOKEN_ERROR", message: "Error getting token", details: error.localizedDescription));
                } else if let tokens = tokens {
                    let sessionTokens = [
                        "AccessToken" : tokens.accessToken!.tokenString!,
                        "IdToken" : tokens.idToken!.tokenString!,
                        "RefreshToken" : tokens.refreshToken!.tokenString!
                    ]
                    print(sessionTokens)
                    result(sessionTokens)
                }
            }
        } else if (call.method == "direct_sign_in") {
            guard let args = call.arguments else {
                return
            }
            if let myArgs = args as? [String: Any],
                let name = myArgs["username"] as? String,
                let psd = myArgs["password"] as? String,
                let email = myArgs["email"] as? String {
                AWSMobileClient.default().signUp(username: email,
                                                 password: psd,
                                                 userAttributes: ["email":email,"name":name]) { (signUpResult, error) in
                                                    if let signUpResult = signUpResult {
                                                        switch(signUpResult.signUpConfirmationState) {
                                                        case .confirmed:
                                                            print("User is signed up and confirmed.")
                                                            result(true)
                                                        case .unconfirmed:
                                                            print("User is not confirmed and needs verification via \(signUpResult.codeDeliveryDetails!.deliveryMedium) sent at \(signUpResult.codeDeliveryDetails!.destination!)")
                                                            result(false)
                                                        case .unknown:
                                                            print("Unexpected case")
                                                            result(false)
                                                        }
                                                    } else if let error = error {
                                                        if let error = error as? AWSMobileClientError {
                                                            result(FlutterError(code: "AWS_LOGIN", message: "AWS Direct Sign in error.", details: error.localizedDescription));
                                                            switch(error) {
                                                            case .usernameExists(let message):
                                                                print(message)
                                                            default:
                                                                break
                                                            }
                                                        }
                                                        print("\(error.localizedDescription)")
                                                    }
                }
            } else {
                result(FlutterError(code: "ERROR", message: "INVALID ARGUMENTS" +
                    "flutter arguments in method: (sendParams)", details: nil))
            }
        } else if (call.method == "facebook_sign_in") {
            let token = call.arguments as! String
            AWSMobileClient.default().federatedSignIn(providerName: IdentityProvider.facebook.rawValue, token: token) { (userState, error)  in
                if let userState = userState {
                    result(userState.rawValue)
                    print("UserState: \(userState.rawValue)")
                } else if let error = error {
                    result(FlutterError(code: "AWS_LOGIN", message: "AWS Facebook Sign in error.", details: error.localizedDescription));
                    print("Federated Sign In failed: \(error.localizedDescription)")
                }
            }
        } else if (call.method == "google_sign_in") {
            let token = call.arguments as! String
            AWSMobileClient.default().federatedSignIn(providerName: IdentityProvider.google.rawValue, token: token) { (userState, error)  in
                if let userState = userState {
                    result(userState.rawValue)
                    print("UserState: \(userState.rawValue)")
                } else if let error = error {
                    result(FlutterError(code: "AWS_LOGIN", message: "AWS Google Sign in error.", details: error.localizedDescription));
                    print("Federated Sign In failed: \(error.localizedDescription)")
                }
            }
        } else {
            result(FlutterError(code: "INVALID_METHOD", message: "Invalid method", details: nil));
            return;
        }
    }
}
