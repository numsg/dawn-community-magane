package com.gsafety.dawn.community.manage.service.serviceimpl;

import com.gsafety.dawn.community.common.util.DateUtil;
import com.gsafety.dawn.community.manage.contract.model.RequestModel;
import com.gsafety.dawn.community.manage.contract.model.refactor.PersonBase;
import com.gsafety.dawn.community.manage.contract.model.refactor.TroubleshootRecord;
import com.gsafety.dawn.community.manage.contract.service.TimerService;
import com.gsafety.dawn.community.manage.contract.service.refactor.TroubleshootRecordService;
import com.gsafety.dawn.community.manage.service.entity.DSourceDataEntity;
import com.gsafety.dawn.community.manage.service.entity.DailyTroubleshootRecordEntity;
import com.gsafety.dawn.community.manage.service.entity.DataSourceEntity;
import com.gsafety.dawn.community.manage.service.repository.DSourceDataRepository;
import com.gsafety.dawn.community.manage.service.repository.DailyTroubleshootRecordRepository;
import com.gsafety.dawn.community.manage.service.repository.DataSourceRepository;
import com.gsafety.dawn.community.manage.service.repository.refactor.TroubleshootRecordRepository;
import com.gsafety.java.common.utils.HttpClientUtil;
import com.gsafety.java.common.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.gsafety.java.common.utils.JsonUtil.toJson;

@Service
@Transactional
public class DataCollectionServiceImpl {
    @Value("${access.data-collection}")
    private String dataCollectionUrl;

   // @Value("${app.villageId}")
   // private String villageId;

    @Value("${app.pageSize}")
    private Integer pageSize;

    // 男
    private static final String maleId = "3f265ff3-75b8-49f1-9669-4506535a500c";
    //女
    private static final String femaleId = "2ae58f9e-65f2-4f2a-8244-ac01d668b7b5";

    // 其他症状
    //0:无
    private static final String noId = "26d8ca3b-8b7b-4109-a992-8052f9defb9d";
    //1:乏力
    private static final String feebleId = "5e647e8b-6396-4e56-854b-ed1be1c60ae3";
    //2:干咳
    private static final String hooseId = "e081de69-a984-4abb-a2a1-ed9996a63917";
    //3:肌痛
    private static final String musclePainId = "0e2adfab-bb9d-4ef9-a4db-6a5baa2d7788";
    //4:寒战
    private static final String arguedId = "be64af80-00ee-4ea5-94ba-3246d7d16230";
    //5:呼吸困难
    private static final String breathId = "b01b1538-31f3-428b-9e33-531af1f40f83";
    //6:咽痛
    private static final String soreThroatId = "3dbbfced-ca40-437b-91d5-70f0c12b32ea";
    //7:头疼
    private static final String headacheId = "ee377b8b-220f-4e8b-a6ce-fe47082e227b";
    //8:眩晕
    private static final String dizzyId = "ce0902a6-e3ee-4055-b1c3-d41a365a5522";
    //9:腹痛
    private static final String bellyacheId = "e1ed1513-cfa1-4343-aa50-a4ab76c09c46";
    //10:腹泻
    private static final String diarrhoeaId = "5f98d8f5-40e9-427d-90c0-e1849a87ae19";
    //11:恶心
    private static final String nauseaId = "1720a8db-a0b4-43d2-8f52-3a4df8e3fca5";
    //12:呕吐
    private static final String vomitId = "2fdd7934-7823-4227-8712-7488ebc7704e";
    //13:鼻塞
    private static final String nasalObstructionId = "f0671f8a-233f-44a5-a785-9e9ab0c18fe8";

    //分类诊疗医疗意见：
    // 0:确诊患者，
    private static final String confirmedPatientId = "c9eedfbc-ae5a-40b7-8a62-c049c5678deb";
    //1:疑似患者，6293737c-5775-426d-9845-f919eafba1be
    private static final String suspectedPatientId = "6293737c-5775-426d-9845-f919eafba1be";
    //3:一版发热患者，c0bb07b2-db54-4fd1-89d3-20b0672a2779
    private static final String feverPatientId = "c0bb07b2-db54-4fd1-89d3-20b0672a2779";

    //2:CT诊断肺炎患者 c9eedfbc-ae5a-40b7-8a62-c049c5678deb
    private static final String CTPatientId = "c9eedfbc-ae5a-40b7-8a62-c049c5678deb";
    //4:密切接触者  6293737c-5775-426d-9845-f919eafba1be
    private static final String contractPatientId = "6293737c-5775-426d-9845-f919eafba1be";


    private String url = "/search/v2";

    // 杨桥湖社区id
    // private static final String communityDataSourceId = "a2e01f0e-6c86-4a41-bcf3-c07c1ffa2f82";
    // 其他状况id
    private static final String otherSymptomId = "582daff0-56a5-45a4-9ca7-dc098c688753";
    @Autowired
    private HttpClientUtil httpClientUtil;

    @Autowired
    DailyTroubleshootRecordRepository recordRepository;

    @Autowired
    private DSourceDataRepository dSourceDataRepository;

    @Autowired
    private TroubleshootRecordRepository troubleshootRecordRepository;

    @Autowired
    private TroubleshootRecordService troubleshootRecordService;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    private Date startTimeDate = DateUtil.getDayStartDate();
    // 小区
    //private List<DSourceDataEntity> plots = new ArrayList<>();
    
    // 其他诊断状况
    private List<DSourceDataEntity> otherSymptoms = new ArrayList<>();

    /**
     * 定时从外部服务查询数据。
     */
    public void timeQuery() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endTimeDate = DateUtil.getDayEndDate();

        System.out.println(formatter.format(startTimeDate));
        System.out.println(formatter.format(endTimeDate));
        RequestModel requestModel = new RequestModel();
        requestModel.setPageSize(pageSize);
        requestModel.setKeyWords("");
        // 改为查询所有社区
        requestModel.setStartDate(startTimeDate);
        requestModel.setEndDate(DateUtil.getDayEndDate());
        Map map = httpClientUtil.httpPost(dataCollectionUrl + url, requestModel, Map.class);
        if (map.get("data") != null && map.get("success").equals(true)) {
            Map data = JsonUtil.fromJson(toJson(map.get("data")), Map.class);
            Integer total = Integer.parseInt(JsonUtil.fromJson(toJson(data.get("total")), String.class));

            int pageTotal = 1;
            if (total > pageSize) {
                if (total % pageSize == 0) {
                    pageTotal = total / pageSize;
                } else {
                    pageTotal = total / pageSize + 1;
                }
            }

           // plots = dSourceDataRepository.queryByDataSourceIdOrderBySortAsc(communityDataSourceId);
            otherSymptoms = dSourceDataRepository.queryByDataSourceIdOrderBySortAsc(otherSymptomId);

            for (int i = 1; i <= pageTotal; i++) {
                requestModel.setPageNo(i);
                requestModel.setStartDate(startTimeDate);
                getDataFromAccess(requestModel);
            }
        }
    }

    public Boolean getDataFromAccess(RequestModel requestModel) {
        Map map = httpClientUtil.httpPost(dataCollectionUrl + url, requestModel, Map.class);
        if (map.get("data") == null || map.get("success").equals(false)) {
            return false;
        }
        // 数据转换
        Map data = JsonUtil.fromJson(toJson(map.get("data")), Map.class);
        List list = JsonUtil.fromJson(toJson(data.get("list")), List.class);
        if (list.isEmpty()) {
            return false;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //  List<TroubleshootRecord> records=new ArrayList<>();
        for (Object obj : list) {

            Map objMap = JsonUtil.fromJson(toJson(obj), Map.class);
            Object recordId = objMap.get("id");
            Object personName = objMap.get("name");
            Object phone = objMap.get("phone");
            Object sex = objMap.get("sex");
            if (personName == null || phone == null || sex == null || troubleshootRecordRepository.existsById(recordId.toString())) {
                continue;
            }
            if ("".equals(personName) || "".equals(phone) || "".equals(sex)) {
                continue;
            }

            TroubleshootRecord record = new TroubleshootRecord();

            record.setId(recordId.toString());
            record.setBuilding(objMap.get("building") != null ? objMap.get("building").toString() : "其它");
            record.setIsByPhone(true);
            //社区id
            record.setDistrictCode(objMap.get("currentVillage").toString());
            record.setMultiTenancy(objMap.get("currentVillage").toString());
            record.setNote(objMap.get("remark") != null ? objMap.get("remark").toString() : "");
            record.setRoomNo(objMap.get("roomNumber") != null ? objMap.get("roomNumber").toString() : "");
            record.setUnitNumber(objMap.get("unit") != null ? objMap.get("unit").toString() : "其它");

            //record.setLeaveArea();
            // record.setCreateDate();

            if (objMap.get("age") != null) {
                record.setAge(Integer.parseInt(objMap.get("age").toString()));
            }
            if (objMap.get("medicalAdvice") != null) {
                //   record.setConfirmed_diagnosis(convertMedicalOpinion(objMap.get("medicalAdvice").toString()));
                record.setMedicalOpinion(convertMedicalOpinion(objMap.get("medicalAdvice").toString()));
            }
            try {
                record.setCreateTime(formatter.parse(objMap.get("createTime").toString()));  //字符串转换
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (objMap.get("touchPersonIsolation") != null) {
                record.setIsContact(objMap.get("touchPersonIsolation").toString().equals("1"));
            }
            if (objMap.get("fever") != null) {
                record.setIsExceedTemp(objMap.get("fever").toString().equals("1"));
            }
            if (objMap.get("symptom") != null) {
                record.setOtherSymptoms(splitOtherSymptom(objMap.get("symptom").toString()));
                //entity.setOtherSymptoms(converOtherSymptoms(objMap.get("symptom").toString()));
            }

            if (objMap.get("communityCode") != null && dSourceDataRepository.existsById(objMap.get("communityCode").toString())) {
                record.setPlot(objMap.get("communityCode").toString());
            } else {
                Random random = new Random();  // 此处带改
                String villageCode = objMap.get("currentVillage").toString();
                DataSourceEntity village=dataSourceRepository.queryByDescription(villageCode);
                List<DSourceDataEntity> plots = dSourceDataRepository.queryByDataSourceIdOrderBySortAsc(village.getId());
                record.setPlot(plots.get(random.nextInt(plots.size())).getId());
            }

            PersonBase personBase = new PersonBase();
            personBase.setAddress(objMap.get("residence").toString());
            personBase.setName(personName.toString());
            personBase.setPhone(phone.toString());
            personBase.setSex(objMap.get("sex").toString().equals("0") ? maleId : femaleId);
            personBase.setIdentificationNumber(objMap.get("idNumber") != null ? objMap.get("idNumber").toString() : "");
            record.setMultiTenancy(objMap.get("currentVillage").toString());

            startTimeDate = record.getCreateTime();
            record.setPersonBase(personBase);

            troubleshootRecordService.add(record);
            //  records.add(record);
        }
        return true;
    }


    private String splitOtherSymptom(String symptoms) {
        StringBuffer symptomIds = new StringBuffer();
        String[] arrays = symptoms.split("-");
        for (String arr : arrays) {
            otherSymptoms.forEach(data -> {
                if (data.getName().equals(arr)) {
                    symptomIds.append(data.getId());
                    symptomIds.append(",");
                }
            });
        }
        if (symptomIds.length() > 0) {
            symptomIds.deleteCharAt(symptomIds.length() - 1);
        }
        return symptomIds.toString();
    }


    private String convertMedicalOpinion(String number) {
        String result = null;
        if (number.equals("0")) {
            result = confirmedPatientId;
        } else if (number.equals("1")) {
            result = suspectedPatientId;
        } else if (number.equals("2")) {
            result = CTPatientId;
        } else if (number.equals("3")) {
            result = feverPatientId;
        } else if (number.equals("4")) {
            result = contractPatientId;
        }

        return result;
    }

    private String converOtherSymptoms(String number) {
        String result = null;
        if (number.equals("0")) {
            result = noId;
        } else if (number.equals("1")) {
            result = feebleId;
        } else if (number.equals("2")) {
            result = hooseId;
        } else if (number.equals("3")) {
            result = musclePainId;
        } else if (number.equals("4")) {
            result = arguedId;
        } else if (number.equals("5")) {
            result = breathId;
        } else if (number.equals("6")) {
            result = soreThroatId;
        } else if (number.equals("7")) {
            result = headacheId;
        } else if (number.equals("8")) {
            result = dizzyId;
        } else if (number.equals("9")) {
            result = bellyacheId;
        } else if (number.equals("10")) {
            result = diarrhoeaId;
        } else if (number.equals("11")) {
            result = nauseaId;
        } else if (number.equals("12")) {
            result = vomitId;
        } else if (number.equals("13")) {
            result = nasalObstructionId;
        }
        return result;
    }
}
