package fitnesstracker.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import fitnesstracker.models.TokenBucket;

@Repository
public interface TokenBucketRepository extends JpaRepository<TokenBucket, String> {
}