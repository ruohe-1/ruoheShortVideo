package com.shortvideo.task;


import com.shortvideo.mapper.SignRecordMapper;
import com.shortvideo.pojo.entity.SignRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.shortvideo.util.RedisConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class SignArchiveTask {
//    每天凌晨 2 点，把昨天的签到数据写入 MySQL
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SignRecordMapper signRecordMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveYesterdaySign(){
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);//拿到昨天的时间
        String month = yesterday.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOffset = yesterday.getDayOfMonth()-1;//拿到偏移量
        //1. 查出所有签到用户的KEY
        String pattern = RedisConstants.USER_SIGN_KEY +"*:"+month;
//        Set<String> keys = stringRedisTemplate.keys(pattern);// 生产环境用 SCAN
        Set<String> keys =  scanKeys(pattern);
        if(keys == null || keys.isEmpty())
        {
            log.info("没有找到昨天签到的用户");
            return;
        }
        //2. 遍历这些KEY，把每个KEY的值（签到数据）写入 MySQL
        List<SignRecord> records = new ArrayList<>();
        for(String key: keys){
            Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, dayOffset);
            if(Boolean.TRUE.equals(isSigned)){
                Long userId = parseUserId(key);
                SignRecord record = new SignRecord();
                record.setUserId(userId);
                record.setSignDate(yesterday.toLocalDate());
                record.setCreatedTime(LocalDateTime.now());
                records.add(record);
            }
        }
        //3. 批量插入
        if(!records.isEmpty()){
            signRecordMapper.batchInsert(records);
            log.info("批量插入签到数据：{}", records.size());
        }
    }
    /**
     * 每月2号凌晨3点，把"上个月整月"的签到数据补全归档
     * 兜底场景：某些天"昨日归档"任务漏跑（服务器重启/停机），这些天的数据会
     * 一直躺在 Redis 位图里，90 天后过期就永久丢了。月度任务一次补全，配合
     * INSERT IGNORE + 唯一键 uk_user_date，重复执行也不会重复插入。
     * 注意：上个月的所有天都必然 <= 昨天，属于"安全区"，可以放心整月归档。
     */
    @Scheduled(cron = "0 0 3 2 * ?")// 每月2号凌晨3点执行
    public void archiveLastMonthSign() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String month = lastMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        //计算那个月有多少天
        int daysInMonth = lastMonth.lengthOfMonth();
        String pattern = RedisConstants.USER_SIGN_KEY + "*:" + month;
        //2. 收集所有用户的签到key
        Set<String> keys = scanKeys(pattern);
        if (keys == null || keys.isEmpty()) {
            log.info("没有找到上个月签到的用户");
            return;
        }
        //3. 遍历这些key拿到记录
        List<SignRecord> records = new ArrayList<>();
        for(String key : keys){
            for (int day = 0; day < daysInMonth; day++) {
                Boolean isSigned = stringRedisTemplate.opsForValue().getBit(key, day);
                if (Boolean.TRUE.equals(isSigned)) {
                    SignRecord record = new SignRecord();
                    record.setUserId(parseUserId(key));
                    record.setSignDate(lastMonth.withDayOfMonth(day + 1));
                    record.setCreatedTime(LocalDateTime.now());
                    records.add(record);
                }
            }
        }
        if(!records.isEmpty())
        {
            // 全量补全可能上万条，分批插入防止单条 SQL 超过 MySQL 包大小限制
            for (int i = 0; i < records.size(); i += 500) {
                signRecordMapper.batchInsert(records.subList(i, Math.min(i + 500, records.size())));
            }
            log.info("上月签到补全归档完成：{} 条", records.size());
        }
    }
    // 解析用户ID
    private Long parseUserId(String key) {
        String[] parts = key.split(":");
        return Long.parseLong(parts[2]);
    }

    // 使用 SCAN 命令查出所有签到用户的KEY
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        try(Cursor<String> cursor = stringRedisTemplate.scan(options)){
            while (cursor.hasNext()){
                keys.add(cursor.next());
            }
        }
        return keys;
    }
}
