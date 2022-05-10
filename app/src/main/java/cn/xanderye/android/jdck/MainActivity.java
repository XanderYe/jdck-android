package cn.xanderye.android.jdck;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import cn.xanderye.android.util.JDUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private Context context;

    Button addBtn, delBtn, inputBtn, getCookieBtn, clearCookieBtn;

    Spinner phoneSpinner;

    WebView webView;

    private static final String JD_URL = "https://plogin.m.jd.com/user/login.action?appid=100&kpkey=&returnurl=http%3A%2F%2Fhome.m.jd.com%2FmyJd%2Fhome.action";

    private static final Pattern PHONE_PATTERN = Pattern.compile("1\\d{10}");

    private SharedPreferences config;

    private String cookie = null;

    private Set<String> phoneSet = new HashSet<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        // 配置存储
        config = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        //支持javascript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        //自适应屏幕
        webView.getSettings().setLoadWithOverviewMode(true);
        resetWebview();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //添加Cookie获取操作
                CookieManager cookieManager = CookieManager.getInstance();
                cookie = cookieManager.getCookie(url);
                super.onPageFinished(view, url);
            }
        });
        // 配置手机号下拉框
        phoneSpinner = findViewById(R.id.phoneSpinner);
        String phoneStr = config.getString("phoneStr", null);
        if (phoneStr != null) {
            String[] phones = phoneStr.split(",");
            phoneSet = new HashSet<>(Arrays.asList(phones));
            updatePhone();
        }

        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final AlertDialog dialog = builder.create();
            View dialogView = View.inflate(context, R.layout.activity_phone, null);
            //设置对话框布局
            dialog.setView(dialogView);
            dialog.show();

            EditText phoneText = dialogView.findViewById(R.id.phoneText);
            Button confirmBtn = dialogView.findViewById(R.id.confirmBtn);
            confirmBtn.setOnClickListener(v2 -> {
                String phone = phoneText.getText().toString();
                Matcher matcher = PHONE_PATTERN.matcher(phone);
                if (!matcher.matches()) {
                    Toast.makeText(this, "手机号输入错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.cancel();
                phoneSet.add(phone);
                updatePhone();
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            });
        });

        delBtn = findViewById(R.id.delBtn);
        delBtn.setOnClickListener(v -> {
            String selectedPhone = (String) phoneSpinner.getSelectedItem();
            if (selectedPhone == null) {
                Toast.makeText(this, "请先选择手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            phoneSet = phoneSet.stream().filter(phone -> !phone.equals(selectedPhone)).collect(Collectors.toSet());
            // 更新手机号
            updatePhone();
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        });

        // 一键输入按钮
        inputBtn = findViewById(R.id.inputBtn);
        inputBtn.setOnClickListener(v -> {
            String selectedPhone = (String) phoneSpinner.getSelectedItem();
            if (selectedPhone == null) {
                Toast.makeText(this, "请先选择手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            webView.loadUrl("javascript:var number='" + selectedPhone + "';var evt=new InputEvent('input',{inputType:'insertText',data:number,dataTransfer:null,isComposing:false});document.getElementsByClassName('acc-input mobile J_ping')[0].value=number;document.getElementsByClassName('acc-input mobile J_ping')[0].dispatchEvent(evt);document.getElementsByClassName('policy_tip-checkbox')[0].click();");
        });
        // 获取cookie按钮
        getCookieBtn = findViewById(R.id.getCookieBtn);
        getCookieBtn.setOnClickListener(v -> {
            Map<String, Object> map = JDUtil.formatCookies(cookie);
            String ptKey = (String) map.get("pt_key");
            String ptPin = (String) map.get("pt_pin");
            if (StringUtils.isAnyBlank(ptKey, ptPin)) {
                Toast.makeText(this, "未获取到Cookie，请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            String cookie = MessageFormat.format("pt_key={0};pt_pin={1};", ptKey, ptPin);
            copyToClipboard(cookie);
            Toast.makeText(this, "获取成功，已复制到剪切板", Toast.LENGTH_SHORT).show();
        });
        // 重置cookie刷新页面按钮
        clearCookieBtn = findViewById(R.id.clearCookieBtn);
        clearCookieBtn.setOnClickListener(v -> {
            resetWebview();
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, 1, "关于");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case 1: {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("@XanderYe 版权所有");
                builder.setPositiveButton("项目页面", (dialog, which) -> {
                    Uri uri = Uri.parse("https://github.com/XanderYe/jdck-android");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(uri);
                    startActivity(intent);
                });
                builder.create().show();
            }
            break;
        }
        return true;
    }

    /**
     * 清空cookie并加载页面
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/5/10
     */
    private void resetWebview() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        webView.clearCache(true);
        webView.loadUrl(JD_URL);
    }

    /**
     * 更新下拉和存储中的手机号
     * @return void
     * @author XanderYe
     * @date 2022/5/10
     */
    private void updatePhone() {
        // 更新手机号
        String newPhoneStr = phoneSet.stream().collect(Collectors.joining(","));
        SharedPreferences.Editor edit = config.edit();
        edit.putString("phoneStr", newPhoneStr);
        edit.apply();
        String[] phones = phoneSet.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, phones);
        phoneSpinner.setAdapter(adapter);
    }

    /**
     * 复制文字到剪切板
     * @param copyStr
     * @return boolean
     * @author XanderYe
     * @date 2022/5/10
     */
    private boolean copyToClipboard(String copyStr) {
        try {
            //获取剪贴板管理器
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", copyStr);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}