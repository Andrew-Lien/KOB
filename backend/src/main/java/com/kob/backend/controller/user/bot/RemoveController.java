package com.kob.backend.controller.user.bot;

import com.kob.backend.service.user.bot.RemoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RemoveController {
    @Autowired
    private RemoveService removeService;

    @PostMapping("/user/bot/remove/") //做一个映射，请求到这个url就执行下面的函数
    public Map<String, String> remove(@RequestParam Map<String, String> data) { //@requestParam 接收参数要的注解
        return removeService.remove(data);
    }
}

