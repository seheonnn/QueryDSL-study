package study.querydsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import study.querydsl.entity.Member;

// 인터페이스 상속 여러개 가능
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

	// JpaRepository 가 메서드 명으로 쿼리 만듦 -> select m from Member m where m.username = ?
	List<Member> findByUsername(String username);
}
