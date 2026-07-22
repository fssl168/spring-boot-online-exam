/***********************************************************
 * @Description : 
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-14 08:25
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.repository;

import lsgwr.exam.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {
    List<Question> findByQuestionTypeId(Integer id);
    /**
     * P-04 修复：按题型分页查询，避免全量返回导致 OOM
     */
    List<Question> findByQuestionTypeId(Integer id, Pageable pageable);
    @Query("select q from Question q order by q.updateTime desc")
    List<Question> findAll();

    /**
     * Batch 7.2.2：分页查询未软删除的题目（visible 为 NULL 视为可见，1=可见，0=已删除）
     * 排序由 Pageable 参数控制，便于前端按字段排序
     */
    @Query("select q from Question q where q.questionVisible is null or q.questionVisible = 1")
    Page<Question> findVisiblePage(Pageable pageable);

    /**
     * B-09 修复：查询所有未软删除的题目（SQL 过滤，与 findVisiblePage 策略一致）
     */
    @Query("select q from Question q where q.questionVisible is null or q.questionVisible = 1 order by q.updateTime desc")
    List<Question> findVisibleAll();
}
