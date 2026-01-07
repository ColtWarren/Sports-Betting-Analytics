package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.AutoSettleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auto-settle")
public class AutoSettleController {
    
    private final AutoSettleService autoSettleService;
    
    @Autowired
    public AutoSettleController(AutoSettleService autoSettleService) {
        this.autoSettleService = autoSettleService;
    }
    
    @PostMapping("/run")
    public Map<String, Object> runAutoSettle() {
        return autoSettleService.autoSettleAllBets();
    }
}
