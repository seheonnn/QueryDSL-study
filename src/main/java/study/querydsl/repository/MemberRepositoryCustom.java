package study.querydsl.repository;

import java.util.List;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

// 사용자 정의 Repository
public interface MemberRepositoryCustom {

	List<MemberTeamDto> search(MemberSearchCondition condition);
}
