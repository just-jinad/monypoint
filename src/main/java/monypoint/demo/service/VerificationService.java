package monypoint.demo.service;

import monypoint.demo.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;
import jakarta.mail.MessagingException;

@Service
public class VerificationService {
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    private CacheManager cacheManager;

    private static final String OTP_CACHE = "otpCache";
    private static final String EMAIL_TOKEN_CACHE = "emailTokenCache";

    public String sendOtp(String phoneNumber) {
        String otp = String.format("%06d", (int)(Math.random() * 1000000));
        Cache cache = cacheManager.getCache(OTP_CACHE);
        if (cache != null) {
            cache.put(phoneNumber, otp);
        }
        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        Cache cache = cacheManager.getCache(OTP_CACHE);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(phoneNumber);
            if (wrapper != null && wrapper.get().equals(otp)) {
                cache.evict(phoneNumber);
                return true;
            }
        }
        return false;
    }

    public String sendEmailVerification(String email) throws MessagingException {
        String token = UUID.randomUUID().toString();
        emailUtil.sendVerificationEmail(email, token);
        Cache cache = cacheManager.getCache(EMAIL_TOKEN_CACHE);
        if (cache != null) {
            cache.put(email, token);
        }
        return token;
    }

    public boolean verifyEmailToken(String email, String token) {
        Cache cache = cacheManager.getCache(EMAIL_TOKEN_CACHE);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(email);
            if (wrapper != null && wrapper.get().equals(token)) {
                cache.evict(email);
                return true;
            }
        }
        return false;
    }
}