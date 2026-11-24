package com.blueocean.library.controller;

import com.blueocean.library.dto.Requests.*;
import com.blueocean.library.service.AccountService;
import com.blueocean.library.service.StatsService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

/** Chức năng của quản trị viên: tài khoản, phân quyền, thống kê. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AccountService accounts;
  private final StatsService stats;

  public AdminController(AccountService a, StatsService s) {
    accounts = a;
    stats = s;
  }

  @GetMapping("/stats")
  public Object stats() {
    return stats.overview();
  }

  @GetMapping("/accounts")
  public Object list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return accounts.list(page, size);
  }

  @GetMapping("/accounts/linkable-readers")
  public Object linkableReaders() {
    return accounts.linkableReaders();
  }

  @PostMapping("/accounts")
  public Object create(@Valid @RequestBody AccountCreate r) {
    return accounts.create(r);
  }

  @PutMapping("/accounts/{id}/role")
  public Object role(@PathVariable Long id, @Valid @RequestBody RoleChange r, Principal p) {
    return accounts.changeRole(id, r, p.getName());
  }

  @PatchMapping("/accounts/{id}/status")
  public Object status(@PathVariable Long id, @Valid @RequestBody StatusChange r, Principal p) {
    return accounts.setEnabled(id, r.enabled(), p.getName());
  }

  @PostMapping("/accounts/{id}/password")
  public void password(@PathVariable Long id, @Valid @RequestBody PasswordReset r) {
    accounts.resetPassword(id, r);
  }
}
