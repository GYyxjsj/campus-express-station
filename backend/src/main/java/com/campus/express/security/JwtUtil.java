package com.campus.express.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JWT 签发与解析（HS256）。密钥/有效期外置配置，生产必须覆盖默认密钥。
 */
@Component
public class JwtUtil {

    @Value("${campus.jwt.secret}")
    private String secret;

    @Value("${campus.jwt.expire-hours:2}")
    private long expireHours;

    public String issue(LoginUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUid()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("roles", user.getRoles())
                .claim("perms", user.getPerms())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireHours * 3600_000L))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 解析失败（过期/篡改）返回 null */
    @SuppressWarnings("unchecked")
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            LoginUser u = new LoginUser();
            u.setUid(Long.valueOf(claims.getSubject()));
            u.setUsername(claims.get("username", String.class));
            u.setRealName(claims.get("realName", String.class));
            u.setRoles(new HashSet<>((List<String>) claims.get("roles", List.class)));
            u.setPerms(new HashSet<>((List<String>) claims.get("perms", List.class)));
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}