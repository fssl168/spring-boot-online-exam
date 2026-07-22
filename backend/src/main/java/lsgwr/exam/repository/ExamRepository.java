/***********************************************************
 * @Description : 
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-14 08:22
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.repository;

import lsgwr.exam.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, String> {
    @Query("select e from Exam e order by e.updateTime desc")
    List<Exam> findAll();

    /**
     * Batch 7.2.3：分页查询未软删除的考试（visible 为 NULL 视为可见，1=可见，0=已删除）
     * 排序由 Pageable 参数控制，便于前端按字段排序
     */
    @Query("select e from Exam e where e.examVisible is null or e.examVisible = 1")
    Page<Exam> findVisiblePage(Pageable pageable);

    /**
     * B-09 修复：查询所有未软删除的考试（SQL 过滤，与 findVisiblePage 策略一致）
     */
    @Query("select e from Exam e where e.examVisible is null or e.examVisible = 1 order by e.updateTime desc")
    List<Exam> findVisibleAll();
}
