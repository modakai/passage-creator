package com.sakura.passage_creator.rednote.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小红书爆款笔记 Mapper。
 */
@Mapper
public interface RednoteNoteMapper extends BaseMapper<RednoteNote> {
}
