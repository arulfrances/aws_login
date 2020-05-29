import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

enum UserState {
  SIGNED_IN,
  GUEST,
  SIGNED_OUT_FEDERATED_TOKENS_INVALID,
  SIGNED_OUT_USER_POOLS_TOKENS_INVALID,
  SIGNED_OUT,
  UNKNOWN,
}

class AWSLogin {
  static const MethodChannel _channel = MethodChannel('awslogin');

  //Amplify
  static Future<bool> get initAmplify async =>
      await _channel.invokeMethod('initAmplify');

  static Future<bool> facebookAmplifySignIn() async =>
      await _channel.invokeMethod('fbAmplify');

  static Future<bool> googleAmplifySignIn() async =>
      await _channel.invokeMethod('googleAmplify');

  static Future<bool> signOutAmplify() async =>
      await _channel.invokeMethod('signOutAmplify');

  static Future<bool> getAmpifySession() async =>
      await _channel.invokeMethod('getSessionAmplify');


  //AWS Mobile Client
  Future<UserState> get initAWS async {
    final String state = await _channel.invokeMethod('initialize');
    return getState(state);
  }

  Future<UserState> get awsSignIn async {
    final String state = await _channel.invokeMethod('aws_sign_in');
    return getState(state);
  }

  static Future<String> get getUserName async =>
      await _channel.invokeMethod('get_username');

  static Future<Map<String, String>> get getSessionTokens async =>
      await _channel.invokeMethod('get_session_tokens');

  static Future<Map<String, String>> get getUserAttributes async =>
      await _channel.invokeMethod('get_user_attributes');

  Future<bool> signIn({@required Map<String, String> arguments}) async =>
      await _channel.invokeMethod('direct_sign_in', arguments);

  Future<UserState> facebookSignIn({@required String fbAuthToken}) async {
    final String state =
        await _channel.invokeMethod('facebook_sign_in', fbAuthToken);
    return getState(state);
  }

  Future<UserState> googleSignIn({@required String googleAuthToken}) async {
    final String state =
        await _channel.invokeMethod('google_sign_in', googleAuthToken);
    return getState(state);
  }

  UserState getState(String state) {
    switch (state) {
      case 'SIGNED_IN':
        return UserState.SIGNED_IN;
        break;
      case 'SIGNED_OUT':
        return UserState.SIGNED_OUT;
        break;
      case 'GUEST':
        return UserState.GUEST;
        break;
      case 'SIGNED_OUT_FEDERATED_TOKENS_INVALID':
        return UserState.SIGNED_OUT_FEDERATED_TOKENS_INVALID;
        break;
      case 'SIGNED_OUT_USER_POOLS_TOKENS_INVALID':
        return UserState.SIGNED_OUT_USER_POOLS_TOKENS_INVALID;
        break;
      case 'UNKNOWN':
      default:
        return UserState.UNKNOWN;
        break;
    }
  }
}
