package com.hardware.sales.controller;

import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证控制器测试，覆盖后台登录、小程序登录和客户注册流程。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 业务管理员角色（ADMIN）仍然只能通过后台 Web 登录入口进入后台。
     */
    @Test
    void shouldAllowAdminLoginToWebAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    /**
     * 供应商账号不可进入后台 Web，只能走小程序登录入口。
     */
    @Test
    void shouldRejectSupplierLoginToWebAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"supplier_tool_01","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("当前后台仅允许业务管理员登录"));
    }

    /**
     * 客户账号可通过小程序登录入口进入商品浏览与预定流程。
     */
    @Test
    void shouldAllowCustomerLoginToMiniapp() throws Exception {
        SysUser customer = new SysUser();
        customer.setUsername("customer_login_case");
        customer.setPassword("admin123");
        customer.setNickname("客户登录测试");
        customer.setPhone("13700009999");
        customer.setRole("CUSTOMER");
        customer.setStatus(1);
        sysUserService.createUser(customer);

        mockMvc.perform(post("/api/auth/miniapp/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"customer_login_case","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"));
    }

    /**
     * 小程序客户注册后应直接获得 CUSTOMER 角色并建立登录态。
     */
    @Test
    void shouldRegisterCustomerWithCustomerRole() throws Exception {
        mockMvc.perform(post("/api/auth/miniapp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registerType":"CUSTOMER",
                                  "username":"customer_new_01",
                                  "password":"123456",
                                  "nickname":"新客户",
                                  "phone":"13712345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
