package com.blueocean.library.service;

import static com.blueocean.library.exception.Errors.*;

import com.blueocean.library.dto.Requests.*;
import com.blueocean.library.entity.*;
import com.blueocean.library.repository.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản trị viên: quản lý tài khoản và phân quyền. */
@Service
@Transactional
public class AccountService {
  private final AccountRepository accounts;
  private final ReaderRepository readers;
  private final PasswordEncoder encoder;

  public AccountService(AccountRepository a, ReaderRepository r, PasswordEncoder e) {
    accounts = a;
    readers = r;
    encoder = e;
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> list(int page, int size) {
    Page<Account> p = accounts.findAll(CatalogService.page(page, size));
    Map<Long, Reader> linked =
        readers.findByAccountIdIn(p.map(a -> a.id).toList()).stream()
            .collect(Collectors.toMap(r -> Views.loaded(r.account).id, Function.identity()));
    return p.map(a -> Views.account(a, linked.get(a.id)));
  }

  /** Độc giả đang hoạt động và chưa có tài khoản: dùng khi tạo/đổi sang tài khoản độc giả. */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> linkableReaders() {
    return readers.findByAccountIsNullAndActiveTrueOrderByCode().stream()
        .map(r -> Map.<String, Object>of("id", r.id, "code", r.code, "fullName", r.fullName))
        .toList();
  }

  public Map<String, Object> create(AccountCreate r) {
    String username = r.username().trim();
    check(!accounts.existsByUsername(username), "Tên đăng nhập đã tồn tại");
    Account a = new Account();
    a.username = username;
    a.passwordHash = encoder.encode(r.password());
    a.role = r.role();
    accounts.save(a);
    Reader linked = r.role().equals("READER") ? link(a, r.readerId()) : null;
    return Views.account(a, linked);
  }

  public Map<String, Object> changeRole(Long id, RoleChange r, String currentUser) {
    Account a = required(accounts.findById(id));
    check(!a.username.equals(currentUser), "Không thể tự đổi vai trò của chính mình");
    if (a.role.equals("ADMIN") && !r.role().equals("ADMIN")) keepOneAdmin(a);
    Reader linked = readers.findByAccountId(a.id).orElse(null);
    if (r.role().equals("READER")) {
      if (linked == null) linked = link(a, r.readerId());
    } else if (linked != null) {
      linked.account = null; // tài khoản nhân viên không gắn với thẻ độc giả
      linked = null;
    }
    a.role = r.role();
    return Views.account(a, linked);
  }

  public Map<String, Object> setEnabled(Long id, boolean enabled, String currentUser) {
    Account a = required(accounts.findById(id));
    check(!a.username.equals(currentUser), "Không thể tự khóa tài khoản của chính mình");
    if (!enabled && a.role.equals("ADMIN")) keepOneAdmin(a);
    a.enabled = enabled;
    return Views.account(a, readers.findByAccountId(a.id).orElse(null));
  }

  public void resetPassword(Long id, PasswordReset r) {
    required(accounts.findById(id)).passwordHash = encoder.encode(r.password());
  }

  private Reader link(Account a, Long readerId) {
    check(readerId != null, "Tài khoản độc giả phải gắn với một độc giả");
    Reader reader = required(readers.lockById(readerId));
    check(reader.active, "Thẻ độc giả không hoạt động");
    check(reader.account == null, "Độc giả này đã có tài khoản");
    reader.account = a;
    return reader;
  }

  private void keepOneAdmin(Account a) {
    check(
        !a.enabled || accounts.countByRoleAndEnabledTrue("ADMIN") > 1,
        "Hệ thống phải còn ít nhất một quản trị viên đang hoạt động");
  }
}
