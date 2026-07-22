/***********************************************************
 * @Description : 考试服务
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-28 08:04
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.controller;

import lsgwr.exam.entity.Exam;
import lsgwr.exam.entity.ExamRecord;
import lsgwr.exam.service.ExamService;
import lsgwr.exam.annotation.RoleRequired;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.enums.ResultEnum;
import lsgwr.exam.exception.ExamException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lsgwr.exam.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
@Api(tags = "Exam APIs")
@RequestMapping("/api/exam")
public class ExamController {
    private static final Logger log = LoggerFactory.getLogger(ExamController.class);

    @Autowired
    private ExamService examService;

    @GetMapping("/question/all")
    @ApiOperation("获取所有问题的列表")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<List<QuestionVo>> getQuestionAll() {
        List<QuestionVo> questionAll = examService.getQuestionAll();
        return new ResultVO<>(0, "获取全部问题列表成功", questionAll);
    }

    @GetMapping("/question/page")
    @ApiOperation("分页查询问题列表（服务端分页，参数：page 0-based, size, sort字段[,asc|desc]）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<PageResultVo<QuestionVo>> getQuestionPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "updateTime,desc") String sort) {
        // 解析 sort 参数，例如 "updateTime,desc" -> Sort.by(DESC, "updateTime")
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(dir, parts[0]));
        PageResultVo<QuestionVo> result = examService.getQuestionPage(pageable);
        return new ResultVO<>(0, "获取问题分页列表成功", result);
    }

    @PostMapping("/question/update")
    @ApiOperation("更新问题")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<QuestionVo> questionUpdate(@RequestBody QuestionVo questionVo, HttpServletRequest request) {
        // P0-4 修复 [I-06, A-02]：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        QuestionVo result = examService.updateQuestion(questionVo, userId, roleId);
        return new ResultVO<>(0, "更新问题成功", result);
    }

    @PostMapping("/question/create")
    @ApiOperation("创建问题")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<String> questionCreate(@RequestBody QuestionCreateSimplifyVo questionCreateSimplifyVo, HttpServletRequest request) {
        QuestionCreateVo questionCreateVo = new QuestionCreateVo();
        BeanUtils.copyProperties(questionCreateSimplifyVo, questionCreateVo);
        String userId = (String) request.getAttribute("user_id");
        questionCreateVo.setQuestionCreatorId(userId);
        examService.questionCreate(questionCreateVo);
        return new ResultVO<>(0, "问题创建成功", null);
    }

    @GetMapping("/question/selection")
    @ApiOperation("获取问题分类的相关选项")
    ResultVO<QuestionSelectionVo> getSelections() {
        QuestionSelectionVo questionSelectionVo = examService.getSelections();
        return new ResultVO<>(0, "获取问题分类选项成功", questionSelectionVo);
    }

    @GetMapping("/question/detail/{id}")
    @ApiOperation("根据问题的id获取问题的详细信息")
    ResultVO<QuestionDetailVo> getQuestionDetail(@PathVariable String id) {
        QuestionDetailVo questionDetailVo = examService.getQuestionDetail(id);
        return new ResultVO<>(0, "获取问题详情成功", questionDetailVo);
    }

    @GetMapping("/all")
    @ApiOperation("获取全部考试的列表")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<List<ExamVo>> getExamAll() {
        List<ExamVo> examVos = examService.getExamAll();
        return new ResultVO<>(0, "获取全部考试的列表成功", examVos);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询考试列表（服务端分页，参数：page 0-based, size, sort字段[,asc|desc]）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<PageResultVo<ExamVo>> getExamPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "updateTime,desc") String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(dir, parts[0]));
        PageResultVo<ExamVo> result = examService.getExamPage(pageable);
        return new ResultVO<>(0, "获取考试分页列表成功", result);
    }

    @GetMapping("/question/type/list")
    @ApiOperation("获取问题列表，按照单选、多选和判断题分类返回")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<ExamQuestionTypeVo> getExamQuestionTypeList() {
        ExamQuestionTypeVo examQuestionTypeVo = examService.getExamQuestionType();
        return new ResultVO<>(0, "获取问题列表成功", examQuestionTypeVo);
    }

    @PostMapping("/create")
    @ApiOperation("创建考试")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Exam> createExam(@RequestBody ExamCreateVo examCreateVo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        Exam exam = examService.create(examCreateVo, userId);
        return new ResultVO<>(0, "创建考试成功", exam);
    }

    @PostMapping("/update")
    @ApiOperation("更新考试")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Exam> updateExam(@RequestBody ExamVo examVo, HttpServletRequest request) {
        // P0-4 修复 [I-05, A-02]：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        Exam exam = examService.update(examVo, userId, roleId);
        return new ResultVO<>(0, "更新考试成功", exam);
    }

    @GetMapping("/card/list")
    @ApiOperation("获取考试列表，适配前端卡片列表")
    ResultVO<List<ExamCardVo>> getExamCardList() {
        List<ExamCardVo> examCardVoList = examService.getExamCardList();
        return new ResultVO<>(0, "获取考试列表卡片成功", examCardVoList);
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("根据考试的id，获取考试详情")
    ResultVO<ExamDetailVo> getExamDetail(@PathVariable String id, HttpServletRequest request) {
        // 校验考试是否在有效期内，非进行中状态不允许进入考试
        lsgwr.exam.enums.ExamStatusEnum status = examService.checkExamAvailable(id);
        if (status != lsgwr.exam.enums.ExamStatusEnum.AVAILABLE) {
            return new ResultVO<>(-1, "考试" + status.getDescription() + "，无法进入", null);
        }
        ExamDetailVo examDetail = examService.getExamDetail(id);
        // 题目随机化：按学生userId打乱题目顺序，防止抄袭
        String userId = (String) request.getAttribute("user_id");
        examDetail.setRadioIds(examService.shuffleQuestionIds(examDetail.getRadioIds(), userId));
        examDetail.setCheckIds(examService.shuffleQuestionIds(examDetail.getCheckIds(), userId));
        examDetail.setJudgeIds(examService.shuffleQuestionIds(examDetail.getJudgeIds(), userId));
        return new ResultVO<>(0, "获取考试详情成功", examDetail);
    }

    @PostMapping("/finish/{examId}")
    @ApiOperation("根据用户提交的答案对指定id的考试判分")
    ResultVO<ExamRecord> finishExam(@PathVariable String examId, @RequestBody HashMap<String, List<String>> answersMap, @RequestHeader(value = "X-Time-Cost", required = false) Integer timeCost, HttpServletRequest request) {
        // 二次校验考试有效期：仅在 AVAILABLE 状态下允许交卷
        lsgwr.exam.enums.ExamStatusEnum status = examService.checkExamAvailable(examId);
        if (status != lsgwr.exam.enums.ExamStatusEnum.AVAILABLE) {
            return new ResultVO<>(-1, "考试" + status.getDescription() + "，无法交卷", null);
        }
        String userId = (String) request.getAttribute("user_id");
        ExamRecord examRecord = examService.judge(userId, examId, answersMap, timeCost);
        log.info("用户{}完成考试{}判分，得分：{}", userId, examId, examRecord.getExamJoinScore());
        return new ResultVO<>(0, "考卷提交成功", examRecord);
    }

    @GetMapping("/record/list")
    @ApiOperation("获取当前用户的考试记录")
    ResultVO<List<ExamRecordVo>> getExamRecordList(HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        List<ExamRecordVo> examRecordVoList = examService.getExamRecordList(userId);
        return new ResultVO<>(0, "获取考试记录成功", examRecordVoList);
    }

    @GetMapping("/record/detail/{recordId}")
    @ApiOperation("根据考试记录id获取考试记录详情")
    ResultVO<RecordDetailVo> getExamRecordDetail(@PathVariable String recordId, HttpServletRequest request) {
        // P0-3 修复 [I-01, A-01]：学生只能查看自己的考试记录；教师/管理员可查看所有
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        RecordDetailVo recordDetailVo = examService.getRecordDetail(recordId);
        if (recordDetailVo == null || recordDetailVo.getExamRecord() == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在");
        }
        boolean isAdmin = roleId != null && (roleId.equals(RoleEnum.TEACHER.getId())
                || roleId.equals(RoleEnum.ADMIN.getId()));
        if (!isAdmin && !userId.equals(recordDetailVo.getExamRecord().getExamJoinerId())) {
            throw new ExamException(-3, "无权查看他人考试记录");
        }
        return new ResultVO<>(0, "获取考试记录详情成功", recordDetailVo);
    }

    @GetMapping("/record/all/{examId}")
    @ApiOperation("获取指定考试的所有学生考试记录（教师查看）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<List<ExamRecordVo>> getExamAllRecords(@PathVariable String examId, HttpServletRequest request) {
        // A-06 修复：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        List<ExamRecordVo> records = examService.getExamAllRecords(examId, userId, roleId);
        return new ResultVO<>(0, "获取考试记录列表成功", records);
    }

    @GetMapping("/score/stat/{examId}")
    @ApiOperation("获取指定考试的成绩统计信息（教师查看）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<ExamScoreStatVo> getExamScoreStat(@PathVariable String examId, HttpServletRequest request) {
        // A-06 修复：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        ExamScoreStatVo statVo = examService.getExamScoreStat(examId, userId, roleId);
        return new ResultVO<>(0, "获取考试成绩统计成功", statVo);
    }

    @DeleteMapping("/question/{questionId}")
    @ApiOperation("软删除题目（教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Void> deleteQuestion(@PathVariable String questionId, HttpServletRequest request) {
        // P0-4 修复 [I-07, A-02]：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.deleteQuestion(questionId, userId, roleId);
        return new ResultVO<>(0, "题目删除成功", null);
    }

    @DeleteMapping("/{examId}")
    @ApiOperation("软删除考试（教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Void> deleteExam(@PathVariable String examId, HttpServletRequest request) {
        // P0-4 修复 [I-07, A-02]：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.deleteExam(examId, userId, roleId);
        return new ResultVO<>(0, "考试删除成功", null);
    }

    // ==================== Batch 3 随机组卷/排名/主观题/批量导入 ====================

    @PostMapping("/random-create")
    @ApiOperation("随机组卷：按题型数量从题库随机抽题（教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Exam> randomExamCreate(@RequestBody @Valid RandomExamCreateVo randomVo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        Exam exam = examService.randomExamCreate(randomVo, userId);
        return new ResultVO<>(0, "随机组卷成功", exam);
    }

    @GetMapping("/ranking/{examId}")
    @ApiOperation("获取指定考试的班级排名（教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<List<ClassRankingVo>> getClassRanking(@PathVariable String examId, HttpServletRequest request) {
        // A-06 修复：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        List<ClassRankingVo> ranking = examService.getClassRanking(examId, userId, roleId);
        return new ResultVO<>(0, "获取班级排名成功", ranking);
    }

    @PostMapping("/essay-grade")
    @ApiOperation("教师批改主观题评分（教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Void> gradeEssay(@RequestBody EssayGradeVo gradeVo, HttpServletRequest request) {
        // A-05 修复：传入 userId/roleId 进行所有权校验
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.gradeEssay(gradeVo.getRecordId(), gradeVo.getQuestionId(), gradeVo.getScore(), userId, roleId);
        return new ResultVO<>(0, "主观题评分成功", null);
    }

    @PostMapping("/import-questions")
    @ApiOperation("批量导入题目（Excel上传，教师/管理员）")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Integer> importQuestions(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        String userId = (String) request.getAttribute("user_id");
        int count = examService.batchImportQuestions(file.getBytes(), userId);
        return new ResultVO<>(0, "批量导入完成，成功" + count + "条", count);
    }

    /**
     * S-01 修复：教师手动设置考试状态（结束/暂停/恢复）
     */
    @PostMapping("/status/{examId}")
    @ApiOperation("设置考试手动状态（教师/管理员）：0=恢复自动, 1=手动结束, 2=手动暂停")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Void> setExamManualStatus(@PathVariable String examId, @RequestParam Integer manualStatus, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.setExamManualStatus(examId, manualStatus, userId, roleId);
        return new ResultVO<>(0, "考试状态设置成功", null);
    }

    /**
     * I-12 修复：学生删除自己的考试记录（仅考试结束后）
     * 学生只能删除自己的记录；ADMIN 可删除任何记录
     */
    @DeleteMapping("/record/{recordId}")
    @ApiOperation("删除考试记录（学生只能删除自己的记录，考试结束后才可删除）")
    ResultVO<Void> deleteExamRecord(@PathVariable String recordId, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.deleteExamRecord(recordId, userId, roleId);
        return new ResultVO<>(0, "考试记录删除成功", null);
    }

    /**
     * I-12 修复：教师/管理员重置考试记录（删除记录，允许学生重考）
     * 仅考试创建者或 ADMIN 可调用
     */
    @PostMapping("/record/reset/{recordId}")
    @ApiOperation("重置考试记录（教师/管理员）：删除记录后学生可重新参加考试")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    ResultVO<Void> resetExamRecord(@PathVariable String recordId, HttpServletRequest request) {
        String userId = (String) request.getAttribute("user_id");
        Integer roleId = (Integer) request.getAttribute("role_id");
        examService.resetExamRecord(recordId, userId, roleId);
        return new ResultVO<>(0, "考试记录重置成功", null);
    }
}
