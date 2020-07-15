package com.ankoye.jelly.user.web.rpc;

import com.ankoye.jelly.base.result.Result;
import com.ankoye.jelly.user.domain.User;
import com.ankoye.jelly.user.model.RegisterForm;
import com.ankoye.jelly.user.service.UserService;
import com.ankoye.jelly.util.TokenUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author ankoye@qq.com
 */
@CrossOrigin
@RestController
@RequestMapping("/rpc/user")
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/current")
    public Result getCurrentUser(@RequestHeader("Authorization") String token) {
        Map<String, Object> claims = TokenUtils.parse(token.substring(7));
        String account = claims.get("account").toString();
        return Result.success(userService.findByAccount(account));
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterForm from) {
        userService.add(from.convertToUser());
        return Result.success().setMessage("注册成功");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("list")
    public Result findByPage() {
        return Result.success();
    }
}