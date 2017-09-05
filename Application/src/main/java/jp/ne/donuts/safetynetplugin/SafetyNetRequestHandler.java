package jp.ne.donuts.safetynetplugin;

import android.app.FragmentManager;
import android.app.FragmentTransaction;

import com.unity3d.player.UnityPlayer;

public class SafetyNetRequestHandler {

    public static void RequestVerification(final byte[] nonce, final String key, String callbackGameObject, String callbackMethod) {

        SafetyNetFragment safetynetExec    = new SafetyNetFragment();
        safetynetExec.cbGameObject   = callbackGameObject;
        safetynetExec.cbMethod       = callbackMethod;
        safetynetExec.act            = UnityPlayer.currentActivity;
        safetynetExec.key            = key;
        safetynetExec.nonceFromUnity = nonce;

        //フラグメントを作成し、アクティビティに追加します。
        FragmentManager fm     = UnityPlayer.currentActivity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(safetynetExec, "TAG");
        ft.commit();
    }
}