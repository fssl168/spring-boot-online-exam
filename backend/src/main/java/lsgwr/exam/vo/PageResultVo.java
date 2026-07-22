/***********************************************************
 * @Description : 分页结果通用VO，封装当前页数据 + 总数 + 分页信息
 *                适配前端 BootstrapTable server-side 分页所需的 { total, rows } 结构
 * @author      : Batch 7.2.2/7.2.3
 * @date        : 2026-07-23
 ***********************************************************/
package lsgwr.exam.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageResultVo<T> {
    /**
     * 当前页数据
     */
    private List<T> rows;
    /**
     * 总记录数
     */
    private long total;
    /**
     * 当前页码（0-based）
     */
    private int page;
    /**
     * 每页大小
     */
    private int size;

    public PageResultVo(List<T> rows, long total, int page, int size) {
        this.rows = rows;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
