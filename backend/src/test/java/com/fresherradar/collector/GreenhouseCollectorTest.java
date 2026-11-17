
package com.fresherradar.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GreenhouseCollectorTest {

    @Test
    void testBulkGreenhouseFetching() {

        String boards =
                "instawork,karya,groww,formaaiinc3,zinnov," +
                "phonepe,inmobi,gitlab,stripe,rubrik," +
                "fivetran,devrev,elastic,coinbase,twilio," +
                "druva,airbnb,bloomreach,postman,discord," +
                "razorpay,observeai,worldquant,tekion,netradyne";

        GreenhouseCollector collector = new GreenhouseCollector(
                new ObjectMapper(),
                true,
                boards
        );

        List<RawJob> jobs = collector.collect();

        Map<String, Long> jobCounts = jobs.stream()
                .collect(Collectors.groupingBy(
                        RawJob::company,
                        Collectors.counting()
                ));

        System.out.println("\n========== GREENHOUSE BULK TEST ==========");

        System.out.println("Total boards tested: "
                + boards.split(",").length);

        System.out.println("Total jobs fetched: " + jobs.size());

        System.out.println("\n----- BOARD-WISE RESULTS -----");

        jobCounts.forEach((board, count) ->
                System.out.println(board + ": " + count + " jobs")
        );

        long successfulBoards = jobCounts.values().stream()
                .filter(count -> count > 0)
                .count();

        System.out.println("\n----- SUMMARY -----");

        System.out.println("Boards returning jobs: " + successfulBoards);

        System.out.println("Boards with zero jobs or failed: "
                + (boards.split(",").length - successfulBoards));

        System.out.println("===========================================\n");
    }
}
