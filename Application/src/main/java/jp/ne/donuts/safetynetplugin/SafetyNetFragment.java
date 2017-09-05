//Android Studio側  SafetyNetの処理クラス
package jp.ne.donuts.safetynetplugin;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;

import org.json.JSONException;
import org.json.JSONObject;

import static com.unity3d.player.UnityPlayer.UnitySendMessage;

public class SafetyNetFragment extends Fragment {

    public String cbGameObject;
    public String cbMethod;
    public byte[] nonceFromUnity;
    String TAG = "Unity/SafetyNet";
    Activity act;
    public String key;
    final String ERROR_STRING = "";
    public SafetyNetFragment() {
    }

    private GoogleApiClient mGoogleApiClient;  //GoogleAPIを使用するためのクライアント

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Starts the flow to verify the device
        checkServiceAvailability();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    //Google play servicesが利用可能かを確認する
    private void checkServiceAvailability() {
        Log.d(TAG, "checkServiceAvailability");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            //Checks if it is already connected to the Google API or not
            SafetyStart();
        }
        try {
            //Google play servicesの利用可否をチェックする
            GoogleApiAvailability checker = GoogleApiAvailability.getInstance();  //getInstance() : GoogleApiAvailabilityのシングルトンインスタンスを返します。
            int result = checker.isGooglePlayServicesAvailable(act);
            if (result == ConnectionResult.SUCCESS) {
                //使用可能
                //Can Use GooglePlayServices, thus tries to establish a connection to Google API
                onGooglePlayServicesAvailable();
            } else {
                //Can not access GooglePlayServices, thus can not verify device
                //Sends Error Back to Unity
                SendToUnity(ERROR_STRING);
            }
        } catch (Exception e) {
            //Exception occurred trying to detect if GooglePlayServices can be used or not
            //Throws Error to Unity
            SendToUnity(ERROR_STRING);
        }

    }

    //Google Play開発者サービスが利用可能だった場合
    private void onGooglePlayServicesAvailable() {
        try {
            mGoogleApiClient = new GoogleApiClient.Builder(act)
                    .addApi(SafetyNet.API)
                    //          .addConnectionCallbacks(mGoogleApiCallback)               //コールバック
                    //          .addOnConnectionFailedListener(mConnectionFaildListener)
                    .build();

            mGoogleApiClient.connect();
            //Successfully Connected, Starts SafetyNet Request
            SafetyStart();
        } catch (Exception e) {
            //Could not establish a connection with Google API
            //Throws Error to Unity
            SendToUnity(ERROR_STRING);
        }
    }

    private void SafetyStart() {
        Log.d(TAG, "------SafetyStart()--------");
        byte[] nonce = nonceFromUnity;

        if(nonce == null) {
            nonce = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18}; // Should be at least 16 bytes in length.
        }

        SafetyNet.SafetyNetApi.attest(mGoogleApiClient, nonce)
                .setResultCallback(new ResultCallback<SafetyNetApi.AttestationResult>() {

                    @Override
                    public void onResult(SafetyNetApi.AttestationResult result) {
                        String str = "";

                        Status status = result.getStatus();
                        if (status.isSuccess()) {
                            Log.d(TAG, "ok");
                            //サービスとの通信が成功したことを示します。  result.getJws Result（）を使用して結果データを取得します。
                            //The connection was successfully done and the result is show.
                            //Get the result through result.getJwsResult（）
                            String[] jwtParts = result.getJwsResult().split("\\.");
                            String decodedPayload = new String(Base64.decode(jwtParts[1], Base64.DEFAULT));
                            try {

                                JSONObject root = new JSONObject(decodedPayload);
                                Log.d("SafetyNet JWT: ", root.toString());
                                str = root.toString();
                                //Sends result back to Unity
                                //Sample Result:
                                // {"nonce":"AQIDBAUGBwgJCgsMDQ4PEBES", "timestampMs":1504572393135,
                                // "apkPackageName":"jp.ne.donuts.lostkingdom",
                                // "apkDigestSha256":"T1OxwsX+AoKSRaUVf65VPN92Q4AsV\/zbL5N\/8IKChnM=","ctsProfileMatch":true,
                                // "apkCertificateDigestSha256":["GsPi0mDezPHDjbLk4dW8noHshlXzkrIAu+XMK+f2zac="],"basicIntegrity":true}
                                SendToUnity(str);
                            } catch (JSONException e) {
                                //There was an exception during decoding of the payload
                                e.printStackTrace();
                                SendToUnity(ERROR_STRING);
                            }
                        } else {
                            //サービスとの通信中にエラーが発生
                            //Error occurred during communication
                            SendToUnity(ERROR_STRING);

                        }

                    }
                });
        /****************************
         *
         * Newest Implementation
         * Does not work on Unity
         * TODO: Find out why!
         *
         * ***************************
        SafetyNet.getClient(act).attest(nonce, key)
                .addOnSuccessListener(act,
                        new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                            @Override
                            public void onSuccess(SafetyNetApi.AttestationResponse response) {
                                // Indicates communication with the service was successful.
                                // Use response.getJwsResult() to get the result data.
                                SendToUnity("ERROR: could not connect to safetynet "+response.getJwsResult());
                            }
                        })
                .addOnFailureListener(act, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred while communicating with the service.
                        if (e instanceof ApiException) {
                            // An error with the Google Play services API contains some
                            // additional details.
                            ApiException apiException = (ApiException) e;
                            // You can retrieve the status code using the
                            // apiException.getStatusCode() method.
                            SendToUnity("ERROR: could not connect to safetynet"+apiException.getStatusMessage());
                        } else {
                            // A different, unknown type of error occurred.
                            Log.d(TAG, "Error: " + e.getMessage());
                        }
                    }
                });
        *************************************************/

    }

    private void SendToUnity(String str)
    {
        if(cbGameObject != null){
            UnitySendMessage(cbGameObject, cbMethod, str);
        }
        else{
            UnitySendMessage("GameObjectName", "MethodName", str);
        }
    }
}
