package com.KairoLink.controller;

import com.KairoLink.entity.IssueReport;
import com.KairoLink.entity.IssueStatus;
import com.KairoLink.service.IssueReportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reports")
public class IssueReportController {

    private final IssueReportService issueReportService;

    public IssueReportController(IssueReportService issueReportService) {
        this.issueReportService = issueReportService;
    }

    @GetMapping("/new")
    public String showReportForm() {
        return "reports/new";
    }

    @PostMapping
    public String createReport(
            Authentication authentication,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam(required = false) Long rideId,
            @RequestParam(required = false) Long bookingId,
            RedirectAttributes redirectAttributes) {
        try {
            issueReportService.createIssue(
                    authentication.getName(),
                    category,
                    description,
                    rideId,
                    bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Your issue has been reported successfully");
            return "redirect:/reports/my-issues";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reports/new";
        }
    }

    @GetMapping("/my-issues")
    public String listMyIssues(Authentication authentication, Model model) {
        List<IssueReport> issues = issueReportService.getMyIssues(authentication.getName());
        model.addAttribute("issues", issues);
        return "reports/my-issues";
    }
}
