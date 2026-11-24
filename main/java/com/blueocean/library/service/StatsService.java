package com.blueocean.library.service;

import com.blueocean.library.entity.BookCopy;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản trị viên: số liệu tổng hợp toàn hệ thống. */
@Service
@Transactional(readOnly = true)
public class StatsService {
  private final EntityManager em;
  private final Clock clock;

  public StatsService(EntityManager em, Clock clock) {
    this.em = em;
    this.clock = clock;
  }

  public Map<String, Object> overview() {
    LocalDate today = LocalDate.now(clock);
    Map<String, Object> m = new LinkedHashMap<>();

    Map<String, Long> roles = new LinkedHashMap<>();
    for (String r : List.of("ADMIN", "LIBRARIAN", "READER")) roles.put(r, 0L);
    for (Object[] row :
        em.createQuery("select a.role, count(a) from Account a group by a.role", Object[].class)
            .getResultList()) roles.put((String) row[0], (Long) row[1]);
    m.put("accountsByRole", roles);
    m.put("accounts", roles.values().stream().mapToLong(Long::longValue).sum());
    m.put("lockedAccounts", count("select count(a) from Account a where a.enabled = false"));

    m.put("readers", count("select count(r) from Reader r"));
    m.put("activeReaders", count("select count(r) from Reader r where r.active = true"));
    m.put("titles", count("select count(b) from BookTitle b"));

    Map<String, Long> copies = new LinkedHashMap<>();
    for (BookCopy.CopyStatus s : BookCopy.CopyStatus.values()) copies.put(s.name(), 0L);
    for (Object[] row :
        em.createQuery("select c.status, count(c) from BookCopy c group by c.status", Object[].class)
            .getResultList()) copies.put(((BookCopy.CopyStatus) row[0]).name(), (Long) row[1]);
    m.put("copiesByStatus", copies);
    m.put("copies", copies.values().stream().mapToLong(Long::longValue).sum());

    m.put("loans", count("select count(l) from Loan l"));
    m.put("openLoans", count("select count(l) from Loan l where l.status = 'OPEN'"));
    m.put("borrowing", count("select count(d) from LoanDetail d where d.status = 'BORROWED'"));
    m.put(
        "overdue",
        em.createQuery(
                "select count(d) from LoanDetail d where d.status = 'BORROWED' and d.dueDate < :t",
                Long.class)
            .setParameter("t", today)
            .getSingleResult());

    m.put(
        "finesCollected",
        sum("select coalesce(sum(d.fineTotal), 0) from LoanDetail d where d.finePaidAt is not null"));
    m.put(
        "finesUnpaid",
        sum(
            "select coalesce(sum(d.fineTotal), 0) from LoanDetail d"
                + " where d.status <> 'BORROWED' and d.fineTotal > 0 and d.finePaidAt is null"));

    m.put(
        "topBooks",
        em.createQuery(
                "select t.code, t.title, count(d) from LoanDetail d join d.bookCopy c join"
                    + " c.bookTitle t group by t.id, t.code, t.title order by count(d) desc, t.code",
                Object[].class)
            .setMaxResults(5)
            .getResultList()
            .stream()
            .map(r -> Map.of("code", r[0], "title", r[1], "count", r[2]))
            .toList());

    // Số phiếu mượn theo tháng, 6 tháng gần nhất (gom nhóm trong Java để chạy được cả MySQL lẫn H2)
    YearMonth now = YearMonth.from(today);
    Map<String, Long> monthly = new LinkedHashMap<>();
    for (int i = 5; i >= 0; i--) monthly.put(now.minusMonths(i).toString(), 0L);
    em.createQuery("select l.borrowedAt from Loan l where l.borrowedAt >= :from", LocalDateTime.class)
        .setParameter("from", now.minusMonths(5).atDay(1).atStartOfDay())
        .getResultList()
        .forEach(t -> monthly.merge(YearMonth.from(t).toString(), 1L, Long::sum));
    m.put("loansByMonth", monthly);
    return m;
  }

  private long count(String jpql) {
    return em.createQuery(jpql, Long.class).getSingleResult();
  }

  private BigDecimal sum(String jpql) {
    return em.createQuery(jpql, BigDecimal.class).getSingleResult();
  }
}
