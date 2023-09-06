package com.ty.app;

import lombok.Data;

import java.util.Date;

/**
 * @author xu.qing
 * @create 2023-09-05 13:39
 * @Description
 */
@Data
class AdsAlarmCount {
    private Long cloudalarmCustomid;
    private Long cloudalarmOrgid;
    private String alarmDate;
    private Integer alarmHour;
    private Long alarmNumber;

    AdsAlarmCount(Long cloudalarmCustomid, Long cloudalarmOrgid, String alarmDate, Integer alarmHour, Long alarmNumber) {
        this.cloudalarmCustomid = cloudalarmCustomid;
        this.cloudalarmOrgid = cloudalarmOrgid;
        this.alarmDate = alarmDate;
        this.alarmHour = alarmHour;
        this.alarmNumber = alarmNumber;
    }
}
