package com.kob.backend.service.user.bot;

import com.kob.backend.pojo.Bot;

import java.util.List;

public interface GetListService {
    List<Bot> getList(); //要得到一串的Bot所以返回的是List，又因为 Get 会和用户信息绑定，一起触发，所以不要参数
}
