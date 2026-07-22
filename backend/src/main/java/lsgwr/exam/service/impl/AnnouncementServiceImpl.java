/***********************************************************
 * @Description : I-08 修复：公告 Service 实现，遵循分层架构
 *                A-11 修复：对公告内容进行 XSS 过滤
 *                I-09/D-06 修复：使用 VO 避免直接接收实体
 ***********************************************************/
package lsgwr.exam.service.impl;

import lsgwr.exam.entity.Announcement;
import lsgwr.exam.enums.ResultEnum;
import lsgwr.exam.exception.ExamException;
import lsgwr.exam.repository.AnnouncementRepository;
import lsgwr.exam.service.AnnouncementService;
import lsgwr.exam.util.AuditLogger;
import lsgwr.exam.vo.AnnouncementSaveVo;
import lsgwr.exam.vo.AnnouncementVo;
import lsgwr.exam.vo.PageResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementServiceImpl.class);

    // A-11 修复：XSS 过滤正则
    private static final Pattern SCRIPT_TAG = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER = Pattern.compile("\\son\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_URL = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern IFRAME_TAG = Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_TAG = Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EMBED_TAG = Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE);

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Override
    public List<AnnouncementVo> listVisible() {
        List<Announcement> announcements = announcementRepository.findVisibleAll();
        List<AnnouncementVo> voList = new ArrayList<>(announcements.size());
        for (Announcement a : announcements) {
            AnnouncementVo vo = new AnnouncementVo();
            BeanUtils.copyProperties(a, vo);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * I-13 修复：分页查询可见公告
     * 排序固定为 pinned DESC + createTime DESC（业务语义：置顶优先 + 最新优先）
     */
    @Override
    public PageResultVo<AnnouncementVo> listVisiblePage(Pageable pageable) {
        Page<Announcement> page = announcementRepository.findVisiblePage(pageable);
        List<AnnouncementVo> voList = new ArrayList<>(page.getContent().size());
        for (Announcement a : page.getContent()) {
            AnnouncementVo vo = new AnnouncementVo();
            BeanUtils.copyProperties(a, vo);
            voList.add(vo);
        }
        return new PageResultVo<>(voList, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public AnnouncementVo create(AnnouncementSaveVo vo, String creatorId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(sanitizeXss(vo.getTitle()));
        announcement.setContent(sanitizeXss(vo.getContent()));
        announcement.setType(vo.getType() != null ? vo.getType() : "info");
        announcement.setPinned(vo.getPinned() != null ? vo.getPinned() : 0);
        announcement.setVisible(vo.getVisible() != null ? vo.getVisible() : 1);
        announcement.setCreatorId(creatorId);
        Date now = new Date();
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        Announcement saved = announcementRepository.save(announcement);
        log.info("管理员{}创建公告：{}", creatorId, saved.getAnnouncementId());
        // P-08 修复：审计日志 - 创建公告
        AuditLogger.success(creatorId, "ANNOUNCEMENT_CREATE",
                String.valueOf(saved.getAnnouncementId()), "title=" + saved.getTitle());
        AnnouncementVo resultVo = new AnnouncementVo();
        BeanUtils.copyProperties(saved, resultVo);
        return resultVo;
    }

    @Override
    public AnnouncementVo update(AnnouncementSaveVo vo) {
        if (vo.getAnnouncementId() == null) {
            throw new ExamException(ResultEnum.PARAM_ERR.getCode(), "公告ID不能为空");
        }
        Announcement existing = announcementRepository.findById(vo.getAnnouncementId())
                .orElseThrow(() -> new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "公告不存在"));
        // I-09/D-06 修复：仅更新允许的字段，creatorId/createTime 不可被覆盖
        existing.setTitle(sanitizeXss(vo.getTitle()));
        existing.setContent(sanitizeXss(vo.getContent()));
        if (vo.getType() != null) existing.setType(vo.getType());
        if (vo.getPinned() != null) existing.setPinned(vo.getPinned());
        if (vo.getVisible() != null) existing.setVisible(vo.getVisible());
        existing.setUpdateTime(new Date());
        Announcement saved = announcementRepository.save(existing);
        // P-08 修复：审计日志 - 更新公告
        AuditLogger.success("system", "ANNOUNCEMENT_UPDATE",
                String.valueOf(saved.getAnnouncementId()), "title=" + saved.getTitle());
        AnnouncementVo resultVo = new AnnouncementVo();
        BeanUtils.copyProperties(saved, resultVo);
        return resultVo;
    }

    @Override
    public void delete(Integer id) {
        if (!announcementRepository.existsById(id)) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "公告不存在");
        }
        announcementRepository.deleteById(id);
        log.info("删除公告：{}", id);
        // P-08 修复：审计日志 - 删除公告
        AuditLogger.success("system", "ANNOUNCEMENT_DELETE", String.valueOf(id), "硬删除");
    }

    /**
     * A-11 修复：过滤 XSS 危险内容
     */
    private String sanitizeXss(String input) {
        if (input == null) return null;
        String result = input;
        result = SCRIPT_TAG.matcher(result).replaceAll("");
        result = IFRAME_TAG.matcher(result).replaceAll("");
        result = OBJECT_TAG.matcher(result).replaceAll("");
        result = EMBED_TAG.matcher(result).replaceAll("");
        result = EVENT_HANDLER.matcher(result).replaceAll("");
        result = JS_URL.matcher(result).replaceAll("");
        return result;
    }
}
