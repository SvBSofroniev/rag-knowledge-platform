package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.entity.AiQuery;

import java.util.UUID;

@Repository
public interface AiQueryRepository
        extends JpaRepository<AiQuery, UUID> {
}