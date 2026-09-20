package com.shortvideo.services;

import com.shortvideo.pojo.vo.message.GroupMessageVO;

public interface BotDispatchService {

    /**
     * 异步处理群消息
     * @param msg 群消息
     */
    void dispatchAsync(GroupMessageVO msg,Long senderId);

    void pushToMembers(GroupMessageVO msg);
}
