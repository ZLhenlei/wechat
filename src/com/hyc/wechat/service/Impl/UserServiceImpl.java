/*
 * Copyright (c) 2019.  黄钰朝
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hyc.wechat.service.Impl;

import com.hyc.wechat.dao.UserDao;
import com.hyc.wechat.exception.DaoException;
import com.hyc.wechat.factory.DaoProxyFactory;
import com.hyc.wechat.model.dto.ServiceResult;
import com.hyc.wechat.model.po.User;
import com.hyc.wechat.service.UserService;
import com.hyc.wechat.service.constants.Status;

import static com.hyc.wechat.service.constants.ServiceMessage.*;
import static com.hyc.wechat.util.Md5Utils.getDigest;

/**
 * @author <a href="mailto:kobe524348@gmail.com">黄钰朝</a>
 * @program wechat
 * @description 负责提供用户相关服务
 * @date 2019-05-02 03:19
 */
public class UserServiceImpl implements UserService {

    private UserDao userDao = (UserDao) DaoProxyFactory.getInstance().getProxyInstance(UserDao.class);

    /**
     * 检查注册用户的信息是否有效
     *
     * @param user 用户对象
     * @return 返回传入时的对象
     */
    @Override
    public ServiceResult checkRegister(User user) {
        try {
            //防止插入id
            user.setId(null);
            //检查邮箱格式
            if (!isValidEmail(user.getEmail())) {
                return new ServiceResult(Status.ERROR, EMAIL_FORMAT_INCORRECT.message, user);
            }
            //检查邮箱是否已被注册
            if (userDao.getUserByEmail(user.getEmail()) != null) {
                return new ServiceResult(Status.ERROR, EMAIL_ALREADY_USED.message, user);
            }
            //检查密码是否合法
            if (!isValidPassword(user.getPassword())) {
                return new ServiceResult(Status.ERROR, INVALID_PASSWORD.message, user);
            }
        } catch (DaoException e) {
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);
        }
        return new ServiceResult(Status.SUCCESS,REGISTER_INFO_VALID.message, user);
}

    /**
     * 添加一个用户账号
     *
     * @param user 用户对象
     * @return 返回传入的用户的对象
     */
    @Override
    public ServiceResult insertUser(User user) {
        try {
            //插入前对用户的密码进行加密
            user.setPassword(getDigest(user.getPassword()));
            if (userDao.insert(user) != 1) {
                return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);
            }
            //插入后销毁用户对象中的密码数据
            user.setPassword(null);
        } catch (DaoException e) {
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);
        }
        return new ServiceResult(Status.SUCCESS, REGISTER_SUCCESS.message, user);
    }

    /**
     * 校验用户的密码
     *
     * @param user 用户对象
     * @return 返回传入的用户对象
     */
    @Override
    public ServiceResult checkPassword(User user) {
        User realUser;
        try {
            realUser = userDao.getUserByEmail(user.getEmail());
            //检查账号是否存在
            if (realUser == null) {
                return new ServiceResult(Status.ERROR, ACCOUNT_NOT_FOUND.message, user);
            }
            //检查密码是否正确
            if (user.getPassword()==null||!realUser.getPassword().equals(getDigest(user.getPassword()))) {
                return new ServiceResult(Status.ERROR, PASSWORD_INCORRECT.message, user);
            }
            //登陆成功后返回该用户的id信息
            user.setId(realUser.getId());
        } catch (DaoException e) {
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);

        }
        return new ServiceResult(Status.SUCCESS, LOGIN_SUCCESS.message, user);
    }

    /**
     * 校验用户名（微信号），是否合法，是否已被占用
     *
     * @param wechatId 微信号
     * @return 返回传入的用户名
     */
    @Override
    public ServiceResult checkWechatId(String wechatId) {
        try {
            if(!isValidWechatId(wechatId)){
                return new ServiceResult(Status.ERROR, WECHAT_ID_INVALID.message,wechatId);
            }
            if (userDao.getUserByWechatId(wechatId) !=null) {
                return new ServiceResult(Status.ERROR, WECHAT_ID_USED.message,wechatId);
            }
        } catch (DaoException e) {
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, wechatId);
        }
        return new ServiceResult(Status.SUCCESS, WECHAT_ID_VALID.message, wechatId);
    }


    /**
     * 通过用户id获取用户个人信息
     *
     * @param id 用户id
     * @return 返回用户的个人信息
     */
    @Override
    public ServiceResult getUserInfo(Object id) {
        User user = null;
        try {
            user = userDao.getUserById(id);
            if(user==null){
                return new ServiceResult(Status.ERROR, NO_USER_INFO.message, user);
            }
        }catch (DaoException e){
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);
        }
        return new ServiceResult(Status.SUCCESS, GET_INFO_SUCCESS.message, user);
    }

    /**
     * 更新用户的个人信息,不包括密码，邮箱
     *
     * @param user 用户对象
     * @return 返回传入的用户对象，如果由密码信息/邮箱信息，将被清空
     */
    @Override
    public ServiceResult updateUserInfo(User user) {
        try {
            //阻止更新用户密码
            user.setPassword(null);
            //阻止更新邮箱
            user.setEmail(null);
            if(userDao.update(user)!=1){
                return new ServiceResult(Status.ERROR, UPDATE_USER_FAILED.message, user);
            }
        }catch (DaoException e){
            e.printStackTrace();
            return new ServiceResult(Status.ERROR, SYSTEM_EXECEPTION.message, user);
        }
        return new ServiceResult(Status.SUCCESS,UPDATE_INFO_SUCCESS.message, user);
    }



    /*
     **************************************************************
     *               检查用户信息
     **************************************************************
     */

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String regex = "\\w[-\\w.+]*@([A-Za-z0-9][-A-Za-z0-9]+\\.)+[A-Za-z]{2,14}";
        return email.matches(regex);
    }


    private boolean isValidWechatId(String wechatId) {
        if (wechatId == null || wechatId.trim().isEmpty()) {
            return false;
        }
        String regex = "[\\w_]{6,20}$";
        return wechatId.matches(regex);
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        String regex = "[\\w_]{6,20}$";
        return password.matches(regex);
    }

    private boolean isValidPhoneNum(String number) {
        if (number == null || number.trim().isEmpty()) {
            return false;
        }
        String regex = "0?(13|14|15|17|18|19)[0-9]{9}";
        return number.matches(regex);
    }

    private boolean isValidIdNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            return false;
        }
        String regex = "\\d{17}[\\d|x]|\\d{15}";
        return number.matches(regex);
    }

    private boolean isValidNickName(String name) {
        if (name == null || name.trim().isEmpty() || name.length() > 20) {
            return false;
        }
        return true;
    }


}
