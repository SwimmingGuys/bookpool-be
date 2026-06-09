package kr.co.bookpool.app.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);
}