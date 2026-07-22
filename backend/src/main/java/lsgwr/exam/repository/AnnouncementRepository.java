/***********************************************************
 * @Description : 系统公告 Repository（Batch 7.3.1）
 * @author      : Batch 7.3.1
 * @date        : 2026-07-23
 ***********************************************************/
package lsgwr.exam.repository;

import lsgwr.exam.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    /**
     * 查询所有可见公告，按 pinned 倒序 + createTime 倒序排列
     * 仅返回 visible=1 的公告（visible 为 NULL 视为可见，兼容存量数据）
     */
    @Query("select a from Announcement a where a.visible is null or a.visible = 1 order by a.pinned desc, a.createTime desc")
    List<Announcement> findVisibleAll();

    /**
     * I-13 修复：分页查询可见公告
     * 排序固定为 pinned 倒序 + createTime 倒序（业务语义：置顶优先 + 最新优先）
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("select a from Announcement a where a.visible is null or a.visible = 1")
    Page<Announcement> findVisiblePage(Pageable pageable);
}
