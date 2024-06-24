package study.querydsl.repository;

import static study.querydsl.entity.QMember.*;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import study.querydsl.entity.Member;

@Repository
public class MemberJpaRepository {

	private final EntityManager em;
	private final JPAQueryFactory queryFactory; // JPAQueryFactory 는 그냥 Bean으로 바로 등록해도 됨

	public MemberJpaRepository(EntityManager em) {
		// public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
		this.em = em;
		this.queryFactory = new JPAQueryFactory(em); // JPAQueryFactory Bean 등록 안 하는 경우
		// this.queryFactory = queryFactory; // JPAQueryFactory Bean 등록 하는 경우 -> 의존성 주입 받아야 하므로 테스트 코드에서 번거로울 수도
	}

	public void save(Member member) {
		em.persist(member);
	}

	public Optional<Member> findById(Long id) {
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}

	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class).getResultList();
	}

	public List<Member> findAll_Querydsl() {
		return queryFactory
			.selectFrom(member)
			.fetch();
	}

	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", username)
			.getResultList();
	}

	public List<Member> findByUsername_Querydsl(String username) {
		return queryFactory
			.selectFrom(member)
			.where(member.username.eq(username))
			.fetch();
	}
}
