package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em; // 이미 동시성 문제, 멀티 쓰레드 환경 고려하여 설계되어 있음

	JPAQueryFactory queryFactory;
	@PersistenceUnit // 엔티티 매니저를 만드는 팩토리
	EntityManagerFactory emf;

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

	/**
	 * 팀 A에 소속된 모든 회원
	 */
	@Test
	public void join() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	/**
	 * 세타 조인
	 * 회원의 이름이 팀 이름과 같은 회원 조회 (전혀 상관없는 조인)
	 */
	@Test
	public void theta_join() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
	 */
	@Test
	public void join_on_filtering() {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team)
			.on(team.name.eq("teamA")) // leftJoin, rightJoin 의 경우 where 로는 불가. on 만 가능
			.fetch();

		// 그냥 join (innerJoin)인 경우 on 절은 where 절에서 필터링 하는 것과 같은 효과
		// List<Tuple> result = queryFactory
		// 	.select(member, team)
		// 	.from(member)
		// 	.join(member.team, team)
		// 	// .on(team.name.eq("teamA"))
		// 	.where(team.name.eq("teamA"))
		// 	.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple=" + tuple);
		}
	}

	/**
	 * 연관관계 없는 엔티티 외부 조인
	 * 회원의 이름이 팀 이름과 같은 대상 외부 조인
	 */
	@Test
	public void join_on_no_relation() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.where(member.username.eq(team.name))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple=" + tuple);
		}
		// 주의! 문법을 잘 봐야 한다. **leftJoin()** 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
		// 일반적인 leftJoin: `leftJoin(member.team, team)`
		// on 절을 이용한 외부조인: `from(member).leftJoin(team).on(xxx)`
	}

	// 페치조인 -> 조인은 쿼리를 두 번 날림. 이를 한 번만 날리도록
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();

		// Lazy이므로 Member만 조회
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 현재 로딩된 엔티티인지 확인
		// 페치 조인 미적용 + Lazy 이므로 현재 로딩되어 있으면 안 됨
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	public void fetchJoinUse() {
		em.flush();
		em.clear();

		// Lazy이므로 Member만 조회
		Member findMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin() // 문법은 동일 fetchJoin()만 붙이기
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		// Team까지 불러옴. but, 쿼리 하나로
		assertThat(loaded).as("페치 조인 적용").isTrue();
	}

	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	public void subQuery() {

		QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 alias 구분을 위함

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
				select(memberSub.age.max())
					.from(memberSub)
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(40);
	}

	/**
	 * 나이가 평균 이상인 회원 조회
	 */
	@Test
	public void subQueryGoe() {

		QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 alias 구분을 위함

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
				select(memberSub.age.avg())
					.from(memberSub)
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(30, 40);
	}

	/**
	 * 나이가 평균 이상인 회원 조회
	 */
	@Test
	public void subQueryIn() {

		QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 alias 구분을 위함

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.in(
				select(memberSub.age)
					.from(memberSub)
					.where(memberSub.age.gt(10))
			))
			.fetch();

		assertThat(result).extracting("age")
			.containsExactly(20, 30, 40);
	}

	@Test
	public void selectSubQuery() {

		QMember memberSub = new QMember("memberSub");

		List<Tuple> result = queryFactory
			.select(member.username,
				select(memberSub.age.avg())
					.from(member))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple= " + tuple);
		}
	}

	// 서브쿼리 한계
	// JPA의 서브쿼리는 from 절에서의 서브쿼리는 지원하지 않음 -> QueryDSL도 지원하지 않음
	// QueryDSL은 JPQL의 빌더 역할이라고 생각하면 됨 -> JPA에서 안 되면 QueryDSL도 안 됨

	// 해결
	// 방법 1. 서브쿼리는 보통 join으로 변경 (가능할 수도, 불가능할 수도) join으로 바꾸는 것이 효율상 더 나을 수도
	// 방법 2. 쿼리를 두 번으로 나눠서 애플리케이션에서 처리 -> 성능상 별로
	// 방법 3. native query 사용

	@Test
	public void basicCase() {
		List<String> result = queryFactory
			.select(member.age
				.when(10).then("열살")
				.when(20).then("스무살")
				.otherwise("기타"))
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}

	@Test
	public void complexCase() {
		List<String> result = queryFactory
			.select(new CaseBuilder()
				.when(member.age.between(0, 20)).then("0~20살")
				.when(member.age.between(21, 30)).then("21~30살")
				.otherwise("기타"))
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}

	@Test
	public void constant() {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple= " + tuple);
		}
	}

	@Test
	public void concat() {

		// {username}_{age} 형태로 만들기    *.stringValue()는 Enum 타입 처리에 자주 활용*
		List<String> result = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
			.where(member.username.eq("member1"))
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}

	// 프로젝션
	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}

	@Test
	public void tupleProjection() {

		// Tuple 은 QueryDSL 의 자료형이므로 repository 단에서만 사용하자!
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);

			System.out.println("username= " + username);
			System.out.println("age= " + age);
		}
	}

	@Test
	public void findDto() {

		// JPQL 에서 DTO 조회하기 - new 이용해야, 생성자 방식만 지원
		List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) "
			+ "from Member m", MemberDto.class).getResultList();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto= " + memberDto);
		}
	}

	/**
	 * QueryDSL 로 DTO 추출하기
	 * 1. 프로퍼티 접근
	 * 2. 필드 직접 접근
	 * 3. 생성자 사용
	 */
	@Test
	public void findDtoBySeㅅter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		// 기본 생성자로 MemberDto 를 만들어서 setter 함 -> @NoArgsConstructor, getter, setter 필요
		for (MemberDto memberDto : result) {
			System.out.println("memberDto= " + memberDto);
		}
	}

	@Test
	public void findDtoByField() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		// getter, setter 없어도 됨 -> 필드에 바로 값 추가
		for (MemberDto memberDto : result) {
			System.out.println("memberDto= " + memberDto);
		}
	}

	@Test
	public void findDtoByConstructor() {
		List<UserDto> result = queryFactory
			.select(Projections.constructor(UserDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		// MemberDto의 생성자 사용 -> 각 필드의 타입이 맞아야. 이름 달라도 됨. 타입에 따라 들어가기 때문
		for (UserDto memberDto : result) {
			System.out.println("memberDto= " + memberDto);
		}
	}

	@Test
	public void findUserDtoByField() {
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
				member.username.as("name"),
				member.age))
			.from(member)
			.fetch();

		// UserDto의 필드명과 member 엔티티의 필드명이 일치하지 않아 결과값 없음 -> as 활용
		for (UserDto userDto : result) {
			System.out.println("userDto" + userDto);
		}
	}

	@Test
	public void findUserDtoByFieldSubQuery() {
		QMember memberSub = new QMember("memberSub");
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
				// member.username.as("name"),
				ExpressionUtils.as(member.username, "name"),

				// 서브쿼리는 .as 사용 불가 -> ExpressionUtils.as(서브쿼리, alias)로 해결
				ExpressionUtils.as(JPAExpressions
					.select(memberSub.age.max())
					.from(memberSub), "age")
			))
			.from(member)
			.fetch();

		// UserDto의 필드명과 member 엔티티의 필드명이 일치하지 않아 결과값 없음 -> as 활용
		for (UserDto userDto : result) {
			System.out.println("userDto" + userDto);
		}
	}

	@Test
	public void findDtoByQueryProjection() {
		// cmd + p 가능
		// 단점. Dto 가 QueryDsl 의존성을 가지게 됨
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto= " + memberDto);
		}
	}

	// 동적 쿼리
	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {

		BooleanBuilder builder = new BooleanBuilder();
		// BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); // username 이 필수인 경우
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}

		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}
		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}

	@Test
	public void dynamicQuery_WhereParam() {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember2(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
			.selectFrom(member)
			.where(usernameEq(usernameCond), ageEq(ageCond))
			// .where(allEq(usernameCond, ageCond)) // where 조건  조립 가능
			.fetch();
	}

	private BooleanExpression usernameEq(String usernameCond) {
		return usernameCond != null ? member.username.eq(usernameCond) : null;
	}

	private BooleanExpression ageEq(Integer ageCond) {
		return ageCond != null ? member.age.eq(ageCond) : null;
	}

	// 광고 상태 isValid + 날짜가 IN = isServiceable
	private BooleanExpression allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}

	// 벌크 연산, 배치 쿼리
	@Test
	public void bulkUpdate() {

		// 벌크 연산은 영속성 컨텍스트를 무시하고 DB에 바로 날림 -> 영속성 컨텍스트와 DB의 상태가 서로 다름
		// 실행 전
		// member1 = 10 -> DB member1
		// member2 = 20 -> DB member2
		// member3 = 30 -> DB member3
		// member4 = 40 -> DB member4

		// count : 영향을 받은 row 수
		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();

		// 벌크 연산 후 영속성 컨텍스트를 초기화 하는 것이 안전하다 !
		em.flush();
		em.clear();

		// 실행 후 (DB 는 바뀌었지만 영속성 컨텍스트는 그대로임)
		// member1 = 10 -> 1 DB 비회원
		// member2 = 20 -> 2 DB 비회원
		// member3 = 30 -> 3 DB member3
		// member4 = 40 -> 4 DB member4

		// DB 에서 검색한 내용보다 영속성 컨텍스트의 내용이 우선권
		// 영속성 컨텍스트에 이미 있다면 DB 에서 검색한 내용 버림
		List<Member> result = queryFactory
			.selectFrom(member)
			.fetch();
		// 실행 전과 똑같은 결과
		for (Member member1 : result) {
			System.out.println("member1= " + member1);
		}
	}

	@Test
	public void bulkAdd() {
		long count = queryFactory
			.update(member)
			.set(member.age, member.age.multiply(2))
			.execute();
	}

	@Test
	public void bulkDelete() {
		long count = queryFactory
			.delete(member)
			.where(member.age.gt(18))
			.execute();
	}

	@Test
	public void sqlFunction() {
		// H2Dialect 에 등록된 함수여야 함. 아니면 H2Dialect 를 상속받아 구현하고 yml 설정해야.
		List<String> result = queryFactory
			.select(Expressions.stringTemplate(
				"function('replace', {0}, {1}, {2})",
				member.username, "member", "M")
			)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}

	@Test
	public void sqlFunction2() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			// .where(member.username.eq(
			// 	Expressions.stringTemplate("function('lower', {0})", member.username))
			// )
			.where(member.username.eq(member.username.lower()))
			.fetch();

		for (String s : result) {
			System.out.println("s= " + s);
		}
	}
}
