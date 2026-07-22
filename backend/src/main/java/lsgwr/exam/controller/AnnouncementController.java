/***********************************************************
 * @Description : 系统公告 Controller（Batch 7.3.1）
 *                I-08 修复：通过 Service 层访问数据，遵循分层架构
 *                I-09/D-06 修复：使用 VO 而非实体接收请求，避免 mass assignment
 *                GET /api/announcement/list 为公开接口（已登录用户均可查看）
 *                POST/PUT/DELETE 为管理员接口
 ***********************************************************/
package lsgwr.exam.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lsgwr.exam.annotation.RoleRequired;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.service.AnnouncementService;
import lsgwr.exam.vo.AnnouncementSaveVo;
import lsgwr.exam.vo.AnnouncementVo;
import lsgwr.exam.vo.PageResultVo;
import lsgwr.exam.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@Api(tags = "Announcement APIs")
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/list")
    @ApiOperation("获取所有可见公告（已登录用户均可查看）")
    ResultVO<List<AnnouncementVo>> list() {
        List<AnnouncementVo> voList = announcementService.listVisible();
        return new ResultVO<>(0, "获取公告列表成功", voList);
    }

    /**
     * I-13 修复：分页查询可见公告
     * 排序固定为 pinned DESC + createTime DESC（业务语义：置顶优先 + 最新优先）
     * 兼容前端 BootstrapTable 的 { total, rows } 结构
     */
    @GetMapping("/page")
    @ApiOperation("分页查询可见公告（参数：page 0-based, size）")
    ResultVO<PageResultVo<AnnouncementVo>> listPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // 业务语义固定排序：pinned DESC + createTime DESC，前端不可控排序字段
        Sort sort = Sort.by(Sort.Direction.DESC, "pinned")
                .and(Sort.by(Sort.Direction.DESC, "createTime"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        PageResultVo<AnnouncementVo> result = announcementService.listVisiblePage(pageable);
        return new ResultVO<>(0, "获取公告分页列表成功", result);
    }

    @PostMapping("/create")
    @ApiOperation("创建公告（管理员）")
    @RoleRequired({RoleEnum.ADMIN})
    ResultVO<AnnouncementVo> create(@RequestBody @Valid AnnouncementSaveVo vo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        AnnouncementVo result = announcementService.create(vo, userId);
        return new ResultVO<>(0, "公告创建成功", result);
    }

    @PostMapping("/update")
    @ApiOperation("更新公告（管理员）")
    @RoleRequired({RoleEnum.ADMIN})
    ResultVO<AnnouncementVo> update(@RequestBody @Valid AnnouncementSaveVo vo) {
        AnnouncementVo result = announcementService.update(vo);
        return new ResultVO<>(0, "公告更新成功", result);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除公告（管理员）")
    @RoleRequired({RoleEnum.ADMIN})
    ResultVO<Void> delete(@PathVariable Integer id) {
        announcementService.delete(id);
        return new ResultVO<>(0, "公告删除成功", null);
    }
}
