package com.blueocean.library.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.blueocean.library.entity.*;
import com.blueocean.library.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminTest {
  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired ReaderRepository readers;
  Account boss;
  Reader reader;

  @BeforeEach
  void seed() {
    boss = account("boss", "ADMIN");
    reader = new Reader();
    reader.code = "DG77";
    reader.fullName = "Reader 77";
    readers.save(reader);
  }

  Account account(String name, String role) {
    Account a = new Account();
    a.username = name;
    a.role = role;
    a.passwordHash = "unused";
    return accounts.save(a);
  }

  // ---- Phân quyền theo vai trò ----

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCannotUseLibrarianFeatures() throws Exception {
    mvc.perform(get("/api/books")).andExpect(status().isForbidden());
    mvc.perform(get("/api/readers")).andExpect(status().isForbidden());
    mvc.perform(get("/api/loans")).andExpect(status().isForbidden());
    mvc.perform(get("/api/me/loans")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "LIBRARIAN")
  void librarianCannotUseAdminOrReaderFeatures() throws Exception {
    mvc.perform(get("/api/admin/accounts")).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
    mvc.perform(get("/api/me/loans")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READER")
  void readerCannotUseAdminFeatures() throws Exception {
    mvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
    mvc.perform(get("/api/readers")).andExpect(status().isForbidden());
  }

  // ---- Quản lý tài khoản ----

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void createReaderAccountLinksReader() throws Exception {
    mvc.perform(
            post("/api/admin/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"username\":\"new.reader\",\"password\":\"Password-123\",\"role\":\"READER\",\"readerId\":"
                        + reader.id
                        + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.readerCode").value("DG77"));
    // độc giả đã có tài khoản thì không thể gắn thêm
    mvc.perform(
            post("/api/admin/accounts")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"username\":\"second\",\"password\":\"Password-123\",\"role\":\"READER\",\"readerId\":"
                        + reader.id
                        + "}"))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void readerAccountRequiresReader() throws Exception {
    mvc.perform(
            post("/api/admin/accounts")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"lonely\",\"password\":\"Password-123\",\"role\":\"READER\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void duplicateUsernameRejected() throws Exception {
    mvc.perform(
            post("/api/admin/accounts")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"boss\",\"password\":\"Password-123\",\"role\":\"LIBRARIAN\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void adminCannotLockOrDemoteSelf() throws Exception {
    mvc.perform(
            patch("/api/admin/accounts/" + boss.id + "/status")
                .with(csrf())
                .contentType("application/json")
                .content("{\"enabled\":false}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/admin/accounts/" + boss.id + "/role")
                .with(csrf())
                .contentType("application/json")
                .content("{\"role\":\"LIBRARIAN\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void lockAndChangeRoleOfOthers() throws Exception {
    Account staff = account("staff2", "LIBRARIAN");
    mvc.perform(
            patch("/api/admin/accounts/" + staff.id + "/status")
                .with(csrf())
                .contentType("application/json")
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
    mvc.perform(
            put("/api/admin/accounts/" + staff.id + "/role")
                .with(csrf())
                .contentType("application/json")
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  @WithMockUser(username = "boss", roles = "ADMIN")
  void statsAvailable() throws Exception {
    mvc.perform(get("/api/admin/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountsByRole.ADMIN").isNumber())
        .andExpect(jsonPath("$.copiesByStatus.AVAILABLE").isNumber())
        .andExpect(jsonPath("$.loansByMonth").isMap())
        .andExpect(jsonPath("$.topBooks").isArray());
  }
}
