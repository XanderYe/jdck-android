package cn.xanderye.android.jdck.util;

import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author XanderYe
 * @description:
 * @date 2022/5/11 14:04
 */
public class QinglongUtil {

    /**
     * 登录
     * @param qlInfo
     * @return java.lang.String
     * @author XanderYe
     * @date 2022/5/11
     */
    public static String login(QlInfo qlInfo) throws IOException {
        String url = qlInfo.getAddress();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (qlInfo.getOldVersion()) {
            url += "/api/login";
        } else {
            url += "/api/user/login";
        }
        url += "?t=" + System.currentTimeMillis();
        JSONObject params = new JSONObject();
        params.put("username", qlInfo.getUsername());
        params.put("password", qlInfo.getPassword());
        HttpUtil.ResEntity resEntity = HttpUtil.doPostJSON(url, params.toJSONString());
        if (resEntity.getStatusCode() != 200) {
            throw new IOException("服务器" + resEntity.getStatusCode() + "错误");
        }
        JSONObject res = JSON.parseObject(resEntity.getResponse());
        if (res.getInteger("code") != 200) {
            throw new IOException(res.getString("message"));
        }
        return res.getJSONObject("data").getString("token");
    }

    /**
     * 获取环境变量
     * @param qlInfo
     * @return java.util.List<cn.xanderye.android.jdck.entity.QlEnv>
     * @author XanderYe
     * @date 2022/5/11
     */
    public static List<QlEnv> getEnvList(QlInfo qlInfo) throws IOException {
        String url = qlInfo.getAddress() + "/api/envs";
        url += "?searchValue=&t=" + System.currentTimeMillis();
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + qlInfo.getToken());
        HttpUtil.ResEntity resEntity = HttpUtil.doGet(url, headers, null, null);
        if (resEntity.getStatusCode() != 200) {
            throw new IOException("服务器" + resEntity.getStatusCode() + "错误");
        }
        JSONObject res = JSON.parseObject(resEntity.getResponse());
        if (res.getInteger("code") != 200) {
            throw new IOException(res.getString("message"));
        }
        return res.getJSONArray("data").toJavaList(QlEnv.class);
    }

    /**
     * 更新环境变量
     * @param qlInfo
     * @param qlEnv
     * @return boolean
     * @author XanderYe
     * @date 2022/5/11
     */
    public static boolean saveEnv(QlInfo qlInfo, QlEnv qlEnv) throws IOException {
        String url = qlInfo.getAddress() + "/api/envs";;
        JSONObject params = new JSONObject();
        if (qlInfo.getOldVersion()) {
            params.put("_id", qlEnv.get_id());
        } else {
            params.put("id", qlEnv.get_id());
        }
        url += "?t=" + System.currentTimeMillis();
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + qlInfo.getToken());
        params.put("name", qlEnv.getName());
        params.put("remarks", qlEnv.getRemarks());
        params.put("value", qlEnv.getValue());
        HttpUtil.ResEntity resEntity = HttpUtil.doPutJSON(url, headers, null, params.toJSONString());
        if (resEntity.getStatusCode() != 200) {
            throw new IOException("服务器" + resEntity.getStatusCode() + "错误");
        }
        JSONObject res = JSON.parseObject(resEntity.getResponse());
        if (res.getInteger("code") != 200) {
            throw new IOException(res.getString("message"));
        }
        return true;
    }
}
