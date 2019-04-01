package com.tango.controllers;

import com.tango.domain.AggregatedStat;
import com.tango.domain.Stat;
import com.tango.services.StatCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.validation.Valid;

@RestController
public class StatController {

    @Nonnull
    private final StatCalculatorService statCalculatorService;

    @Autowired
    public StatController(@Nonnull StatCalculatorService statCalculatorService) {
        this.statCalculatorService = statCalculatorService;
    }

    //DeferredResult is unnecessary in this case because method add is fast
    @RequestMapping(path = "/transactions", method = RequestMethod.POST)
    public ResponseEntity<?> registryTransaction(@Valid @RequestBody Stat stat) {
        statCalculatorService.add(stat);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @RequestMapping(path = "/statistics", method = RequestMethod.GET)
    public ResponseEntity<AggregatedStat> getStatistics() {
        return ResponseEntity.ok(statCalculatorService.getCurrentAggStat());
    }
}
