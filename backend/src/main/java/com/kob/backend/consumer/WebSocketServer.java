package com.kob.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.utils.Game;
import com.kob.backend.consumer.utils.JwtAuthentication;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;



@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {
    // final = const in c++
    final private static ConcurrentHashMap<Integer,WebSocketServer> users = new ConcurrentHashMap<>(); //api给的一个线程安全的哈希表
    final private static CopyOnWriteArraySet<User> matchpool = new CopyOnWriteArraySet<>(); //这里开个匹配池，开成一个线程安全的set，User（是自己的）填进泛型里
    //可以把用户id映射出来
    //静态变量和成员变量的区别，定义成静态的是对所有实例可见，是所有实例的全局变量，
    //要求每个线程里的用户都要有，不加的话，就是每个实例有自己独一份的这个
    private User user; //用户信息存在成员变量里面的user里面

    private Session session = null;

    private static UserMapper userMapper; //这行以及下面四行是UserMapper特殊的注入方式
    //非传统单例模式，单例就是同时只允许存在一个类的实例UserMapper不是单利模式
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        // 建立连接，一旦建立就执行里面的函数
        this.session = session;
        System.out.println("connected!");
        Integer userId = JwtAuthentication.getUserId(token); //解析出用户id
        this.user =  userMapper.selectById(userId); //拿到的id去数据库里查出来
        if (this.user != null) {
            users.put(userId, this);
        } else {
            this.session.close(); //断开连接
        }


        System.out.println(users);
    }

    @OnClose
    public void onClose() {
        System.out.println("disconnected!");
        if (this.user != null) {
            users.remove(this.user.getId());
            matchpool.remove(this.user);
        }
    }


    private void startMatching() {
        System.out.println("start matching!");
        matchpool.add(this.user); //user添进匹配池

        while (matchpool.size() >= 2) {
            Iterator<User> it = matchpool.iterator();
            User a = it.next(), b = it.next();
            matchpool.remove(a);
            matchpool.remove(b);

            Game game = new Game(13, 14, 20);
            game.createMap();

            JSONObject respA = new JSONObject();
            respA.put("event", "start-matching");
            respA.put("opponent_username", b.getUsername());
            respA.put("opponent_photo", b.getPhoto());
            respA.put("gamemap", game.getG());
            users.get(a.getId()).sendMessage(respA.toJSONString());

            JSONObject respB = new JSONObject();
            respB.put("event", "start-matching");
            respB.put("opponent_username", a.getUsername());
            respB.put("opponent_photo", a.getPhoto());
            respB.put("gamemap", game.getG());
            users.get(b.getId()).sendMessage(respB.toJSONString());
        }
    }

    private void stopMatching() {
        System.out.println("stop matching");
        matchpool.remove(this.user); //断开连接，维护匹配池子里的用户，给他踢出去
    }

    @OnMessage
    public void onMessage(String message, Session session) {  // 通过通信结果，当做路由
        System.out.println("receive message!");
        JSONObject data = JSONObject.parseObject(message);// 这里把拿到的message作为json解析出来
        String event = data.getString("event"); //把前端发送来的是否在匹配的 event 从 message里取出来
        if ("start-matching".equals(event)) { //把event作为参数，防止event为空，报异常，eq比==还要多比引用的数据类型
            startMatching();
        } else if ("stop-matching".equals(event)) {
            stopMatching();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}