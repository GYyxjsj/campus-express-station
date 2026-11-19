package com.campus.express.controller;

import com.campus.express.common.R;
import com.campus.express.security.UserContext;
import com.campus.express.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 好友私聊（登录即可用，仅 ACCEPTED 好友之间） */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> conversations() {
        return R.ok(chatService.conversations(UserContext.require().getUid()));
    }

    @GetMapping("/messages/{peerId}")
    public R<List<Map<String, Object>>> messages(@PathVariable Long peerId) {
        return R.ok(chatService.messages(UserContext.require().getUid(), peerId));
    }

    @PostMapping("/send")
    public R<Map<String, Object>> send(@RequestBody Map<String, Object> body) {
        Long peerId = body.get("peerId") == null ? null : Long.valueOf(String.valueOf(body.get("peerId")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        return R.ok(chatService.send(UserContext.require().getUid(), peerId, content));
    }

    @GetMapping("/unread")
    public R<Long> unread() {
        return R.ok(chatService.unread(UserContext.require().getUid()));
    }
}