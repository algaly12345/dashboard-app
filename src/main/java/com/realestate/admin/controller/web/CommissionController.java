package com.realestate.admin.controller.web;

import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Commission;
import com.realestate.admin.entity.CommissionWithdrawalRequest;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.CommissionRepository;
import com.realestate.admin.repository.CommissionWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionRepository commissionRepository;
    private final CommissionWithdrawalRequestRepository withdrawalRequestRepository;
    private final AppUserRepository appUserRepository;
    private final com.realestate.admin.service.NotificationSendService notificationSendService;

    // ==================== Commissions ====================

    @GetMapping("/commissions")
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Commission.Status statusEnum = (status != null && !status.isBlank())
                ? Commission.Status.valueOf(status) : null;

        Page<Commission> result = commissionRepository.search(statusEnum, PageRequest.of(page, 15));

        List<Long> userIds = result.getContent().stream().map(Commission::getUserId).distinct().toList();
        Map<Long, AppUser> users = new HashMap<>();
        for (AppUser u : appUserRepository.findAllById(userIds)) users.put(u.getId(), u);

        Map<String, Long> statusCounts = new HashMap<>();
        Map<String, BigDecimal> statusSums = new HashMap<>();
        for (Object[] row : commissionRepository.summaryGroupedByStatus()) {
            String key = ((Commission.Status) row[0]).name();
            statusCounts.put(key, (Long) row[1]);
            statusSums.put(key, (BigDecimal) row[2]);
        }

        model.addAttribute("commissions", result);
        model.addAttribute("users", users);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("statusSums", statusSums);
        model.addAttribute("status", status);
        model.addAttribute("pendingWithdrawalCount", withdrawalRequestRepository.countByStatus(CommissionWithdrawalRequest.Status.pending));
        model.addAttribute("activePage", "commissions");

        return "commissions";
    }

    @PostMapping("/commissions/{id}/approve")
    public String approveCommission(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        commissionRepository.findById(id).ifPresent(c -> {
            c.setStatus(Commission.Status.APPROVED);
            c.setApprovedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            commissionRepository.save(c);
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions";
    }

    @PostMapping("/commissions/{id}/make-available")
    public String makeAvailable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        commissionRepository.findById(id).ifPresent(c -> {
            c.setStatus(Commission.Status.AVAILABLE);
            c.setAvailableAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            commissionRepository.save(c);
            notifyUser(c.getUserId(), "أصبح رصيدك متاحًا للسحب",
                    "مكافأة إحالة بقيمة " + c.getAmount() + " ر.س أصبحت متاحة للسحب الآن.");
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions";
    }

    @PostMapping("/commissions/{id}/cancel")
    public String cancelCommission(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        commissionRepository.findById(id).ifPresent(c -> {
            c.setStatus(Commission.Status.CANCELLED);
            c.setUpdatedAt(LocalDateTime.now());
            commissionRepository.save(c);
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions";
    }

    // ==================== Withdrawal requests ====================

    @GetMapping("/commissions/withdrawals")
    public String withdrawals(@RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {

        CommissionWithdrawalRequest.Status statusEnum = (status != null && !status.isBlank())
                ? CommissionWithdrawalRequest.Status.valueOf(status) : null;

        Page<CommissionWithdrawalRequest> result = withdrawalRequestRepository.search(statusEnum, PageRequest.of(page, 15));

        List<Long> userIds = result.getContent().stream().map(CommissionWithdrawalRequest::getUserId).distinct().toList();
        Map<Long, AppUser> users = new HashMap<>();
        for (AppUser u : appUserRepository.findAllById(userIds)) users.put(u.getId(), u);

        model.addAttribute("withdrawals", result);
        model.addAttribute("users", users);
        model.addAttribute("status", status);
        model.addAttribute("activePage", "commissions");

        return "commission-withdrawals";
    }

    @PostMapping("/commissions/withdrawals/{id}/approve")
    public String approveWithdrawal(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        withdrawalRequestRepository.findById(id).ifPresent(w -> {
            w.setStatus(CommissionWithdrawalRequest.Status.approved);
            w.setProcessedAt(LocalDateTime.now());
            w.setUpdatedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(w);
            notifyUser(w.getUserId(), "تمت الموافقة على طلب السحب",
                    "تمت الموافقة على طلب سحب بقيمة " + w.getAmount() + " ر.س، وسيتم التحويل قريبًا.");
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions/withdrawals";
    }

    @PostMapping("/commissions/withdrawals/{id}/reject")
    public String rejectWithdrawal(@PathVariable Long id,
                                    @RequestParam(required = false) String note,
                                    RedirectAttributes redirectAttributes) {
        withdrawalRequestRepository.findById(id).ifPresent(w -> {
            w.setStatus(CommissionWithdrawalRequest.Status.rejected);
            w.setNote(note);
            w.setProcessedAt(LocalDateTime.now());
            w.setUpdatedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(w);
            String reasonText = (note != null && !note.isBlank()) ? "\nالسبب: " + note : "";
            notifyUser(w.getUserId(), "تم رفض طلب السحب",
                    "تم رفض طلب سحب بقيمة " + w.getAmount() + " ر.س." + reasonText);
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions/withdrawals";
    }

    @PostMapping("/commissions/withdrawals/{id}/mark-paid")
    public String markPaid(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        withdrawalRequestRepository.findById(id).ifPresent(w -> {
            w.setStatus(CommissionWithdrawalRequest.Status.paid);
            w.setProcessedAt(LocalDateTime.now());
            w.setUpdatedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(w);

            // Mark this user's AVAILABLE commissions as WITHDRAWN, up to the paid amount's worth of records.
            commissionRepository.search(Commission.Status.AVAILABLE, PageRequest.of(0, 1000)).getContent().stream()
                    .filter(c -> c.getUserId().equals(w.getUserId()))
                    .forEach(c -> {
                        c.setStatus(Commission.Status.WITHDRAWN);
                        c.setUpdatedAt(LocalDateTime.now());
                        commissionRepository.save(c);
                    });

            notifyUser(w.getUserId(), "تم تحويل مبلغ السحب",
                    "تم تحويل مبلغ " + w.getAmount() + " ر.س لحسابك البنكي بنجاح.");
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/commissions/withdrawals";
    }

    private void notifyUser(Long userId, String title, String body) {
        if (userId == null) return;
        appUserRepository.findById(userId).ifPresent(user -> {
            String token = user.getCmFirebaseToken();
            if (token == null || token.length() <= 50) return;
            notificationSendService.sendToToken(token, title, body);
        });
    }
}
