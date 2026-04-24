package com.sfc.flowtest.review.mapper;

import com.sfc.flowtest.review.entity.MsReviewRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 审核流水数据访问层。
 */
@Mapper
public interface MsReviewRecordMapper {

    /**
     * 新增一条审核流水记录。
     *
     * @param record 流水实体
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO ms_review_record(
              manuscript_id, action, from_status, to_status, opinion, reject_level, operator_id, operator_name
            )
            VALUES(
              #{manuscriptId}, #{action}, #{fromStatus}, #{toStatus}, #{opinion}, #{rejectLevel}, #{operatorId}, #{operatorName}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MsReviewRecord record);

    /**
     * 按稿件ID查询审核流水（时间正序）。
     *
     * @param manuscriptId 稿件ID
     * @return 流水列表
     */
    @Select("""
            SELECT id, manuscript_id, action, from_status, to_status, opinion, reject_level, operator_id, operator_name, created_at
            FROM ms_review_record
            WHERE manuscript_id = #{manuscriptId}
            ORDER BY created_at ASC, id ASC
            """)
    List<MsReviewRecord> selectByManuscriptId(@Param("manuscriptId") Long manuscriptId);
}
