package cn.xanderye.android.jdck.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yezhendong
 * @description:
 * @date 2022/5/11 14:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QlInfo {
    private String address;

    private Boolean oldVersion;

    private String username;

    private String password;

    private String token;
}
