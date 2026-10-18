package com.shortvideo.services.impl;

import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.mapper.FollowMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.user.LoginDTO;
import com.shortvideo.pojo.dto.user.RegisterDTO;
import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.entity.UserLow;
import com.shortvideo.pojo.vo.*;
import com.shortvideo.pojo.vo.user.*;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.services.UserServices;
import com.shortvideo.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
public class UserServicesImpl implements UserServices {

    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private AliyunOSSperator aliyunOSSperator;
    @Transactional
    @Override
    public LoginVO register(RegisterDTO registerDTO,String ip) {
        UserLow userLow = userMapper.findByPhone(registerDTO.getPhone());
        //手机号重复
        if(userLow != null)
        {
            return null;
        }
        UserHigh exitUser = userMapper.findByUsername(registerDTO.getUsername());
        if(exitUser != null)
        {
            return null;
        }
        String code = redisCacheUtil.get(registerDTO.getPhone());
        log.info("从Redis取验证码: key=[{}], 取到的值=[{}], 用户输入=[{}]", registerDTO.getPhone(), code, registerDTO.getCode());
        if(code == null || !code.equals(registerDTO.getCode()))
        {
            return null;
        }
        String finalPassword = passwordUtil.encrypt(registerDTO.getPassword());
//        String salt = passwordUtil.getSalt(finalPassword);
//        log.info("加密后的密码=[{}], 盐=[{}]", finalPassword, salt);
        //封装用户信息
        UserHigh userHigh = new UserHigh();
        userHigh.setUsername(registerDTO.getUsername());
        userHigh.setNickname(registerDTO.getUsername());
        //不再存密码到high表
//        userHigh.setPassword(finalPassword);
        userMapper.insertRegisterHigh(userHigh);
        //封装敏感信息
        UserLow userLowInfo = new UserLow();
        userLowInfo.setUserId(userHigh.getId());
        userLowInfo.setPhone(registerDTO.getPhone());
        // bcrypt 密文存敏感表
        userLowInfo.setPasswordHash(finalPassword);
        //先写死 后续部署配置好nginx配置即可
        userLowInfo.setLastLoginIp(ip);
        userMapper.insertRegisterLow(userLowInfo);
        String token = jwtUtil.generateToken(userHigh.getId(), userHigh.getUsername());
        //封装返回信息
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(userHigh.getId());
        loginVO.setToken(token);
        loginVO.setRole(userHigh.getRole());
        loginVO.setNickname(userHigh.getNickname());
        loginVO.setAvatarUrl(userHigh.getAvatarUrl());
        return loginVO;
    }
    //发送验证码
    @Override
    public String sendCode(RegisterDTO registerDTO) {
        String phone = registerDTO.getPhone();
        if(phone == null)
        {
            return null;
        }
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < 6; i++)
        {
            code.append((int)(Math.random() * 10));
        }
        redisCacheUtil.set(registerDTO.getPhone(), code.toString(), 5, TimeUnit.MINUTES);
        log.info("验证码存入Redis: key=[{}], value=[{}]", registerDTO.getPhone(), code);
        return code.toString();
    }

    //登录功能 （有部分账号是md5加密的 得惰性兼容 登录成功后会更新为bcrypt）已全部转换
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LoginVO loginVO = new LoginVO();
        Long userId = null;
        UserLow userLow = null;
        UserHigh userHigh = null;
        boolean isPhone = loginDTO.getAccount().matches("1[3-9]\\d{9}");
        if(isPhone)//用的手机号登录 得查low表
        {
            userLow = userMapper.findByPhone(loginDTO.getAccount());
            if(userLow == null) return null;
            userId = userLow.getUserId();

            userHigh = userMapper.findByUserId(userLow.getUserId());
            //账号状态检查
            LocalDateTime now = LocalDateTime.now();
            if (userHigh.getStatus() != 1) {
                LocalDateTime banUntil = userHigh.getBanUntil();
                if (banUntil != null && LocalDateTime.now().isAfter(banUntil)) {
                    userMapper.updateUserStatus(userHigh.getId(), 1);   //恢复
                } else {
                    throw new BusinessException(userHigh.getStatus() == 3 ? "账号已被封禁" : "账号已被冻结");
                }
            }
        }else{
            userHigh = userMapper.findByUsername(loginDTO.getAccount());
            if(userHigh == null) return null;
            //账号状态检查
            LocalDateTime now = LocalDateTime.now();
            if (userHigh.getStatus() != 1) {
                LocalDateTime banUntil = userHigh.getBanUntil();
                if (banUntil != null && LocalDateTime.now().isAfter(banUntil)) {
                    userMapper.updateUserStatus(userHigh.getId(), 1);   //恢复
                } else {
                    throw new BusinessException(userHigh.getStatus() == 3 ? "账号已被封禁" : "账号已被冻结");
                }
            }
            userId = userHigh.getId();
            userLow = userMapper.findByUserIdUserLow(userId);
        }

        if (userLow == null) return null;
        String raw = loginDTO.getPassword();
        // 新: bcrypt 密文 旧: 只有盐
        String storedLow = userLow.getPasswordHash();
        // 旧: "md5密文:盐"；  新: null/空
        String storedHigh = userHigh.getPassword();

        boolean ok;
        if (storedLow != null && storedLow.startsWith("$2")) {
            //—— bcrypt ——
            ok = passwordUtil.matches(raw, storedLow);
        } else if (storedHigh != null && storedHigh.contains(":")) {
            //—— 旧格式：MD5("密文:盐")，按旧算法校验 ——
            ok = legacyMd5Match(raw, storedHigh);
            if (ok) {
                //登录成功 → 当场用明文重新 bcrypt，升级存进敏感表
                String newHash = passwordUtil.encrypt(raw);
                try {
                    userMapper.updatePasswordHash(userId, newHash);
                    userMapper.clearLegacyPassword(userId);   //把密文对从高频表清掉
                    log.info("存量用户密码已惰性升级为 bcrypt: userId={}", userId);
                } catch (Exception e) {
                    log.warn("密码升级失败(本次仍登录成功): userId={}", userId, e);
                }
            }
        } else {
            //既没有 bcrypt 也没有旧密文对
            ok = false;
        }

        if (!ok) {
            return null;
        }
        //生成token
        String token = jwtUtil.generateToken(userId, loginDTO.getAccount());
        loginVO.setUserId(userId);
        loginVO.setToken(token);
        loginVO.setRole(userHigh.getRole());
        loginVO.setNickname(userHigh.getNickname());
        loginVO.setAvatarUrl(userHigh.getAvatarUrl());
        return loginVO;

    }

    // 旧 MD5 数据兼容校验：仅用于存量用户迁移，新用户一律走 bcrypt
    private boolean legacyMd5Match(String rawPassword, String storedFull) {
        if (rawPassword == null || storedFull == null || !storedFull.contains(":")) {
            return false;
        }
        String salt = storedFull.substring(storedFull.indexOf(':') + 1);
        String md5 = org.springframework.util.DigestUtils
                .md5DigestAsHex((rawPassword + salt).getBytes());
        return (md5 + ":" + salt).equals(storedFull);
    }

    //获取当前用户信息
    @Override
    public UserProfileVO profile() {
        UserProfileVO userProfileVO = new UserProfileVO();
        Long userId = CurrentHolderUtil.getCurrent();
//        if(userId == null)
//        {
//            return null;
//        }
        //封装信息
        UserHigh userHigh = userMapper.findByUserId(userId);
        userProfileVO.setUserId(userHigh.getId());
        userProfileVO.setUsername(userHigh.getUsername());
        userProfileVO.setNickname(userHigh.getNickname());
        userProfileVO.setAvatarUrl(userHigh.getAvatarUrl());
        userProfileVO.setSignature(userHigh.getSignature());
        userProfileVO.setRole(userHigh.getRole());
        userProfileVO.setFollowCount(userHigh.getFollowCount());
        userProfileVO.setFollowerCount(userHigh.getFollowerCount());
        userProfileVO.setTotalLikes(userHigh.getTotalLikes());
        userProfileVO.setIsFollowed(false);
        List<VideoCardVO> videoData = videoMapper.getVideoDataByUserId(userId);
        userProfileVO.setVideoData(videoData);
        userProfileVO.setVideoCount(videoData.size());
        return userProfileVO;
    }

    @Override
    public Boolean updateUserInfo(UpdateProfileDTO updateProfileDTO) {
        updateProfileDTO.setUserId(CurrentHolderUtil.getCurrent());
        return userMapper.updateUserInfo(updateProfileDTO) > 0;
    }

    @Override
    public UserProfileVO getOtherProfile(Long userId) {
        Long currentUserId = CurrentHolderUtil.getCurrent();
        UserProfileVO userProfileVO = new UserProfileVO();
        UserHigh userHigh = userMapper.findByUserId(userId);
        List<VideoCardVO> videoData = videoMapper.getVideoDataByUserId(userId);
        //查询是否关注了他
        Integer result = followMapper.isFollowById(currentUserId, userId);
        Boolean isFollowed = result != null && result > 0;
        userProfileVO.setUserId(userHigh.getId());
        userProfileVO.setUsername(userHigh.getUsername());
        userProfileVO.setNickname(userHigh.getNickname());
        userProfileVO.setAvatarUrl(userHigh.getAvatarUrl());
        userProfileVO.setSignature(userHigh.getSignature());
        userProfileVO.setRole(userHigh.getRole());
        userProfileVO.setFollowCount(userHigh.getFollowCount());
        userProfileVO.setFollowerCount(userHigh.getFollowerCount());
        userProfileVO.setTotalLikes(userHigh.getTotalLikes());
        userProfileVO.setIsFollowed(isFollowed);
        userProfileVO.setVideoCount(videoData.size());
        userProfileVO.setVideoData(videoData);
        return userProfileVO;
    }
    //获取用户列表
    @Override
    public CursorPageVO<UserListVO> list(int size, Long lastId) {
        List<UserListVO> userListVOList = userMapper.list(size+1, lastId);
        boolean hasMore = false;
        if(userListVOList.size() > size)
        {
            hasMore = true;
            userListVOList.remove(userListVOList.size() - 1);//移除最后一条
        }
        //计算最后一个ID(作为下次请求的lastId)
        Long newLastId = userListVOList.isEmpty() ? 0 : userListVOList.getLast().getId();

        return new CursorPageVO<>(userListVOList, newLastId, hasMore, size);
    }
    //Redis Lua 脚本：原子签到 = "查重(GETBIT) + 置位(SETBIT) + 续期(EXPIRE)" 一步完成。
    //Lua 在 Redis 内是原子执行的，彻底防止并发/重复请求造成重复签到。
    //KEYS[1] = 当月位图 key，ARGV[1] = 今天在当月第几天-1(位图下标)；返回 0=今天已签到 1=签到成功
    private static final DefaultRedisScript<Long> SIGN_SCRIPT = new DefaultRedisScript<>(
            "local offset = tonumber(ARGV[1])\n" +
            "if redis.call('GETBIT', KEYS[1], offset) == 1 then return 0 end\n" +
            "redis.call('SETBIT', KEYS[1], offset, 1)\n" +
            "redis.call('EXPIRE', KEYS[1], 7776000)\n" +// 续期90天(秒)，防止中途过期丢数据
            "return 1\n", Long.class);

    //签到功能
    @Override
    public SignResultVO sign(Long userId) {
        //        if(stringRedisTemplate.hasKey(key)) throw new RuntimeException("今天已经签到过了");
        //        这里判断的逻辑是错误的 key是整个月 如果有一天签到的话剩下的都签不了
        //2. 组装位图 key：user:sign:{userId}:{yyyyMM}
        LocalDateTime today = LocalDateTime.now();
        String todayStr = today.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + todayStr;
        //3. 用 Lua 原子执行"查重+置位+续期"，返回 0 表示今天已签到
        Long signed = stringRedisTemplate.execute(SIGN_SCRIPT,
                List.of(key),
                String.valueOf(today.getDayOfMonth() - 1));
        if(signed != null && signed == 0L)
            throw new BusinessException("今天已经签到过了");
        //3. 签到
//        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth-1, true);
//        stringRedisTemplate.expire(key, 90, TimeUnit.DAYS);//设置过期时间 3个月
        //4. 计算这个月连续签到的天数
        int continuousDays = calcContinuousDays(userId,today);
        //5. 组装VO返回
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSigned(true);
        signResultVO.setContinuousDays(continuousDays);
        return signResultVO;
    }



    //二开：统计用户签到
    @Override
    public SignDetailVO signCount(Integer year, Integer month) {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new BusinessException("用户未登录");
//        String yearAndMonth = String.valueOf(year) + String.valueOf(month);// "202604" 这个是错误的
        String yearAndMonth = String.format("%04d%02d", year, month);// "202604"
        String key = RedisConstants.USER_SIGN_KEY + userId + ":"+ yearAndMonth;
        // 1. 查 Redis 位图
        List<Integer> signedDays = new ArrayList<>();
        for(int i = 0; i < 31; i++)
        {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            if(Boolean.TRUE.equals(bit)){
                signedDays.add(1 + i);// 转为 1~31 日
            }
        }
        //2. 如果redis没有签到记录 查数据库
        if(signedDays.isEmpty())
        {
            signedDays = userMapper.signCount(userId, yearAndMonth);
        }
        SignDetailVO vo = new SignDetailVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setSignedDays(signedDays);
        vo.setTotalDays(signedDays.size());
        return vo;
    }

    //计算连续签到的天数
    private int calcContinuousDays(Long userId, LocalDateTime today) {
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + today.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int continuous = 0;
        int offset = today.getDayOfMonth() - 1;
        //从今天开始往前逐位检查
        while (offset >= 0)
        {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, offset);
            if(Boolean.TRUE.equals(bit)){
                continuous++;
                offset--;
            }else {
                //走到0就停 拿到连续天数
                break;
            }
        }
        return continuous;
    }

    //计算已经签到的天数(这个不是连续签到的)
    private int calcSignDays(Long userId, LocalDateTime today) {
        String todayStr = today.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + todayStr;
        int dayOfMonth = today.getDayOfMonth();
        //获取本月截止今天为止的所有的签到记录 返回的是一个十进制数字
        List<Long> bits = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));//从0开始
        if(bits == null || bits.isEmpty())
            return 0;
        Long num = bits.get(0);
        if(num == null || num == 0)
            return 0;
        int count = 0;
        while (true)
        {
            //7.让这个数字与1做与运算，得到数字的最后一个bit位
            if((num & 1) == 0)
            {
                //8.判断这个bit位是否为0
                //9.如果为0，说明未签到，反之已签到 计数器+1
                break;
            }else {
                count++;

            }
            num >>>= 1; //10.bit位右移一位 无符号的右移 有符号是>>
        }
        return count;
    }

    //上传头像
    @Override
    public String uploadAvatar(MultipartFile file) {
        if(file == null || file.isEmpty()) throw new BusinessException("头像文件不能为空");
        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image")) throw new BusinessException("头像必须是图片");
        if (file.getSize() > 5 * 1024 * 1024) throw new BusinessException("头像不能超过 5MB");
        try (InputStream in = file.getInputStream()) {   // 流式，不用 getBytes()
            return aliyunOSSperator.upload(in, file.getSize(),
                    contentType, file.getOriginalFilename());
        } catch (Exception e) {
            log.error("上传头像失败", e);
            throw new RuntimeException("上传头像失败");
        }
    }
}
