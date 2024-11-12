package Analysis.Team2.controller;

import Analysis.Team2.service.AnalysisService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"http://changdoc.s3-website-ap-southeast-1.amazonaws.com", "http://localhost:3000"}, allowCredentials = "true")
@RequestMapping("/analysis")
public class MainController {

    @Autowired
    AnalysisService analysisService;

    @PostMapping("/main")
    public CompletableFuture<ResponseEntity<String>> mainRequest(@RequestBody String requestBody) {
        JSONObject jsonRequest = new JSONObject(requestBody);
        String city = jsonRequest.getString("region_city");
        String dong = jsonRequest.getString("region_dong");
        String category1 = jsonRequest.getString("category1");
        String category2 = jsonRequest.getString("category2");
        List<String> customerAge = jsonRequest.getJSONArray("customerAge").toList().stream().map(Object::toString).toList();

        // Call stored procedure to get code
        CompletableFuture<String> codeFuture = analysisService.getCodeAsync(category1, category2);

        return codeFuture.thenCompose(code -> {
            CompletableFuture<List<Map<String, Object>>> daySalesFuture = analysisService.getDaySalesAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> genderAgeDataFuture = analysisService.getGenderAgeDistributionAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> hourlySalesFuture = analysisService.getHourlySalesAsync(city, dong, category1, category2);
            CompletableFuture<String> maxLiftConsequentFuture = analysisService.getMaxLiftConsequentAsync(category1, category2);
            CompletableFuture<Map<String, String>> indicatorFuture = analysisService.getIndicatorAsync(city, dong);
            CompletableFuture<Map<String, Integer>> merchantFuture = analysisService.getMerchantAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> merchantCntFuture = analysisService.getMerchantCntAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> yearAmtFuture = analysisService.getYearAmtAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> unitPriceCntFuture = analysisService.getUnitPriceCntAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> recentMonthlySalesFuture = analysisService.getRecentMonthlySalesAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> averageFlowpopFuture = analysisService.getAverageFlowpopAsync(city, dong);
            CompletableFuture<List<Map<String, Object>>> customerPercentageChangeFuture = analysisService.getCustomerPercentageChangeAsync(city, dong, category1, category2);
            CompletableFuture<Map<String, Object>> fullBusinessAnalysisFuture = analysisService.getFullBusinessAnalysisAsync(city, dong, category1, category2);
            CompletableFuture<List<Map<String, Object>>> estimatedAmtFuture = analysisService.getEstimatedAmtAsync(city, dong, category1, category2);

            return CompletableFuture.allOf(daySalesFuture, genderAgeDataFuture, hourlySalesFuture, maxLiftConsequentFuture, indicatorFuture, merchantFuture,
                            merchantCntFuture, yearAmtFuture, unitPriceCntFuture, recentMonthlySalesFuture, averageFlowpopFuture, customerPercentageChangeFuture, fullBusinessAnalysisFuture, estimatedAmtFuture)
                    .thenCompose(v -> {
                        List<Map<String, Object>> daySalesData = daySalesFuture.join();
                        List<Map<String, Object>> genderAgeData = genderAgeDataFuture.join();
                        List<Map<String, Object>> hourlySalesData = hourlySalesFuture.join();
                        String maxLiftConsequent = maxLiftConsequentFuture.join();
                        Map<String, String> indicator = indicatorFuture.join();
                        Map<String, Integer> merchantData = merchantFuture.join();
                        List<Map<String, Object>> merchantCntData = merchantCntFuture.join();
                        List<Map<String, Object>> yearAmtData = yearAmtFuture.join();
                        List<Map<String, Object>> unitPriceCntData = unitPriceCntFuture.join();
                        List<Map<String, Object>> recentMonthlySalesData = recentMonthlySalesFuture.join();
                        List<Map<String, Object>> averageFlowpopData = averageFlowpopFuture.join();
                        List<Map<String, Object>> customerPercentageChangeData = customerPercentageChangeFuture.join();
                        Map<String, Object> fullBusinessAnalysisData = fullBusinessAnalysisFuture.join();
                        List<Map<String, Object>> estimatedAmtData = estimatedAmtFuture.join();

                        Map<String, Object> predictionInput = new HashMap<>();
                        if (!estimatedAmtData.isEmpty()) {
                            Map<String, Object> data = estimatedAmtData.get(0);
                            predictionInput.put("admi_cty_no", new Long[]{(Long) data.get("admiNum")});
                            predictionInput.put("card_tpbuz_cd", new String[]{(String) data.get("tpbuzNum")});
                            predictionInput.put("amt", new int[]{((Number) data.get("amt")).intValue()});
                            predictionInput.put("cnt", new Double[]{((Number) data.get("cnt")).doubleValue()});
                            predictionInput.put("TOTAL_POPULATION", new Double[]{((Number) data.get("totalPop")).doubleValue()});
                            predictionInput.put("store_avg_period", new Double[]{((Number) data.get("operPer")).doubleValue()});
                            predictionInput.put("shutdown_avg_period", new Double[]{((Number) data.get("closPer")).doubleValue()});
                            predictionInput.put("changing_tag", new String[]{(String) data.get("indc")});
                        }

                        CompletableFuture<Map<String, Object>> predictionFuture = analysisService.getPredictionAsync(predictionInput);
                        CompletableFuture<List<Map<String, Object>>> timeSeriesPredictionFuture = analysisService.getTimeSeriesPredictionAsync(city, code);

                        return CompletableFuture.allOf(predictionFuture, timeSeriesPredictionFuture)
                                .thenApply(v2 -> {
                                    Map<String, Object> predictionResult = predictionFuture.join();
                                    List<Map<String, Object>> timeSeriesPredictionResult = timeSeriesPredictionFuture.join();

                                    JSONObject responseJSON = new JSONObject();

                                    JSONArray daySalesArray = new JSONArray();
                                    for (Map<String, Object> daySale : daySalesData) {
                                        JSONObject daySaleJSON = new JSONObject();
                                        daySaleJSON.put("day", daySale.get("day"));
                                        daySaleJSON.put("totalAmt", daySale.get("totalAmt"));
                                        daySalesArray.put(daySaleJSON);
                                    }

                                    JSONArray genderAgeArray = new JSONArray();
                                    for (Map<String, Object> data : genderAgeData) {
                                        JSONObject dataJSON = new JSONObject();
                                        dataJSON.put("sex", data.get("sex"));
                                        dataJSON.put("ageLabel", data.get("ageLabel"));
                                        dataJSON.put("percentage", data.get("percentage"));
                                        genderAgeArray.put(dataJSON);
                                    }

                                    JSONArray hourlySalesArray = new JSONArray();
                                    for (Map<String, Object> hourSale : hourlySalesData) {
                                        JSONObject hourSaleJSON = new JSONObject();
                                        hourSaleJSON.put("hourLabel", hourSale.get("hourLabel"));
                                        hourSaleJSON.put("amtPercentage", hourSale.get("amtPercentage"));
                                        hourSaleJSON.put("cntPercentage", hourSale.get("cntPercentage"));
                                        hourlySalesArray.put(hourSaleJSON);
                                    }

                                    JSONArray merchantCntArray = new JSONArray();
                                    for (Map<String, Object> merchantCnt : merchantCntData) {
                                        JSONObject merchantCntJSON = new JSONObject();
                                        merchantCntJSON.put("년월", merchantCnt.get("v_ta_ym"));
                                        merchantCntJSON.put("상가수", merchantCnt.get("v_mer_cnt"));
                                        merchantCntArray.put(merchantCntJSON);
                                    }

                                    JSONArray yearAmtArray = new JSONArray();
                                    for (Map<String, Object> yearAmt : yearAmtData) {
                                        JSONObject yearAmtJSON = new JSONObject();
                                        yearAmtJSON.put("년월", yearAmt.get("v_ta_ymd"));
                                        yearAmtJSON.put("월매출액합계", yearAmt.get("v_amt"));
                                        yearAmtArray.put(yearAmtJSON);
                                    }

                                    JSONArray unitPriceCntArray = new JSONArray();
                                    for (Map<String, Object> unitPriceCnt : unitPriceCntData) {
                                        JSONObject unitPriceCntJSON = new JSONObject();
                                        unitPriceCntJSON.put("년월", unitPriceCnt.get("v_ta_ymd"));
                                        unitPriceCntJSON.put("매출건수", unitPriceCnt.get("v_cnt"));
                                        unitPriceCntJSON.put("상가당매출액(평균)", unitPriceCnt.get("v_unit_price"));
                                        unitPriceCntArray.put(unitPriceCntJSON);
                                    }

                                    JSONArray recentMonthlySalesArray = new JSONArray();
                                    for (Map<String, Object> monthlySale : recentMonthlySalesData) {
                                        JSONObject monthlySaleJSON = new JSONObject();
                                        monthlySaleJSON.put("salesMonth", monthlySale.get("salesMonth"));
                                        monthlySaleJSON.put("totalSales", monthlySale.get("totalSales"));
                                        recentMonthlySalesArray.put(monthlySaleJSON);
                                    }

                                    JSONArray averageFlowpopArray = new JSONArray();
                                    for (Map<String, Object> flowpop : averageFlowpopData) {
                                        JSONObject flowpopJSON = new JSONObject();
                                        flowpopJSON.put("ageGroup", flowpop.get("ageGroup"));
                                        flowpopJSON.put("mCntAvg", flowpop.get("mCntAvg"));
                                        flowpopJSON.put("fCntAvg", flowpop.get("fCntAvg"));
                                        averageFlowpopArray.put(flowpopJSON);
                                    }

                                    JSONArray customerPercentageChangeArray = new JSONArray();
                                    for (Map<String, Object> customerChange : customerPercentageChangeData) {
                                        JSONObject customerChangeJSON = new JSONObject();
                                        customerChangeJSON.put("sex", customerChange.get("sex"));
                                        customerChangeJSON.put("age", customerChange.get("age"));
                                        customerChangeJSON.put("totalCntPrevious", customerChange.get("totalCntPrevious"));
                                        customerChangeJSON.put("totalCntCurrent", customerChange.get("totalCntCurrent"));
                                        customerChangeJSON.put("percentageChange", customerChange.get("percentageChange"));
                                        customerPercentageChangeArray.put(customerChangeJSON);
                                    }

                                    responseJSON.put("genderAgeDistribution", genderAgeArray);
                                    responseJSON.put("daySales", daySalesArray);
                                    responseJSON.put("hourlySales", hourlySalesArray);
                                    responseJSON.put("indicator", new JSONObject(indicator));
                                    responseJSON.put("merchantData", new JSONObject(merchantData));
                                    responseJSON.put("merchantCntData", merchantCntArray);
                                    responseJSON.put("yearAmtData", yearAmtArray);
                                    responseJSON.put("unitPriceCntData", unitPriceCntArray);
                                    responseJSON.put("recentMonthlySales", recentMonthlySalesArray);
                                    responseJSON.put("averageFlowpop", averageFlowpopArray);
                                    responseJSON.put("customerPercentageChange", customerPercentageChangeArray);
                                    responseJSON.put("fullBusinessAnalysis", new JSONObject(fullBusinessAnalysisData));
                                    responseJSON.put("predictionResult", predictionResult);
                                    responseJSON.put("timeSeriesPrediction", new JSONArray(timeSeriesPredictionResult));

                                    System.out.println("asdfasdfasdf");
                                    System.out.println(predictionResult);
                                    System.out.println("");
                                    System.out.println(timeSeriesPredictionResult);
                                    String gptInputContent = generateGPTInputContent(genderAgeData, customerAge, predictionResult, timeSeriesPredictionResult);
                                    CompletableFuture<String> gptResponseFuture = analysisService.getGPTResponseAsync(gptInputContent);
                                    String gptResponse = gptResponseFuture.join();

                                    JSONObject gptResponseJSON = new JSONObject(gptResponse);
                                    String gptContent = gptResponseJSON.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                                    responseJSON.put("gptResponse", gptContent);

                                    responseJSON.put("status", "success");
                                    responseJSON.put("message", "데이터 전달 성공");

                                    return ResponseEntity.ok(responseJSON.toString());
                                });
                    });
        });
    }

    private String generateGPTInputContent(List<Map<String, Object>> genderAgeData, List<String> customerAge, Map<String, Object> predictionResult, List<Map<String, Object>> timeSeriesPredictionResult) {
        StringBuilder inputContent = new StringBuilder();
        inputContent.append("나는 ");
        for (int i = 0; i < customerAge.size(); i++) {
            inputContent.append(customerAge.get(i)).append("를");
            if (i < customerAge.size() - 1) {
                inputContent.append(", ");
            } else {
                inputContent.append(" 타겟으로 창업을 준비중이야. ");
            }
        }

        inputContent.append("내가 하려는 창업에 대한 분석 데이터를 너에게 공유할테니, 차근차근 분석하되 답변에 포함하지는 말고 이후에 내가 원하는 질문에 대한 답변만 해줬으면 해. ");

        inputContent.append("첫번째, 내가 창업하고자 하는 지역에서 해당 업종의 전체 월 매출 합산 예상 데이터야. ");
        inputContent.append("예상 매출액 : ").append(predictionResult.get("predicted_value")).append("원. ");
//        inputContent.append("예상 매출액 : 500000000원. ");

        inputContent.append("두번째, 내가 창업하고자 하는 지역에서 해당 업종의 일별 예상 매출액 추이 모델을 통해 도출된 데이터야. ");
        for (Map<String, Object> data : timeSeriesPredictionResult) {
            inputContent.append(data.get("timestamp")).append(" : ")
                    .append(data.get("mean")).append(", ");
        }

        inputContent.append("세번째, 주고객 연령 적합도를 확인하기 위한, 내가 창업하고자 하는 업종의 소비자의 성별/연령대별 고객 비중 데이터야. ");
        for (Map<String, Object> data : genderAgeData) {
            inputContent.append(data.get("sex")).append(" | ").append(data.get("ageLabel")).append(" | ").append(data.get("percentage")).append("%, ");
        }

        inputContent.append("위 데이터들을 기반으로 나의 창업 계획에 대한 너의 의견은 어떠한지 듣고싶어. ");
        inputContent.append("대답은 미사여구를 붙이지말고, 업종의 전체 월 매출 합산 예상 데이터, 업종의 일별 매출액 추이 예상 데이터, 주고객 연령 적합도 각각에 대한 분석을 바탕으로 창업에 대해 부정적인지, 보통인지, 긍정적인지를 신호등색깔(적색,황색,녹색)로 각각에 대해 판단해서 답변해줘. 두번째, 세번째를 분석할때는 현재 날짜와 대비해서 미래에 어떤 예상을 보이는지 엄격하게 살펴봐야해.");
        inputContent.append("답변하는 방식은 예를 들어, 예상 매출 데이터는 '적색', 예상 매출액 추이 데이터는 '황색', 주고객 연령 적합도는 '녹색' 이면, 답변은 설명과 미사여구를 붙이지말고 '적색,황색,녹색' 이라고 답변해줘. ");
        inputContent.append("숨을 깊게 들이마시고, 차근차근 확인해봐. 잘 수행하면 팁으로 50$ 줄게. ");

//        for (Map.Entry<String, Object> entry : predictionResult.entrySet()) {
//            inputContent.append(entry.getKey()).append(" | ").append(entry.getValue()).append(" ");
//        }

        return inputContent.toString();
    }
}
