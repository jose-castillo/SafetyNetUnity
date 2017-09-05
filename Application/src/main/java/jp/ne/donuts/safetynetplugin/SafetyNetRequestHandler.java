package jp.ne.donuts.safetynetplugin;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import com.unity3d.player.UnityPlayer;

public class SafetyNetRequestHandler {

    //Unity側のこのインタフェースから派生クラスが作成されます
    public static interface IListener {
        public void onComplete(boolean status);
    }

    private static Activity mActivity;
    private IListener mListener;

    public static SafetyNetRequestHandler mListner = null;

    public static SafetyNetRequestHandler instance() {
        return mListner;
    }

    public static void RequestVerification(final byte[] nonce, final String key, String callbackGameObject, String callbackMethod) {

        SafetyNetFragment safetynetExec    = new SafetyNetFragment();
        safetynetExec.cbGameObject   = callbackGameObject;
        safetynetExec.cbMethod       = callbackMethod;
        safetynetExec.act            = UnityPlayer.currentActivity;
        safetynetExec.key            = key;
        safetynetExec.nonceFromUnity = nonce;


        mActivity = UnityPlayer.currentActivity;
        //フラグメントを作成し、アクティビティに追加します。
        FragmentManager fm = mActivity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(safetynetExec, "TAG");
        ft.commit();
    }

    // このメソッドが呼び出されるとリスナーに通知する
    public void onComplete(boolean v) {
        mListener.onComplete(v);
    }
}