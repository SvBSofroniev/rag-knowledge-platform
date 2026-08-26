package src.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import src.dashboard.dto.DashboardResponse;
import src.dashboard.service.DashboardService;
import src.entity.User;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard(
            @AuthenticationPrincipal User currentUser
    ) {
        return dashboardService.getDashboard(
                currentUser
        );
    }
}