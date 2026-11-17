# Measuring accuracy and cost

No accuracy or cost number is reported in this repo because none has been measured.

## Accuracy
1. After a real refresh, pick at least 100 collected jobs (a random sample, not only obvious ones).
2. For each, a human decides: is it genuinely open to freshers/interns, its category (from `Taxonomy.CATEGORIES`) and employment type.
3. Fill `docs/eval/labelled-jobs.csv` (header already present):
   `title,company,location,description,expected_fresher_eligible,expected_category,expected_employment_type`
4. `POST /api/admin/evaluate` returns accuracy for fresher-eligibility, category and employment type, plus the first mismatches.
Report the sample size alongside any percentage.

## Cost per 1,000 processed jobs
Every LLM call (including failed parses) is stored with provider-reported input/output tokens and the cost from `LLM_INPUT_PRICE_PER_M` / `LLM_OUTPUT_PRICE_PER_M`. `GET /api/admin/metrics/cost` returns `costUsdPer1000Jobs = total cost / calls * 1000`. Set the prices from your provider's current price list first, or the cost reads 0.
