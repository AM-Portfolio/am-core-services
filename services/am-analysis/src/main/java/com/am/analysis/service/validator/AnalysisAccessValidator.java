package com.am.analysis.service.validator;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalysisAccessValidator {

    /**
     * Verifies if the user is authorized to access the entity.
     * - Private entities (Portfolio, Trade, Basket) MUST be owned by the requesting user.
     * - Public entities (Market Index, ETF, Mutual Fund) have no owner (null) and are accessible to anyone.
     */
    public void verifyAccess(AnalysisEntity entity, String userId) {
        // If type is explicitly private OR if the entity has an owner defined
        boolean isPrivate = isPrivateEntity(entity.getType());
        
        if (isPrivate) {
             // Strict check for private types
             if (entity.getOwnerId() == null || !userId.equals(entity.getOwnerId())) {
                 log.warn("Unauthorized access: User {} attempted to access Private Entity {} (Owner: {})", userId, entity.getId(), entity.getOwnerId());
                 throw new SecurityException("Unauthorized access to private resource");
             }
        }
        // Implicit check: If not private type, and ownerId IS set (e.g. private basket?), should we check?
        // Ideally yes.
        else if (entity.getOwnerId() != null && !userId.equals(entity.getOwnerId())) {
             log.warn("Unauthorized access: User {} attempted to access User-Owned Entity {} (Owner: {})", userId, entity.getId(), entity.getOwnerId());
             throw new SecurityException("Unauthorized access to user-owned resource");
        }
        
        // If OwnerId is null (Public Data), access is GRANTED.
    }

    private boolean isPrivateEntity(AnalysisEntityType type) {
        return type == AnalysisEntityType.PORTFOLIO || type == AnalysisEntityType.TRADE || type == AnalysisEntityType.BASKET;
    }
}
