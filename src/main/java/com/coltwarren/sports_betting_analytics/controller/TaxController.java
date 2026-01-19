package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.W2GForm;
import com.coltwarren.sports_betting_analytics.repository.W2GFormRepository;
import com.coltwarren.sports_betting_analytics.service.TaxReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tax")
public class TaxController {

    @Autowired
    private TaxReportService taxReportService;

    @Autowired
    private W2GFormRepository w2gFormRepository;

    /**
     * Tax Center Dashboard
     */
    @GetMapping
    public String taxCenter(@RequestParam(required = false) Integer year, Model model) {
        // Default to current year if not specified
        if (year == null) {
            year = Year.now().getValue();
        }

        Map<String, Object> summary = taxReportService.getTaxSummary(year);
        model.addAttribute("summary", summary);
        model.addAttribute("currentYear", year);

        return "tax/dashboard";
    }

    /**
     * Win/Loss Statement
     */
    @GetMapping("/win-loss")
    public String winLossStatement(@RequestParam(required = false) Integer year, Model model) {
        if (year == null) {
            year = Year.now().getValue();
        }

        List<Bet> bets = taxReportService.getBetsForTaxYear(year);
        Map<String, Object> summary = taxReportService.getTaxSummary(year);

        model.addAttribute("bets", bets);
        model.addAttribute("summary", summary);
        model.addAttribute("currentYear", year);

        return "tax/win-loss";
    }

    /**
     * W-2G Form Tracker
     */
    @GetMapping("/w2g")
    public String w2gTracker(@RequestParam(required = false) Integer year, Model model) {
        if (year == null) {
            year = Year.now().getValue();
        }

        List<W2GForm> forms = w2gFormRepository.findByTaxYear(year);
        List<Bet> w2gBets = taxReportService.getW2GBets(year);

        model.addAttribute("forms", forms);
        model.addAttribute("w2gBets", w2gBets);
        model.addAttribute("currentYear", year);

        return "tax/w2g";
    }

    /**
     * Deduction Calculator
     */
    @GetMapping("/deductions")
    public String deductionCalculator(@RequestParam(required = false) Integer year, Model model) {
        if (year == null) {
            year = Year.now().getValue();
        }

        Map<String, Object> summary = taxReportService.getTaxSummary(year);
        model.addAttribute("summary", summary);
        model.addAttribute("currentYear", year);

        return "tax/deductions";
    }

    /**
     * Mark W-2G form as received
     */
    @PostMapping("/w2g/{id}/received")
    @ResponseBody
    public String markW2GReceived(@PathVariable Long id) {
        W2GForm form = w2gFormRepository.findById(id).orElse(null);
        if (form != null) {
            form.setFormReceived(true);
            w2gFormRepository.save(form);
            return "success";
        }
        return "error";
    }
}
