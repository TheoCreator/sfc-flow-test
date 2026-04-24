package com.sfc.flowtest.manuscript.mapper;

import com.sfc.flowtest.manuscript.entity.MsManuscriptAsset;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 稿件素材数据访问。
 */
@Mapper
public interface MsManuscriptAssetMapper {

    @Insert("""
            INSERT INTO ms_manuscript_asset(manuscript_id, storage_object_key, original_filename, content_type, size_bytes)
            VALUES(#{manuscriptId}, #{storageObjectKey}, #{originalFilename}, #{contentType}, #{sizeBytes})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MsManuscriptAsset row);

    @Select("""
            SELECT id, manuscript_id, storage_object_key, original_filename, content_type, size_bytes, created_at
            FROM ms_manuscript_asset
            WHERE id = #{id}
            """)
    MsManuscriptAsset selectById(@Param("id") Long id);
}
