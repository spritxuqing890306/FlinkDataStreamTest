package com.ty.app;

import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import io.debezium.data.Envelope;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;

/**
 * @author Gjf
 * @create 2022/6/30 15:14
 */

//@SuppressWarnings("unchecked")
public class CustomDeserialization implements DebeziumDeserializationSchema<String> {
    @Override
    public void deserialize(SourceRecord sourceRecord, Collector<String> collector) throws Exception {
        //用来存储输出数据
        JSONObject resultObject = new JSONObject();

        //TODO 1 获取数据库名，数据表名
        String dataValue = sourceRecord.topic();
        //切分字符串
        String[] StringValue = dataValue.split("\\.");
        String dataName = StringValue[1];
        String tableName = StringValue[2];

        //TODO 2 获取type类型
        Envelope.Operation operation = Envelope.operationFor(sourceRecord);
        String type = operation.toString().toLowerCase();

        if ("create".equals(type)) {
            type = "insert";
        }

        Struct value = (Struct) sourceRecord.value();

        //TODO 3 获取before数据
        Struct before = value.getStruct("before");
        //存放before数据
        JSONObject beforeJOSN = new JSONObject();

        if (before != null) {
            Schema beforeSchema = before.schema();
            List<Field> beforeFields = beforeSchema.fields();
            for (Field field : beforeFields) {
                beforeJOSN.put(field.name(), before.get(field));
            }

        }

        //TODO 4 获取after数据
        Struct after = value.getStruct("after");
        //存放after数据
        JSONObject afterJSON = new JSONObject();

        if (after != null) {
            Schema afterSchema = after.schema();
            List<Field> afterFields = afterSchema.fields();

            for (Field field : afterFields) {
                    afterJSON.put(field.name(), after.get(field));
                }
        }

        //TODO 5 获取的数据写入resultObject对象
        resultObject.put("dataName", dataName);
        resultObject.put("tableName", tableName);
        resultObject.put("before", beforeJOSN);
        resultObject.put("after", afterJSON);
        resultObject.put("type", type);

        //TODO 6 数据输出
        collector.collect(resultObject.toJSONString());

    }

    @Override
    public TypeInformation<String> getProducedType() {
        return BasicTypeInfo.STRING_TYPE_INFO;
    }
}
