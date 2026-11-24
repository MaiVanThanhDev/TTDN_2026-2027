package com.blueocean.library.config;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Chỉ tồn tại ở profile demo: cung cấp tài khoản demo cho màn hình đăng nhập. */
@RestController
@Profile("demo")
public class DemoAccountsController {
  private final String password;

  public DemoAccountsController(@Value("${DEMO_PASSWORD}") String password) {
    this.password = password;
  }

  @GetMapping("/api/demo-accounts")
  public List<Map<String, String>> accounts() {
    return List.of(
        Map.of("username", "librarian", "label", "Thủ thư", "note", "Sách, độc giả, mượn trả, quá hạn", "password", password),
        Map.of("username", "reader", "label", "Độc giả", "note", "Tra cứu & lịch sử mượn", "password", password),
        Map.of("username", "admin", "label", "Quản trị", "note", "Tài khoản, phân quyền, thống kê", "password", password));
  }
}
