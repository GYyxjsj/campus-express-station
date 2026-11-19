package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.security.UserContext;
import com.campus.express.service.FriendService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 好友（学生社区功能，登录即可用） */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public R<List<Map<String, Object>>> my() {
        return R.ok(friendService.myFriends(UserContext.require().getUid()));
    }

    @GetMapping("/requests")
    public R<Map<String, Object>> requests() {
        return R.ok(friendService.requests(UserContext.require().getUid()));
    }

    @PostMapping("/request")
    @OpLog(module = "friend", action = "发起好友申请")
    public R<Void> request(@RequestBody Map<String, String> body) {
        friendService.request(UserContext.require(), body.get("keyword"), body.get("remark"));
        return R.ok();
    }

    @PostMapping("/request/{id}/respond")
    public R<Void> respond(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean accept = Boolean.TRUE.equals(body.get("accept")) || "true".equals(String.valueOf(body.get("accept")));
        friendService.respond(UserContext.require(), id, accept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        friendService.remove(UserContext.require().getUid(), id);
        return R.ok();
    }
}