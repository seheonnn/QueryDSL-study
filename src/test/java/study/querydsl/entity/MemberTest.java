package study.querydsl.entity;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
	// @Commit // 다른 테스트와 꼬일 수 있기 때문에 주석 처리
class MemberTest {

	@Autowired
	EntityManager em;

	@Test
	public void testEntity() {
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

		// 초기화
		em.flush(); // 영속성 컨텍스트의 object 날림
		em.clear(); // 영속성 컨텍스트 초기화 (캐시 등)

		// 확인
		List<Member> members = em.createQuery("select m from Member m", Member.class)
			.getResultList();

		for (Member member : members) {
			System.out.println("member = " + member);
			System.out.println("-> member.team = " + member.getTeam());
		}
	}

}
