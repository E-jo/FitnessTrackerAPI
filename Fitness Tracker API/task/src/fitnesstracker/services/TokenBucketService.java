package fitnesstracker.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger.*;
import fitnesstracker.repositories.TokenBucketRepository;
import fitnesstracker.models.TokenBucket;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.Duration;


@Component
@Data
public class TokenBucketService {
    private final TokenBucketRepository tokenBucketRepository;
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    public TokenBucketService(TokenBucketRepository tokenBucketRepository) {
        this.tokenBucketRepository = tokenBucketRepository;

        tokenBucketRepository.findAll().forEach(bucket -> tokenBuckets.putIfAbsent(bucket.getClientId(), bucket));
    }

    public boolean grantAccess(String clientId) {
        tokenBucketRepository.findAll().forEach(bucket ->
                tokenBuckets.putIfAbsent(bucket.getClientId(), bucket));

        AtomicBoolean validRequest = new AtomicBoolean(false);

        tokenBuckets.compute(clientId, (key, bucket) -> {
            TokenBucket updatedBucket = processBucket(clientId, bucket, validRequest);
            tokenBucketRepository.save(updatedBucket);
            System.out.println("updatedBucket: " + updatedBucket);
            return updatedBucket;
        });

        System.out.println("Current tokenBuckets map:");
        for (String key : tokenBuckets.keySet()) {
            System.out.println(key + ": " + tokenBuckets.get(key));
        }

        System.out.println("validRequest for " + clientId + " now " + validRequest.get());

        return validRequest.get();
    }

    private TokenBucket processBucket(String clientId, TokenBucket bucket, AtomicBoolean validRequest) {
        if (bucket == null) {
            System.out.println("Creating new bucket for " + clientId);
            bucket = new TokenBucket();
            bucket.setClientId(clientId);
            bucket.setTokens(new AtomicInteger(1));
            bucket.setValidRequest(new AtomicBoolean(true));
            bucket.setLastUpdated(LocalDateTime.now());
            tokenBucketRepository.save(bucket);
            System.out.println("Saving new bucket: " + bucket);
        }

        refillBucket(bucket);

        if (bucket.getTokens().get() > 0) {
            System.out.println(clientId + ": " + bucket.getTokens().get());
            bucket.getTokens().decrementAndGet();
            bucket.getValidRequest().set(true);
        } else {
            bucket.getValidRequest().set(false);
        }
        System.out.println("validRequest for " + clientId + " set to " + validRequest.get());

        return bucket;
    }

    private void refillBucket(TokenBucket bucket) {
        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = Duration.between(bucket.getLastUpdated(), now).toSeconds();
        System.out.println(bucket.getClientId() + ": " + secondsElapsed + " seconds since last token");
        if (secondsElapsed >= 1 && bucket.getTokens().get() < 1) {
            System.out.println("Refilling bucket for " + bucket.getClientId());
            System.out.println("Current bucket: " + bucket);
            bucket.setTokens(new AtomicInteger(1));
            bucket.setLastUpdated(now);
            System.out.println("Updated bucket: " + bucket);
        }
    }
}
