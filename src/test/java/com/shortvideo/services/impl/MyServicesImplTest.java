package com.shortvideo.services.impl;

import com.shortvideo.mapper.MyMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.my.MyVideoVO;
import com.shortvideo.util.CurrentHolderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MyServicesImpl 单元测试 —— 拿 getMyVideoList 当第一个样板。
 *
 * 三个核心注解，先记住它们就够了：
 *  @Mock        —— 造一个「假的」MyMapper，不查数据库，行为由我们规定；
 *  @InjectMocks —— 把上面那些假对象，塞进「真的」MyServicesImpl 里（代替 Spring 的 @Autowired 注入）；
 *  @ExtendWith  —— 打开 Mockito 对 JUnit5 的支持（不写它 mock 不生效）。
 *
 * 每个测试的固定套路：
 *  1. 准备输入（CurrentHolderUtil.setCurrent 模拟登录用户）；
 *  2. 规定 mock 的行为（when(...).thenReturn(...)，即「当被这样调用时返回什么」）；
 *  3. 调用被测方法；
 *  4. 断言结果（assertThat(...)，不符合预期测试就红）。
 */
@ExtendWith(MockitoExtension.class)
class MyServicesImplTest {

    @Mock
    private MyMapper myMapper;        // 假 mapper
    @Mock
    private VideoMapper videoMapper;  // 这个用例用不到，但字段存在，所以也造一个

    @InjectMocks
    private MyServicesImpl myServices; // 被测的真实对象

    @AfterEach
    void tearDown() {
        // 每个测试跑完清理 ThreadLocal，防止污染下一个用例
        CurrentHolderUtil.removeCurrent();
    }

    /** 辅助方法：快速造一个只有 videoId 的视频对象 */
    private MyVideoVO video(long id) {
        MyVideoVO vo = new MyVideoVO();
        vo.setVideoId(id);
        return vo;
    }

    @Test
    @DisplayName("未登录 → 抛异常，且不查数据库")
    void shouldThrowWhenNotLoggedIn() {
        // 不 setCurrent，getCurrent() 返回 null
        assertThatThrownBy(() -> myServices.getMyVideoList(10, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("未登录");

        // 提前抛异常了，mapper 根本不该被调用
        verify(myMapper, never()).getMyVideoList(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("空列表 → 返回空分页")
    void shouldReturnEmptyPageWhenNoVideo() {
        CurrentHolderUtil.setCurrent(1L);
        // 规定：mapper 被 (userId=1, size=11, lastId=null) 调用时返回空列表
        // 注意 size+1：service 内部会传 size+1 去「多查一条探测还有没有下一页」
        when(myMapper.getMyVideoList(1L, 11, null)).thenReturn(new ArrayList<>());

        CursorPageVO<MyVideoVO> result = myServices.getMyVideoList(10, null);

        assertThat(result.getRecords()).isNull();
        assertThat(result.getHasMore()).isFalse();
    }

    @Test
    @DisplayName("不足一页(3 < 10) → 全返回，hasMore=false，游标=最后一条id")
    void shouldReturnAllWhenLessThanOnePage() {
        CurrentHolderUtil.setCurrent(1L);
        when(myMapper.getMyVideoList(1L, 11, null))
                .thenReturn(List.of(video(5L), video(4L), video(3L)));

        CursorPageVO<MyVideoVO> result = myServices.getMyVideoList(10, null);

        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getLastId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("刚好一页(10 = 10) → hasMore=false")
    void shouldNotHaveMoreWhenExactlyOnePage() {
        CurrentHolderUtil.setCurrent(1L);
        List<MyVideoVO> list = new ArrayList<>();
        for (long id = 10; id >= 1; id--) list.add(video(id)); // 正好 10 条
        when(myMapper.getMyVideoList(1L, 11, null)).thenReturn(list);

        CursorPageVO<MyVideoVO> result = myServices.getMyVideoList(10, null);

        assertThat(result.getRecords()).hasSize(10);
        assertThat(result.getHasMore()).isFalse();   // 10 条没有 > 10，所以 hasMore=false
        assertThat(result.getLastId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("超过一页(11 > 10) → 只返回10条，hasMore=true，游标=第10条id")
    void shouldHasMoreWhenOverOnePage() {
        CurrentHolderUtil.setCurrent(1L);
        List<MyVideoVO> list = new ArrayList<>();
        for (long id = 11; id >= 1; id--) list.add(video(id)); // 11 条，多 1 条用于探测
        when(myMapper.getMyVideoList(1L, 11, null)).thenReturn(list);

        CursorPageVO<MyVideoVO> result = myServices.getMyVideoList(10, null);

        assertThat(result.getRecords()).hasSize(10); // 第 11 条被 remove 掉了
        assertThat(result.getHasMore()).isTrue();
        assertThat(result.getLastId()).isEqualTo(2L); // 剩余 10 条里最后一条 id=2
    }
}
