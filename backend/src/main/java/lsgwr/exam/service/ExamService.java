/***********************************************************
 * @Description : 考试接口
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-28 08:05
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.service;

import lsgwr.exam.entity.Exam;
import lsgwr.exam.entity.ExamRecord;
import lsgwr.exam.enums.ExamStatusEnum;
import lsgwr.exam.vo.*;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;

public interface ExamService {
    /**
     * 获取所有的问题列表
     */
    List<QuestionVo> getQuestionAll();

    /**
     * Batch 7.2.2：分页查询题目列表（服务端分页）
     *
     * @param pageable 分页参数（page/size/sort）
     * @return 分页结果，包含当前页数据和总数
     */
    PageResultVo<QuestionVo> getQuestionPage(Pageable pageable);

    /**
     * 根据前端传过来的问题实体更新问题和选项
     *
     * @param questionVo 问题实体
     * @param userId     当前操作用户id（用于所有权校验）
     * @param roleId     当前操作用户角色id（ADMIN 可操作所有资源）
     */
    QuestionVo updateQuestion(QuestionVo questionVo, String userId, Integer roleId);

    /**
     * 问题创建
     *
     * @param questionCreateVo 问题创建实体类
     */
    void questionCreate(QuestionCreateVo questionCreateVo);

    /**
     * 获取问题的选项、分类和难度的下拉列表
     *
     * @return 选项、分类和难度的封装对象
     */
    QuestionSelectionVo getSelections();

    /**
     * 获取问题详情
     *
     * @param id 问题的id
     * @return 问题详情的封装VO
     */
    QuestionDetailVo getQuestionDetail(String id);

    /**
     * 获取全部考试的列表
     */
    List<ExamVo> getExamAll();

    /**
     * Batch 7.2.3：分页查询考试列表（服务端分页）
     *
     * @param pageable 分页参数（page/size/sort）
     * @return 分页结果，包含当前页数据和总数
     */
    PageResultVo<ExamVo> getExamPage(Pageable pageable);

    /**
     * 获取所有问题的下拉列表，方便前端创建考试时筛选
     *
     * @return 适配前端的问题下拉列表
     */
    ExamQuestionTypeVo getExamQuestionType();

    /**
     * 根据前端组装的参数进行考试创建
     *
     * @param examCreateVo 前端组装的考试对象
     * @param userId       用户id
     * @return 创建好的考试
     */
    Exam create(ExamCreateVo examCreateVo, String userId);

    /**
     * 获取考试卡片列表
     *
     * @return 考试卡片列表
     */
    List<ExamCardVo> getExamCardList();

    /**
     * 根据考试的id获取考试的详情
     *
     * @param id exam表的主键
     * @return 考试详情的封装的VO对象
     */
    ExamDetailVo getExamDetail(String id);

    /**
     * 根据用户提交的作答信息进行判分
     *
     * @param userId     考试人
     * @param examId     参与的考试
     * @param answersMap 作答情况
     * @param timeCost   考试耗时（秒）
     * @return 本次考试记录
     */
    ExamRecord judge(String userId, String examId, HashMap<String, List<String>> answersMap, Integer timeCost);

    /**
     * 根据用户id获取此用户的所有考试信息
     *
     * @param userId 用户id
     * @return 该用户的所有考试记录
     */
    List<ExamRecordVo> getExamRecordList(String userId);

    /**
     * 获取指定某次考试记录的详情
     *
     * @param recordId 考试记录的id
     * @return 考试详情
     */
    RecordDetailVo getRecordDetail(String recordId);


    /**
     * 更新考试
     *
     * @param examVo 获取所有考试的接口中返回的考试信息结构
     * @param userId 当前的用户（用于所有权校验）
     * @param roleId 当前用户角色id（ADMIN 可操作所有资源）
     * @return 更新后的考试详情
     */
    Exam update(ExamVo examVo, String userId, Integer roleId);

    /**
     * 根据考试id检查考试当前是否可参加
     *
     * @param examId 考试id
     * @return 考试状态枚举
     */
    ExamStatusEnum checkExamAvailable(String examId);

    /**
     * 按学生userId确定性打乱题目ID顺序，防止抄袭
     *
     * @param questionIds 题目ID数组
     * @param seed        随机种子（通常为userId）
     * @return 打乱后的题目ID数组
     */
    String[] shuffleQuestionIds(String[] questionIds, String seed);

    /**
     * 获取指定考试的所有学生考试记录（教师查看）
     *
     * @param examId 考试id
     * @param userId 当前操作用户id（用于所有权校验）
     * @param roleId 当前操作用户角色id
     * @return 所有考试记录VO列表
     */
    List<ExamRecordVo> getExamAllRecords(String examId, String userId, Integer roleId);

    /**
     * 获取指定考试的成绩统计信息
     *
     * @param examId 考试id
     * @param userId 当前操作用户id（用于所有权校验）
     * @param roleId 当前操作用户角色id
     * @return 成绩统计VO
     */
    ExamScoreStatVo getExamScoreStat(String examId, String userId, Integer roleId);

    /**
     * 软删除题目：将 questionVisible 置为 0
     *
     * @param questionId 题目id
     * @param userId     当前操作用户id（用于所有权校验）
     * @param roleId     当前操作用户角色id（ADMIN 可操作所有资源）
     */
    void deleteQuestion(String questionId, String userId, Integer roleId);

    /**
     * 软删除考试：将 examVisible 置为 0
     *
     * @param examId 考试id
     * @param userId 当前操作用户id（用于所有权校验）
     * @param roleId 当前操作用户角色id（ADMIN 可操作所有资源）
     */
    void deleteExam(String examId, String userId, Integer roleId);

    /**
     * 随机组卷：按题型数量从题库随机抽题，自动组装考试
     *
     * @param randomVo 随机组卷规则
     * @param userId   创建者id
     * @return 创建好的考试
     */
    Exam randomExamCreate(RandomExamCreateVo randomVo, String userId);

    /**
     * 班级排名：按考试分组统计学生成绩排名
     *
     * @param examId 考试id
     * @param userId 当前操作用户id（用于所有权校验）
     * @param roleId 当前操作用户角色id
     * @return 排名列表
     */
    List<ClassRankingVo> getClassRanking(String examId, String userId, Integer roleId);

    /**
     * 教师批改主观题：提交主观题评分
     *
     * @param recordId 考试记录id
     * @param questionId 题目id
     * @param score   教师评分
     * @param userId  当前操作用户id（用于所有权校验）
     * @param roleId  当前操作用户角色id
     */
    void gradeEssay(String recordId, String questionId, Integer score, String userId, Integer roleId);

    /**
     * 批量导入题目：解析 Excel 并逐行创建题目
     *
     * @param data     Excel 文件字节数组
     * @param creatorId 创建者id
     * @return 导入成功条数
     */
    int batchImportQuestions(byte[] data, String creatorId);

    /**
     * S-01 修复：设置考试手动状态（教师手动结束/恢复考试）
     *
     * @param examId       考试id
     * @param manualStatus 手动状态：0=恢复自动计算, 1=手动结束, 2=手动暂停
     * @param userId       当前操作用户id（用于所有权校验）
     * @param roleId       当前操作用户角色id
     */
    void setExamManualStatus(String examId, Integer manualStatus, String userId, Integer roleId);

    /**
     * I-12 修复：学生删除自己的考试记录（仅当考试已结束时允许）
     *
     * @param recordId 考试记录id
     * @param userId   当前操作用户id（必须为记录所有人）
     * @param roleId   当前操作用户角色id
     */
    void deleteExamRecord(String recordId, String userId, Integer roleId);

    /**
     * I-12 修复：教师/管理员重置某条考试记录（删除后允许学生重考）
     * 仅考试创建者或管理员可调用；删除后学生可重新参加考试
     *
     * @param recordId 考试记录id
     * @param userId   当前操作用户id（必须为考试创建者）
     * @param roleId   当前操作用户角色id
     */
    void resetExamRecord(String recordId, String userId, Integer roleId);
}
