package com.blueocean.library.repository;

import com.blueocean.library.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Loan e where e.id=:id")
  java.util.Optional<Loan> lockById(@Param("id") Long id);

  @EntityGraph(attributePaths = {"reader", "createdBy"})
  org.springframework.data.domain.Page<Loan> findAll(org.springframework.data.domain.Pageable page);

  @EntityGraph(attributePaths = {"reader", "createdBy"})
  org.springframework.data.domain.Page<Loan> findByReaderAccountUsername(
      String username, org.springframework.data.domain.Pageable page);

  /**
   * Tìm phiếu mượn cho thủ thư. {@code q} (đã chuyển chữ thường, dạng %...%) khớp mã/họ tên độc
   * giả hoặc mã bản sách; {@code loanId} khớp đúng số phiếu. Chuỗi rỗng nghĩa là bỏ qua điều kiện.
   */
  @EntityGraph(attributePaths = {"reader", "createdBy"})
  @Query(
      """
      select l from Loan l
      where (:status = '' or l.status = :status)
        and (:overdue = false or exists (
              select 1 from LoanDetail d
              where d.loan = l and d.status = 'BORROWED' and d.dueDate < :today))
        and (:q = ''
             or l.id = :loanId
             or lower(l.reader.code) like :q escape '!'
             or lower(l.reader.fullName) like :q escape '!'
             or exists (
                  select 1 from LoanDetail d2
                  where d2.loan = l and lower(d2.bookCopy.code) like :q escape '!'))
      """)
  org.springframework.data.domain.Page<Loan> search(
      @Param("q") String q,
      @Param("loanId") Long loanId,
      @Param("status") String status,
      @Param("overdue") boolean overdue,
      @Param("today") java.time.LocalDate today,
      org.springframework.data.domain.Pageable page);
}
