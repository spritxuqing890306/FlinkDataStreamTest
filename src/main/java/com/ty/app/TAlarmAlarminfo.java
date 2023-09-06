package com.ty.app;

import lombok.Data;

import java.math.BigInteger;

/**
 * @author xu.qing
 * @create 2023-09-05 17:20
 * @Description
 */
@Data
public class TAlarmAlarminfo {

    private Long organizationId;
    private Integer alarmHour;
    private Long fromCustomID;
    private String alarmDay;
    private Long alarmNum;

    public TAlarmAlarminfo(Long fromCustomID, Long organizationId, String alarmDay, Integer alarmHour) {
        this.organizationId = organizationId;
        this.alarmHour = alarmHour;
        this.alarmDay = alarmDay;
        this.fromCustomID = fromCustomID;
        this.alarmNum = 1L;
    }

    public TAlarmAlarminfo() {
    }
}