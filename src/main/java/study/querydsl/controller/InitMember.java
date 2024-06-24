package study.querydsl.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

// local 로 실행하면 실행
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

	private final InitMemberService initMemberService;

	// @PostConstruct, @Transactional 분리해야. 동시에 안 됨
	@PostConstruct
	public void init() {
		initMemberService.init();
	}

	@Component
	static class InitMemberService {
		@PersistenceContext
		private EntityManager em;

		@Transactional
		public void init() {
			Team teamA = new Team("teamA");
			Team teamB = new Team("teamB");
			em.persist(teamA);
			em.persist(teamB);

			for (int i = 0; i < 100; i++) {
				Team selectedTeam = i % 2 == 0 ? teamA : teamB;
				em.persist(new Member("member" + i, i, selectedTeam));
			}
		}
	}
}
