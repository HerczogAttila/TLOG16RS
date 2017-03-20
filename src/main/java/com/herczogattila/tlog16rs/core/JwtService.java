/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.security.Key;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author precognox
 */
public class JwtService {
    private static final String secret = "FhrIoDY9m3NdYj47rJQH";
    private static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private static final byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secret);
    private static final Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
    private static final long fiveMinutes = 300000;
    
    public static String createJWT(String name) {
        long expirationMillis = System.currentTimeMillis() + fiveMinutes;
        Date exp = new Date(expirationMillis);
        
        JwtBuilder builder = Jwts.builder()
                .setSubject(name)
                .setExpiration(exp)
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
    
    public static Claims parseJWT(String jwt) throws SignatureException {
        Claims claims = Jwts.parser()
                .setSigningKey(apiKeySecretBytes)
                .parseClaimsJws(jwt)
                .getBody();
        
        return claims;
    }
}
