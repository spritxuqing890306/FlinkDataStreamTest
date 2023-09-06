package com.ty.app;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssignerSupplier;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;


public class Flink_CDC {
    public static void main(String[] args) throws Exception {
        //TODO 设为root用户
        // System.setProperty("HADOOP_USER_NAME", "root");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        // env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //TODO 设置ck   regionalattrischeck
        // env.setStateBackend(new FsStateBackend("hdfs:///flink/checkpoint/g/Flink_CDC"));
        // env.enableCheckpointing(Time.minutes(10).toMilliseconds());
        // env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointTimeout(Time.minutes(5).toMilliseconds());
        // env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);
        // env.getCheckpointConfig().setMinPauseBetweenCheckpoints(Time.minutes(5).toMilliseconds());
        // env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        //TODO 1 读取测试库数据

        //198
        MySqlSource<String> sourceFunction = MySqlSource.<String>builder()
                .hostname("192.168.0.7")
                .username("root")
                .password("ty123456")
                .port(3306)
                .databaseList("test")
                // .tableList("t_sys_organization")
                .tableList("test.t_alarm_alarminfo")
                .deserializer(new CustomDeserialization())
                .includeSchemaChanges(true)
                .startupOptions(StartupOptions.latest())
                .build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DataStreamSource<String> streamSource = env.fromSource(sourceFunction, WatermarkStrategy.noWatermarks(),"mysql");

        // streamSource.print("stream");
        // 过滤掉非insert的数据
        SingleOutputStreamOperator<JSONObject> insertAlarmStream = streamSource.map(new MapFunction<String,JSONObject>() {
            @Override
            public JSONObject map(String s) throws Exception {
                return JSON.parseObject(s);
            }
        }).filter(new FilterFunction<JSONObject>() {
            @Override
            public boolean filter(JSONObject o) throws Exception {
                return "insert".equals(o.get("type"));
            }
        });
        SingleOutputStreamOperator<JSONObject> jsonObjectSingleOutputStreamOperator = insertAlarmStream.assignTimestampsAndWatermarks(WatermarkStrategy.<JSONObject>forBoundedOutOfOrderness(Duration.ofMillis(1000)).withTimestampAssigner((event,timestamp)->event.getJSONObject("after").getLong("EXT_ALARMTIME")));

        jsonObjectSingleOutputStreamOperator.print();

        SingleOutputStreamOperator<TAlarmAlarminfo> alarmInfoBeanStream = jsonObjectSingleOutputStreamOperator.flatMap(new FlatMapFunction<JSONObject, TAlarmAlarminfo>() {
            @Override
            public void flatMap(JSONObject o, Collector<TAlarmAlarminfo> collector) throws Exception {
                JSONObject after = (JSONObject)o.get("after");
                Integer cloudalarmCustomid = (Integer)after.get("FromCustomID");
                Integer platformOrgid = (Integer)after.get("ORGANIZATIONID");
                Date alarmDate = new Date((Long)after.get("EXT_ALARMTIME"));
                String alarmDay = simpleDateFormat.format(alarmDate);
                Integer alarmHour = alarmDate.getHours();
                collector.collect(new TAlarmAlarminfo((long) cloudalarmCustomid.intValue(), (long)platformOrgid.intValue(),alarmDay,alarmHour));
            }
        });

        SingleOutputStreamOperator<TAlarmAlarminfo> alarmNumStream = alarmInfoBeanStream.keyBy(new KeySelector<TAlarmAlarminfo, String>() {
            @Override
            public String getKey(TAlarmAlarminfo o) {
                return o.getOrganizationId() + "-" + o.getAlarmDay() + "-" + o.getAlarmHour();
            }
        }).window(TumblingEventTimeWindows.of(Time.seconds(10))).sum("alarmNum");

        alarmNumStream.print("alarmNumStream");

        alarmNumStream.addSink(new CustomSinkFunction());


        //TODO 4 启动程序
        env.execute();

    }
}
