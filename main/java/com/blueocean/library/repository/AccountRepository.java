package com.blueocean.library.repository;

import com.blueocean.library.entity.*;
import org.springframework.data.jpa.repository.*;

public interface AccountRepository extends JpaRepository<Account, Long> {
  java.util.Optional<Account> findByUsername(String username);

  boolean existsByUsername(String username);

  long countByRoleAndEnabledTrue(String role);
}
