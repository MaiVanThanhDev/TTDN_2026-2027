package com.blueocean.library.config;

import com.blueocean.library.entity.*;
import com.blueocean.library.repository.*;
import com.blueocean.library.service.FineCalculator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

/** Dữ liệu mẫu cho demo: chỉ nạp khi CSDL chưa có thể loại nào. */
@Configuration
@Profile("demo")
public class SampleData {
  @Bean
  @Order(2)
  CommandLineRunner sample(
      TransactionTemplate tx,
      AccountRepository accounts,
      ReaderRepository readers,
      CategoryRepository categories,
      PublisherRepository publishers,
      AuthorRepository authors,
      BookTitleRepository titles,
      BookCopyRepository copies,
      LoanRepository loans,
      FineCalculator fines,
      Clock clock,
      @Value("${library.loan-days}") int loanDays,
      @Value("${library.daily-fine}") BigDecimal rate) {
    return args ->
        tx.executeWithoutResult(
            s -> {
              if (categories.count() > 0) return;

              // ---- Thể loại, NXB, tác giả ----
              Map<String, Category> cat = new LinkedHashMap<>();
              String[][] cats = {
                {"CNTT", "Công nghệ thông tin"},
                {"VH", "Văn học"},
                {"KT", "Kinh tế"},
                {"KN", "Kỹ năng sống"},
                {"LS", "Lịch sử"},
                {"TN", "Thiếu nhi"}
              };
              for (String[] c : cats) {
                Category x = new Category();
                x.code = c[0];
                x.name = c[1];
                cat.put(c[0], categories.save(x));
              }
              Map<String, Publisher> pub = new LinkedHashMap<>();
              for (String n :
                  new String[] {
                    "NXB Trẻ", "NXB Kim Đồng", "NXB Lao Động", "NXB Thế Giới", "NXB Tổng hợp TP.HCM"
                  }) {
                Publisher x = new Publisher();
                x.name = n;
                pub.put(n, publishers.save(x));
              }
              Map<String, Author> au = new LinkedHashMap<>();
              for (String n :
                  new String[] {
                    "Nguyễn Nhật Ánh", "Tô Hoài", "Nam Cao", "Robert C. Martin", "Martin Fowler",
                    "Dale Carnegie", "Yuval Noah Harari", "Paulo Coelho", "Joshua Bloch",
                    "Trần Trọng Kim", "Nguyễn Du", "Adam Khoo"
                  }) {
                Author x = new Author();
                x.name = n;
                au.put(n, authors.save(x));
              }

              // ---- Đầu sách + bản sách ----
              // code, tên, thể loại, NXB, năm, tác giả (phân cách ;), số bản, kệ
              Object[][] books = {
                {"S001", "Clean Code", "CNTT", "NXB Lao Động", 2019, "Robert C. Martin", 3, "A1"},
                {"S002", "Refactoring", "CNTT", "NXB Thế Giới", 2020, "Martin Fowler", 2, "A1"},
                {"S003", "Effective Java", "CNTT", "NXB Lao Động", 2021, "Joshua Bloch", 2, "A2"},
                {"S004", "Clean Architecture", "CNTT", "NXB Thế Giới", 2022, "Robert C. Martin", 2, "A2"},
                {"S005", "Mắt biếc", "VH", "NXB Trẻ", 2019, "Nguyễn Nhật Ánh", 3, "B1"},
                {"S006", "Cho tôi xin một vé đi tuổi thơ", "VH", "NXB Trẻ", 2018, "Nguyễn Nhật Ánh", 2, "B1"},
                {"S007", "Chí Phèo", "VH", "NXB Tổng hợp TP.HCM", 2017, "Nam Cao", 2, "B2"},
                {"S008", "Truyện Kiều", "VH", "NXB Tổng hợp TP.HCM", 2015, "Nguyễn Du", 2, "B2"},
                {"S009", "Nhà giả kim", "VH", "NXB Thế Giới", 2020, "Paulo Coelho", 3, "B3"},
                {"S010", "Dế Mèn phiêu lưu ký", "TN", "NXB Kim Đồng", 2021, "Tô Hoài", 3, "C1"},
                {"S011", "Đắc nhân tâm", "KN", "NXB Tổng hợp TP.HCM", 2016, "Dale Carnegie", 3, "D1"},
                {"S012", "Tôi tài giỏi, bạn cũng thế", "KN", "NXB Lao Động", 2018, "Adam Khoo", 2, "D1"},
                {"S013", "Sapiens: Lược sử loài người", "LS", "NXB Thế Giới", 2019, "Yuval Noah Harari", 2, "E1"},
                {"S014", "Việt Nam sử lược", "LS", "NXB Tổng hợp TP.HCM", 2014, "Trần Trọng Kim", 2, "E1"},
                {"S015", "Bí quyết làm giàu", "KT", "NXB Lao Động", 2020, "Adam Khoo", 2, "F1"},
                {"S016", "Homo Deus", "LS", "NXB Thế Giới", 2021, "Yuval Noah Harari", 1, "E2"}
              };
              List<BookCopy> all = new ArrayList<>();
              for (Object[] b : books) {
                BookTitle t = new BookTitle();
                t.code = (String) b[0];
                t.title = (String) b[1];
                t.category = cat.get((String) b[2]);
                t.publisher = pub.get((String) b[3]);
                t.publicationYear = (Integer) b[4];
                for (String a : ((String) b[5]).split(";")) t.authors.add(au.get(a));
                titles.save(t);
                for (int i = 1; i <= (Integer) b[6]; i++) {
                  BookCopy c = new BookCopy();
                  c.code = t.code + "-" + String.format("%02d", i);
                  c.bookTitle = t;
                  c.location = "Kệ " + b[7];
                  all.add(copies.save(c));
                }
              }

              // ---- Độc giả ----
              List<Reader> rs = new ArrayList<>();
              readers.findAll().forEach(rs::add); // DG-DEMO gắn với tài khoản reader
              String[][] people = {
                {"DG001", "Nguyễn Văn An", "an.nguyen@example.com", "0901234561"},
                {"DG002", "Trần Thị Bình", "binh.tran@example.com", "0901234562"},
                {"DG003", "Lê Hoàng Cường", "cuong.le@example.com", "0901234563"},
                {"DG004", "Phạm Minh Dũng", "dung.pham@example.com", "0901234564"},
                {"DG005", "Hoàng Thu Hà", "ha.hoang@example.com", "0901234565"},
                {"DG006", "Vũ Đức Khang", "khang.vu@example.com", "0901234566"},
                {"DG007", "Đỗ Ngọc Lan", "lan.do@example.com", "0901234567"},
                {"DG008", "Bùi Quang Minh", "minh.bui@example.com", null}
              };
              for (String[] p : people) {
                Reader r = new Reader();
                r.code = p[0];
                r.fullName = p[1];
                r.email = p[2];
                r.phone = p[3];
                rs.add(readers.save(r));
              }
              rs.get(rs.size() - 1).active = false; // một thẻ đã ngừng hoạt động

              // ---- Phiếu mượn (ngày mượn tính lùi từ hôm nay) ----
              Account staff = accounts.findByUsername("librarian").orElseThrow();
              Reader demo = rs.get(0);
              LocalDateTime now = LocalDateTime.now(clock);
              Iterator<BookCopy> pool = all.iterator();
              // độc giả, số ngày trước, số cuốn, cách trả: null = đang mượn, GOOD/DAMAGED/LOST,
              // trả sau bao nhiêu ngày, đã thu tiền phạt?
              Object[][] plan = {
                {demo, 3, 2, null, 0, false}, //  đang mượn, còn hạn
                {demo, 40, 1, "GOOD", 18, true}, // đã trả trễ 4 ngày, đã nộp phạt
                {rs.get(1), 5, 2, null, 0, false},
                {rs.get(2), 20, 1, null, 0, false}, // quá hạn 6 ngày
                {rs.get(3), 25, 2, null, 0, false}, // quá hạn 11 ngày
                {rs.get(4), 30, 1, "GOOD", 10, false}, // trả đúng hạn
                {rs.get(5), 35, 1, "GOOD", 20, false}, // trả trễ 6 ngày, chưa nộp phạt
                {rs.get(6), 45, 1, "DAMAGED", 12, true}, // hư hỏng, có bồi thường, đã thu
                {rs.get(2), 60, 1, "LOST", 16, false}, // mất sách, chưa thu
                {rs.get(1), 1, 1, null, 0, false}
              };
              for (Object[] p : plan) {
                Loan loan = new Loan();
                loan.reader = (Reader) p[0];
                loan.createdBy = staff;
                loan.borrowedAt = now.minusDays((Integer) p[1]).withHour(9).withMinute(30);
                String how = (String) p[3];
                for (int i = 0; i < (Integer) p[2]; i++) {
                  BookCopy c = pool.next();
                  LoanDetail d = new LoanDetail();
                  d.loan = loan;
                  d.bookCopy = c;
                  d.dailyRate = rate;
                  d.dueDate = loan.borrowedAt.toLocalDate().plusDays(loanDays);
                  if (how == null) {
                    c.status = BookCopy.CopyStatus.BORROWED;
                  } else {
                    d.status = "RETURNED";
                    d.conditionOnReturn = how;
                    d.returnedAt = loan.borrowedAt.plusDays((Integer) p[4]).withHour(15);
                    d.returnedBy = staff;
                    d.lateFee = fines.lateFee(d.dueDate, d.returnedAt.toLocalDate(), rate);
                    switch (how) {
                      case "DAMAGED" -> {
                        d.compensation = new BigDecimal("50000");
                        d.compensationReason = "Rách bìa, ướt nhiều trang";
                        c.status = BookCopy.CopyStatus.DAMAGED;
                      }
                      case "LOST" -> {
                        d.compensation = new BigDecimal("150000");
                        d.compensationReason = "Độc giả làm mất sách";
                        c.status = BookCopy.CopyStatus.LOST;
                      }
                      default -> c.status = BookCopy.CopyStatus.AVAILABLE;
                    }
                    d.fineTotal = d.lateFee.add(d.compensation);
                    if ((Boolean) p[5] && d.fineTotal.signum() > 0) {
                      d.finePaidAt = d.returnedAt.plusMinutes(10);
                      d.paidBy = staff;
                    }
                  }
                  loan.details.add(d);
                }
                if (how != null) loan.status = "CLOSED";
                loans.save(loan);
              }
            });
  }
}
