package co.u1stmain.awslogin;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.IdentityProvider;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * AwsloginPlugin
 */
public class AwsloginPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {

    private MethodChannel channel;
    private Context mContext;
    private Activity mActivity;
    private Result mResult;
    private static final String TAG = "AWS_LOGIN";

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        mContext = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "awslogin");
        channel.setMethodCallHandler(this);
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "awslogin");
        channel.setMethodCallHandler(new AwsloginPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull final Result result) {
        mResult = result;
        switch (call.method) {
            case "initialize":
                AWSMobileClient.getInstance().initialize(mContext, new Callback<UserStateDetails>() {
                            @Override
                            public void onResult(final UserStateDetails userStateDetails) {
                                Log.i(TAG, "onResult: " + userStateDetails.getUserState());
                                ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        mResult.success(userStateDetails.getUserState().name());
                                    }
                                });
                            }

                            @Override
                            public void onError(final Exception e) {
                                Log.e(TAG, "AWS Initialization error.", e);
                                ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        mResult.error("Failed", e.getMessage(), "AWS Initialization error.");
                                    }
                                });
                            }
                        }
                );
                break;
            case "get_username":
                ui(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (AWSMobileClient.getInstance().isSignedIn()) {
                                final String username = AWSMobileClient.getInstance().getUsername();
                                if (username != null)
                                    mResult.success(username);
                                else
                                    mResult.success("");
                            } else
                                mResult.success("");
                        } catch (Exception e) {
                            mResult.error("getUserNameFailed", e.getMessage(), "AWS Get Username error.");
                        }
                    }
                });
                break;
            case "get_session_tokens":
                ui(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HashMap<String, String> sessionToken = new HashMap<>();
                            sessionToken.put("AccessToken", AWSMobileClient.getInstance().getTokens().getAccessToken().getTokenString());
                            sessionToken.put("IdToken", AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString());
                            sessionToken.put("RefreshToken", AWSMobileClient.getInstance().getTokens().getRefreshToken().getTokenString());
                            mResult.success(sessionToken);
                        } catch (Exception e) {
                            mResult.error("getSessionTokens", e.getMessage(), "AWS Get SessionTokens error.");
                        }
                    }
                });
                break;
            case "get_user_attributes":
                AWSMobileClient.getInstance().getUserAttributes(new Callback<Map<String, String>>() {
                    @Override
                    public void onResult(final Map<String, String> result) {
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                mResult.success(result);
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                mResult.error("getUserAttributes", e.getMessage(), "AWS Get UserAttributes error.");
                            }
                        });
                    }
                });
                break;
            case "direct_sign_in":
                final Map<String, String> attributes = new HashMap<>();
                attributes.put("email", String.valueOf(call.argument("email")));
                attributes.put("name", String.valueOf(call.argument("username")));
                final Map<String, String> validationData = new HashMap<>();
                validationData.put("autoConfirmUser", "true");
                validationData.put("autoVerifyEmail", "true");
                AWSMobileClient.getInstance().signUp(String.valueOf(call.argument("email")), String.valueOf(call.argument("password")), attributes, validationData, new Callback<SignUpResult>() {
                    @Override
                    public void onResult(final SignUpResult signUpResult) {
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Sign-up callback state: " + signUpResult.getConfirmationState());
                                if (!signUpResult.getConfirmationState()) {
                                    final UserCodeDeliveryDetails details = signUpResult.getUserCodeDeliveryDetails();
                                    Log.e(TAG, "Sign-up done, but confirmation required.");
                                    mResult.success(false);
                                } else {
                                    Log.e(TAG, "Sign-up done.");
                                    mResult.success(true);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                mResult.error(TAG, e.getMessage(), "AWS Sign up error.");
                            }
                        });
                    }
                });
                break;
            case "aws_sign_in":
                AWSMobileClient.getInstance().showSignIn(
                        mActivity,
                        SignInUIOptions.builder()
                                .nextActivity(mActivity.getClass())
                                .build(),
                        new Callback<UserStateDetails>() {
                            @Override
                            public void onResult(final UserStateDetails result) {
                                Log.d(TAG, "onResult: " + result.getUserState());
                                ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        switch (result.getUserState()) {
                                            case SIGNED_IN:
                                                Log.i(TAG, "logged in!");
                                                mResult.success(result.getUserState().name());
                                                break;
                                            case SIGNED_OUT:
                                                Log.i(TAG, "onResult: User did not choose to sign-in");
                                                mResult.success(result.getUserState().name());
                                                break;
                                            default:
                                                AWSMobileClient.getInstance().signOut();
                                                break;
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(final Exception e) {
                                Log.e(TAG, "onError: ", e);
                                ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        mResult.error(TAG, e.getMessage(), "AWS Sign in error.");
                                    }
                                });
                            }
                        }
                );
                break;
            case "facebook_sign_in":
                String fb_auth_token = call.arguments.toString();
                AWSMobileClient.getInstance().federatedSignIn(IdentityProvider.FACEBOOK.toString(), fb_auth_token, new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(final UserStateDetails result) {
                        Log.d(TAG, "onResult: " + result.getDetails());
                        Log.d(TAG, "onResult: " + result.getUserState());
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                switch (result.getUserState()) {
                                    case SIGNED_IN:
                                        Log.i(TAG, "logged in!");
                                        mResult.success(result.getUserState().name());
                                        break;
                                    case SIGNED_OUT:
                                        Log.i(TAG, "onResult: User did not choose to sign-in");
                                        mResult.success(result.getUserState().name());
                                        break;
                                    default:
                                        AWSMobileClient.getInstance().signOut();
                                        break;
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        Log.e(TAG, "AWS Sign in error.", e);
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                mResult.error(TAG, e.getMessage(), "AWS Sign in error.");
                            }
                        });
                    }
                });
                break;
            case "google_sign_in":
                String google_auth_token = call.arguments.toString();
                AWSMobileClient.getInstance().federatedSignIn(IdentityProvider.GOOGLE.toString(), google_auth_token, new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(final UserStateDetails result) {
                        Log.d(TAG, "onResult: " + result.getUserState());
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                switch (result.getUserState()) {
                                    case SIGNED_IN:
                                        Log.i(TAG, "logged in!");
                                        mResult.success(result.getUserState().name());
                                        break;
                                    case SIGNED_OUT:
                                        Log.i(TAG, "onResult: User did not choose to sign-in");
                                        mResult.success(result.getUserState().name());
                                        break;
                                    default:
                                        AWSMobileClient.getInstance().signOut();
                                        break;
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        Log.e(TAG, "AWS Sign in error.", e);
                        ui(new Runnable() {
                            @Override
                            public void run() {
                                mResult.error(TAG, e.getMessage(), "AWS Sign in error.");
                            }
                        });
                    }
                });
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {

    }

    private void ui(@NonNull Runnable runnable) {
        mActivity.runOnUiThread(runnable);
    }
}

//AWSMobileClient.getInstance().getUsername()       //String
//AWSMobileClient.getInstance().isSignedIn()        //Boolean
//AWSMobileClient.getInstance().getIdentityId()     //String
//AWSMobileClient.getInstance().getTokens();
//AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
//AWSMobileClient.getInstance().getCredentials();
