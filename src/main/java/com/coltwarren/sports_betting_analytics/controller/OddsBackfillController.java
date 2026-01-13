package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.OddsBackfillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backfill")
public class OddsBackfillController {
    
    private final OddsBackfillService backfillService;
    
    @Autowired
    public OddsBackfillController(OddsBackfillService backfillService) {
        this.backfillService = backfillService;
    }
    
    @PostMapping("/closing-lines")
    public Map<String, Object> backfillClosingLines() {
        return backfillService.backfillClosingLines();
    }
}
