package cn.xanderye.android.jdck.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import cn.xanderye.android.jdck.config.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 短信监听器
 */
public class SMSReceiver extends BroadcastReceiver {

    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    private static final String TAG = SMSReceiver.class.getSimpleName();

    public static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    @Override
    public void onReceive(Context context, Intent intent) {
        //调用短信内容获取类
        getMsg(context, intent);
    }
    
    /**
     * 短信内容的获取
     * @param context
     * @param intent
     * @return void
     * @author XanderYe
     * @date 2022/5/19
     */
    private void getMsg(Context context, Intent intent) {
        //解析短信内容 pdus短信单位pdu
        if (intent.getAction().equals(SMS_RECEIVED)) {
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            assert pdus != null;
            for (Object pdu : pdus) {
                //封装短信参数的对象
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                //获取发送短信的手机号
                String number = sms.getOriginatingAddress();
                //获取接收短信手机号的完整信息
                String body = sms.getMessageBody();
                getCode(context, body);
            }
        }
    }

    private void getCode(Context context, String body) {
        Matcher matcher = CODE_PATTERN.matcher(body);
        if (matcher.find()) {
            String code = matcher.group();
            Log.d(TAG, "获取到验证码：" + code);
            WebView webView = Config.getInstance().getWebView();
            if (webView != null) {
                webView.post(() -> {
                    String execJs = "var code = '" + code + "';document.getElementById('authcode').value = code;";
                    execJs += "var evt=new InputEvent('input',{inputType:'insertText',data:code,dataTransfer:null,isComposing:false});";
                    execJs += "document.getElementById('authcode').dispatchEvent(evt);";
                    execJs += "setTimeout(() => {document.getElementsByClassName(\"btn J_ping\")[0].click();}, 200);";
                    webView.loadUrl("javascript:" + execJs);
                    Toast.makeText(context, "已填入验证码", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}