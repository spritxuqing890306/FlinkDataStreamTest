package com.ty.app;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Gjf
 * @create 2023/2/21 13:46
 *
 *
 * 自定义写入mysql,演练
 *
 */


public class CustomSinkFunction extends RichSinkFunction<TAlarmAlarminfo> {
    private DruidPooledConnection connectionPerformance;
    private PreparedStatement ps;
    private DruidDataSource dataSourcePerformance;

    @Override
    public void open(Configuration parameters) throws Exception {
        //todo 1 连接不同的业务库
        dataSourcePerformance = DruidDSUtil.createDataSource("test");
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    @Override
    public void invoke(TAlarmAlarminfo value, Context context) throws Exception {
            connectionPerformance = dataSourcePerformance.getConnection();
            Long cloudalarmcustomid = value.getFromCustomID();
            Long organizationId = value.getOrganizationId();
            Integer alarmHour = value.getAlarmHour();
            String alarmDay = value.getAlarmDay();
            Long alarmNum = value.getAlarmNum();
            String sql;

            //如果已经存在，则update，否则replace
            sql = "select 1 from ads_alarm_count"
                    + " where cloudalarm_orgid=" + organizationId
                    + " and  alarm_date='" + alarmDay +
                    "' and alarm_hour="+alarmHour;

            ps = connectionPerformance.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                //todo 如果查询到结果，更新操作 update +1
                sql="update ads_alarm_count set alarm_number="+alarmNum+
                        " where cloudalarm_orgid="+organizationId+
                        " and alarm_date='"+alarmDay+
                        "' and alarm_hour=" + alarmHour;
                System.out.println("update>>>"+sql);

                try {
                    ps = connectionPerformance.prepareStatement(sql);
                    ps.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    ps.close();
                    connectionPerformance.close();
                }
            } else {
                //todo 如果没有查询到结果，插入操作 put 1
                sql = "replace into ads_alarm_count values(?,?,?,?,?)";
                System.out.println("insert>>>"+sql);
                try {
                    ps = connectionPerformance.prepareStatement(sql);
                    ps.setLong(1,cloudalarmcustomid);
                    ps.setLong(2,organizationId);
                    ps.setString(3,alarmDay);
                    ps.setLong(4,alarmHour);
                    ps.setLong(5,alarmNum);
                    ps.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    ps.close();
                    connectionPerformance.close();
                }
            }
    }
}
