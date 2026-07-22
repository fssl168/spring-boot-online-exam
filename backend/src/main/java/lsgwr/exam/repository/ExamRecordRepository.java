/***********************************************************
 * @Description : 
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-14 08:23
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.repository;

import lsgwr.exam.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, String> {
    /**
     * 获取指定用户参加过的所有考试
     *
     * @param userId 用户id
     * @return 用户参加过的所有考试
     */
    List<ExamRecord> findByExamJoinerIdOrderByExamJoinDateDesc(String userId);

    /**
     * 获取指定考试的所有考试记录
     *
     * @param examId 考试id
     * @return 该考试的所有记录
     */
    List<ExamRecord> findByExamId(String examId);

    /**
     * P0-2 修复 [D-01, I-02]：查询指定用户对指定考试的记录，用于重复提交校验
     *
     * @param examId     考试id
     * @param joinerId   参与者id
     * @return 已存在的考试记录列表（按业务约束最多 1 条）
     */
    List<ExamRecord> findByExamIdAndExamJoinerId(String examId, String joinerId);
}
