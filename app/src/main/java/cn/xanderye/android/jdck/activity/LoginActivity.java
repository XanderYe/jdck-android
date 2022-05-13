package cn.xanderye.android.jdck.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cn.xanderye.android.jdck.R;
import cn.xanderye.android.jdck.config.Config;
import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import cn.xanderye.android.jdck.util.QinglongUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author XanderYe
 * @description:
 * @date 2022/5/11 13:39
 */
public class LoginActivity extends AppCompatActivity {

    private Context context;

    private SharedPreferences config;

    private EditText addressText, usernameText, passwordText, tokenText;

    private Button loginBtn, cancelBtn;

    private CheckBox oldVersionCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this;

        // 配置存储
        config = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        String qlJSON = config.getString("qlJSON", null);
        QlInfo qlInfo = new QlInfo("", true, "", "", "");
        if (qlJSON != null) {
            qlInfo = JSON.parseObject(qlJSON, QlInfo.class);
            Config.getInstance().setQlInfo(qlInfo);
        }

        addressText = findViewById(R.id.addressText);
        addressText.setText(qlInfo.getAddress());
        usernameText = findViewById(R.id.usernameText);
        usernameText.setText(qlInfo.getUsername());
        passwordText = findViewById(R.id.passwordText);
        passwordText.setText(qlInfo.getPassword());
        tokenText = findViewById(R.id.tokenText);
        oldVersionCheckBox = findViewById(R.id.oldVersionCheckBox);
        oldVersionCheckBox.setChecked(qlInfo.getOldVersion());

        loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(v -> {
            String addr = addressText.getEditableText().toString();
            String user = usernameText.getEditableText().toString();
            String pwd = passwordText.getEditableText().toString();
            String token = tokenText.getEditableText().toString();
            if (StringUtils.isBlank(addr)) {
                Toast.makeText(this, "地址不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (StringUtils.isBlank(token) && StringUtils.isAnyBlank(user, pwd)) {
                Toast.makeText(this, "账户密码或token必须有一个方式", Toast.LENGTH_SHORT).show();
                return;
            }
            QlInfo qlInfo2 = new QlInfo();
            qlInfo2.setAddress(addr.trim());
            qlInfo2.setUsername(user.trim());
            qlInfo2.setPassword(pwd.trim());
            qlInfo2.setToken(token.trim());
            qlInfo2.setOldVersion(oldVersionCheckBox.isChecked());
            ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
            singleThreadExecutor.execute(() -> {
                Looper.prepare();
                try {
                    if (StringUtils.isNotBlank(token)) {
                        try {
                            List<QlEnv> qlEnvList = QinglongUtil.getEnvList(qlInfo2);
                            Config.getInstance().setQlEnvList(qlEnvList);
                            Config.getInstance().setQlInfo(qlInfo2);
                            loginSuccess(qlInfo2);
                        } catch (IOException e) {
                            Toast.makeText(this, "青龙token已失效，请重新登录", Toast.LENGTH_SHORT).show();
                        } finally {
                            Looper.loop();
                        }
                        return;
                    }
                    String tk = QinglongUtil.login(qlInfo2);
                    if (StringUtils.isBlank(tk)) {
                        Toast.makeText(this, "登录失败，token为空", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        return;
                    }
                    qlInfo2.setToken(tk);
                    loginSuccess(qlInfo2);
                } catch (IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    Looper.loop();
                }
            });
            singleThreadExecutor.shutdown();
        });

        cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> {
            this.finish();
        });
    }

    private void loginSuccess(QlInfo qlInfo) throws IOException {
        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        // 存储内存
        Config.getInstance().setQlInfo(qlInfo);
        // 数据持久化
        SharedPreferences.Editor edit = config.edit();
        edit.putString("qlJSON", JSON.toJSONString(qlInfo));
        edit.apply();
        // 获取环境变量
        List<QlEnv> qlEnvList = QinglongUtil.getEnvList(qlInfo);
        Config.getInstance().setQlEnvList(qlEnvList);
        this.finish();
    }
}
