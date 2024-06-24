package study.querydsl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuerydslApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuerydslApplication.class, args);
	}

	// JPAQueryFactory Bean 등록
	// @Bean
	// public JPAQueryFactory jpaQueryFactory(EntityManager em) {
	// 	return new JPAQueryFactory(em);
	// }

}
