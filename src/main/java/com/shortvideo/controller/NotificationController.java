package com.shortvideo.controller;

import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.notification.NotificationReadVO;
import com.shortvideo.pojo.vo.notification.NotificationVO;
import com.shortvideo.services.NotificationServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationServices notificationServices;
    //获取通知信息列表
    @GetMapping("/list")
    public Result<CursorPageVO<NotificationVO>> getNotificationList(@RequestParam(required = false) Long lastId,
                                                                    @RequestParam(required = false) Integer pageSize) {
        CursorPageVO<NotificationVO> cursorPageVO = notificationServices.getNotificationList(lastId, pageSize);
        if (cursorPageVO.getRecords() == null || cursorPageVO.getRecords().isEmpty())
            return Result.success(null);
        return Result.success(cursorPageVO);
    }

    //已读通知
    @PutMapping("/read")
    public Result<NotificationReadVO> readNotification(@RequestParam Long notificationId) {
        NotificationReadVO notificationReadVO = notificationServices.readNotification(notificationId);
        if (notificationReadVO == null)
            return Result.error(404, "通知不存在");
        return Result.success(notificationReadVO);
    }
}
