/***********************************************************
 * @Description : I-08 修复：公告 Service 接口，遵循分层架构
 ***********************************************************/
package lsgwr.exam.service;

import lsgwr.exam.vo.AnnouncementSaveVo;
import lsgwr.exam.vo.AnnouncementVo;
import lsgwr.exam.vo.PageResultVo;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AnnouncementService {
    List<AnnouncementVo> listVisible();

    /**
     * I-13 修复：分页查询可见公告
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResultVo<AnnouncementVo> listVisiblePage(Pageable pageable);

    AnnouncementVo create(AnnouncementSaveVo vo, String creatorId);

    AnnouncementVo update(AnnouncementSaveVo vo);

    void delete(Integer id);
}
