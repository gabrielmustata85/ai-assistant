package com.ai.assistant.company;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.stereotype.Component;

@Component
public class CompanyAccessGuard {

    /** Aruncă dacă userul autentificat nu este proprietarul firmei. */
    public void assertOwner(Long ownerUserId) {
        Long current = CurrentUser.id();
        if (current == null || ownerUserId == null || !current.equals(ownerUserId)) {
            throw new CompanyAccessDeniedException();
        }
    }
}
