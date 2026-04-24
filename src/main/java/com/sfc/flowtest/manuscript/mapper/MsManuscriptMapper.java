package com.sfc.flowtest.manuscript.mapper;

import com.sfc.flowtest.manuscript.entity.MsManuscript;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 稿件数据访问层。
 */
@Mapper
public interface MsManuscriptMapper {

    /**
     * 新增稿件。
     *
     * @param manuscript 稿件实体
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO ms_manuscript(title, body, status, reject_review_level)
            VALUES(#{title}, #{body}, #{status}, #{rejectReviewLevel})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MsManuscript manuscript);

    /**
     * 按主键查询稿件。
     *
     * @param id 稿件ID
     * @return 稿件实体
     */
    @Select("""
            SELECT id, title, body, status, reject_review_level, created_at, updated_at
            FROM ms_manuscript
            WHERE id = #{id}
            """)
    MsManuscript selectById(@Param("id") Long id);

    /**
     * 查询稿件列表（支持状态过滤和可选分页）。
     *
     * @param statuses 多状态过滤
     * @param keyword  标题/正文检索关键词
     * @param limit    分页大小
     * @param offset   分页偏移
     * @return 稿件列表
     */
    @Select("""
            <script>
            SELECT id, title, body, status, reject_review_level, created_at, updated_at
            FROM ms_manuscript
            <where>
              <if test="statuses != null and statuses.size() > 0">
                status IN
                <foreach collection="statuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%') OR body LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY updated_at DESC, id DESC
            <if test="limit != null and offset != null">
              LIMIT #{limit} OFFSET #{offset}
            </if>
            </script>
            """)
    List<MsManuscript> selectList(@Param("statuses") List<String> statuses,
                                  @Param("keyword") String keyword,
                                  @Param("limit") Integer limit,
                                  @Param("offset") Integer offset);

    /**
     * 统计与列表查询相同筛选条件下的稿件条数。
     *
     * @param statuses 多状态过滤
     * @param keyword  标题/正文检索关键词
     * @return 条数
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM ms_manuscript
            <where>
              <if test="statuses != null and statuses.size() > 0">
                status IN
                <foreach collection="statuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%') OR body LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            </script>
            """)
    long countList(@Param("statuses") List<String> statuses, @Param("keyword") String keyword);

    /**
     * 更新稿件标题和正文。
     *
     * @param id    稿件ID
     * @param title 标题
     * @param body  正文
     * @return 影响行数
     */
    @Update("""
            UPDATE ms_manuscript
            SET title = #{title}, body = #{body}
            WHERE id = #{id}
            """)
    int updateDraftContent(@Param("id") Long id, @Param("title") String title, @Param("body") String body);

    /**
     * 更新稿件状态和退回关卡。
     *
     * @param id                稿件ID
     * @param status            目标状态
     * @param rejectReviewLevel 退回关卡
     * @return 影响行数
     */
    @Update("""
            UPDATE ms_manuscript
            SET status = #{status}, reject_review_level = #{rejectReviewLevel}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("rejectReviewLevel") Integer rejectReviewLevel);
}
