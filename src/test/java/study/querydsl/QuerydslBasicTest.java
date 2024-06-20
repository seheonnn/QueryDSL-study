package study.querydsl;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em; // 이미 동시성 문제, 멀티 쓰레드 환경 고려하여 설계되어 있음

	JPAQueryFactory queryFactory;

	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);

		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);

		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	public void startJPQL() {
		// member1을 찾아라
		String qlString =
			"select m from Member m " +
				"where m.username = :username";
		Member findMember = em.createQuery(qlString, Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {
		// 방법 1) QMember m = QMember.member;

		// 방법 2) QMember m = new QMember("m"); // 주로 같은 테이블을 조인해야 하는 경우에만 사용
		// Member findMember = queryFactory
		// 	.select(m)
		// 	.from(m)
		// 	.where(m.username.eq("member1")) // 파라미터 바인딩
		// 	.fetchOne();

		// 방법 3) option + Enter 로 static import (QMember.member -> member) (권장)
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1")) // 파라미터 바인딩
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
				.and(member.age.eq(10)))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				// and인 경우
				member.username.eq("member1"),
				member.age.eq(10),
				null // null인 경우는 무시하기 때문에 동적 쿼리에 사용
			)
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void resultFetch() {
		// List<Member> fetch = queryFactory
		// 	.selectFrom(member)
		// 	.fetch();
		//
		// Member fetchOne = queryFactory
		// 	.selectFrom(member)
		// 	.fetchOne();
		//
		// Member fetchFirst = queryFactory
		// 	.selectFrom(member)
		// 	.fetchFirst();// == .limit(1).fetchOne();

		// // ======
		// QueryResults<Member> results = queryFactory
		// 	.selectFrom(member)
		// 	.fetchResults();
		//
		// results.getTotal();
		//
		// // Query 두 번 실행, TotalCount 도 가져옴
		// // -> 페이징 처리를 위힘
		// List<Member> content = results.getResults();

		// =====
		long total = queryFactory
			.selectFrom(member)
			.fetchCount();
	}

	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단 2에서 최원 이름이 없으면 마지막에 출력 (nulls last)
	 * **/
	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast()) // or nullsFirst
			.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	public void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void paging2() {

		// where 나 join 을 추가하는 경우 카운트 쿼리와 컨텐츠 쿼리에 모두 붙기 때문에 성능상 별로. 따로따로 작성하는 것이 좋다 !
		QueryResults<Member> queryResults = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults(); // 쿼리 두 번

		// 총 4개 2개씩 2페이지
		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2); // 한 페이지당 개수
	}

	@Test
	void aggregation() {

		List<Tuple> result = queryFactory
			.select(
				member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.min()
			)
			.from(member)
			.fetch();

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 *
	 */
	@Test
	public void group() {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			// .having() // having 도 가능
			.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
	}
}
