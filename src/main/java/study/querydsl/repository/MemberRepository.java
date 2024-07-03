package study.querydsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import study.querydsl.entity.Member;

// 인터페이스 상속 여러개 가능
// QuerydslPredicateExecutor<Member> 는 실무에서 거의 사용 불가
// 한계) 묵시적 조인은 가능하지만 left join 은 불가
// 클라이언트 코드가 QueryDsl 에 의존. 사용시 QMember 필요
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom,
	QuerydslPredicateExecutor<Member> {

	// JpaRepository 가 메서드 명으로 쿼리 만듦 -> select m from Member m where m.username = ?
	List<Member> findByUsername(String username);
}
