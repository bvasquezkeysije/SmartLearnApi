package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByStatusOrderByNameAsc(Integer status);

    @Query(
            """
            select u
            from User u
            where u.status = :status
              and u.id <> :excludeUserId
              and (
                  lower(coalesce(u.name, '')) like lower(concat('%', :query, '%'))
                  or lower(coalesce(u.username, '')) like lower(concat('%', :query, '%'))
                  or lower(coalesce(u.email, '')) like lower(concat('%', :query, '%'))
              )
            order by u.name asc, u.id asc
            """)
    List<User> searchActiveRecipients(
            @Param("status") Integer status,
            @Param("excludeUserId") Long excludeUserId,
            @Param("query") String query,
            Pageable pageable);
}
