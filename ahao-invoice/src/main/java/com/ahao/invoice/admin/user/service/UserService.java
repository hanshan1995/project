package com.ahao.invoice.admin.user.service;

import com.ahao.invoice.admin.user.entity.UserDO;
import com.ahao.invoice.base.service.PageService;
import org.springframework.stereotype.Service;

/**
 * Created by Avalon on 2017/5/11.
 */
@Service
public interface UserService extends PageService<UserDO> {

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在为true
     */
    boolean existUsername(String username);

    /**
     * 在登录时更新角色登录时间和ip
     *
     * @param username 用户名
     * @return 更新成功返回true
     */
    boolean updateLoginMsg(String username);
}
