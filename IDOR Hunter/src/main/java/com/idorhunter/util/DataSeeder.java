package com.idorhunter.util;

import com.idorhunter.model.AppUser;
import com.idorhunter.model.Classification;
import com.idorhunter.model.Document;
import com.idorhunter.model.Role;
import com.idorhunter.repository.DocumentRepository;
import com.idorhunter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.idorhunter.util.LabCredentials.*;

/**
 * Populates the embedded H2 database with deterministic, synthetic
 * accounts and documents every time the application starts, so the lab is
 * reproducible and self-contained. No real personal data is used anywhere.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, DocumentRepository documentRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Lab data already present, skipping seeding.");
            return;
        }

        AppUser userA = userRepository.save(new AppUser(USER_A_USERNAME, "User A (Alice)",
                passwordEncoder.encode(USER_A_PASSWORD), Role.USER, "+15550100001"));
        AppUser userB = userRepository.save(new AppUser(USER_B_USERNAME, "User B (Bilal)",
                passwordEncoder.encode(USER_B_PASSWORD), Role.USER, "+15550100002"));
        userRepository.save(new AppUser(ADMIN_USERNAME, "Security Administrator",
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN, "+15550100099"));

        documentRepository.save(new Document(
                "Alice - Travel Itinerary",
                "Flight AA204 departs 09:40. Hotel reservation #884213 at Harbor Inn, rooms 1-2 Oct.",
                Classification.PUBLIC, userA.getUsername()));

        documentRepository.save(new Document(
                "Alice - Personal Tax Summary 2025",
                "Annual income: $94,200.00. SSN (synthetic): 000-11-2222. Filing status: Single.",
                Classification.CONFIDENTIAL, userA.getUsername()));

        documentRepository.save(new Document(
                "Bilal - Performance Review Draft",
                "Manager notes (synthetic): strong Q3 delivery, recommend promotion to Senior Engineer.",
                Classification.PRIVATE, userB.getUsername()));

        documentRepository.save(new Document(
                "Bilal - Medical Appointment Notes",
                "Synthetic clinical note: routine checkup, blood pressure 118/76, no follow-up required.",
                Classification.CONFIDENTIAL, userB.getUsername()));

        documentRepository.save(new Document(
                "Bilal - Banking Reference Letter",
                "Synthetic bank letter confirming account in good standing since 2019.",
                Classification.PRIVATE, userB.getUsername()));

        log.info("Seeded lab accounts: '{}', '{}', '{}' (passwords: see util.LabCredentials / README)",
                USER_A_USERNAME, USER_B_USERNAME, ADMIN_USERNAME);
    }
}
