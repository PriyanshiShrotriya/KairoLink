package com.KairoLink.service;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.IssueReport;
import com.KairoLink.entity.IssueStatus;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.User;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.IssueReportRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class IssueReportService {

    private final IssueReportRepository issueReportRepository;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public IssueReportService(
            IssueReportRepository issueReportRepository,
            UserRepository userRepository,
            RideRepository rideRepository,
            BookingRepository bookingRepository) {
        this.issueReportRepository = issueReportRepository;
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<IssueReport> getAllIssuesForAdmin() {
        return issueReportRepository.findAllWithDetails();
    }

    public List<IssueReport> getMyIssues(String email) {
        User reporter = findUser(email);
        return issueReportRepository.findByReporterIdWithDetails(reporter.getId());
    }

    @Transactional
    public IssueReport createIssue(
            String email,
            String category,
            String description,
            Long rideId,
            Long bookingId) {

        User reporter = findUser(email);

        validateInput(category, description);

        IssueReport report = new IssueReport();
        report.setReporter(reporter);
        report.setCategory(category.trim());
        report.setDescription(description.trim());
        report.setStatus(IssueStatus.OPEN);

        if (rideId != null) {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
            report.setRide(ride);
        }

        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            report.setBooking(booking);
        }

        return issueReportRepository.save(report);
    }

    @Transactional
    public IssueReport updateStatus(Long issueId, IssueStatus newStatus) {
        IssueReport report = issueReportRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        report.setStatus(newStatus);

        if (newStatus == IssueStatus.RESOLVED) {
            report.setResolvedAt(Instant.now());
        } else {
            report.setResolvedAt(null);
        }

        return issueReportRepository.save(report);
    }

    private User findUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateInput(String category, String description) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (category.length() > 50) {
            throw new IllegalArgumentException("Category must not exceed 50 characters");
        }
        if (description.length() > 2000) {
            throw new IllegalArgumentException("Description must not exceed 2000 characters");
        }
    }
}
